use axum::http::HeaderMap;
use base64::Engine;
use rand::RngCore;

use crate::http::cookies::{append_set_cookie, parse_cookie_header, CookieOpts};

pub const CSRF_COOKIE_NAME: &str = "ew_csrf_v1";

pub fn issue_csrf_token(
    headers: &mut HeaderMap,
    same_site: &str,
    secure: bool,
    max_age_s: u64,
) -> String {
    let mut bytes = [0u8; 24];
    rand::rngs::OsRng.fill_bytes(&mut bytes);
    let token = base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(bytes);

    append_set_cookie(
        headers,
        CSRF_COOKIE_NAME,
        &token,
        CookieOpts {
            http_only: false,
            max_age_s: Some(max_age_s),
            same_site: same_site.to_string(),
            secure,
            path: "/".to_string(),
        },
    );
    token
}

pub fn require_csrf(req_headers: &HeaderMap) -> bool {
    let method = req_headers
        .get(axum::http::header::ACCESS_CONTROL_REQUEST_METHOD)
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");
    let _ = method; // reserved if we later need special casing for preflight

    // The caller should check the HTTP method; this function does just token comparison.
    let cookies = parse_cookie_header(req_headers);
    let cookie_token = cookies.get(CSRF_COOKIE_NAME).cloned().unwrap_or_default();
    let header_token = req_headers
        .get("x-csrf-token")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("")
        .trim()
        .to_string();
    !cookie_token.is_empty() && cookie_token == header_token
}
