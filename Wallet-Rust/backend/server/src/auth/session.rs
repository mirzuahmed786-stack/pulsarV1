use axum::http::HeaderMap;
use jsonwebtoken::{decode, encode, Algorithm, DecodingKey, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};

use crate::http::cookies::{append_set_cookie, parse_cookie_header, CookieOpts};
use crate::http::csrf::CSRF_COOKIE_NAME;

pub const SESSION_COOKIE_NAME: &str = "ew_session_v1";
const SESSION_ISSUER: &str = "earth-backend";
const SESSION_AUDIENCE: &str = "earth-wallet-web";

#[derive(Debug, Clone)]
pub struct SessionAuth {
    pub provider: String,
    pub sub: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct SessionClaims {
    // jose/jwt standard claims
    iss: String,
    aud: String,
    iat: usize,
    exp: usize,

    // custom payload
    provider: String,
    sub: String,
}

pub fn derive_session_key(secret: &str) -> [u8; 32] {
    let mut h = Sha256::new();
    h.update(secret.as_bytes());
    let out = h.finalize();
    let mut key = [0u8; 32];
    key.copy_from_slice(&out[..]);
    key
}

pub fn issue_session_cookie(
    headers: &mut HeaderMap,
    key: &[u8; 32],
    provider: &str,
    sub: &str,
    ttl_s: u64,
    same_site: &str,
    secure: bool,
) -> anyhow::Result<()> {
    let now = (std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()) as usize;

    let claims = SessionClaims {
        iss: SESSION_ISSUER.to_string(),
        aud: SESSION_AUDIENCE.to_string(),
        iat: now,
        exp: now + ttl_s as usize,
        provider: provider.to_string(),
        sub: sub.to_string(),
    };

    let jwt = encode(
        &Header::new(Algorithm::HS256),
        &claims,
        &EncodingKey::from_secret(key),
    )?;

    append_set_cookie(
        headers,
        SESSION_COOKIE_NAME,
        &jwt,
        CookieOpts {
            http_only: true,
            max_age_s: Some(ttl_s),
            same_site: same_site.to_string(),
            secure,
            path: "/".to_string(),
        },
    );
    Ok(())
}

pub fn require_session(headers: &HeaderMap, key: &[u8; 32]) -> Result<SessionAuth, &'static str> {
    let cookies = parse_cookie_header(headers);
    let token = cookies
        .get(SESSION_COOKIE_NAME)
        .cloned()
        .unwrap_or_default();
    if token.is_empty() {
        return Err("Not authenticated");
    }
    let mut validation = Validation::new(Algorithm::HS256);
    validation.set_issuer(&[SESSION_ISSUER]);
    validation.set_audience(&[SESSION_AUDIENCE]);
    let data = decode::<SessionClaims>(&token, &DecodingKey::from_secret(key), &validation)
        .map_err(|_| "Invalid session")?;
    let provider = data.claims.provider;
    let sub = data.claims.sub;
    if provider.is_empty() || sub.is_empty() {
        return Err("Invalid session");
    }
    Ok(SessionAuth { provider, sub })
}

pub fn clear_session_cookies(headers: &mut HeaderMap, same_site: &str, secure: bool) {
    append_set_cookie(
        headers,
        SESSION_COOKIE_NAME,
        "",
        CookieOpts {
            http_only: true,
            max_age_s: Some(0),
            same_site: same_site.to_string(),
            secure,
            path: "/".to_string(),
        },
    );
    append_set_cookie(
        headers,
        CSRF_COOKIE_NAME,
        "",
        CookieOpts {
            http_only: false,
            max_age_s: Some(0),
            same_site: same_site.to_string(),
            secure,
            path: "/".to_string(),
        },
    );
}
