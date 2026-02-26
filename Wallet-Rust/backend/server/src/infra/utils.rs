use base64::Engine;
use rand::RngCore;

use crate::http::csrf::require_csrf;

pub(crate) fn random_base64url(len_bytes: usize) -> String {
    let mut bytes = vec![0u8; len_bytes];
    rand::rngs::OsRng.fill_bytes(&mut bytes);
    base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(bytes)
}

pub(crate) fn chrono_ms_now() -> i64 {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or(std::time::Duration::from_secs(0));
    now.as_millis() as i64
}

pub(crate) fn require_csrf_for_state_change(method: &str, headers: &axum::http::HeaderMap) -> bool {
    let m = method.to_uppercase();
    if m == "GET" || m == "HEAD" || m == "OPTIONS" {
        return true;
    }
    require_csrf(headers)
}
