use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    response::IntoResponse,
    Json,
};
use axum::{
    middleware,
    routing::{get, post},
    Router,
};

use crate::{
    app::{AppState, APPLE_FLOW_COOKIE, FLOW_TTL_S, GOOGLE_FLOW_COOKIE, SESSION_TTL_S},
    auth::{
        apple::apple_exchange,
        google::google_exchange,
        kek::derive_cloud_kek_hex,
        session::{clear_session_cookies, require_session},
    },
    http::origin::{require_allowed_origin_for_state_change, OriginPolicy},
    http::{
        cookies::{append_set_cookie, CookieOpts},
        csrf::issue_csrf_token,
    },
};

pub fn router(origin_policy: OriginPolicy) -> Router<AppState> {
    Router::new()
        .route("/apple/prepare", get(apple_prepare))
        .route("/google/prepare", get(google_prepare))
        .route("/google/exchange", post(google_exchange_handler))
        .route("/apple/exchange", post(apple_exchange_handler))
        .route("/logout", post(auth_logout))
        .route("/me", get(auth_me))
        .route("/kek", get(auth_kek))
        .layer(middleware::from_fn_with_state(
            origin_policy,
            require_allowed_origin_for_state_change,
        ))
}

pub(crate) async fn apple_prepare(State(st): State<AppState>) -> impl IntoResponse {
    let state = crate::app::random_base64url(24);
    let nonce = crate::app::random_base64url(24);
    let exp = (crate::app::chrono_ms_now() + (FLOW_TTL_S * 1000) as i64).max(0);
    let cookie_payload =
        serde_json::json!({ "state": state, "nonce": nonce, "exp": exp }).to_string();

    let mut headers = HeaderMap::new();
    issue_csrf_token(
        &mut headers,
        &st.cfg.cookie_samesite,
        st.cfg.cookie_secure(),
        SESSION_TTL_S,
    );
    append_set_cookie(
        &mut headers,
        APPLE_FLOW_COOKIE,
        &cookie_payload,
        CookieOpts {
            path: "/".into(),
            http_only: true,
            secure: st.cfg.cookie_secure(),
            same_site: st.cfg.cookie_samesite.clone(),
            max_age_s: Some(FLOW_TTL_S),
        },
    );
    (
        headers,
        Json(serde_json::json!({ "ok": true, "state": state, "nonce": nonce })),
    )
}

pub(crate) async fn google_prepare(State(st): State<AppState>) -> impl IntoResponse {
    let state = crate::app::random_base64url(24);
    let nonce = crate::app::random_base64url(24);
    let exp = (crate::app::chrono_ms_now() + (FLOW_TTL_S * 1000) as i64).max(0);
    let cookie_payload =
        serde_json::json!({ "state": state, "nonce": nonce, "exp": exp }).to_string();

    let mut headers = HeaderMap::new();
    issue_csrf_token(
        &mut headers,
        &st.cfg.cookie_samesite,
        st.cfg.cookie_secure(),
        SESSION_TTL_S,
    );
    append_set_cookie(
        &mut headers,
        GOOGLE_FLOW_COOKIE,
        &cookie_payload,
        CookieOpts {
            path: "/".into(),
            http_only: true,
            secure: st.cfg.cookie_secure(),
            same_site: st.cfg.cookie_samesite.clone(),
            max_age_s: Some(FLOW_TTL_S),
        },
    );
    (
        headers,
        Json(serde_json::json!({ "ok": true, "state": state, "nonce": nonce })),
    )
}

pub(crate) async fn google_exchange_handler(
    State(st): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<crate::auth::google::GoogleExchangeInput>,
) -> impl IntoResponse {
    let request_id = crate::app::incoming_request_id(Some(&headers));
    match google_exchange(&st, headers, input).await {
        Ok(resp) => resp.into_response(),
        Err(e) => {
            let msg = e.to_string();
            let status =
                if msg.starts_with("Missing required env var") || msg.contains("not configured") {
                    StatusCode::INTERNAL_SERVER_ERROR
                } else {
                    StatusCode::UNAUTHORIZED
                };
            tracing::warn!(error = %msg, "google auth exchange failed");
            crate::app::api_error_response(
                status,
                "auth_exchange_failed",
                "Authentication exchange failed",
                Some(msg),
                request_id,
            )
            .into_response()
        }
    }
}

pub(crate) async fn apple_exchange_handler(
    State(st): State<AppState>,
    headers: HeaderMap,
    Json(input): Json<crate::auth::apple::AppleExchangeInput>,
) -> impl IntoResponse {
    let request_id = crate::app::incoming_request_id(Some(&headers));
    match apple_exchange(&st, headers, input).await {
        Ok(resp) => resp.into_response(),
        Err(e) => {
            let msg = e.to_string();
            let status =
                if msg.starts_with("Missing required env var") || msg.contains("not configured") {
                    StatusCode::INTERNAL_SERVER_ERROR
                } else {
                    StatusCode::UNAUTHORIZED
                };
            tracing::warn!(error = %msg, "apple auth exchange failed");
            crate::app::api_error_response(
                status,
                "auth_exchange_failed",
                "Authentication exchange failed",
                Some(msg),
                request_id,
            )
            .into_response()
        }
    }
}

pub(crate) async fn auth_me(State(st): State<AppState>, headers: HeaderMap) -> impl IntoResponse {
    match require_session(&headers, &st.session_key) {
        Ok(a) => {
            let mut out_headers = HeaderMap::new();
            issue_csrf_token(
                &mut out_headers,
                &st.cfg.cookie_samesite,
                st.cfg.cookie_secure(),
                SESSION_TTL_S,
            );
            (
                out_headers,
                Json(serde_json::json!({ "ok": true, "provider": a.provider, "sub": a.sub })),
            )
                .into_response()
        }
        Err(msg) => (
            StatusCode::UNAUTHORIZED,
            Json(serde_json::json!({ "error": msg })),
        )
            .into_response(),
    }
}

pub(crate) async fn auth_kek(State(st): State<AppState>, headers: HeaderMap) -> impl IntoResponse {
    if !st.cfg.enable_server_kek {
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "Server KEK mode is disabled" })),
        )
            .into_response();
    }
    let auth = match require_session(&headers, &st.session_key) {
        Ok(a) => a,
        Err(msg) => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(serde_json::json!({ "error": msg })),
            )
                .into_response()
        }
    };
    let kek = derive_cloud_kek_hex(&st.cloud_kek_key, &auth.provider, &auth.sub);
    (
        StatusCode::OK,
        Json(serde_json::json!({ "ok": true, "provider": auth.provider, "sub": auth.sub, "kek": kek })),
    )
        .into_response()
}

pub(crate) async fn auth_logout(
    State(st): State<AppState>,
    headers: HeaderMap,
) -> impl IntoResponse {
    if !crate::app::require_csrf_for_state_change("POST", &headers) {
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "CSRF check failed" })),
        )
            .into_response();
    }
    let mut out_headers = HeaderMap::new();
    clear_session_cookies(
        &mut out_headers,
        &st.cfg.cookie_samesite,
        st.cfg.cookie_secure(),
    );
    (out_headers, Json(serde_json::json!({ "ok": true }))).into_response()
}
