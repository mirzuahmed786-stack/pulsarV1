use std::time::Duration;

use base64::Engine;
use redis::{aio::ConnectionManager, AsyncCommands};

use crate::{
    app::{chrono_ms_now, CacheEntry},
    config::Config,
    infra::{cache::CacheStore, rate_limit_store::RateLimitStore},
};

#[derive(Clone)]
pub(crate) struct RedisBackendHandles {
    pub(crate) proxy_cache: std::sync::Arc<dyn CacheStore>,
    pub(crate) rpc_cache: std::sync::Arc<dyn CacheStore>,
    pub(crate) rate_limits: std::sync::Arc<dyn RateLimitStore>,
    pub(crate) client: redis::Client,
}

pub(crate) async fn build_redis_backends(cfg: &Config) -> anyhow::Result<RedisBackendHandles> {
    let client = redis::Client::open(cfg.infra_redis_url.clone())?;
    let manager = tokio::time::timeout(
        Duration::from_millis(cfg.infra_redis_connect_timeout_ms),
        ConnectionManager::new(client.clone()),
    )
    .await
    .map_err(|_| anyhow::anyhow!("redis connect timeout"))??;
    let timeout = Duration::from_millis(cfg.infra_redis_command_timeout_ms);
    let prefix = cfg.infra_redis_key_prefix.trim().to_string();
    let proxy_cache = std::sync::Arc::new(RedisCacheStore::new(
        manager.clone(),
        timeout,
        prefix.clone(),
        "proxy",
    ));
    let rpc_cache = std::sync::Arc::new(RedisCacheStore::new(
        manager.clone(),
        timeout,
        prefix.clone(),
        "rpc",
    ));
    let rate_limits = std::sync::Arc::new(RedisRateLimitStore::new(manager, timeout, prefix));
    Ok(RedisBackendHandles {
        proxy_cache,
        rpc_cache,
        rate_limits,
        client,
    })
}

struct RedisCacheStore {
    manager: ConnectionManager,
    timeout: Duration,
    data_prefix: String,
    index_key: String,
}

impl RedisCacheStore {
    fn new(manager: ConnectionManager, timeout: Duration, prefix: String, namespace: &str) -> Self {
        Self {
            manager,
            timeout,
            data_prefix: format!("{prefix}:cache:{namespace}"),
            index_key: format!("{prefix}:cache:{namespace}:idx"),
        }
    }

    fn member(logical_key: &str) -> String {
        base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(logical_key.as_bytes())
    }

    fn decode_member(member: &str) -> Option<String> {
        let raw = base64::engine::general_purpose::URL_SAFE_NO_PAD
            .decode(member.as_bytes())
            .ok()?;
        String::from_utf8(raw).ok()
    }

    fn data_key(&self, logical_key: &str) -> String {
        format!("{}:{}", self.data_prefix, Self::member(logical_key))
    }
}

#[async_trait::async_trait]
impl CacheStore for RedisCacheStore {
    async fn get(&self, key: &str) -> Option<CacheEntry> {
        let redis_key = self.data_key(key);
        let mut conn = self.manager.clone();
        let result = tokio::time::timeout(self.timeout, conn.get::<_, Option<String>>(&redis_key))
            .await
            .ok()?
            .ok()?;
        let raw = result?;
        serde_json::from_str::<CacheEntry>(&raw).ok()
    }

    async fn put(&self, key: String, entry: CacheEntry) {
        let ttl_ms = (entry.expires_at_ms - chrono_ms_now()).max(1) as u64;
        let payload = match serde_json::to_string(&entry) {
            Ok(v) => v,
            Err(_) => return,
        };
        let redis_key = self.data_key(&key);
        let member = Self::member(&key);
        let mut conn = self.manager.clone();
        let _ = tokio::time::timeout(self.timeout, async {
            let _: () = redis::cmd("PSETEX")
                .arg(redis_key)
                .arg(ttl_ms)
                .arg(payload)
                .query_async(&mut conn)
                .await?;
            let _: () = conn
                .zadd(&self.index_key, member, entry.expires_at_ms)
                .await?;
            redis::RedisResult::Ok(())
        })
        .await;
    }

    async fn remove(&self, key: &str) {
        let redis_key = self.data_key(key);
        let member = Self::member(key);
        let mut conn = self.manager.clone();
        let _ = tokio::time::timeout(self.timeout, async {
            let _: usize = conn.del(redis_key).await?;
            let _: usize = conn.zrem(&self.index_key, member).await?;
            redis::RedisResult::Ok(())
        })
        .await;
    }

    async fn len(&self) -> usize {
        let mut conn = self.manager.clone();
        tokio::time::timeout(self.timeout, conn.zcard::<_, usize>(&self.index_key))
            .await
            .ok()
            .and_then(|v| v.ok())
            .unwrap_or(0)
    }

    async fn remove_expired_keys(&self, now_ms: i64) -> Vec<String> {
        let mut conn = self.manager.clone();
        let members = tokio::time::timeout(
            self.timeout,
            conn.zrangebyscore::<_, _, _, Vec<String>>(&self.index_key, i64::MIN, now_ms),
        )
        .await
        .ok()
        .and_then(|v| v.ok())
        .unwrap_or_default();
        if members.is_empty() {
            return Vec::new();
        }
        for member in &members {
            let redis_key = format!("{}:{}", self.data_prefix, member);
            let _: redis::RedisResult<usize> = conn.del(redis_key).await;
        }
        let _: redis::RedisResult<usize> =
            conn.zrembyscore(&self.index_key, i64::MIN, now_ms).await;
        members
            .iter()
            .filter_map(|member| Self::decode_member(member))
            .collect()
    }

    async fn trim_to(&self, max_entries: usize) -> Vec<String> {
        let mut conn = self.manager.clone();
        let len = tokio::time::timeout(self.timeout, conn.zcard::<_, usize>(&self.index_key))
            .await
            .ok()
            .and_then(|v| v.ok())
            .unwrap_or(0);
        if len <= max_entries {
            return Vec::new();
        }
        let overflow = len - max_entries;
        let members = tokio::time::timeout(
            self.timeout,
            conn.zrange::<_, Vec<String>>(&self.index_key, 0, overflow as isize - 1),
        )
        .await
        .ok()
        .and_then(|v| v.ok())
        .unwrap_or_default();
        for member in &members {
            let redis_key = format!("{}:{}", self.data_prefix, member);
            let _: redis::RedisResult<usize> = conn.del(redis_key).await;
            let _: redis::RedisResult<usize> = conn.zrem(&self.index_key, member).await;
        }
        members
            .iter()
            .filter_map(|member| Self::decode_member(member))
            .collect()
    }
}

struct RedisRateLimitStore {
    manager: ConnectionManager,
    timeout: Duration,
    prefix: String,
}

impl RedisRateLimitStore {
    fn new(manager: ConnectionManager, timeout: Duration, prefix: String) -> Self {
        Self {
            manager,
            timeout,
            prefix,
        }
    }

    fn key(&self, raw_key: &str) -> String {
        let member = base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(raw_key.as_bytes());
        format!("{}:rl:{}", self.prefix, member)
    }
}

#[async_trait::async_trait]
impl RateLimitStore for RedisRateLimitStore {
    async fn consume(&self, key: &str, now_ms: i64, window_ms: i64, max_requests: u32) -> bool {
        let lua = r#"
local key = KEYS[1]
local now_ms = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local max_requests = tonumber(ARGV[3])
local ttl_ms = tonumber(ARGV[4])
local ws = redis.call('HGET', key, 'ws')
local count = redis.call('HGET', key, 'count')
if (not ws) or (now_ms - tonumber(ws) >= window_ms) then
  ws = now_ms
  count = 1
else
  count = tonumber(count) + 1
end
redis.call('HSET', key, 'ws', ws, 'count', count)
redis.call('PEXPIRE', key, ttl_ms)
if tonumber(count) > max_requests then
  return 1
end
return 0
"#;
        let mut conn = self.manager.clone();
        let redis_key = self.key(key);
        let ttl_ms = window_ms.saturating_mul(2);
        let result = tokio::time::timeout(self.timeout, async {
            redis::Script::new(lua)
                .key(redis_key)
                .arg(now_ms)
                .arg(window_ms)
                .arg(max_requests)
                .arg(ttl_ms)
                .invoke_async::<i32>(&mut conn)
                .await
        })
        .await;
        match result {
            Ok(Ok(v)) => v == 1,
            _ => false,
        }
    }

    async fn remove_stale(&self, _now_ms: i64, _max_age_ms: i64) {}
}
