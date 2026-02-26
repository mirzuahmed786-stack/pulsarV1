use dashmap::DashMap;

use crate::app::CacheEntry;

#[async_trait::async_trait]
pub(crate) trait CacheStore: Send + Sync {
    async fn get(&self, key: &str) -> Option<CacheEntry>;
    async fn put(&self, key: String, entry: CacheEntry);
    async fn remove(&self, key: &str);
    async fn len(&self) -> usize;
    async fn remove_expired_keys(&self, now_ms: i64) -> Vec<String>;
    async fn trim_to(&self, max_entries: usize) -> Vec<String>;
}

#[derive(Default)]
pub(crate) struct InMemoryCacheStore {
    entries: DashMap<String, CacheEntry>,
}

#[async_trait::async_trait]
impl CacheStore for InMemoryCacheStore {
    async fn get(&self, key: &str) -> Option<CacheEntry> {
        self.entries.get(key).map(|entry| entry.value().clone())
    }

    async fn put(&self, key: String, entry: CacheEntry) {
        self.entries.insert(key, entry);
    }

    async fn remove(&self, key: &str) {
        self.entries.remove(key);
    }

    async fn len(&self) -> usize {
        self.entries.len()
    }

    async fn remove_expired_keys(&self, now_ms: i64) -> Vec<String> {
        let expired: Vec<String> = self
            .entries
            .iter()
            .filter(|entry| entry.expires_at_ms <= now_ms)
            .map(|entry| entry.key().clone())
            .collect();
        for key in &expired {
            self.entries.remove(key);
        }
        expired
    }

    async fn trim_to(&self, max_entries: usize) -> Vec<String> {
        let len = self.entries.len();
        if len <= max_entries {
            return Vec::new();
        }
        let overflow = len - max_entries;
        let mut ordered: Vec<(String, i64)> = self
            .entries
            .iter()
            .map(|entry| (entry.key().clone(), entry.expires_at_ms))
            .collect();
        ordered.sort_by_key(|(_, expires_at_ms)| *expires_at_ms);
        let removed: Vec<String> = ordered
            .into_iter()
            .take(overflow)
            .map(|(key, _)| key)
            .collect();
        for key in &removed {
            self.entries.remove(key);
        }
        removed
    }
}
