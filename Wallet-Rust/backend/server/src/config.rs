use anyhow::{anyhow, Result};

#[derive(Debug, Clone)]
pub struct Config {
    pub port: u16,
    pub node_env: String,
    pub cors_origins: String,
    pub cookie_samesite: String,
    pub session_jwt_secret: String,
    pub cloud_kek_secret: String,

    // OAuth / Cloud recovery
    pub google_oauth_client_id: String,
    pub google_oauth_client_secret: String,
    pub google_oauth_redirect_uri: String,

    pub apple_client_id: String,
    pub apple_team_id: String,
    pub apple_key_id: String,
    pub apple_private_key_pem: String,
    pub apple_oauth_redirect_uri: String,

    // Proxy allowlists
    pub external_api_allowlist: String,
    pub proxy_host_allowlist: String,
    pub external_host_allowlist: String,
    pub rpc_url_allowlist: String,

    pub zerox_api_key: String,
    pub fallback_aggregator_url: String,
    pub fallback_aggregator_provider: String,
    pub fallback_aggregator_api_key: String,
    pub fallback_aggregator_api_header: String,
    pub fallback_aggregator_quote_path: String,
    pub fallback_aggregator_price_path: String,
    pub fallback_aggregator_version: String,

    pub jupiter_api_url: String,
    pub jupiter_api_key: String,

    pub admin_token: String,
    pub allow_insecure_admin_dev: bool,
    pub disable_admin_scripts: bool,
    pub solana_admin_strict: bool,
    pub enable_server_kek: bool,
    pub rpc_max_request_body_bytes: usize,
    pub rpc_max_response_body_bytes: usize,
    pub rpc_max_batch_items: usize,
    pub rpc_per_host_max_concurrency: usize,
    pub rpc_method_rate_limit_max_requests: u32,
    pub rpc_method_rate_limit_window_ms: i64,
    pub infra_store_backend: String,
    pub infra_redis_url: String,
    pub infra_redis_key_prefix: String,
    pub infra_redis_required: bool,
    pub infra_redis_connect_timeout_ms: u64,
    pub infra_redis_command_timeout_ms: u64,
}

impl Config {
    pub fn from_env() -> Result<Self> {
        let port = std::env::var("PORT")
            .ok()
            .and_then(|s| s.parse::<u16>().ok())
            .unwrap_or(3001);

        let node_env = std::env::var("NODE_ENV").unwrap_or_else(|_| "development".to_string());
        let cors_origins = std::env::var("CORS_ORIGINS").unwrap_or_default();
        let cookie_samesite =
            std::env::var("COOKIE_SAMESITE").unwrap_or_else(|_| "Lax".to_string());

        let session_jwt_secret = std::env::var("SESSION_JWT_SECRET").unwrap_or_default();
        let cloud_kek_secret = std::env::var("CLOUD_KEK_SECRET").unwrap_or_default();

        let google_oauth_client_id = std::env::var("GOOGLE_OAUTH_CLIENT_ID").unwrap_or_default();
        let google_oauth_client_secret =
            std::env::var("GOOGLE_OAUTH_CLIENT_SECRET").unwrap_or_default();
        let google_oauth_redirect_uri = std::env::var("GOOGLE_OAUTH_REDIRECT_URI")
            .unwrap_or_else(|_| "postmessage".to_string());

        let apple_client_id = std::env::var("APPLE_CLIENT_ID").unwrap_or_default();
        let apple_team_id = std::env::var("APPLE_TEAM_ID").unwrap_or_default();
        let apple_key_id = std::env::var("APPLE_KEY_ID").unwrap_or_default();
        let apple_private_key_pem = std::env::var("APPLE_PRIVATE_KEY_PEM").unwrap_or_default();
        let apple_oauth_redirect_uri =
            std::env::var("APPLE_OAUTH_REDIRECT_URI").unwrap_or_default();
        let external_api_allowlist = std::env::var("EXTERNAL_API_ALLOWLIST").unwrap_or_default();
        let proxy_host_allowlist =
            std::env::var("ALLOWED_PROXY_HOSTS").unwrap_or_else(|_| external_api_allowlist.clone());
        let external_host_allowlist = std::env::var("ALLOWED_EXTERNAL_HOSTS")
            .unwrap_or_else(|_| external_api_allowlist.clone());
        let rpc_url_allowlist = std::env::var("RPC_URL_ALLOWLIST").unwrap_or_default();
        let zerox_api_key = std::env::var("ZEROX_API_KEY").unwrap_or_default();
        let fallback_aggregator_url = std::env::var("FALLBACK_AGGREGATOR_URL").unwrap_or_default();
        let fallback_aggregator_provider = std::env::var("FALLBACK_AGGREGATOR_PROVIDER")
            .unwrap_or_else(|_| "zeroex_compat".to_string());
        let fallback_aggregator_api_key =
            std::env::var("FALLBACK_AGGREGATOR_API_KEY").unwrap_or_default();
        let fallback_aggregator_api_header = std::env::var("FALLBACK_AGGREGATOR_API_HEADER")
            .unwrap_or_else(|_| "x-api-key".to_string());
        let fallback_aggregator_quote_path = std::env::var("FALLBACK_AGGREGATOR_QUOTE_PATH")
            .unwrap_or_else(|_| "/swap/allowance-holder/quote".to_string());
        let fallback_aggregator_price_path = std::env::var("FALLBACK_AGGREGATOR_PRICE_PATH")
            .unwrap_or_else(|_| "/swap/allowance-holder/price".to_string());
        let fallback_aggregator_version =
            std::env::var("FALLBACK_AGGREGATOR_VERSION").unwrap_or_default();

        let jupiter_api_url = std::env::var("JUPITER_API_URL")
            .unwrap_or_else(|_| "https://api.jup.ag/swap/v1".to_string());
        let jupiter_api_key = std::env::var("JUPITER_API_KEY").unwrap_or_default();

        let admin_token = std::env::var("ADMIN_TOKEN").unwrap_or_default();
        let allow_insecure_admin_dev =
            std::env::var("ALLOW_INSECURE_ADMIN_DEV").unwrap_or_else(|_| "0".to_string()) == "1";
        let disable_admin_scripts =
            std::env::var("DISABLE_ADMIN_SCRIPTS").unwrap_or_else(|_| "0".to_string()) == "1";
        let solana_admin_strict =
            std::env::var("SOLANA_AMM_ADMIN_STRICT").unwrap_or_else(|_| "0".to_string()) == "1";
        let enable_server_kek =
            std::env::var("ENABLE_SERVER_KEK").unwrap_or_else(|_| "0".to_string()) == "1";
        let rpc_max_request_body_bytes =
            parse_env_usize_nonzero("RPC_MAX_REQUEST_BODY_BYTES", 256 * 1024)?;
        let rpc_max_response_body_bytes =
            parse_env_usize_nonzero("RPC_MAX_RESPONSE_BODY_BYTES", 2 * 1024 * 1024)?;
        let rpc_max_batch_items = parse_env_usize_nonzero("RPC_MAX_BATCH_ITEMS", 25)?;
        let rpc_per_host_max_concurrency =
            parse_env_usize_nonzero("RPC_PER_HOST_MAX_CONCURRENCY", 16)?;
        let rpc_method_rate_limit_max_requests =
            parse_env_u32_nonzero("RPC_METHOD_RATE_LIMIT_MAX_REQUESTS", 60)?;
        let rpc_method_rate_limit_window_ms =
            parse_env_i64_nonzero("RPC_METHOD_RATE_LIMIT_WINDOW_MS", 60_000)?;
        let infra_store_backend =
            std::env::var("INFRA_STORE_BACKEND").unwrap_or_else(|_| "memory".to_string());
        let infra_redis_url = std::env::var("INFRA_REDIS_URL")
            .unwrap_or_else(|_| "redis://127.0.0.1:6379/".to_string());
        let infra_redis_key_prefix =
            std::env::var("INFRA_REDIS_KEY_PREFIX").unwrap_or_else(|_| "elementa".to_string());
        let infra_redis_required =
            std::env::var("INFRA_REDIS_REQUIRED").unwrap_or_else(|_| "0".to_string()) == "1";
        let infra_redis_connect_timeout_ms =
            parse_env_u64_nonzero("INFRA_REDIS_CONNECT_TIMEOUT_MS", 800)?;
        let infra_redis_command_timeout_ms =
            parse_env_u64_nonzero("INFRA_REDIS_COMMAND_TIMEOUT_MS", 500)?;
        if !matches!(
            infra_store_backend.trim().to_ascii_lowercase().as_str(),
            "memory" | "redis"
        ) {
            return Err(anyhow!("INFRA_STORE_BACKEND must be one of: memory, redis"));
        }
        let fallback_provider_normalized = fallback_aggregator_provider.trim().to_ascii_lowercase();
        if !matches!(
            fallback_provider_normalized.as_str(),
            "zeroex_compat" | "oneinch"
        ) {
            return Err(anyhow!(
                "FALLBACK_AGGREGATOR_PROVIDER must be one of: zeroex_compat, oneinch"
            ));
        }

        // We won't hard-error on missing secrets yet because we'll bring endpoints online incrementally.
        // But keep a cheap sanity check so production doesn't accidentally run wide open.
        if node_env == "production" {
            if session_jwt_secret.trim().len() < 16 {
                return Err(anyhow!("SESSION_JWT_SECRET is required in production"));
            }
            if cloud_kek_secret.trim().len() < 16 {
                return Err(anyhow!("CLOUD_KEK_SECRET is required in production"));
            }
            if enable_server_kek {
                return Err(anyhow!("ENABLE_SERVER_KEK must be disabled in production"));
            }
            if rpc_url_allowlist.trim().is_empty() {
                return Err(anyhow!("RPC_URL_ALLOWLIST is required in production"));
            }
            if allow_insecure_admin_dev {
                return Err(anyhow!(
                    "ALLOW_INSECURE_ADMIN_DEV must be disabled in production"
                ));
            }
            if infra_store_backend.trim().eq_ignore_ascii_case("redis")
                && infra_redis_url.trim().is_empty()
            {
                return Err(anyhow!(
                    "INFRA_REDIS_URL is required when INFRA_STORE_BACKEND=redis"
                ));
            }
        }
        if solana_admin_strict {
            let keypair = std::env::var("SOLANA_DEPLOYER_KEYPAIR").unwrap_or_default();
            let program_id = std::env::var("SOLANA_AMM_PROGRAM_ID").unwrap_or_default();
            if keypair.trim().is_empty() || program_id.trim().is_empty() {
                return Err(anyhow!(
                    "SOLANA_DEPLOYER_KEYPAIR and SOLANA_AMM_PROGRAM_ID are required when SOLANA_AMM_ADMIN_STRICT=1"
                ));
            }
        }

        Ok(Self {
            port,
            node_env,
            cors_origins,
            cookie_samesite,
            session_jwt_secret,
            cloud_kek_secret,
            google_oauth_client_id,
            google_oauth_client_secret,
            google_oauth_redirect_uri,
            apple_client_id,
            apple_team_id,
            apple_key_id,
            apple_private_key_pem,
            apple_oauth_redirect_uri,
            external_api_allowlist,
            proxy_host_allowlist,
            external_host_allowlist,
            rpc_url_allowlist,
            zerox_api_key,
            fallback_aggregator_url,
            fallback_aggregator_provider: fallback_provider_normalized,
            fallback_aggregator_api_key,
            fallback_aggregator_api_header,
            fallback_aggregator_quote_path,
            fallback_aggregator_price_path,
            fallback_aggregator_version,
            jupiter_api_url,
            jupiter_api_key,
            admin_token,
            allow_insecure_admin_dev,
            disable_admin_scripts,
            solana_admin_strict,
            enable_server_kek,
            rpc_max_request_body_bytes,
            rpc_max_response_body_bytes,
            rpc_max_batch_items,
            rpc_per_host_max_concurrency,
            rpc_method_rate_limit_max_requests,
            rpc_method_rate_limit_window_ms,
            infra_store_backend,
            infra_redis_url,
            infra_redis_key_prefix,
            infra_redis_required,
            infra_redis_connect_timeout_ms,
            infra_redis_command_timeout_ms,
        })
    }

    pub fn cookie_secure(&self) -> bool {
        self.node_env == "production"
    }
}

fn parse_env_usize_nonzero(key: &str, default: usize) -> Result<usize> {
    let raw = match std::env::var(key) {
        Ok(v) => v,
        Err(_) => return Ok(default),
    };
    let parsed = raw
        .trim()
        .parse::<usize>()
        .map_err(|_| anyhow!("{key} must be a positive integer"))?;
    if parsed == 0 {
        return Err(anyhow!("{key} must be > 0"));
    }
    Ok(parsed)
}

fn parse_env_u32_nonzero(key: &str, default: u32) -> Result<u32> {
    let raw = match std::env::var(key) {
        Ok(v) => v,
        Err(_) => return Ok(default),
    };
    let parsed = raw
        .trim()
        .parse::<u32>()
        .map_err(|_| anyhow!("{key} must be a positive integer"))?;
    if parsed == 0 {
        return Err(anyhow!("{key} must be > 0"));
    }
    Ok(parsed)
}

fn parse_env_i64_nonzero(key: &str, default: i64) -> Result<i64> {
    let raw = match std::env::var(key) {
        Ok(v) => v,
        Err(_) => return Ok(default),
    };
    let parsed = raw
        .trim()
        .parse::<i64>()
        .map_err(|_| anyhow!("{key} must be a positive integer"))?;
    if parsed <= 0 {
        return Err(anyhow!("{key} must be > 0"));
    }
    Ok(parsed)
}

fn parse_env_u64_nonzero(key: &str, default: u64) -> Result<u64> {
    let raw = match std::env::var(key) {
        Ok(v) => v,
        Err(_) => return Ok(default),
    };
    let parsed = raw
        .trim()
        .parse::<u64>()
        .map_err(|_| anyhow!("{key} must be a positive integer"))?;
    if parsed == 0 {
        return Err(anyhow!("{key} must be > 0"));
    }
    Ok(parsed)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::{Mutex, OnceLock};

    static TEST_LOCK: OnceLock<Mutex<()>> = OnceLock::new();

    fn test_lock() -> std::sync::MutexGuard<'static, ()> {
        TEST_LOCK
            .get_or_init(|| Mutex::new(()))
            .lock()
            .expect("lock")
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

    #[test]
    fn allowlist_aliases_fallback_to_external_api_allowlist() {
        let _l = test_lock();
        let _env = EnvGuard::set("NODE_ENV", "development");
        let _sess = EnvGuard::set("SESSION_JWT_SECRET", "test_session_secret_1234567890");
        let _kek = EnvGuard::set("CLOUD_KEK_SECRET", "test_cloud_kek_secret_1234567890");
        let _external = EnvGuard::set("EXTERNAL_API_ALLOWLIST", "api.0x.org");
        let _proxy = EnvGuard::remove("ALLOWED_PROXY_HOSTS");
        let _ext = EnvGuard::remove("ALLOWED_EXTERNAL_HOSTS");
        let cfg = Config::from_env().expect("config");
        assert_eq!(cfg.proxy_host_allowlist, "api.0x.org");
        assert_eq!(cfg.external_host_allowlist, "api.0x.org");
    }

    #[test]
    fn production_rejects_server_kek_mode() {
        let _l = test_lock();
        let _env = EnvGuard::set("NODE_ENV", "production");
        let _sess = EnvGuard::set("SESSION_JWT_SECRET", "test_session_secret_1234567890");
        let _kek = EnvGuard::set("CLOUD_KEK_SECRET", "test_cloud_kek_secret_1234567890");
        let _rpc = EnvGuard::set("RPC_URL_ALLOWLIST", "rpc.ankr.com");
        let _server_kek = EnvGuard::set("ENABLE_SERVER_KEK", "1");
        let err = Config::from_env().expect_err("must fail in production");
        assert!(err.to_string().contains("ENABLE_SERVER_KEK"));
    }

    #[test]
    fn production_requires_rpc_allowlist() {
        let _l = test_lock();
        let _env = EnvGuard::set("NODE_ENV", "production");
        let _sess = EnvGuard::set("SESSION_JWT_SECRET", "test_session_secret_1234567890");
        let _kek = EnvGuard::set("CLOUD_KEK_SECRET", "test_cloud_kek_secret_1234567890");
        let _server_kek = EnvGuard::set("ENABLE_SERVER_KEK", "0");
        let _rpc = EnvGuard::remove("RPC_URL_ALLOWLIST");
        let err = Config::from_env().expect_err("must fail in production");
        assert!(err.to_string().contains("RPC_URL_ALLOWLIST"));
    }

    #[test]
    fn production_rejects_insecure_admin_mode() {
        let _l = test_lock();
        let _env = EnvGuard::set("NODE_ENV", "production");
        let _sess = EnvGuard::set("SESSION_JWT_SECRET", "test_session_secret_1234567890");
        let _kek = EnvGuard::set("CLOUD_KEK_SECRET", "test_cloud_kek_secret_1234567890");
        let _rpc = EnvGuard::set("RPC_URL_ALLOWLIST", "rpc.ankr.com");
        let _server_kek = EnvGuard::set("ENABLE_SERVER_KEK", "0");
        let _insecure = EnvGuard::set("ALLOW_INSECURE_ADMIN_DEV", "1");
        let err = Config::from_env().expect_err("must fail in production");
        assert!(err.to_string().contains("ALLOW_INSECURE_ADMIN_DEV"));
    }

    #[test]
    fn rejects_invalid_rpc_tunable_env_values() {
        let _l = test_lock();
        let _env = EnvGuard::set("NODE_ENV", "development");
        let _sess = EnvGuard::set("SESSION_JWT_SECRET", "test_session_secret_1234567890");
        let _kek = EnvGuard::set("CLOUD_KEK_SECRET", "test_cloud_kek_secret_1234567890");
        let _bad_size = EnvGuard::set("RPC_MAX_REQUEST_BODY_BYTES", "0");
        let err = Config::from_env().expect_err("must reject zero size");
        assert!(err.to_string().contains("RPC_MAX_REQUEST_BODY_BYTES"));
    }

    #[test]
    fn rejects_invalid_redis_timeout_value() {
        let _l = test_lock();
        let _env = EnvGuard::set("NODE_ENV", "development");
        let _sess = EnvGuard::set("SESSION_JWT_SECRET", "test_session_secret_1234567890");
        let _kek = EnvGuard::set("CLOUD_KEK_SECRET", "test_cloud_kek_secret_1234567890");
        let _bad_timeout = EnvGuard::set("INFRA_REDIS_COMMAND_TIMEOUT_MS", "0");
        let err = Config::from_env().expect_err("must reject zero redis timeout");
        assert!(err.to_string().contains("INFRA_REDIS_COMMAND_TIMEOUT_MS"));
    }

    #[test]
    fn production_redis_backend_requires_url() {
        let _l = test_lock();
        let _env = EnvGuard::set("NODE_ENV", "production");
        let _sess = EnvGuard::set("SESSION_JWT_SECRET", "test_session_secret_1234567890");
        let _kek = EnvGuard::set("CLOUD_KEK_SECRET", "test_cloud_kek_secret_1234567890");
        let _rpc = EnvGuard::set("RPC_URL_ALLOWLIST", "rpc.ankr.com");
        let _server_kek = EnvGuard::set("ENABLE_SERVER_KEK", "0");
        let _backend = EnvGuard::set("INFRA_STORE_BACKEND", "redis");
        let _redis_url = EnvGuard::set("INFRA_REDIS_URL", "");
        let err = Config::from_env().expect_err("must fail without redis url");
        assert!(err.to_string().contains("INFRA_REDIS_URL"));
    }
}
