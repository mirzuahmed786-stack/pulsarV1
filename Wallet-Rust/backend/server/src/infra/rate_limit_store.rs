use dashmap::DashMap;

#[derive(Clone, Copy)]
pub(crate) struct RateLimitWindow {
    pub(crate) window_start_ms: i64,
    pub(crate) count: u32,
}

#[async_trait::async_trait]
pub(crate) trait RateLimitStore: Send + Sync {
    async fn consume(&self, key: &str, now_ms: i64, window_ms: i64, max_requests: u32) -> bool;
    async fn remove_stale(&self, now_ms: i64, max_age_ms: i64);
}

#[derive(Default)]
pub(crate) struct InMemoryRateLimitStore {
    windows: DashMap<String, RateLimitWindow>,
}

#[async_trait::async_trait]
impl RateLimitStore for InMemoryRateLimitStore {
    async fn consume(&self, key: &str, now_ms: i64, window_ms: i64, max_requests: u32) -> bool {
        let mut block = false;
        match self.windows.get_mut(key) {
            Some(mut entry) => {
                if now_ms - entry.window_start_ms >= window_ms {
                    *entry = RateLimitWindow {
                        window_start_ms: now_ms,
                        count: 1,
                    };
                } else {
                    entry.count = entry.count.saturating_add(1);
                    if entry.count > max_requests {
                        block = true;
                    }
                }
            }
            None => {
                self.windows.insert(
                    key.to_string(),
                    RateLimitWindow {
                        window_start_ms: now_ms,
                        count: 1,
                    },
                );
            }
        }
        block
    }

    async fn remove_stale(&self, now_ms: i64, max_age_ms: i64) {
        let stale: Vec<String> = self
            .windows
            .iter()
            .filter(|entry| now_ms - entry.window_start_ms > max_age_ms)
            .map(|entry| entry.key().clone())
            .collect();
        for key in stale {
            self.windows.remove(&key);
        }
    }
}
