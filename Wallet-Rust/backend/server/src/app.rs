#[cfg(test)]
use earth_solana_tooling::types::{
    InitParams as SolInitParams, InitResult as SolInitResult, MintParams as SolMintParams,
    MintResult as SolMintResult, SolanaAmmConfig as SolanaAmmToolingConfig,
    SwapTxParams as SolSwapTxParams, SwapTxResult as SolSwapTxResult,
};

pub use crate::infra::app_builder::build_app;
pub use crate::infra::constants::*;

pub(crate) use crate::infra::admin::{
    backend_state_file, get_private_key, is_admin_allowed, repo_root_from_server_crate,
};
pub(crate) use crate::infra::contracts::{
    api_error_response, inc_reject_reason_counter, incoming_request_id, invalid_json_response,
    parse_strict_query, parse_u64_str, sanitize_deployed_contracts, validate_evm_quote_query,
    validate_jupiter_quote_query, validation_error_response, BatchEnvelope, CacheEntry,
    CacheOutcome, EvmQuoteQuery, JupiterQuoteQuery, ProxyFetchError, ProxyUrlQuery,
};
pub(crate) use crate::infra::middleware::{
    consume_rate_limit_bucket, rate_limit_client_key, rate_limit_middleware_impl,
    request_context_middleware_impl,
};
pub(crate) use crate::infra::proxy::{
    cache_key, cached_json_fetch, proxy_timeout_for_type, should_expose_proxy_error_details,
    status_from_proxy_error, validate_external_base_url, validate_proxy_url,
};
pub(crate) use crate::infra::rpc::{
    extract_chain_id_hint, rpc_call_with_cache, rpc_error_envelope, validate_rpc_payload,
    validate_rpc_url,
};
pub(crate) use crate::infra::state::{ApiMetrics, AppState};
pub(crate) use crate::infra::utils::{
    chrono_ms_now, random_base64url, require_csrf_for_state_change,
};

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::Config;
    use crate::infra::middleware::should_skip_rate_limit;
    use crate::infra::observability::health;
    use crate::infra::rpc::host_matches_domain_or_subdomain;
    use crate::infra::state::SolanaAmmApi;
    use crate::routes::solana_amm::{solana_amm_init, solana_amm_mint, solana_amm_swap_tx};
    use axum::{
        body::{to_bytes, Body},
        http::{HeaderMap as AxumHeaderMap, Method, Request},
        routing::get,
        routing::post,
        Router,
    };
    use serde_json::json;
    use std::{path::PathBuf, sync::Arc, sync::OnceLock};
    use tokio::sync::{Mutex, MutexGuard};
    use tower::util::ServiceExt;

    mod phase2_regression_matrix;

    static TEST_LOCK: OnceLock<Mutex<()>> = OnceLock::new();

    async fn global_test_lock() -> MutexGuard<'static, ()> {
        TEST_LOCK.get_or_init(|| Mutex::new(())).lock().await
    }

    struct EnvGuard {
        key: String,
        prev: Option<String>,
    }

    impl EnvGuard {
        fn set<K: Into<String>, V: Into<String>>(key: K, value: V) -> Self {
            let key = key.into();
            let prev = std::env::var(&key).ok();
            std::env::set_var(&key, value.into());
            Self { key, prev }
        }

        fn remove<K: Into<String>>(key: K) -> Self {
            let key = key.into();
            let prev = std::env::var(&key).ok();
            std::env::remove_var(&key);
            Self { key, prev }
        }
    }

    impl Drop for EnvGuard {
        fn drop(&mut self) {
            if let Some(v) = &self.prev {
                std::env::set_var(&self.key, v);
            } else {
                std::env::remove_var(&self.key);
            }
        }
    }

    struct FileGuard {
        path: PathBuf,
        prev: Option<Vec<u8>>,
    }

    impl FileGuard {
        fn write(path: PathBuf, content: &[u8]) -> Self {
            let prev = std::fs::read(&path).ok();
            if let Some(parent) = path.parent() {
                std::fs::create_dir_all(parent).ok();
            }
            std::fs::write(&path, content).expect("write test file");
            Self { path, prev }
        }
    }

    impl Drop for FileGuard {
        fn drop(&mut self) {
            match &self.prev {
                Some(bytes) => {
                    let _ = std::fs::write(&self.path, bytes);
                }
                None => {
                    let _ = std::fs::remove_file(&self.path);
                }
            }
        }
    }

    fn solana_cfg_path() -> PathBuf {
        backend_state_file("solana_amm_testnet.json")
    }

    fn test_config() -> Config {
        Config {
            port: 0,
            node_env: "test".to_string(),
            cors_origins: "http://localhost:5173".to_string(),
            cookie_samesite: "Lax".to_string(),
            session_jwt_secret: "test_session_secret_1234567890".to_string(),
            cloud_kek_secret: "test_cloud_kek_secret_1234567890".to_string(),
            google_oauth_client_id: String::new(),
            google_oauth_client_secret: String::new(),
            google_oauth_redirect_uri: "postmessage".to_string(),
            apple_client_id: String::new(),
            apple_team_id: String::new(),
            apple_key_id: String::new(),
            apple_private_key_pem: String::new(),
            apple_oauth_redirect_uri: String::new(),
            external_api_allowlist: String::new(),
            proxy_host_allowlist: String::new(),
            external_host_allowlist: String::new(),
            rpc_url_allowlist: String::new(),
            zerox_api_key: String::new(),
            fallback_aggregator_url: String::new(),
            fallback_aggregator_provider: "zeroex_compat".to_string(),
            fallback_aggregator_api_key: String::new(),
            fallback_aggregator_api_header: "x-api-key".to_string(),
            fallback_aggregator_quote_path: "/swap/allowance-holder/quote".to_string(),
            fallback_aggregator_price_path: "/swap/allowance-holder/price".to_string(),
            fallback_aggregator_version: String::new(),
            jupiter_api_url: "https://api.jup.ag/swap/v1".to_string(),
            jupiter_api_key: String::new(),
            admin_token: "test-admin-token".to_string(),
            allow_insecure_admin_dev: false,
            disable_admin_scripts: false,
            solana_admin_strict: false,
            enable_server_kek: false,
            rpc_max_request_body_bytes: 256 * 1024,
            rpc_max_response_body_bytes: 2 * 1024 * 1024,
            rpc_max_batch_items: 25,
            rpc_per_host_max_concurrency: 8,
            rpc_method_rate_limit_max_requests: 60,
            rpc_method_rate_limit_window_ms: 60_000,
            infra_store_backend: "memory".to_string(),
            infra_redis_url: "redis://127.0.0.1:6379/".to_string(),
            infra_redis_key_prefix: "elementa-test".to_string(),
            infra_redis_required: false,
            infra_redis_connect_timeout_ms: 50,
            infra_redis_command_timeout_ms: 500,
        }
    }

    #[derive(Clone)]
    struct MockSolanaAmmApi {
        cfg: SolanaAmmToolingConfig,
        swap_out: SolSwapTxResult,
        mint_out: SolMintResult,
        init_out: SolInitResult,
        load_err: Option<String>,
        swap_err: Option<String>,
        mint_err: Option<String>,
        init_err: Option<String>,
    }

    impl SolanaAmmApi for MockSolanaAmmApi {
        fn load_config(
            &self,
            _config_path: &std::path::Path,
        ) -> anyhow::Result<SolanaAmmToolingConfig> {
            if let Some(msg) = &self.load_err {
                return Err(anyhow::anyhow!(msg.clone()));
            }
            Ok(self.cfg.clone())
        }

        fn build_swap_tx(
            &self,
            _config: &SolanaAmmToolingConfig,
            _params: SolSwapTxParams,
        ) -> anyhow::Result<SolSwapTxResult> {
            if let Some(msg) = &self.swap_err {
                return Err(anyhow::anyhow!(msg.clone()));
            }
            Ok(self.swap_out.clone())
        }

        fn mint_test_tokens(
            &self,
            _config: &SolanaAmmToolingConfig,
            _keypair_path: &str,
            _params: SolMintParams,
        ) -> anyhow::Result<SolMintResult> {
            if let Some(msg) = &self.mint_err {
                return Err(anyhow::anyhow!(msg.clone()));
            }
            Ok(self.mint_out.clone())
        }

        fn init_amm(
            &self,
            _config_path: &std::path::Path,
            _params: SolInitParams,
        ) -> anyhow::Result<SolInitResult> {
            if let Some(msg) = &self.init_err {
                return Err(anyhow::anyhow!(msg.clone()));
            }
            Ok(self.init_out.clone())
        }
    }

    fn sample_solana_config() -> SolanaAmmToolingConfig {
        SolanaAmmToolingConfig {
            program_id: "DHnQfvfUy7Yt92BxZxpj1G8UMWXyKyGGYHvUn2Lrb1xK".to_string(),
            pool: "Bv2iqnDkvcrRzXJQJRQtg7EDe1TR8tL1X7GrRzyE2C6G".to_string(),
            fee_bps: 30,
            token_a: earth_solana_tooling::types::TokenConfig {
                mint: "FGahtsaiERiPJ8KrSejkt2xdwCzpJ6BEEKwLoiByGgun".to_string(),
                symbol: "TSTA".to_string(),
                decimals: 6,
            },
            token_b: earth_solana_tooling::types::TokenConfig {
                mint: "BRncLKqYfdgxtr2V4L7u2GWKUnPa7Hhjn6Y5pGsgj7x5".to_string(),
                symbol: "TSTB".to_string(),
                decimals: 6,
            },
            vault_a: "3ZaLvkkLXk8iAoKfyAuWAoB4SFuBbE4G1py3sdhQcdvA".to_string(),
            vault_b: "GNQ2gjidAqADxAUkSi33kFwtwGUiHfg1NCs6Uyn5KSnm".to_string(),
            lp_mint: "GewiMu62Mz15BNhvonPVHhc6yUGYXKSyjLNyunzypLfS".to_string(),
            rpc_url: "https://api.testnet.solana.com".to_string(),
        }
    }

    fn sample_mock_api() -> MockSolanaAmmApi {
        let cfg = sample_solana_config();
        MockSolanaAmmApi {
            cfg: cfg.clone(),
            swap_out: SolSwapTxResult {
                swap_transaction: "ZmFrZS10eA==".to_string(),
                last_valid_block_height: 123456,
            },
            mint_out: SolMintResult {
                mint: "FGahtsaiERiPJ8KrSejkt2xdwCzpJ6BEEKwLoiByGgun".to_string(),
                destination: "11111111111111111111111111111111".to_string(),
                amount: "1000".to_string(),
                tx_signature: "mock-mint-signature".to_string(),
            },
            init_out: SolInitResult {
                config: cfg,
                tx_signatures: vec!["sig-1".to_string(), "sig-2".to_string()],
            },
            load_err: None,
            swap_err: None,
            mint_err: None,
            init_err: None,
        }
    }

    async fn build_mock_solana_app(api: Arc<dyn SolanaAmmApi>) -> Router {
        let st = AppState::new_with_solana(test_config(), api)
            .await
            .expect("build test state");
        Router::new()
            .route("/api/solana/amm/swap-tx", post(solana_amm_swap_tx))
            .route("/api/solana/amm/mint", post(solana_amm_mint))
            .route("/api/solana/amm/init", post(solana_amm_init))
            .route("/health", get(health))
            .with_state(st)
    }

    async fn json_request(
        app: Router,
        method: Method,
        path: &str,
        body: serde_json::Value,
        admin_token: Option<&str>,
    ) -> (axum::http::StatusCode, serde_json::Value) {
        let mut extra = Vec::new();
        if let Some(t) = admin_token {
            extra.push(("x-admin-token", t.to_string()));
        }
        let (status, _headers, json) =
            json_request_with_headers(app, method, path, Some(body), &extra).await;
        (status, json)
    }

    async fn json_request_with_headers(
        app: Router,
        method: Method,
        path: &str,
        body: Option<serde_json::Value>,
        extra_headers: &[(&str, String)],
    ) -> (axum::http::StatusCode, AxumHeaderMap, serde_json::Value) {
        let mut rb = Request::builder().method(method).uri(path);
        if body.is_some() {
            rb = rb.header("content-type", "application/json");
        }
        for (k, v) in extra_headers {
            rb = rb.header(*k, v);
        }
        let req = rb
            .body(match body {
                Some(v) => Body::from(v.to_string()),
                None => Body::empty(),
            })
            .expect("build request");
        let resp = app.oneshot(req).await.expect("router response");
        let status = resp.status();
        let headers = resp.headers().clone();
        let bytes = to_bytes(resp.into_body(), usize::MAX)
            .await
            .expect("read response body");
        let json = serde_json::from_slice::<serde_json::Value>(&bytes).unwrap_or(json!({}));
        (status, headers, json)
    }

    async fn raw_request(
        app: Router,
        method: Method,
        path: &str,
        raw_body: &str,
        admin_token: Option<&str>,
    ) -> (axum::http::StatusCode, serde_json::Value) {
        let mut rb = Request::builder()
            .method(method)
            .uri(path)
            .header("content-type", "application/json");
        if let Some(t) = admin_token {
            rb = rb.header("x-admin-token", t);
        }
        let req = rb
            .body(Body::from(raw_body.to_string()))
            .expect("build request");
        let resp = app.oneshot(req).await.expect("router response");
        let status = resp.status();
        let bytes = to_bytes(resp.into_body(), usize::MAX)
            .await
            .expect("read response body");
        let json = serde_json::from_slice::<serde_json::Value>(&bytes).unwrap_or(json!({}));
        (status, json)
    }

    async fn text_request(
        app: Router,
        method: Method,
        path: &str,
    ) -> (axum::http::StatusCode, String) {
        let req = Request::builder()
            .method(method)
            .uri(path)
            .body(Body::empty())
            .expect("build request");
        let resp = app.oneshot(req).await.expect("router response");
        let status = resp.status();
        let bytes = to_bytes(resp.into_body(), usize::MAX)
            .await
            .expect("read response body");
        (status, String::from_utf8_lossy(&bytes).to_string())
    }

    fn set_cookie_headers(headers: &AxumHeaderMap) -> Vec<String> {
        headers
            .get_all(axum::http::header::SET_COOKIE)
            .iter()
            .filter_map(|v| v.to_str().ok().map(|s| s.to_string()))
            .collect()
    }

    fn find_set_cookie(headers: &AxumHeaderMap, cookie_name: &str) -> Option<String> {
        set_cookie_headers(headers)
            .into_iter()
            .find(|line| line.starts_with(&format!("{cookie_name}=")))
    }

    fn parse_set_cookie_value(set_cookie_line: &str) -> Option<String> {
        let first = set_cookie_line.split(';').next()?;
        let (_k, v) = first.split_once('=')?;
        Some(
            urlencoding::decode(v)
                .map(|d| d.into_owned())
                .unwrap_or_else(|_| v.to_string()),
        )
    }

    fn make_session_cookie(cfg: &Config, provider: &str, sub: &str) -> String {
        let mut headers = AxumHeaderMap::new();
        let key = crate::auth::session::derive_session_key(&cfg.session_jwt_secret);
        crate::auth::session::issue_session_cookie(
            &mut headers,
            &key,
            provider,
            sub,
            3600,
            &cfg.cookie_samesite,
            cfg.cookie_secure(),
        )
        .expect("issue session cookie");
        let line = find_set_cookie(&headers, crate::auth::session::SESSION_COOKIE_NAME)
            .expect("session cookie");
        let value = parse_set_cookie_value(&line).expect("session cookie value");
        format!("{}={}", crate::auth::session::SESSION_COOKIE_NAME, value)
    }

    async fn make_wallet_put_auth_headers(
        app: Router,
        cfg: &Config,
    ) -> Vec<(&'static str, String)> {
        let session_cookie = make_session_cookie(cfg, "google", "wallet-user-1");
        let (_status_prepare, prepare_headers, _prepare_body) =
            json_request_with_headers(app, Method::GET, "/api/auth/google/prepare", None, &[])
                .await;
        let csrf_line = find_set_cookie(&prepare_headers, "ew_csrf_v1").expect("csrf cookie");
        let csrf_token = parse_set_cookie_value(&csrf_line).expect("csrf token");
        vec![
            (
                "cookie",
                format!("{session_cookie}; ew_csrf_v1={csrf_token}"),
            ),
            ("x-csrf-token", csrf_token),
        ]
    }

    fn valid_wallet_blob_payload() -> serde_json::Value {
        json!({
            "encryptedSeedBlob": "AAECAwQFBgcICQoLDA0ODxAREhM=",
            "walletId": "wallet-rl-test",
            "version": 1,
            "createdAt": 1710000000000_i64,
            "secretType": "mnemonic",
            "hdIndex": 0,
            "kekKdf": serde_json::Value::Null
        })
    }

    #[test]
    fn rpc_payload_validation_rejects_disallowed_method() {
        let payload = json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "personal_sign",
            "params": []
        });
        let err = validate_rpc_payload(&payload).expect_err("must reject");
        assert!(err.contains("RPC method not allowed"));
    }

    #[test]
    fn rpc_payload_validation_accepts_allowed_method() {
        let payload = json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "eth_sendRawTransaction",
            "params": ["0xdeadbeef"]
        });
        assert!(validate_rpc_payload(&payload).is_ok());
    }

    #[test]
    fn domain_match_requires_dot_boundary() {
        assert!(host_matches_domain_or_subdomain("rpc.ankr.com", "ankr.com"));
        assert!(!host_matches_domain_or_subdomain(
            "evilankr.com",
            "ankr.com"
        ));
        assert!(!host_matches_domain_or_subdomain(
            "ankr.com.evil.com",
            "ankr.com"
        ));
    }

    #[test]
    fn rate_limit_skip_paths_match_contract() {
        assert!(should_skip_rate_limit("/health"));
        assert!(should_skip_rate_limit("/metrics"));
        assert!(should_skip_rate_limit("/api/evm/status"));
        assert!(should_skip_rate_limit("/api/jupiter/status"));
        assert!(!should_skip_rate_limit("/api/proxy/prices"));
    }

    #[test]
    fn proxy_error_status_mapping_uses_upstream_http_code() {
        assert_eq!(
            status_from_proxy_error("HTTP 429"),
            axum::http::StatusCode::TOO_MANY_REQUESTS
        );
        assert_eq!(
            status_from_proxy_error("HTTP 404"),
            axum::http::StatusCode::NOT_FOUND
        );
        assert_eq!(
            status_from_proxy_error("upstream timeout"),
            axum::http::StatusCode::BAD_GATEWAY
        );
    }

    #[test]
    fn deployed_contract_sanitizer_keeps_only_known_valid_addresses() {
        let input = json!({
            "ElementaToken": "0x1111111111111111111111111111111111111111",
            "UniswapV2Router": "0x2222222222222222222222222222222222222222",
            "tokens": { "USDC": "0x3333333333333333333333333333333333333333" },
            "MockUSD": "not-an-address",
            "junk": "0x4444444444444444444444444444444444444444"
        });

        let out = sanitize_deployed_contracts(&input);
        assert_eq!(
            out.get("ElementaToken").and_then(|v| v.as_str()),
            Some("0x1111111111111111111111111111111111111111")
        );
        assert_eq!(
            out.get("UniswapV2Router").and_then(|v| v.as_str()),
            Some("0x2222222222222222222222222222222222222222")
        );
        assert!(out.get("tokens").is_none());
        assert!(out.get("junk").is_none());
        assert!(out.get("MockUSD").is_none());
    }

    #[tokio::test]
    async fn strict_solana_admin_requires_envs_at_startup() {
        let _l = global_test_lock().await;
        let _strict = EnvGuard::set("SOLANA_AMM_ADMIN_STRICT", "1");
        let _kp = EnvGuard::remove("SOLANA_DEPLOYER_KEYPAIR");
        let _pid = EnvGuard::remove("SOLANA_AMM_PROGRAM_ID");
        let err = Config::from_env().expect_err("must fail");
        assert!(err
            .to_string()
            .contains("SOLANA_DEPLOYER_KEYPAIR and SOLANA_AMM_PROGRAM_ID"));
    }

    #[tokio::test]
    async fn swap_tx_missing_fields_contract_error() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/swap-tx",
            json!({}),
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Missing amountIn, minOut, direction, or userPublicKey")
        );
    }

    #[tokio::test]
    async fn swap_tx_not_initialized_when_config_unreadable() {
        let _l = global_test_lock().await;
        let _cfg = FileGuard::write(solana_cfg_path(), b"{");
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/swap-tx",
            json!({
                "amountIn": "1000",
                "minOut": "900",
                "direction": "AtoB",
                "userPublicKey": "11111111111111111111111111111111"
            }),
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::INTERNAL_SERVER_ERROR);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Solana AMM not initialized")
        );
    }

    #[tokio::test]
    async fn mint_missing_fields_contract_error() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/mint",
            json!({}),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Missing userPublicKey, token, or amount")
        );
    }

    #[tokio::test]
    async fn mint_not_initialized_when_config_unreadable() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let _cfg = FileGuard::write(solana_cfg_path(), b"{");
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/mint",
            json!({
                "userPublicKey": "11111111111111111111111111111111",
                "token": "A",
                "amount": "1000"
            }),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::INTERNAL_SERVER_ERROR);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Solana AMM not initialized")
        );
    }

    #[tokio::test]
    async fn init_missing_required_env_contract_error() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::remove("SOLANA_DEPLOYER_KEYPAIR");
        let _pid = EnvGuard::remove("SOLANA_AMM_PROGRAM_ID");
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/init",
            json!({}),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::INTERNAL_SERVER_ERROR);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Missing SOLANA_DEPLOYER_KEYPAIR or SOLANA_AMM_PROGRAM_ID")
        );
    }

    #[tokio::test]
    async fn mint_forbidden_without_admin_token() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/mint",
            json!({
                "userPublicKey": "11111111111111111111111111111111",
                "token": "A",
                "amount": "1000"
            }),
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::FORBIDDEN);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Forbidden")
        );
    }

    #[tokio::test]
    async fn init_forbidden_without_admin_token() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) =
            json_request(app, Method::POST, "/api/solana/amm/init", json!({}), None).await;
        assert_eq!(status, axum::http::StatusCode::FORBIDDEN);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Forbidden")
        );
    }

    #[tokio::test]
    async fn swap_tx_invalid_direction_contract_error() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/swap-tx",
            json!({
                "amountIn": "1000",
                "minOut": "900",
                "direction": "BAD",
                "userPublicKey": "11111111111111111111111111111111"
            }),
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Invalid direction")
        );
    }

    #[tokio::test]
    async fn swap_tx_zero_amount_contract_error() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/swap-tx",
            json!({
                "amountIn": "0",
                "minOut": "900",
                "direction": "AtoB",
                "userPublicKey": "11111111111111111111111111111111"
            }),
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Invalid amountIn")
        );
    }

    #[tokio::test]
    async fn mint_invalid_token_contract_error() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/mint",
            json!({
                "userPublicKey": "11111111111111111111111111111111",
                "token": "C",
                "amount": "1000"
            }),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Token must be A or B")
        );
    }

    #[tokio::test]
    async fn mint_zero_amount_contract_error() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/mint",
            json!({
                "userPublicKey": "11111111111111111111111111111111",
                "token": "A",
                "amount": "0"
            }),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Invalid amount")
        );
    }

    #[tokio::test]
    async fn swap_tx_malformed_json_returns_400() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _) =
            raw_request(app, Method::POST, "/api/solana/amm/swap-tx", "{", None).await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
    }

    #[tokio::test]
    async fn swap_tx_success_with_mock_api() {
        let _l = global_test_lock().await;
        let mock = sample_mock_api();
        let app = build_mock_solana_app(Arc::new(mock)).await;
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/swap-tx",
            json!({
                "amountIn": "1000",
                "minOut": "900",
                "direction": "AtoB",
                "userPublicKey": "11111111111111111111111111111111"
            }),
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(
            body.get("swapTransaction").and_then(|v| v.as_str()),
            Some("ZmFrZS10eA==")
        );
        assert_eq!(
            body.get("lastValidBlockHeight").and_then(|v| v.as_u64()),
            Some(123456)
        );
    }

    #[tokio::test]
    async fn mint_success_with_mock_api() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let mock = sample_mock_api();
        let app = build_mock_solana_app(Arc::new(mock)).await;
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/mint",
            json!({
                "userPublicKey": "11111111111111111111111111111111",
                "token": "A",
                "amount": "1000"
            }),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(body.get("ok").and_then(|v| v.as_bool()), Some(true));
        assert_eq!(
            body.get("txSignature").and_then(|v| v.as_str()),
            Some("mock-mint-signature")
        );
    }

    #[tokio::test]
    async fn init_success_with_mock_api() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let _pid = EnvGuard::set(
            "SOLANA_AMM_PROGRAM_ID",
            "DHnQfvfUy7Yt92BxZxpj1G8UMWXyKyGGYHvUn2Lrb1xK",
        );
        let mock = sample_mock_api();
        let app = build_mock_solana_app(Arc::new(mock)).await;
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/init",
            json!({}),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(body.get("ok").and_then(|v| v.as_bool()), Some(true));
        assert_eq!(
            body.get("txSignatures")
                .and_then(|v| v.as_array())
                .map(|a| a.len()),
            Some(2)
        );
    }

    #[tokio::test]
    async fn swap_tx_internal_error_mapping_is_stable() {
        let _l = global_test_lock().await;
        let mut mock = sample_mock_api();
        mock.swap_err = Some("rpc timeout details".to_string());
        let app = build_mock_solana_app(Arc::new(mock)).await;
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/swap-tx",
            json!({
                "amountIn": "1000",
                "minOut": "900",
                "direction": "AtoB",
                "userPublicKey": "11111111111111111111111111111111"
            }),
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::INTERNAL_SERVER_ERROR);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Swap tx failed")
        );
        assert!(body.get("details").is_none());
    }

    #[tokio::test]
    async fn mint_internal_error_mapping_is_stable() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let mut mock = sample_mock_api();
        mock.mint_err = Some("rpc timeout details".to_string());
        let app = build_mock_solana_app(Arc::new(mock)).await;
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/mint",
            json!({
                "userPublicKey": "11111111111111111111111111111111",
                "token": "A",
                "amount": "1000"
            }),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::INTERNAL_SERVER_ERROR);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Mint failed")
        );
        assert!(body.get("details").is_none());
    }

    #[tokio::test]
    async fn init_internal_error_mapping_is_stable() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let _pid = EnvGuard::set(
            "SOLANA_AMM_PROGRAM_ID",
            "DHnQfvfUy7Yt92BxZxpj1G8UMWXyKyGGYHvUn2Lrb1xK",
        );
        let mut mock = sample_mock_api();
        mock.init_err = Some("rpc timeout details".to_string());
        let app = build_mock_solana_app(Arc::new(mock)).await;
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/solana/amm/init",
            json!({}),
            Some("test-admin-token"),
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::INTERNAL_SERVER_ERROR);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Solana AMM init failed")
        );
        assert!(body.get("details").is_none());
    }

    #[tokio::test]
    async fn health_includes_solana_metrics_and_last_init() {
        let _l = global_test_lock().await;
        let _kp = EnvGuard::set("SOLANA_DEPLOYER_KEYPAIR", "C:\\tmp\\dummy-id.json");
        let _pid = EnvGuard::set(
            "SOLANA_AMM_PROGRAM_ID",
            "DHnQfvfUy7Yt92BxZxpj1G8UMWXyKyGGYHvUn2Lrb1xK",
        );
        let app = build_mock_solana_app(Arc::new(sample_mock_api())).await;

        let (_status_swap, _) = json_request(
            app.clone(),
            Method::POST,
            "/api/solana/amm/swap-tx",
            json!({
                "amountIn": "1000",
                "minOut": "900",
                "direction": "AtoB",
                "userPublicKey": "11111111111111111111111111111111"
            }),
            None,
        )
        .await;
        let (_status_mint, _) = json_request(
            app.clone(),
            Method::POST,
            "/api/solana/amm/mint",
            json!({
                "userPublicKey": "11111111111111111111111111111111",
                "token": "A",
                "amount": "1000"
            }),
            Some("test-admin-token"),
        )
        .await;
        let (_status_init, _) = json_request(
            app.clone(),
            Method::POST,
            "/api/solana/amm/init",
            json!({}),
            Some("test-admin-token"),
        )
        .await;
        let (status_health, health_body) =
            json_request(app, Method::GET, "/health", json!({}), None).await;
        assert_eq!(status_health, axum::http::StatusCode::OK);
        assert_eq!(
            health_body
                .get("solana")
                .and_then(|s| s.get("metrics"))
                .and_then(|m| m.get("swapTxOk"))
                .and_then(|v| v.as_u64()),
            Some(1)
        );
        assert_eq!(
            health_body
                .get("solana")
                .and_then(|s| s.get("metrics"))
                .and_then(|m| m.get("mintOk"))
                .and_then(|v| v.as_u64()),
            Some(1)
        );
        assert_eq!(
            health_body
                .get("solana")
                .and_then(|s| s.get("metrics"))
                .and_then(|m| m.get("initOk"))
                .and_then(|v| v.as_u64()),
            Some(1)
        );
        assert_eq!(
            health_body
                .get("solana")
                .and_then(|s| s.get("lastInit"))
                .and_then(|l| l.get("last_init_success"))
                .and_then(|v| v.as_bool()),
            Some(true)
        );
    }

    #[tokio::test]
    async fn metrics_exposes_rpc_rate_limit_counters() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = text_request(app, Method::GET, "/metrics").await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert!(body.contains("elementa_rpc_method_rate_limited_single_total"));
        assert!(body.contains("elementa_rpc_method_rate_limited_batch_total"));
    }

    #[tokio::test]
    async fn metrics_exposes_latency_histogram_and_cache_gauges() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (_health_status, _health_headers, _health_body) =
            json_request_with_headers(app.clone(), Method::GET, "/health", None, &[]).await;
        let (status, body) = text_request(app, Method::GET, "/metrics").await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert!(body.contains("elementa_http_request_latency_ms_bucket"));
        assert!(body.contains("elementa_cache_entries{cache=\"proxy\"}"));
        assert!(body.contains("elementa_cache_entries{cache=\"rpc\"}"));
    }

    #[tokio::test]
    async fn metrics_exposes_upstream_error_cardinality_series() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, body) = text_request(app, Method::GET, "/metrics").await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert!(body.contains("elementa_upstream_error_total{source=\"all\"}"));
        assert!(body.contains("# TYPE elementa_upstream_error_total counter"));
    }

    #[tokio::test]
    async fn metrics_exposes_reject_reason_counters() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (_status, _headers, _body) = json_request_with_headers(
            app.clone(),
            Method::GET,
            "/api/proxy/history?url=https%3A%2F%2Fapi.coingecko.com%2Fapi%2Fv3%2Fping&unexpected=1",
            None,
            &[],
        )
        .await;

        let (status, body) = text_request(app, Method::GET, "/metrics").await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert!(body.contains("elementa_api_reject_reason_total{reason=\"invalid_query\"} 1"));
    }

    #[tokio::test]
    async fn metrics_exposes_cloud_blob_reject_reason_counters() {
        let _l = global_test_lock().await;
        let cfg = test_config();
        let app = build_app(cfg.clone()).await.expect("build app");
        let mut headers = make_wallet_put_auth_headers(app.clone(), &cfg).await;
        headers.push(("x-real-ip", "198.51.100.92".to_string()));

        for _ in 0..=CLOUD_BLOB_WRITE_MAX_REQUESTS {
            let _ = json_request_with_headers(
                app.clone(),
                Method::PUT,
                "/api/wallet/blob",
                Some(valid_wallet_blob_payload()),
                &headers,
            )
            .await;
        }

        let (status, body) = text_request(app, Method::GET, "/metrics").await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert!(body.contains(
            "elementa_api_reject_reason_total{reason=\"cloud_blob_write_rate_limited\"} 1"
        ));
    }

    #[tokio::test]
    async fn contract_api_deployed_exists_and_returns_json_object() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) =
            json_request_with_headers(app, Method::GET, "/api/deployed", None, &[]).await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert!(body.is_object());
    }

    #[tokio::test]
    async fn contract_google_prepare_cookies_and_json_shape() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, headers, body) =
            json_request_with_headers(app, Method::GET, "/api/auth/google/prepare", None, &[])
                .await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(body.get("ok").and_then(|v| v.as_bool()), Some(true));
        assert!(body.get("state").and_then(|v| v.as_str()).is_some());
        assert!(body.get("nonce").and_then(|v| v.as_str()).is_some());
        assert_eq!(body.as_object().map(|m| m.len()), Some(3));

        let csrf_cookie = find_set_cookie(&headers, "ew_csrf_v1").expect("csrf cookie");
        assert!(csrf_cookie.contains("Path=/"));
        assert!(csrf_cookie.contains("SameSite=Lax"));
        assert!(csrf_cookie.contains("Max-Age=3600"));
        assert!(!csrf_cookie.contains("HttpOnly"));
        assert!(!csrf_cookie.contains("Secure"));

        let flow_cookie =
            find_set_cookie(&headers, "ew_google_oauth_flow_v1").expect("google flow cookie");
        assert!(flow_cookie.contains("Path=/"));
        assert!(flow_cookie.contains("SameSite=Lax"));
        assert!(flow_cookie.contains("HttpOnly"));
        assert!(flow_cookie.contains("Max-Age=600"));
        assert!(!flow_cookie.contains("Secure"));
    }

    #[tokio::test]
    async fn contract_apple_prepare_cookies_and_json_shape() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, headers, body) =
            json_request_with_headers(app, Method::GET, "/api/auth/apple/prepare", None, &[]).await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(body.get("ok").and_then(|v| v.as_bool()), Some(true));
        assert!(body.get("state").and_then(|v| v.as_str()).is_some());
        assert!(body.get("nonce").and_then(|v| v.as_str()).is_some());
        assert_eq!(body.as_object().map(|m| m.len()), Some(3));

        let flow_cookie =
            find_set_cookie(&headers, "ew_apple_oauth_flow_v1").expect("apple flow cookie");
        assert!(flow_cookie.contains("Path=/"));
        assert!(flow_cookie.contains("SameSite=Lax"));
        assert!(flow_cookie.contains("HttpOnly"));
        assert!(flow_cookie.contains("Max-Age=600"));
    }

    #[tokio::test]
    async fn contract_auth_me_unauthorized_error_shape() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) =
            json_request_with_headers(app, Method::GET, "/api/auth/me", None, &[]).await;
        assert_eq!(status, axum::http::StatusCode::UNAUTHORIZED);
        assert_eq!(
            body,
            serde_json::json!({
                "error": "Not authenticated"
            })
        );
    }

    #[tokio::test]
    async fn contract_auth_me_success_shape_and_csrf_cookie() {
        let _l = global_test_lock().await;
        let cfg = test_config();
        let app = build_app(cfg.clone()).await.expect("build app");
        let session_cookie = make_session_cookie(&cfg, "google", "user-sub-123");
        let headers = vec![("cookie", session_cookie)];
        let (status, resp_headers, body) =
            json_request_with_headers(app, Method::GET, "/api/auth/me", None, &headers).await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(
            body,
            serde_json::json!({
                "ok": true,
                "provider": "google",
                "sub": "user-sub-123"
            })
        );
        let csrf_cookie = find_set_cookie(&resp_headers, "ew_csrf_v1").expect("csrf cookie");
        assert!(csrf_cookie.contains("Path=/"));
        assert!(csrf_cookie.contains("SameSite=Lax"));
        assert!(csrf_cookie.contains("Max-Age=3600"));
    }

    #[tokio::test]
    async fn contract_logout_csrf_failure_shape() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) =
            json_request_with_headers(app, Method::POST, "/api/auth/logout", Some(json!({})), &[])
                .await;
        assert_eq!(status, axum::http::StatusCode::FORBIDDEN);
        assert_eq!(body, serde_json::json!({ "error": "CSRF check failed" }));
    }

    #[tokio::test]
    async fn contract_logout_success_clears_cookies_with_expected_attrs() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");

        let (_status_prepare, prepare_headers, _prepare_body) = json_request_with_headers(
            app.clone(),
            Method::GET,
            "/api/auth/google/prepare",
            None,
            &[],
        )
        .await;
        let csrf_line = find_set_cookie(&prepare_headers, "ew_csrf_v1").expect("csrf cookie");
        let csrf_token = parse_set_cookie_value(&csrf_line).expect("csrf token");
        let headers = vec![
            ("cookie", format!("ew_csrf_v1={csrf_token}")),
            ("x-csrf-token", csrf_token),
        ];

        let (status, logout_headers, body) = json_request_with_headers(
            app,
            Method::POST,
            "/api/auth/logout",
            Some(json!({})),
            &headers,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(body, serde_json::json!({ "ok": true }));

        let session_clear =
            find_set_cookie(&logout_headers, "ew_session_v1").expect("session clear cookie");
        assert!(session_clear.contains("Path=/"));
        assert!(session_clear.contains("SameSite=Lax"));
        assert!(session_clear.contains("HttpOnly"));
        assert!(session_clear.contains("Max-Age=0"));
        assert!(!session_clear.contains("Secure"));

        let csrf_clear = find_set_cookie(&logout_headers, "ew_csrf_v1").expect("csrf clear cookie");
        assert!(csrf_clear.contains("Path=/"));
        assert!(csrf_clear.contains("SameSite=Lax"));
        assert!(csrf_clear.contains("Max-Age=0"));
        assert!(!csrf_clear.contains("HttpOnly"));
    }

    #[tokio::test]
    async fn contract_wallet_put_blob_csrf_failure_shape() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) =
            json_request_with_headers(app, Method::PUT, "/api/wallet/blob", Some(json!({})), &[])
                .await;
        assert_eq!(status, axum::http::StatusCode::FORBIDDEN);
        assert_eq!(body, serde_json::json!({ "error": "CSRF check failed" }));
    }

    #[tokio::test]
    async fn contract_wallet_blob_write_rate_limited_returns_429() {
        let _l = global_test_lock().await;
        let cfg = test_config();
        let app = build_app(cfg.clone()).await.expect("build app");
        let mut headers = make_wallet_put_auth_headers(app.clone(), &cfg).await;
        headers.push(("x-real-ip", "198.51.100.90".to_string()));

        for _ in 0..CLOUD_BLOB_WRITE_MAX_REQUESTS {
            let (status, _resp_headers, _body) = json_request_with_headers(
                app.clone(),
                Method::PUT,
                "/api/wallet/blob",
                Some(valid_wallet_blob_payload()),
                &headers,
            )
            .await;
            assert_eq!(status, axum::http::StatusCode::OK);
        }

        let (status, _resp_headers, body) = json_request_with_headers(
            app,
            Method::PUT,
            "/api/wallet/blob",
            Some(valid_wallet_blob_payload()),
            &headers,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::TOO_MANY_REQUESTS);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("cloud_blob_write_rate_limited")
        );
    }

    #[tokio::test]
    async fn contract_wallet_blob_read_rate_limited_returns_429() {
        let _l = global_test_lock().await;
        let cfg = test_config();
        let app = build_app(cfg.clone()).await.expect("build app");
        let mut put_headers = make_wallet_put_auth_headers(app.clone(), &cfg).await;
        put_headers.push(("x-real-ip", "198.51.100.91".to_string()));

        let (put_status, _put_resp_headers, _put_body) = json_request_with_headers(
            app.clone(),
            Method::PUT,
            "/api/wallet/blob",
            Some(valid_wallet_blob_payload()),
            &put_headers,
        )
        .await;
        assert_eq!(put_status, axum::http::StatusCode::OK);

        let session_cookie = make_session_cookie(&cfg, "google", "wallet-user-1");
        let read_headers = vec![
            ("cookie", session_cookie),
            ("x-real-ip", "198.51.100.91".to_string()),
        ];
        for _ in 0..CLOUD_BLOB_READ_MAX_REQUESTS {
            let (status, _resp_headers, _body) = json_request_with_headers(
                app.clone(),
                Method::GET,
                "/api/wallet/blob",
                None,
                &read_headers,
            )
            .await;
            assert_eq!(status, axum::http::StatusCode::OK);
        }

        let (status, _resp_headers, body) =
            json_request_with_headers(app, Method::GET, "/api/wallet/blob", None, &read_headers)
                .await;
        assert_eq!(status, axum::http::StatusCode::TOO_MANY_REQUESTS);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("cloud_blob_read_rate_limited")
        );
    }

    #[tokio::test]
    async fn contract_404_error_shape() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) =
            json_request_with_headers(app, Method::GET, "/not-found", None, &[]).await;
        assert_eq!(status, axum::http::StatusCode::NOT_FOUND);
        assert_eq!(body, serde_json::json!({ "error": "Route not found" }));
    }

    #[tokio::test]
    async fn contract_rpc_proxy_rejects_non_post() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) = json_request_with_headers(
            app,
            Method::GET,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            None,
            &[],
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::METHOD_NOT_ALLOWED);
        assert_eq!(
            body.get("error")
                .and_then(|v| v.get("message"))
                .and_then(|v| v.as_str()),
            Some("RPC proxy only accepts POST")
        );
    }

    #[tokio::test]
    async fn contract_rpc_proxy_rejects_disallowed_method() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) = json_request_with_headers(
            app,
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            Some(json!({
                "jsonrpc": "2.0",
                "id": 1,
                "method": "personal_sign",
                "params": []
            })),
            &[],
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert!(body
            .get("error")
            .and_then(|v| v.get("message"))
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .contains("RPC method not allowed"));
    }

    #[tokio::test]
    async fn contract_rpc_proxy_rejects_unknown_jsonrpc_fields() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) = json_request_with_headers(
            app,
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            Some(json!({
                "jsonrpc": "2.0",
                "id": 1,
                "method": "eth_chainId",
                "params": [],
                "unexpected": "field"
            })),
            &[],
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::BAD_REQUEST);
        assert!(body
            .get("error")
            .and_then(|v| v.get("message"))
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .contains("Unknown JSON-RPC field"));
    }

    #[tokio::test]
    async fn contract_rpc_proxy_batch_rejects_disallowed_item_with_jsonrpc_error() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (status, _headers, body) = json_request_with_headers(
            app,
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            Some(json!([{
                "jsonrpc": "2.0",
                "id": 42,
                "method": "personal_sign",
                "params": []
            }])),
            &[],
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::OK);
        let first = body
            .as_array()
            .and_then(|arr| arr.first())
            .cloned()
            .unwrap_or(json!({}));
        assert_eq!(first.get("id").and_then(|v| v.as_i64()), Some(42));
        assert!(first
            .get("error")
            .and_then(|e| e.get("message"))
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .contains("RPC method not allowed"));
    }

    #[tokio::test]
    async fn contract_rpc_proxy_rejects_payload_too_large() {
        let _l = global_test_lock().await;
        let mut cfg = test_config();
        cfg.rpc_max_request_body_bytes = 64;
        let app = build_app(cfg).await.expect("build app");
        let large_payload = format!(
            r#"{{"jsonrpc":"2.0","id":1,"method":"eth_chainId","params":["{}"]}}"#,
            "a".repeat(256)
        );
        let (status, body) = raw_request(
            app,
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            &large_payload,
            None,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::PAYLOAD_TOO_LARGE);
        assert!(body
            .get("error")
            .and_then(|v| v.get("message"))
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .contains("RPC payload too large"));
    }

    #[tokio::test]
    async fn contract_rpc_proxy_rejects_batch_too_large() {
        let _l = global_test_lock().await;
        let mut cfg = test_config();
        cfg.rpc_max_batch_items = 1;
        let app = build_app(cfg).await.expect("build app");
        let (status, _headers, body) = json_request_with_headers(
            app,
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            Some(json!([
                { "jsonrpc": "2.0", "id": 1, "method": "eth_chainId", "params": [] },
                { "jsonrpc": "2.0", "id": 2, "method": "eth_chainId", "params": [] }
            ])),
            &[],
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::PAYLOAD_TOO_LARGE);
        assert!(body
            .get("error")
            .and_then(|v| v.get("message"))
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .contains("RPC batch too large"));
    }

    #[tokio::test]
    async fn contract_rpc_proxy_method_bucket_exhaustion_single_returns_429() {
        let _l = global_test_lock().await;
        let mut cfg = test_config();
        cfg.rpc_method_rate_limit_max_requests = 1;
        cfg.rpc_method_rate_limit_window_ms = 60_000;
        let app = build_app(cfg).await.expect("build app");
        let payload = Some(json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "eth_chainId",
            "params": []
        }));
        let headers = [("x-real-ip", "198.51.100.77".to_string())];
        let _first = json_request_with_headers(
            app.clone(),
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            payload.clone(),
            &headers,
        )
        .await;
        let (status, _resp_headers, body) = json_request_with_headers(
            app.clone(),
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            payload,
            &headers,
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::TOO_MANY_REQUESTS);
        assert_eq!(
            body.get("error")
                .and_then(|v| v.get("message"))
                .and_then(|v| v.as_str()),
            Some("RPC method rate limit exceeded for this client")
        );
        let (_h_status, _h_headers, health_body) =
            json_request_with_headers(app, Method::GET, "/health", None, &[]).await;
        assert_eq!(
            health_body
                .get("apiMetrics")
                .and_then(|m| m.get("rpcMethodRateLimitedSingle"))
                .and_then(|v| v.as_u64()),
            Some(1)
        );
    }

    #[tokio::test]
    async fn contract_rpc_proxy_method_bucket_exhaustion_batch_returns_jsonrpc_error() {
        let _l = global_test_lock().await;
        let mut cfg = test_config();
        cfg.rpc_method_rate_limit_max_requests = 1;
        cfg.rpc_method_rate_limit_window_ms = 60_000;
        let app = build_app(cfg).await.expect("build app");
        let (status, _headers, body) = json_request_with_headers(
            app.clone(),
            Method::POST,
            "/api/proxy/rpc?url=https%3A%2F%2Frpc.ankr.com%2Feth",
            Some(json!([
                { "jsonrpc": "2.0", "id": 1, "method": "eth_chainId", "params": [] },
                { "jsonrpc": "2.0", "id": 2, "method": "eth_chainId", "params": [] }
            ])),
            &[("x-real-ip", "198.51.100.88".to_string())],
        )
        .await;
        assert_eq!(status, axum::http::StatusCode::OK);
        let arr = body.as_array().cloned().unwrap_or_default();
        let second = arr.get(1).cloned().unwrap_or(json!({}));
        assert_eq!(second.get("id").and_then(|v| v.as_i64()), Some(2));
        assert_eq!(
            second
                .get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_i64()),
            Some(-32005)
        );
        assert_eq!(
            second
                .get("error")
                .and_then(|e| e.get("message"))
                .and_then(|v| v.as_str()),
            Some("RPC method rate limit exceeded for this client")
        );
        let (_h_status, _h_headers, health_body) =
            json_request_with_headers(app, Method::GET, "/health", None, &[]).await;
        assert_eq!(
            health_body
                .get("apiMetrics")
                .and_then(|m| m.get("rpcMethodRateLimitedBatch"))
                .and_then(|v| v.as_u64()),
            Some(1)
        );
    }

    #[tokio::test]
    async fn contract_baseline_api_error_rate_stays_below_phase0_threshold() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");

        let (_s0, _h0, baseline_health) =
            json_request_with_headers(app.clone(), Method::GET, "/health", None, &[]).await;
        let baseline_total = baseline_health
            .get("apiMetrics")
            .and_then(|m| m.get("requestsTotal"))
            .and_then(|v| v.as_u64())
            .unwrap_or(0);
        let baseline_invalid_query = baseline_health
            .get("apiMetrics")
            .and_then(|m| m.get("rejectReasons"))
            .and_then(|r| r.get("invalidQuery"))
            .and_then(|v| v.as_u64())
            .unwrap_or(0);

        for _ in 0..120u32 {
            let (status, _headers, _body) =
                json_request_with_headers(app.clone(), Method::GET, "/health", None, &[]).await;
            assert_eq!(status, axum::http::StatusCode::OK);
        }

        let (_bad_status, _bad_headers, _bad_body) = json_request_with_headers(
            app.clone(),
            Method::GET,
            "/api/proxy/history?url=https%3A%2F%2Fapi.coingecko.com%2Fapi%2Fv3%2Fping&unexpected=1",
            None,
            &[],
        )
        .await;

        let (_s1, _h1, final_health) =
            json_request_with_headers(app, Method::GET, "/health", None, &[]).await;
        let final_total = final_health
            .get("apiMetrics")
            .and_then(|m| m.get("requestsTotal"))
            .and_then(|v| v.as_u64())
            .unwrap_or(baseline_total);
        let final_invalid_query = final_health
            .get("apiMetrics")
            .and_then(|m| m.get("rejectReasons"))
            .and_then(|r| r.get("invalidQuery"))
            .and_then(|v| v.as_u64())
            .unwrap_or(baseline_invalid_query);

        let delta_total = final_total.saturating_sub(baseline_total);
        let delta_errors = final_invalid_query.saturating_sub(baseline_invalid_query);
        assert!(delta_total > 0, "expected request counter to increase");

        let api_error_rate = (delta_errors as f64) / (delta_total as f64);
        assert!(
            api_error_rate <= 0.015,
            "api error rate {api_error_rate} exceeded phase 0 max 0.015"
        );
    }

    #[tokio::test]
    async fn production_never_allows_insecure_admin_shortcut() {
        let _l = global_test_lock().await;
        let mut cfg = test_config();
        cfg.node_env = "production".to_string();
        cfg.allow_insecure_admin_dev = true;
        cfg.admin_token.clear();
        let app = build_app(cfg).await.expect("build app");
        let (status, body) =
            json_request(app, Method::POST, "/api/solana/amm/init", json!({}), None).await;
        assert_eq!(status, axum::http::StatusCode::FORBIDDEN);
        assert_eq!(
            body.get("error").and_then(|v| v.as_str()),
            Some("Forbidden")
        );
    }

    #[tokio::test]
    async fn redis_backend_unavailable_falls_back_to_memory_when_not_required() {
        let _l = global_test_lock().await;
        let mut cfg = test_config();
        cfg.infra_store_backend = "redis".to_string();
        cfg.infra_redis_url = "redis://127.0.0.1:1/".to_string();
        cfg.infra_redis_required = false;
        let app = build_app(cfg).await.expect("build app with fallback");
        let (status, _headers, body) =
            json_request_with_headers(app, Method::GET, "/health", None, &[]).await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert_eq!(
            body.get("infra")
                .and_then(|v| v.get("backendMode"))
                .and_then(|v| v.as_str()),
            Some("memory-fallback")
        );
    }

    #[tokio::test]
    async fn redis_backend_unavailable_fails_when_required() {
        let _l = global_test_lock().await;
        let mut cfg = test_config();
        cfg.infra_store_backend = "redis".to_string();
        cfg.infra_redis_url = "redis://127.0.0.1:1/".to_string();
        cfg.infra_redis_required = true;
        let err = build_app(cfg)
            .await
            .expect_err("must fail when redis required");
        assert!(err
            .to_string()
            .contains("Redis infra backend initialization failed"));
    }

    #[tokio::test]
    async fn metrics_exposes_infra_backend_and_redis_ping_series() {
        let _l = global_test_lock().await;
        let app = build_app(test_config()).await.expect("build app");
        let (_health_status, _health_headers, _health_body) =
            json_request_with_headers(app.clone(), Method::GET, "/health", None, &[]).await;
        let (status, body) = text_request(app, Method::GET, "/metrics").await;
        assert_eq!(status, axum::http::StatusCode::OK);
        assert!(body.contains("elementa_infra_backend_info"));
        assert!(body.contains("elementa_redis_ping_total{result=\"ok\"}"));
        assert!(body.contains("elementa_redis_ping_total{result=\"error\"}"));
        assert!(body.contains("elementa_redis_ping_latency_ms_avg"));
    }

    #[tokio::test]
    async fn redis_integration_cache_ttl_and_rate_limit_window() {
        let _l = global_test_lock().await;
        let redis_url = match std::env::var("REDIS_TEST_URL") {
            Ok(v) if !v.trim().is_empty() => v,
            _ => return,
        };

        let mut cfg = test_config();
        cfg.infra_store_backend = "redis".to_string();
        cfg.infra_redis_required = true;
        cfg.infra_redis_url = redis_url;
        cfg.infra_redis_key_prefix = format!("elementa-it-{}", chrono_ms_now());
        cfg.infra_redis_connect_timeout_ms = 500;
        cfg.infra_redis_command_timeout_ms = 500;

        let st = AppState::new_with_solana(cfg, Arc::new(sample_mock_api()))
            .await
            .expect("redis state");
        let now = chrono_ms_now();
        st.proxy_cache
            .put(
                "integration:key".to_string(),
                CacheEntry {
                    value: json!({ "ok": true }),
                    expires_at_ms: now + 120,
                    is_error: false,
                },
            )
            .await;
        assert!(st.proxy_cache.get("integration:key").await.is_some());
        tokio::time::sleep(std::time::Duration::from_millis(180)).await;
        assert!(st.proxy_cache.get("integration:key").await.is_none());

        let rl_key = "integration:bucket";
        let allowed = st
            .rate_limits
            .consume(rl_key, chrono_ms_now(), 120, 1)
            .await;
        assert!(!allowed);
        let blocked = st
            .rate_limits
            .consume(rl_key, chrono_ms_now(), 120, 1)
            .await;
        assert!(blocked);
        tokio::time::sleep(std::time::Duration::from_millis(140)).await;
        let reset_allowed = st
            .rate_limits
            .consume(rl_key, chrono_ms_now(), 120, 1)
            .await;
        assert!(!reset_allowed);
    }
}
