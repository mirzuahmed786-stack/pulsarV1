use dashmap::DashMap;
use std::{
    sync::{
        atomic::{AtomicI64, AtomicU64, Ordering},
        Arc,
    },
    time::Duration,
};

use crate::{
    app::{
        chrono_ms_now, CACHE_JANITOR_INTERVAL_S, MAX_PROXY_CACHE_ENTRIES, MAX_RPC_CACHE_ENTRIES,
        RATE_LIMIT_WINDOW_MS,
    },
    cloud_store::StoreKind,
    config::Config,
    infra::{
        cache::{CacheStore, InMemoryCacheStore},
        rate_limit_store::{InMemoryRateLimitStore, RateLimitStore},
        redis_store::build_redis_backends,
    },
};
use earth_solana_tooling::{
    service as solana_service,
    types::{
        InitParams as SolInitParams, InitResult as SolInitResult, MintParams as SolMintParams,
        MintResult as SolMintResult, SolanaAmmConfig as SolanaAmmToolingConfig,
        SwapTxParams as SolSwapTxParams, SwapTxResult as SolSwapTxResult,
    },
};
use serde_json::Value;

#[derive(Clone)]
pub(crate) struct SwapQuoteSnapshot {
    pub(crate) expires_at_ms: i64,
    pub(crate) request: Value,
    pub(crate) response: Value,
    pub(crate) provider_payload: Value,
    pub(crate) quote_hash: String,
}

pub(crate) trait SolanaAmmApi: Send + Sync {
    fn load_config(&self, config_path: &std::path::Path) -> anyhow::Result<SolanaAmmToolingConfig>;
    fn build_swap_tx(
        &self,
        config: &SolanaAmmToolingConfig,
        params: SolSwapTxParams,
    ) -> anyhow::Result<SolSwapTxResult>;
    fn mint_test_tokens(
        &self,
        config: &SolanaAmmToolingConfig,
        keypair_path: &str,
        params: SolMintParams,
    ) -> anyhow::Result<SolMintResult>;
    fn init_amm(
        &self,
        config_path: &std::path::Path,
        params: SolInitParams,
    ) -> anyhow::Result<SolInitResult>;
}

pub(crate) struct RealSolanaAmmApi;

impl SolanaAmmApi for RealSolanaAmmApi {
    fn load_config(&self, config_path: &std::path::Path) -> anyhow::Result<SolanaAmmToolingConfig> {
        solana_service::load_config(config_path)
    }

    fn build_swap_tx(
        &self,
        config: &SolanaAmmToolingConfig,
        params: SolSwapTxParams,
    ) -> anyhow::Result<SolSwapTxResult> {
        solana_service::build_swap_tx(config, params)
    }

    fn mint_test_tokens(
        &self,
        config: &SolanaAmmToolingConfig,
        keypair_path: &str,
        params: SolMintParams,
    ) -> anyhow::Result<SolMintResult> {
        solana_service::mint_test_tokens(config, keypair_path, params)
    }

    fn init_amm(
        &self,
        config_path: &std::path::Path,
        params: SolInitParams,
    ) -> anyhow::Result<SolInitResult> {
        solana_service::init_amm(config_path, params)
    }
}

#[derive(Clone)]
pub(crate) struct AppState {
    pub(crate) cfg: Config,
    pub(crate) http: reqwest::Client,
    pub(crate) session_key: [u8; 32],
    pub(crate) cloud_kek_key: [u8; 32],
    pub(crate) solana_api: Arc<dyn SolanaAmmApi>,
    pub(crate) store: StoreKind,
    pub(crate) proxy_cache: Arc<dyn CacheStore>,
    pub(crate) rpc_cache: Arc<dyn CacheStore>,
    pub(crate) proxy_inflight: Arc<DashMap<String, Arc<tokio::sync::Mutex<()>>>>,
    pub(crate) rpc_inflight: Arc<DashMap<String, Arc<tokio::sync::Mutex<()>>>>,
    pub(crate) rpc_host_cooldown_until_ms: Arc<DashMap<String, i64>>,
    pub(crate) rpc_host_penalty: Arc<DashMap<String, u32>>,
    pub(crate) rpc_host_semaphores: Arc<DashMap<String, Arc<tokio::sync::Semaphore>>>,
    pub(crate) rate_limits: Arc<dyn RateLimitStore>,
    pub(crate) infra_backend_mode: String,
    pub(crate) redis_client: Option<redis::Client>,
    pub(crate) infra_metrics: Arc<InfraMetrics>,
    pub(crate) started_at: std::time::Instant,
    pub(crate) api_metrics: Arc<ApiMetrics>,
    pub(crate) solana_metrics: Arc<SolanaMetrics>,
    pub(crate) solana_runtime: Arc<tokio::sync::RwLock<SolanaRuntimeState>>,
    pub(crate) swap_quotes: Arc<DashMap<String, SwapQuoteSnapshot>>,
}

impl AppState {
    pub(crate) async fn new_with_solana(
        cfg: Config,
        solana_api: Arc<dyn SolanaAmmApi>,
    ) -> anyhow::Result<Self> {
        let http = reqwest::Client::builder()
            .timeout(Duration::from_secs(15))
            .user_agent("earth-backend")
            .build()?;
        let session_key = crate::auth::session::derive_session_key(&cfg.session_jwt_secret);
        let cloud_kek_key = crate::auth::kek::derive_cloud_kek_key(&cfg.cloud_kek_secret);
        let store = StoreKind::from_env()?;
        store.init().await?;
        let proxy_inflight = Arc::new(DashMap::new());
        let rpc_inflight = Arc::new(DashMap::new());
        let rpc_host_cooldown_until_ms = Arc::new(DashMap::new());
        let rpc_host_penalty = Arc::new(DashMap::new());
        let rpc_host_semaphores = Arc::new(DashMap::new());
        let (proxy_cache, rpc_cache, rate_limits, infra_backend_mode, redis_client) = if cfg
            .infra_store_backend
            .trim()
            .eq_ignore_ascii_case("redis")
        {
            match build_redis_backends(&cfg).await {
                Ok(redis) => (
                    redis.proxy_cache,
                    redis.rpc_cache,
                    redis.rate_limits,
                    "redis".to_string(),
                    Some(redis.client),
                ),
                Err(err) => {
                    if cfg.infra_redis_required {
                        return Err(anyhow::anyhow!(
                                "Redis infra backend initialization failed and INFRA_REDIS_REQUIRED=1: {err}"
                            ));
                    }
                    tracing::warn!(error = %err, "redis backend unavailable, falling back to in-memory infra store");
                    (
                        Arc::new(InMemoryCacheStore::default()) as Arc<dyn CacheStore>,
                        Arc::new(InMemoryCacheStore::default()) as Arc<dyn CacheStore>,
                        Arc::new(InMemoryRateLimitStore::default()) as Arc<dyn RateLimitStore>,
                        "memory-fallback".to_string(),
                        None,
                    )
                }
            }
        } else {
            (
                Arc::new(InMemoryCacheStore::default()) as Arc<dyn CacheStore>,
                Arc::new(InMemoryCacheStore::default()) as Arc<dyn CacheStore>,
                Arc::new(InMemoryRateLimitStore::default()) as Arc<dyn RateLimitStore>,
                "memory".to_string(),
                None,
            )
        };
        let api_metrics = Arc::new(ApiMetrics::default());
        let infra_metrics = Arc::new(InfraMetrics::default());
        spawn_cache_janitor(
            proxy_cache.clone(),
            rpc_cache.clone(),
            proxy_inflight.clone(),
            rpc_inflight.clone(),
            rpc_host_cooldown_until_ms.clone(),
            rpc_host_penalty.clone(),
            rate_limits.clone(),
        );
        Ok(Self {
            cfg,
            http,
            session_key,
            cloud_kek_key,
            solana_api,
            store,
            proxy_cache,
            rpc_cache,
            proxy_inflight,
            rpc_inflight,
            rpc_host_cooldown_until_ms,
            rpc_host_penalty,
            rpc_host_semaphores,
            rate_limits,
            infra_backend_mode,
            redis_client,
            infra_metrics,
            started_at: std::time::Instant::now(),
            api_metrics,
            solana_metrics: Arc::new(SolanaMetrics::default()),
            solana_runtime: Arc::new(tokio::sync::RwLock::new(SolanaRuntimeState::default())),
            swap_quotes: Arc::new(DashMap::new()),
        })
    }
}

#[derive(Default)]
pub(crate) struct InfraMetrics {
    pub(crate) redis_ping_ok: AtomicU64,
    pub(crate) redis_ping_err: AtomicU64,
    pub(crate) redis_ping_latency_sum_ms: AtomicU64,
    pub(crate) redis_ping_latency_samples: AtomicU64,
    pub(crate) redis_last_ping_ok_ms: AtomicI64,
}

#[derive(Default)]
pub(crate) struct ApiMetrics {
    pub(crate) requests_total: AtomicU64,
    pub(crate) batch_requests: AtomicU64,
    pub(crate) proxy_requests: AtomicU64,
    pub(crate) proxy_cache_hit: AtomicU64,
    pub(crate) proxy_cache_miss: AtomicU64,
    pub(crate) proxy_error_cache_hit: AtomicU64,
    pub(crate) rpc_requests: AtomicU64,
    pub(crate) rpc_cache_hit: AtomicU64,
    pub(crate) rpc_cache_miss: AtomicU64,
    pub(crate) rpc_cooldown_hits: AtomicU64,
    pub(crate) rpc_method_blocked: AtomicU64,
    pub(crate) rpc_payload_invalid: AtomicU64,
    pub(crate) rpc_host_blocked: AtomicU64,
    pub(crate) rpc_method_rate_limited_single: AtomicU64,
    pub(crate) rpc_method_rate_limited_batch: AtomicU64,
    pub(crate) reject_invalid_query: AtomicU64,
    pub(crate) reject_invalid_json_body: AtomicU64,
    pub(crate) reject_validation_failed: AtomicU64,
    pub(crate) reject_proxy_url_blocked: AtomicU64,
    pub(crate) reject_blocked_rpc_url: AtomicU64,
    pub(crate) reject_method_not_allowed: AtomicU64,
    pub(crate) reject_rpc_payload_too_large: AtomicU64,
    pub(crate) reject_invalid_jsonrpc_payload: AtomicU64,
    pub(crate) reject_empty_rpc_batch: AtomicU64,
    pub(crate) reject_rpc_batch_too_large: AtomicU64,
    pub(crate) reject_invalid_jsonrpc_request: AtomicU64,
    pub(crate) reject_rpc_method_rate_limited_single: AtomicU64,
    pub(crate) reject_rpc_method_rate_limited_batch: AtomicU64,
    pub(crate) reject_cloud_blob_read_rate_limited: AtomicU64,
    pub(crate) reject_cloud_blob_write_rate_limited: AtomicU64,
    pub(crate) reject_cloud_blob_validation_failed: AtomicU64,
    pub(crate) cloud_blob_read_rate_limited: AtomicU64,
    pub(crate) cloud_blob_write_rate_limited: AtomicU64,
    pub(crate) route_latency_histograms: DashMap<String, RouteLatencyHistogram>,
    pub(crate) upstream_error_total: AtomicU64,
    pub(crate) upstream_error_by_source: DashMap<String, AtomicU64>,
}

#[derive(Default)]
pub(crate) struct RouteLatencyHistogram {
    pub(crate) bucket_le_25_ms: AtomicU64,
    pub(crate) bucket_le_50_ms: AtomicU64,
    pub(crate) bucket_le_100_ms: AtomicU64,
    pub(crate) bucket_le_250_ms: AtomicU64,
    pub(crate) bucket_le_500_ms: AtomicU64,
    pub(crate) bucket_le_1000_ms: AtomicU64,
    pub(crate) bucket_le_2500_ms: AtomicU64,
    pub(crate) bucket_le_5000_ms: AtomicU64,
    pub(crate) bucket_inf: AtomicU64,
    pub(crate) sum_ms: AtomicU64,
    pub(crate) count: AtomicU64,
}

impl ApiMetrics {
    pub(crate) fn observe_route_latency(&self, route: &str, elapsed_ms: u64) {
        let entry = self
            .route_latency_histograms
            .entry(route.to_string())
            .or_default();
        entry.observe(elapsed_ms);
    }

    pub(crate) fn record_upstream_error(&self, source: &str) {
        self.upstream_error_total.fetch_add(1, Ordering::Relaxed);
        let counter = self
            .upstream_error_by_source
            .entry(source.to_string())
            .or_insert_with(|| AtomicU64::new(0));
        counter.fetch_add(1, Ordering::Relaxed);
    }
}

impl RouteLatencyHistogram {
    fn observe(&self, elapsed_ms: u64) {
        self.count.fetch_add(1, Ordering::Relaxed);
        self.sum_ms.fetch_add(elapsed_ms, Ordering::Relaxed);
        if elapsed_ms <= 25 {
            self.bucket_le_25_ms.fetch_add(1, Ordering::Relaxed);
        }
        if elapsed_ms <= 50 {
            self.bucket_le_50_ms.fetch_add(1, Ordering::Relaxed);
        }
        if elapsed_ms <= 100 {
            self.bucket_le_100_ms.fetch_add(1, Ordering::Relaxed);
        }
        if elapsed_ms <= 250 {
            self.bucket_le_250_ms.fetch_add(1, Ordering::Relaxed);
        }
        if elapsed_ms <= 500 {
            self.bucket_le_500_ms.fetch_add(1, Ordering::Relaxed);
        }
        if elapsed_ms <= 1000 {
            self.bucket_le_1000_ms.fetch_add(1, Ordering::Relaxed);
        }
        if elapsed_ms <= 2500 {
            self.bucket_le_2500_ms.fetch_add(1, Ordering::Relaxed);
        }
        if elapsed_ms <= 5000 {
            self.bucket_le_5000_ms.fetch_add(1, Ordering::Relaxed);
        }
        self.bucket_inf.fetch_add(1, Ordering::Relaxed);
    }
}

#[derive(Default)]
pub(crate) struct SolanaMetrics {
    pub(crate) swap_tx_ok: AtomicU64,
    pub(crate) swap_tx_err: AtomicU64,
    pub(crate) mint_ok: AtomicU64,
    pub(crate) mint_err: AtomicU64,
    pub(crate) init_ok: AtomicU64,
    pub(crate) init_err: AtomicU64,
}

#[derive(Default, Clone, serde::Serialize)]
pub(crate) struct SolanaRuntimeState {
    pub(crate) last_init_success: Option<bool>,
    pub(crate) last_init_at_ms: Option<i64>,
    pub(crate) last_init_error: Option<String>,
}

fn spawn_cache_janitor(
    proxy_cache: Arc<dyn CacheStore>,
    rpc_cache: Arc<dyn CacheStore>,
    proxy_inflight: Arc<DashMap<String, Arc<tokio::sync::Mutex<()>>>>,
    rpc_inflight: Arc<DashMap<String, Arc<tokio::sync::Mutex<()>>>>,
    rpc_host_cooldown_until_ms: Arc<DashMap<String, i64>>,
    rpc_host_penalty: Arc<DashMap<String, u32>>,
    rate_limits: Arc<dyn RateLimitStore>,
) {
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_secs(CACHE_JANITOR_INTERVAL_S));
        interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Skip);
        loop {
            interval.tick().await;
            let now = chrono_ms_now();

            let proxy_expired = proxy_cache.remove_expired_keys(now).await;
            for key in proxy_expired {
                proxy_inflight.remove(&key);
            }

            let rpc_expired = rpc_cache.remove_expired_keys(now).await;
            for key in rpc_expired {
                rpc_inflight.remove(&key);
            }

            rate_limits
                .remove_stale(now, RATE_LIMIT_WINDOW_MS * 2)
                .await;

            let expired_cooldowns: Vec<String> = rpc_host_cooldown_until_ms
                .iter()
                .filter(|entry| *entry.value() <= now)
                .map(|entry| entry.key().clone())
                .collect();
            for key in expired_cooldowns {
                rpc_host_cooldown_until_ms.remove(&key);
                let should_remove_penalty =
                    rpc_host_penalty.get(&key).map(|p| *p <= 1).unwrap_or(false);
                if should_remove_penalty {
                    rpc_host_penalty.remove(&key);
                }
            }

            trim_cache_if_needed(&*proxy_cache, &proxy_inflight, MAX_PROXY_CACHE_ENTRIES).await;
            trim_cache_if_needed(&*rpc_cache, &rpc_inflight, MAX_RPC_CACHE_ENTRIES).await;
        }
    });
}

async fn trim_cache_if_needed(
    cache: &dyn CacheStore,
    inflight: &DashMap<String, Arc<tokio::sync::Mutex<()>>>,
    max_entries: usize,
) {
    for key in cache.trim_to(max_entries).await {
        inflight.remove(&key);
    }
}
