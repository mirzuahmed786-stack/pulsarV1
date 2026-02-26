use axum::{
    extract::{Request, State},
    http::{HeaderMap, HeaderValue, StatusCode},
    middleware::Next,
    response::{IntoResponse, Response},
    Json,
};
use std::sync::atomic::Ordering;
use tracing::Instrument;

use crate::infra::constants::{HEADER_REQUEST_ID, RATE_LIMIT_MAX_REQUESTS, RATE_LIMIT_WINDOW_MS};
use crate::infra::state::AppState;
use crate::infra::utils::{chrono_ms_now, random_base64url};

pub(crate) async fn request_context_middleware_impl(
    State(st): State<AppState>,
    req: Request,
    next: Next,
) -> Response {
    let started = std::time::Instant::now();
    let method = req.method().clone();
    let path = req.uri().path().to_string();
    let route_family = route_family(&path);
    let request_id = req
        .headers()
        .get(HEADER_REQUEST_ID)
        .and_then(|v| v.to_str().ok())
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| random_base64url(12));

    let span = tracing::info_span!(
        "http_request",
        request_id = %request_id,
        method = %method,
        path = %path,
        route = %route_family
    );

    let mut resp = next.run(req).instrument(span).await;
    if let Ok(hv) = HeaderValue::from_str(&request_id) {
        resp.headers_mut().insert(HEADER_REQUEST_ID, hv);
    }

    let latency_ms = started.elapsed().as_millis() as u64;
    st.api_metrics
        .observe_route_latency(route_family, latency_ms);

    tracing::info!(
        request_id = %request_id,
        method = %method,
        path = %path,
        route = %route_family,
        status = resp.status().as_u16(),
        latency_ms = latency_ms,
        "http request"
    );

    resp
}

fn route_family(path: &str) -> &'static str {
    if path == "/health" {
        "/health"
    } else if path == "/metrics" {
        "/metrics"
    } else if path.starts_with("/api/auth") {
        "/api/auth"
    } else if path.starts_with("/api/wallet") {
        "/api/wallet"
    } else if path.starts_with("/api/proxy") {
        "/api/proxy"
    } else if path.starts_with("/api/evm") {
        "/api/evm"
    } else if path.starts_with("/api/jupiter") {
        "/api/jupiter"
    } else if path.starts_with("/api/testnet-amm") {
        "/api/testnet-amm"
    } else if path.starts_with("/api/solana/amm") {
        "/api/solana/amm"
    } else if path.starts_with("/api") {
        "/api/other"
    } else {
        "/other"
    }
}

pub(crate) async fn rate_limit_middleware_impl(
    State(st): State<AppState>,
    req: Request,
    next: Next,
) -> Response {
    st.api_metrics
        .requests_total
        .fetch_add(1, Ordering::Relaxed);
    let path = req.uri().path().to_string();
    if should_skip_rate_limit(&path) {
        return next.run(req).await;
    }

    let now = chrono_ms_now();
    let client = rate_limit_client_key(req.headers());
    let key = format!("{}:{}", client, path);
    let block = st
        .rate_limits
        .consume(&key, now, RATE_LIMIT_WINDOW_MS, RATE_LIMIT_MAX_REQUESTS)
        .await;

    if block {
        return (
            StatusCode::TOO_MANY_REQUESTS,
            Json(serde_json::json!({ "error": "Too many requests" })),
        )
            .into_response();
    }

    next.run(req).await
}

pub(crate) fn should_skip_rate_limit(path: &str) -> bool {
    if path == "/health" || path == "/metrics" {
        return true;
    }
    path.ends_with("/status")
}

pub(crate) async fn consume_rate_limit_bucket(
    st: &AppState,
    key: &str,
    now: i64,
    window_ms: i64,
    max_requests: u32,
) -> bool {
    st.rate_limits
        .consume(key, now, window_ms, max_requests)
        .await
}

pub(crate) fn rate_limit_client_key(headers: &HeaderMap) -> String {
    if let Some(v) = headers
        .get("x-forwarded-for")
        .and_then(|h| h.to_str().ok())
        .and_then(|s| s.split(',').next())
        .map(|s| s.trim())
        .filter(|s| !s.is_empty())
    {
        return v.to_string();
    }
    if let Some(v) = headers
        .get("x-real-ip")
        .and_then(|h| h.to_str().ok())
        .map(|s| s.trim())
        .filter(|s| !s.is_empty())
    {
        return v.to_string();
    }
    "global".to_string()
}
