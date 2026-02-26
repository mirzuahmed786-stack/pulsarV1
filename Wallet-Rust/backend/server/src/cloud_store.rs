use anyhow::{anyhow, Context, Result};
use serde_json::Value;
use std::collections::HashMap;
use std::path::{Path, PathBuf};

const MAX_STORE_FILE_BYTES: usize = 16 * 1024 * 1024;
const MAX_STORE_ENTRIES: usize = 100_000;
const MAX_USER_KEY_LEN: usize = 256;
const MAX_ENCRYPTED_SEED_BLOB_LEN: usize = 2_000_000;
const MAX_WALLET_ID_LEN: usize = 128;

fn validate_user_key(user_key: &str) -> Result<()> {
    if user_key.is_empty() || user_key.len() > MAX_USER_KEY_LEN {
        return Err(anyhow!("invalid user key"));
    }
    Ok(())
}

fn validate_record_shape(record: &Value) -> Result<()> {
    let Some(obj) = record.as_object() else {
        return Err(anyhow!("blob record must be a JSON object"));
    };

    // Keep schema strict to avoid silently accepting malformed/corrupt records.
    let allowed = [
        "encryptedSeedBlob",
        "walletId",
        "version",
        "createdAt",
        "secretType",
        "hdIndex",
        "kekKdf",
        "updatedAt",
    ];
    for key in obj.keys() {
        if !allowed.contains(&key.as_str()) {
            return Err(anyhow!("unknown blob field: {}", key));
        }
    }

    let encrypted_seed_blob = obj
        .get("encryptedSeedBlob")
        .and_then(|v| v.as_str())
        .unwrap_or("");
    if encrypted_seed_blob.is_empty() || encrypted_seed_blob.len() > MAX_ENCRYPTED_SEED_BLOB_LEN {
        return Err(anyhow!("invalid encryptedSeedBlob"));
    }

    let wallet_id = obj.get("walletId").and_then(|v| v.as_str()).unwrap_or("");
    if wallet_id.is_empty() || wallet_id.len() > MAX_WALLET_ID_LEN {
        return Err(anyhow!("invalid walletId"));
    }

    let version = obj.get("version").and_then(|v| v.as_i64()).unwrap_or(0);
    if !(1..=10).contains(&version) {
        return Err(anyhow!("invalid version"));
    }

    let created_at = obj.get("createdAt").and_then(|v| v.as_i64()).unwrap_or(0);
    if created_at <= 0 {
        return Err(anyhow!("invalid createdAt"));
    }

    let secret_type = obj.get("secretType").and_then(|v| v.as_str()).unwrap_or("");
    if secret_type != "mnemonic" && secret_type != "raw32" {
        return Err(anyhow!("invalid secretType"));
    }

    let hd_index = obj.get("hdIndex").and_then(|v| v.as_i64()).unwrap_or(-1);
    if !(0..=0x7fffffff).contains(&hd_index) {
        return Err(anyhow!("invalid hdIndex"));
    }

    if let Some(updated_at) = obj.get("updatedAt").and_then(|v| v.as_i64()) {
        if updated_at <= 0 {
            return Err(anyhow!("invalid updatedAt"));
        }
    }

    if let Some(kek_kdf) = obj.get("kekKdf") {
        if !kek_kdf.is_null() && !kek_kdf.is_object() {
            return Err(anyhow!("invalid kekKdf"));
        }
    }

    Ok(())
}

#[derive(Clone)]
pub enum StoreKind {
    File { path: PathBuf },
    Sqlite { pool: sqlx::SqlitePool },
}

impl StoreKind {
    pub fn from_env() -> Result<Self> {
        let kind = std::env::var("CLOUD_RECOVERY_STORE").unwrap_or_else(|_| "file".to_string());
        if kind.trim().eq_ignore_ascii_case("sqlite") {
            let p = std::env::var("CLOUD_RECOVERY_SQLITE_PATH")
                .ok()
                .map(PathBuf::from)
                .unwrap_or_else(|| default_state_dir().join("cloud_recovery.sqlite3"));
            return Ok(StoreKind::Sqlite {
                pool: sqlx::SqlitePool::connect_lazy(&format!("sqlite://{}", p.display()))?,
            });
        }
        let p = default_state_dir().join("cloud_recovery_blobs.json");
        Ok(StoreKind::File { path: p })
    }

    pub async fn init(&self) -> Result<()> {
        match self {
            StoreKind::File { path } => {
                if let Some(dir) = path.parent() {
                    tokio::fs::create_dir_all(dir).await.ok();
                }
                Ok(())
            }
            StoreKind::Sqlite { pool } => {
                sqlx::query(
                    r#"
                    CREATE TABLE IF NOT EXISTS cloud_recovery_blobs (
                        user_key TEXT PRIMARY KEY,
                        blob_json TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    );
                    "#,
                )
                .execute(pool)
                .await?;
                Ok(())
            }
        }
    }

    pub async fn get(&self, user_key: &str) -> Result<Option<Value>> {
        validate_user_key(user_key)?;
        match self {
            StoreKind::Sqlite { pool } => {
                let row: Option<(String,)> =
                    sqlx::query_as("SELECT blob_json FROM cloud_recovery_blobs WHERE user_key = ?")
                        .bind(user_key)
                        .fetch_optional(pool)
                        .await?;
                let Some((blob_json,)) = row else {
                    return Ok(None);
                };
                let v: Value = serde_json::from_str(&blob_json)
                    .map_err(|e| anyhow!("malformed cloud recovery blob json in sqlite: {e}"))?;
                validate_record_shape(&v)
                    .map_err(|e| anyhow!("invalid cloud recovery blob record in sqlite: {e}"))?;
                Ok(Some(v))
            }
            StoreKind::File { path } => {
                let store = read_json_store(path).await?;
                let Some(v) = store.get(user_key).cloned() else {
                    return Ok(None);
                };
                validate_record_shape(&v).map_err(|e| {
                    anyhow!("invalid cloud recovery blob record in file store: {e}")
                })?;
                Ok(Some(v))
            }
        }
    }

    pub async fn put(&self, user_key: &str, record: Value) -> Result<()> {
        validate_user_key(user_key)?;
        validate_record_shape(&record)?;
        match self {
            StoreKind::Sqlite { pool } => {
                let updated_at = record
                    .get("updatedAt")
                    .and_then(|v| v.as_i64())
                    .unwrap_or_else(chrono_ms_now);
                let blob_json = serde_json::to_string(&record)?;
                sqlx::query(
                    "INSERT INTO cloud_recovery_blobs(user_key, blob_json, updated_at) VALUES(?,?,?) \
                     ON CONFLICT(user_key) DO UPDATE SET blob_json=excluded.blob_json, updated_at=excluded.updated_at",
                )
                .bind(user_key)
                .bind(blob_json)
                .bind(updated_at)
                .execute(pool)
                .await?;
                Ok(())
            }
            StoreKind::File { path } => {
                let mut store = read_json_store(path).await?;
                if store.len() >= MAX_STORE_ENTRIES && !store.contains_key(user_key) {
                    return Err(anyhow!("cloud recovery store entry limit reached"));
                }
                store.insert(user_key.to_string(), record);
                write_json_store(path, &store).await?;
                Ok(())
            }
        }
    }
}

fn default_state_dir() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("..")
        .join("state")
}

async fn read_json_store(path: &Path) -> Result<HashMap<String, Value>> {
    let raw_bytes = match tokio::fs::read(path).await {
        Ok(s) => s,
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => return Ok(Default::default()),
        Err(e) => return Err(e.into()),
    };
    if raw_bytes.len() > MAX_STORE_FILE_BYTES {
        return Err(anyhow!(
            "cloud recovery store exceeds max size ({} bytes)",
            MAX_STORE_FILE_BYTES
        ));
    }
    let raw = std::str::from_utf8(&raw_bytes).context("cloud recovery store is not valid UTF-8")?;
    let v: Value = serde_json::from_str(raw)
        .map_err(|e| anyhow!("cloud recovery store is malformed JSON: {e}"))?;
    let Some(obj) = v.as_object() else {
        return Err(anyhow!("cloud recovery store root must be an object"));
    };
    if obj.len() > MAX_STORE_ENTRIES {
        return Err(anyhow!(
            "cloud recovery store exceeds max entries ({})",
            MAX_STORE_ENTRIES
        ));
    }
    let mut out = HashMap::new();
    for (k, v) in obj {
        validate_user_key(k)?;
        validate_record_shape(v)
            .map_err(|e| anyhow!("invalid cloud recovery blob record for key '{}': {e}", k))?;
        out.insert(k.clone(), v.clone());
    }
    Ok(out)
}

async fn write_json_store(path: &Path, store: &HashMap<String, Value>) -> Result<()> {
    let dir = path.parent().ok_or_else(|| anyhow!("invalid store path"))?;
    tokio::fs::create_dir_all(dir).await.ok();
    let tmp = path.with_extension("json.tmp");
    if store.len() > MAX_STORE_ENTRIES {
        return Err(anyhow!(
            "cloud recovery store exceeds max entries ({})",
            MAX_STORE_ENTRIES
        ));
    }
    for (key, value) in store {
        validate_user_key(key)?;
        validate_record_shape(value)
            .map_err(|e| anyhow!("invalid cloud recovery blob record for key '{}': {e}", key))?;
    }
    let raw = serde_json::to_string_pretty(store)?;
    if raw.len() > MAX_STORE_FILE_BYTES {
        return Err(anyhow!(
            "cloud recovery store exceeds max size ({} bytes)",
            MAX_STORE_FILE_BYTES
        ));
    }
    tokio::fs::write(&tmp, raw)
        .await
        .context("write tmp store")?;
    // Best-effort replace for Windows (not truly atomic).
    let _ = tokio::fs::remove_file(path).await;
    tokio::fs::rename(&tmp, path)
        .await
        .context("rename store")?;
    Ok(())
}

fn chrono_ms_now() -> i64 {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default();
    now.as_millis() as i64
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn test_record() -> Value {
        json!({
            "encryptedSeedBlob": "AAECAwQFBgc=",
            "walletId": "wallet-1",
            "version": 1,
            "createdAt": 1710000000000_i64,
            "secretType": "mnemonic",
            "hdIndex": 0,
            "kekKdf": serde_json::Value::Null,
            "updatedAt": 1710000001000_i64
        })
    }

    fn temp_store_path(name: &str) -> PathBuf {
        let unique = format!(
            "elementa_cloud_store_{}_{}_{}.json",
            name,
            std::process::id(),
            chrono_ms_now()
        );
        std::env::temp_dir().join(unique)
    }

    #[tokio::test]
    async fn file_store_put_rejects_invalid_record_shape() {
        let path = temp_store_path("invalid_shape");
        let store = StoreKind::File { path: path.clone() };
        let invalid = json!({
            "encryptedSeedBlob": "x",
            "walletId": "w1",
            "version": 1,
            "createdAt": 1,
            "secretType": "mnemonic",
            "hdIndex": 0,
            "updatedAt": 2,
            "unexpected": true
        });
        let err = store
            .put("google:sub-1", invalid)
            .await
            .expect_err("must reject unknown fields");
        assert!(err.to_string().contains("unknown blob field"));
        let _ = tokio::fs::remove_file(path).await;
    }

    #[tokio::test]
    async fn file_store_get_rejects_malformed_json() {
        let path = temp_store_path("malformed");
        tokio::fs::write(&path, "{not-json")
            .await
            .expect("write malformed");
        let store = StoreKind::File { path: path.clone() };
        let err = store
            .get("google:sub-1")
            .await
            .expect_err("must reject malformed store");
        assert!(err.to_string().contains("malformed JSON"));
        let _ = tokio::fs::remove_file(path).await;
    }

    #[tokio::test]
    async fn file_store_put_and_get_valid_record_roundtrip() {
        let path = temp_store_path("roundtrip");
        let store = StoreKind::File { path: path.clone() };
        let rec = test_record();
        store
            .put("google:sub-1", rec.clone())
            .await
            .expect("store valid record");
        let got = store
            .get("google:sub-1")
            .await
            .expect("read valid record")
            .expect("record exists");
        assert_eq!(
            got.get("walletId").and_then(|v| v.as_str()),
            Some("wallet-1")
        );
        let _ = tokio::fs::remove_file(path).await;
    }
}
