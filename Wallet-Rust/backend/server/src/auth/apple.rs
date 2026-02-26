use anyhow::{anyhow, Context, Result};
use axum::http::HeaderMap;
use jsonwebtoken::{encode, Algorithm, EncodingKey, Header};
use serde::Deserialize;

use crate::{
    auth::{jwks, kek::derive_cloud_kek_hex, session::issue_session_cookie},
    config::Config,
    http::{
        cookies::{append_set_cookie, parse_cookie_header, CookieOpts},
        csrf::issue_csrf_token,
    },
};

const APPLE_FLOW_COOKIE: &str = "ew_apple_oauth_flow_v1";
const SESSION_TTL_S: u64 = 60 * 60;

#[derive(Debug, Clone, Deserialize)]
struct FlowCookie {
    state: String,
    nonce: String,
    exp: i64,
}

#[derive(Debug, Clone, Deserialize)]
struct AppleIdTokenClaims {
    sub: String,
    #[serde(default)]
    email: String,
    #[serde(default)]
    nonce: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct AppleExchangeInput {
    #[serde(default)]
    pub code: String,
    #[serde(default)]
    pub id_token: String,
    #[serde(default)]
    pub state: String,
}

pub async fn apple_exchange(
    st: &crate::app::AppState,
    headers: HeaderMap,
    input: AppleExchangeInput,
) -> Result<(HeaderMap, axum::Json<serde_json::Value>)> {
    let cfg = &st.cfg;
    let http = &st.http;
    let session_key = &st.session_key;
    let cloud_kek_key = &st.cloud_kek_key;

    if cfg.apple_client_id.trim().is_empty() {
        return Err(anyhow!("Missing required env var: APPLE_CLIENT_ID"));
    }

    let cookies = parse_cookie_header(&headers);
    let flow = cookies
        .get(APPLE_FLOW_COOKIE)
        .and_then(|raw| serde_json::from_str::<FlowCookie>(raw).ok())
        .ok_or_else(|| anyhow!("OAuth flow not initialized"))?;
    if input.state.trim().is_empty() {
        return Err(anyhow!("Missing state"));
    }
    if input.state.trim() != flow.state {
        return Err(anyhow!("Invalid state"));
    }
    if chrono_ms_now() > flow.exp {
        return Err(anyhow!("Invalid state"));
    }

    let mut id_token = input.id_token.trim().to_string();
    if !input.code.trim().is_empty() {
        // Prefer backend code exchange when configured.
        let client_secret = build_apple_client_secret(cfg)?;
        let mut form = std::collections::HashMap::<&str, String>::new();
        form.insert("client_id", cfg.apple_client_id.clone());
        form.insert("client_secret", client_secret);
        form.insert("code", input.code.trim().to_string());
        form.insert("grant_type", "authorization_code".to_string());
        if !cfg.apple_oauth_redirect_uri.trim().is_empty() {
            form.insert("redirect_uri", cfg.apple_oauth_redirect_uri.clone());
        }

        let token_res = http
            .post("https://appleid.apple.com/auth/token")
            .form(&form)
            .send()
            .await
            .context("apple token exchange")?;
        let status = token_res.status();
        let token_json: serde_json::Value = token_res
            .json()
            .await
            .unwrap_or_else(|_| serde_json::json!({}));
        if !status.is_success() {
            return Err(anyhow!("Apple token exchange failed"));
        }
        id_token = token_json
            .get("id_token")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_string();
    }

    if id_token.len() < 50 {
        return Err(anyhow!("Missing id_token"));
    }

    let jwks = jwks::fetch_jwks(st, "https://appleid.apple.com/auth/keys").await?;
    let claims: AppleIdTokenClaims = jwks::decode_rs256(
        &id_token,
        &jwks,
        &cfg.apple_client_id,
        &["https://appleid.apple.com"],
    )?;

    if claims.sub.trim().is_empty() {
        return Err(anyhow!("Invalid Apple token (missing sub)"));
    }
    if claims.nonce.is_empty() || claims.nonce != flow.nonce {
        return Err(anyhow!("Invalid nonce"));
    }

    let mut response = serde_json::json!({
        "ok": true,
        "provider": "apple",
        "user": {
            "sub": claims.sub,
            "email": claims.email,
            "name": "",
            "picture": "",
        },
    });
    if cfg.enable_server_kek {
        let kek = derive_cloud_kek_hex(cloud_kek_key, "apple", &claims.sub);
        response["kek"] = serde_json::Value::String(kek);
    }

    let mut out_headers = HeaderMap::new();
    issue_session_cookie(
        &mut out_headers,
        session_key,
        "apple",
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
    append_set_cookie(
        &mut out_headers,
        APPLE_FLOW_COOKIE,
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

fn build_apple_client_secret(cfg: &Config) -> Result<String> {
    if cfg.apple_team_id.trim().is_empty() {
        return Err(anyhow!("Missing required env var: APPLE_TEAM_ID"));
    }
    if cfg.apple_key_id.trim().is_empty() {
        return Err(anyhow!("Missing required env var: APPLE_KEY_ID"));
    }
    if cfg.apple_private_key_pem.trim().is_empty() {
        return Err(anyhow!("Missing required env var: APPLE_PRIVATE_KEY_PEM"));
    }
    if cfg.apple_oauth_redirect_uri.trim().is_empty() {
        return Err(anyhow!(
            "Missing required env var: APPLE_OAUTH_REDIRECT_URI"
        ));
    }

    let pkcs8 = if cfg.apple_private_key_pem.contains("\\n") {
        cfg.apple_private_key_pem.replace("\\n", "\n")
    } else {
        cfg.apple_private_key_pem.clone()
    };
    let key = EncodingKey::from_ec_pem(pkcs8.as_bytes()).context("parse APPLE_PRIVATE_KEY_PEM")?;

    let now = (std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()) as usize;

    let claims = serde_json::json!({
        "iss": cfg.apple_team_id,
        "aud": "https://appleid.apple.com",
        "sub": cfg.apple_client_id,
        "iat": now,
        "exp": now + 300,
    });

    let mut header = Header::new(Algorithm::ES256);
    header.kid = Some(cfg.apple_key_id.clone());
    let jwt = encode(&header, &claims, &key).context("sign apple client_secret")?;
    Ok(jwt)
}

fn chrono_ms_now() -> i64 {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default();
    now.as_millis() as i64
}
