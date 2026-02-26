use anyhow::{anyhow, Context, Result};
use axum::http::HeaderMap;
use serde::Deserialize;

use crate::{
    auth::{jwks, kek::derive_cloud_kek_hex, session::issue_session_cookie},
    http::{
        cookies::{append_set_cookie, parse_cookie_header, CookieOpts},
        csrf::issue_csrf_token,
    },
};

const GOOGLE_FLOW_COOKIE: &str = "ew_google_oauth_flow_v1";
const SESSION_TTL_S: u64 = 60 * 60;

#[derive(Debug, Clone, Deserialize)]
struct FlowCookie {
    state: String,
    nonce: String,
    exp: i64,
}

#[derive(Debug, Clone, Deserialize)]
struct GoogleIdTokenClaims {
    sub: String,
    #[serde(default)]
    email: String,
    #[serde(default)]
    name: String,
    #[serde(default)]
    picture: String,
    #[serde(default)]
    nonce: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct GoogleExchangeInput {
    pub code: String,
    #[serde(default)]
    pub code_verifier: String,
    #[serde(default)]
    pub state: String,
}

pub async fn google_exchange(
    st: &crate::app::AppState,
    headers: HeaderMap,
    input: GoogleExchangeInput,
) -> Result<(HeaderMap, axum::Json<serde_json::Value>)> {
    let cfg = &st.cfg;
    let http = &st.http;
    let session_key = &st.session_key;
    let cloud_kek_key = &st.cloud_kek_key;

    if cfg.google_oauth_client_id.trim().is_empty() {
        return Err(anyhow!("Missing required env var: GOOGLE_OAUTH_CLIENT_ID"));
    }
    if cfg.google_oauth_client_secret.trim().is_empty() {
        return Err(anyhow!(
            "Missing required env var: GOOGLE_OAUTH_CLIENT_SECRET"
        ));
    }

    let code = input.code.trim();
    if code.len() < 10 {
        return Err(anyhow!("Missing code"));
    }

    let cookies = parse_cookie_header(&headers);
    let flow = cookies
        .get(GOOGLE_FLOW_COOKIE)
        .and_then(|raw| serde_json::from_str::<FlowCookie>(raw).ok())
        .ok_or_else(|| anyhow!("OAuth flow not initialized"))?;
    if input.state.trim().is_empty() {
        return Err(anyhow!("Missing state"));
    }
    if input.state.trim() != flow.state {
        return Err(anyhow!("Invalid state"));
    }
    if input.code_verifier.trim().is_empty() {
        return Err(anyhow!("Missing code_verifier"));
    }
    if cfg.google_oauth_redirect_uri.trim().is_empty()
        || cfg.google_oauth_redirect_uri.trim() == "postmessage"
    {
        return Err(anyhow!(
            "Backend not configured for Google PKCE redirect flow (GOOGLE_OAUTH_REDIRECT_URI)"
        ));
    }
    if chrono_ms_now() > flow.exp {
        return Err(anyhow!("Invalid state"));
    }

    let mut form = std::collections::HashMap::<&str, String>::new();
    form.insert("code", code.to_string());
    form.insert("client_id", cfg.google_oauth_client_id.clone());
    form.insert("client_secret", cfg.google_oauth_client_secret.clone());
    form.insert("redirect_uri", cfg.google_oauth_redirect_uri.clone());
    form.insert("grant_type", "authorization_code".to_string());
    if !input.code_verifier.trim().is_empty() {
        form.insert("code_verifier", input.code_verifier.trim().to_string());
    }

    let token_res = http
        .post("https://oauth2.googleapis.com/token")
        .form(&form)
        .send()
        .await
        .context("google token exchange")?;
    let status = token_res.status();
    let token_json: serde_json::Value = token_res
        .json()
        .await
        .unwrap_or_else(|_| serde_json::json!({}));
    if !status.is_success() {
        return Err(anyhow!("Google token exchange failed"));
    }
    let id_token = token_json
        .get("id_token")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    if id_token.is_empty() {
        return Err(anyhow!("Missing id_token from Google"));
    }

    let jwks = jwks::fetch_jwks(st, "https://www.googleapis.com/oauth2/v3/certs").await?;
    let claims: GoogleIdTokenClaims = jwks::decode_rs256(
        &id_token,
        &jwks,
        &cfg.google_oauth_client_id,
        &["https://accounts.google.com", "accounts.google.com"],
    )?;

    if claims.nonce.is_empty() || claims.nonce != flow.nonce {
        return Err(anyhow!("Invalid nonce"));
    }

    if claims.sub.trim().is_empty() {
        return Err(anyhow!("Invalid Google token (missing sub)"));
    }

    let mut response = serde_json::json!({
        "ok": true,
        "provider": "google",
        "user": {
            "sub": claims.sub,
            "email": claims.email,
            "name": claims.name,
            "picture": claims.picture,
        },
    });
    if cfg.enable_server_kek {
        let kek = derive_cloud_kek_hex(cloud_kek_key, "google", &claims.sub);
        response["kek"] = serde_json::Value::String(kek);
    }

    let mut out_headers = HeaderMap::new();
    issue_session_cookie(
        &mut out_headers,
        session_key,
        "google",
        &claims.sub,
        SESSION_TTL_S,
        &cfg.cookie_samesite,
        cfg.cookie_secure(),
    )?;
    issue_csrf_token(
        &mut out_headers,
        &cfg.cookie_samesite,
        cfg.cookie_secure(),
        SESSION_TTL_S,
    );
    // Clear flow cookie after use.
    append_set_cookie(
        &mut out_headers,
        GOOGLE_FLOW_COOKIE,
        "",
        CookieOpts {
            http_only: true,
            max_age_s: Some(0),
            same_site: cfg.cookie_samesite.clone(),
            secure: cfg.cookie_secure(),
            path: "/".to_string(),
        },
    );

    Ok((out_headers, axum::Json(response)))
}

fn chrono_ms_now() -> i64 {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default();
    now.as_millis() as i64
}
