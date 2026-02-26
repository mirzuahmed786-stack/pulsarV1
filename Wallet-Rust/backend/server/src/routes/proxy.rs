use axum::{
    body::Bytes,
    extract::{Query, State},
    http::{HeaderMap, Method, StatusCode},
    response::IntoResponse,
    routing::{any, get},
    Json, Router,
};
use std::{collections::HashMap, sync::atomic::Ordering, sync::Arc, time::Duration};

use crate::app::{AppState, CacheEntry};

fn is_coingecko_degraded_error(msg: &str) -> bool {
    let m = msg.to_ascii_lowercase();
    m.starts_with("http 429")
        || m.contains("returned 429")
        || m.contains("returned 5")
        || m.contains("timed out")
        || m.contains("cooldown active")
}

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/history", get(proxy_history))
        .route("/prices", get(proxy_prices))
        .route("/rpc", any(proxy_rpc))
}

pub(crate) async fn proxy_history(
    State(st): State<AppState>,
    headers: HeaderMap,
    Query(params): Query<HashMap<String, String>>,
) -> impl IntoResponse {
    let request_id = crate::app::incoming_request_id(Some(&headers));
    let params = match crate::app::parse_strict_query::<crate::app::ProxyUrlQuery>(
        &st.api_metrics,
        "/api/proxy/history",
        params,
        Some(&headers),
    ) {
        Ok(v) => v,
        Err(resp) => return resp,
    };
    st.api_metrics
        .proxy_requests
        .fetch_add(1, Ordering::Relaxed);
    let raw_url = params.url;
    let url = match crate::app::validate_proxy_url(&st, &raw_url) {
        Ok(u) => u,
        Err(e) => {
            crate::app::inc_reject_reason_counter(&st.api_metrics, "proxy_url_blocked");
            return crate::app::api_error_response(
                StatusCode::BAD_REQUEST,
                "proxy_url_blocked",
                "Blocked URL",
                Some(e),
                request_id,
            );
        }
    };
    let is_coingecko_market_chart = url.host_str() == Some("api.coingecko.com")
        && url.path().to_ascii_lowercase().contains("/market_chart");

    match crate::app::cached_json_fetch(
        &st,
        crate::app::cache_key("history", &raw_url),
        Duration::from_secs(30),
        Some(Duration::from_secs(
            crate::app::PROXY_HISTORY_STALE_IF_ERROR_S,
        )),
        url,
        crate::app::proxy_timeout_for_type("history"),
    )
    .await
    {
        Ok((data, cache_outcome)) => {
            let mut response_headers = HeaderMap::new();
            response_headers.insert(
                "X-Cache",
                axum::http::HeaderValue::from_str(cache_outcome.as_header_value()).unwrap(),
            );
            response_headers.insert(
                crate::app::HEADER_BACKEND_CACHE,
                cache_outcome.as_header_value().parse().unwrap(),
            );
            (response_headers, Json(data)).into_response()
        }
        Err(e) => {
            st.api_metrics.record_upstream_error("proxy_history");
            if is_coingecko_market_chart && is_coingecko_degraded_error(&e.message) {
                tracing::info!(error = %e.message, from_cache = e.from_cache, "proxy history degraded upstream");
            } else {
                tracing::warn!(error = %e.message, from_cache = e.from_cache, "proxy history failed");
            }
            let mut response_headers = HeaderMap::new();
            let cache = if e.from_cache { "HIT" } else { "MISS" };
            response_headers.insert(crate::app::HEADER_BACKEND_CACHE, cache.parse().unwrap());

            if is_coingecko_market_chart && is_coingecko_degraded_error(&e.message) {
                let fallback = serde_json::json!({
                    "prices": [],
                    "market_caps": [],
                    "total_volumes": []
                });
                let now_for_insert = crate::app::chrono_ms_now();
                st.proxy_cache
                    .put(
                        crate::app::cache_key("history", &raw_url),
                        CacheEntry {
                            value: fallback.clone(),
                            expires_at_ms: now_for_insert
                                + Duration::from_secs(
                                    crate::app::PROXY_HISTORY_RATE_LIMIT_FALLBACK_S,
                                )
                                .as_millis() as i64,
                            is_error: false,
                        },
                    )
                    .await;
                response_headers.insert(
                    crate::app::HEADER_BACKEND_CACHE,
                    axum::http::HeaderValue::from_static("FALLBACK"),
                );
                return (StatusCode::OK, response_headers, Json(fallback)).into_response();
            }

            if crate::app::should_expose_proxy_error_details(&st) {
                (
                    crate::app::status_from_proxy_error(&e.message),
                    response_headers,
                    Json(serde_json::json!({
                        "error": {
                            "code": "proxy_failed",
                            "message": "Proxy failed",
                            "details": e.message,
                            "requestId": request_id
                        }
                    })),
                )
                    .into_response()
            } else {
                (
                    crate::app::status_from_proxy_error(&e.message),
                    response_headers,
                    Json(serde_json::json!({
                        "error": {
                            "code": "proxy_failed",
                            "message": "Proxy failed",
                            "requestId": request_id
                        }
                    })),
                )
                    .into_response()
            }
        }
    }
}

pub(crate) async fn proxy_prices(
    State(st): State<AppState>,
    headers: HeaderMap,
    Query(params): Query<HashMap<String, String>>,
) -> impl IntoResponse {
    let request_id = crate::app::incoming_request_id(Some(&headers));
    let params = match crate::app::parse_strict_query::<crate::app::ProxyUrlQuery>(
        &st.api_metrics,
        "/api/proxy/prices",
        params,
        Some(&headers),
    ) {
        Ok(v) => v,
        Err(resp) => return resp,
    };
    st.api_metrics
        .proxy_requests
        .fetch_add(1, Ordering::Relaxed);
    let raw_url = params.url;
    let url = match crate::app::validate_proxy_url(&st, &raw_url) {
        Ok(u) => u,
        Err(e) => {
            crate::app::inc_reject_reason_counter(&st.api_metrics, "proxy_url_blocked");
            return crate::app::api_error_response(
                StatusCode::BAD_REQUEST,
                "proxy_url_blocked",
                "Blocked URL",
                Some(e),
                request_id,
            );
        }
    };
    let is_coingecko_api = url.host_str() == Some("api.coingecko.com");
    match crate::app::cached_json_fetch(
        &st,
        crate::app::cache_key("prices", &raw_url),
        Duration::from_secs(60),
        Some(Duration::from_secs(
            crate::app::PROXY_PRICES_STALE_IF_ERROR_S,
        )),
        url,
        crate::app::proxy_timeout_for_type("prices"),
    )
    .await
    {
        Ok((data, cache_outcome)) => {
            let mut response_headers = HeaderMap::new();
            response_headers.insert(
                "X-Cache",
                axum::http::HeaderValue::from_str(cache_outcome.as_header_value()).unwrap(),
            );
            response_headers.insert(
                crate::app::HEADER_BACKEND_CACHE,
                cache_outcome.as_header_value().parse().unwrap(),
            );
            (response_headers, Json(data)).into_response()
        }
        Err(e) => {
            st.api_metrics.record_upstream_error("proxy_prices");
            if is_coingecko_api && is_coingecko_degraded_error(&e.message) {
                tracing::info!(error = %e.message, from_cache = e.from_cache, "proxy prices degraded upstream");
            } else {
                tracing::warn!(error = %e.message, from_cache = e.from_cache, "proxy prices failed");
            }
            let mut response_headers = HeaderMap::new();
            let cache = if e.from_cache { "HIT" } else { "MISS" };
            response_headers.insert(crate::app::HEADER_BACKEND_CACHE, cache.parse().unwrap());
            if is_coingecko_api && is_coingecko_degraded_error(&e.message) {
                let fallback = serde_json::json!({});
                let now_for_insert = crate::app::chrono_ms_now();
                st.proxy_cache
                    .put(
                        crate::app::cache_key("prices", &raw_url),
                        CacheEntry {
                            value: fallback.clone(),
                            expires_at_ms: now_for_insert
                                + Duration::from_secs(crate::app::PROXY_PRICES_STALE_IF_ERROR_S)
                                    .as_millis() as i64,
                            is_error: false,
                        },
                    )
                    .await;
                response_headers.insert(
                    crate::app::HEADER_BACKEND_CACHE,
                    axum::http::HeaderValue::from_static("FALLBACK"),
                );
                return (StatusCode::OK, response_headers, Json(fallback)).into_response();
            }

            if crate::app::should_expose_proxy_error_details(&st) {
                (
                    crate::app::status_from_proxy_error(&e.message),
                    response_headers,
                    Json(serde_json::json!({
                        "error": {
                            "code": "price_proxy_failed",
                            "message": "Price proxy failed",
                            "details": e.message,
                            "requestId": request_id
                        }
                    })),
                )
                    .into_response()
            } else {
                (
                    crate::app::status_from_proxy_error(&e.message),
                    response_headers,
                    Json(serde_json::json!({
                        "error": {
                            "code": "price_proxy_failed",
                            "message": "Price proxy failed",
                            "requestId": request_id
                        }
                    })),
                )
                    .into_response()
            }
        }
    }
}

pub(crate) async fn proxy_rpc(
    State(st): State<AppState>,
    method: Method,
    headers: HeaderMap,
    Query(params): Query<HashMap<String, String>>,
    body: Bytes,
) -> impl IntoResponse {
    let request_id = crate::app::incoming_request_id(Some(&headers));
    st.api_metrics.rpc_requests.fetch_add(1, Ordering::Relaxed);
    let params = match crate::app::parse_strict_query::<crate::app::ProxyUrlQuery>(
        &st.api_metrics,
        "/api/proxy/rpc",
        params,
        Some(&headers),
    ) {
        Ok(v) => v,
        Err(resp) => return resp,
    };
    let raw_url = params.url;
    // Some clients may accidentally pass a CSV list in `url`.
    // Use the first candidate to preserve backward compatibility.
    let raw_url = raw_url
        .split(',')
        .map(|s| s.trim())
        .find(|s| !s.is_empty())
        .unwrap_or("");
    let upstream_host_hint = url::Url::parse(&raw_url)
        .ok()
        .and_then(|u| u.host_str().map(|h| h.to_lowercase()))
        .unwrap_or_else(|| "unknown".to_string());
    let url = match crate::app::validate_rpc_url(&st, &raw_url) {
        Ok(u) => u,
        Err(e) => {
            st.api_metrics
                .rpc_host_blocked
                .fetch_add(1, Ordering::Relaxed);
            crate::app::inc_reject_reason_counter(&st.api_metrics, "blocked_rpc_url");
            tracing::warn!(
                rpc_method = "unknown",
                chain_id = "unknown",
                upstream_host = %upstream_host_hint,
                reject_reason = %format!("blocked_rpc_url: {e}"),
                "rpc proxy request rejected"
            );
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({
                    "error": {
                        "code": "blocked_rpc_url",
                        "message": "Blocked rpcUrl",
                        "details": e,
                        "requestId": request_id
                    }
                })),
            )
                .into_response();
        }
    };

    if method != Method::POST {
        crate::app::inc_reject_reason_counter(&st.api_metrics, "method_not_allowed");
        tracing::warn!(
            rpc_method = "unknown",
            chain_id = "unknown",
            upstream_host = %upstream_host_hint,
            reject_reason = "method_not_allowed",
            "rpc proxy request rejected"
        );
        return (
            StatusCode::METHOD_NOT_ALLOWED,
            Json(serde_json::json!({
                "error": {
                    "code": "method_not_allowed",
                    "message": "RPC proxy only accepts POST",
                    "requestId": request_id
                }
            })),
        )
            .into_response();
    }
    if body.len() > st.cfg.rpc_max_request_body_bytes {
        st.api_metrics
            .rpc_payload_invalid
            .fetch_add(1, Ordering::Relaxed);
        crate::app::inc_reject_reason_counter(&st.api_metrics, "rpc_payload_too_large");
        tracing::warn!(
            rpc_method = "unknown",
            chain_id = "unknown",
            upstream_host = %upstream_host_hint,
            reject_reason = "payload_too_large",
            "rpc proxy request rejected"
        );
        return (
            StatusCode::PAYLOAD_TOO_LARGE,
            Json(serde_json::json!({
                "error": {
                    "code": "rpc_payload_too_large",
                    "message": format!("RPC payload too large (max {} bytes)", st.cfg.rpc_max_request_body_bytes),
                    "requestId": request_id
                }
            })),
        )
            .into_response();
    }

    let now = crate::app::chrono_ms_now();
    let client = crate::app::rate_limit_client_key(&headers);
    let payload = match serde_json::from_slice::<serde_json::Value>(&body) {
        Ok(v) => v,
        Err(_) => {
            st.api_metrics
                .rpc_payload_invalid
                .fetch_add(1, Ordering::Relaxed);
            crate::app::inc_reject_reason_counter(&st.api_metrics, "invalid_jsonrpc_payload");
            tracing::warn!(
                rpc_method = "unknown",
                chain_id = "unknown",
                upstream_host = %upstream_host_hint,
                reject_reason = "invalid_jsonrpc_payload",
                "rpc proxy request rejected"
            );
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({
                    "error": {
                        "code": "invalid_jsonrpc_payload",
                        "message": "Invalid JSON-RPC payload",
                        "requestId": request_id
                    }
                })),
            )
                .into_response();
        }
    };

    if payload.is_array() {
        let items = payload.as_array().cloned().unwrap_or_default();
        let len = items.len();
        if len == 0 {
            crate::app::inc_reject_reason_counter(&st.api_metrics, "empty_rpc_batch");
            tracing::warn!(
                rpc_method = "batch",
                chain_id = "unknown",
                upstream_host = %upstream_host_hint,
                reject_reason = "empty_batch",
                "rpc proxy request rejected"
            );
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({
                    "error": {
                        "code": "empty_rpc_batch",
                        "message": "Empty RPC batch is not allowed",
                        "requestId": request_id
                    }
                })),
            )
                .into_response();
        }
        if len > st.cfg.rpc_max_batch_items {
            st.api_metrics
                .rpc_payload_invalid
                .fetch_add(1, Ordering::Relaxed);
            crate::app::inc_reject_reason_counter(&st.api_metrics, "rpc_batch_too_large");
            tracing::warn!(
                rpc_method = "batch",
                chain_id = "unknown",
                upstream_host = %upstream_host_hint,
                reject_reason = "batch_too_large",
                "rpc proxy request rejected"
            );
            return (
                StatusCode::PAYLOAD_TOO_LARGE,
                Json(serde_json::json!({
                    "error": {
                        "code": "rpc_batch_too_large",
                        "message": format!("RPC batch too large (max {} items)", st.cfg.rpc_max_batch_items),
                        "requestId": request_id
                    }
                })),
            )
                .into_response();
        }

        let semaphore = Arc::new(tokio::sync::Semaphore::new(
            crate::app::RPC_BATCH_CONCURRENCY_LIMIT,
        ));
        let mut tasks = Vec::with_capacity(len);
        let mut out = vec![serde_json::Value::Null; len];
        for (idx, item) in items.into_iter().enumerate() {
            let item_method = item
                .get("method")
                .and_then(|v| v.as_str())
                .unwrap_or("unknown");
            let item_chain_id = crate::app::extract_chain_id_hint(&item);
            if let Err(err_msg) = crate::app::validate_rpc_payload(&item) {
                if err_msg.starts_with("RPC method not allowed") {
                    st.api_metrics
                        .rpc_method_blocked
                        .fetch_add(1, Ordering::Relaxed);
                } else {
                    st.api_metrics
                        .rpc_payload_invalid
                        .fetch_add(1, Ordering::Relaxed);
                }
                crate::app::inc_reject_reason_counter(&st.api_metrics, "invalid_jsonrpc_request");
                tracing::warn!(
                    rpc_method = %item_method,
                    chain_id = %item_chain_id,
                    upstream_host = %upstream_host_hint,
                    reject_reason = %err_msg,
                    "rpc proxy request rejected"
                );
                out[idx] = crate::app::rpc_error_envelope(&item, -32601, &err_msg);
                continue;
            }
            let method_name = item
                .get("method")
                .and_then(|v| v.as_str())
                .unwrap_or("unknown");
            let method_key = format!("{}:/api/proxy/rpc:{}", client, method_name);
            if crate::app::consume_rate_limit_bucket(
                &st,
                &method_key,
                now,
                st.cfg.rpc_method_rate_limit_window_ms,
                st.cfg.rpc_method_rate_limit_max_requests,
            )
            .await
            {
                st.api_metrics
                    .rpc_method_rate_limited_batch
                    .fetch_add(1, Ordering::Relaxed);
                crate::app::inc_reject_reason_counter(
                    &st.api_metrics,
                    "rpc_method_rate_limited_batch",
                );
                tracing::warn!(
                    rpc_method = %method_name,
                    chain_id = %item_chain_id,
                    upstream_host = %upstream_host_hint,
                    reject_reason = "method_rate_limited_batch",
                    "rpc proxy request rejected"
                );
                out[idx] = crate::app::rpc_error_envelope(
                    &item,
                    -32005,
                    "RPC method rate limit exceeded for this client",
                );
                continue;
            }

            let st_cloned = st.clone();
            let url_cloned = url.clone();
            let sem = semaphore.clone();
            tasks.push(tokio::spawn(async move {
                let _permit = match sem.acquire_owned().await {
                    Ok(p) => p,
                    Err(_) => {
                        return (
                            idx,
                            serde_json::json!({
                                "jsonrpc": item.get("jsonrpc").and_then(|v| v.as_str()).unwrap_or("2.0"),
                                "id": item.get("id").cloned().unwrap_or(serde_json::Value::Null),
                                "error": { "code": -32000, "message": "RPC batch scheduler unavailable" }
                            }),
                        );
                    }
                };
                let value = match crate::app::rpc_call_with_cache(&st_cloned, &url_cloned, item.clone()).await {
                    Ok((v, _)) => v,
                    Err(e) => serde_json::json!({
                        "jsonrpc": item.get("jsonrpc").and_then(|v| v.as_str()).unwrap_or("2.0"),
                        "id": item.get("id").cloned().unwrap_or(serde_json::Value::Null),
                        "error": { "code": -32000, "message": e }
                    }),
                };
                (idx, value)
            }));
        }
        for task in tasks {
            if let Ok((idx, value)) = task.await {
                out[idx] = value;
            }
        }
        return (StatusCode::OK, Json(serde_json::Value::Array(out))).into_response();
    }

    if let Err(err_msg) = crate::app::validate_rpc_payload(&payload) {
        let method_name = payload
            .get("method")
            .and_then(|v| v.as_str())
            .unwrap_or("unknown");
        let chain_id = crate::app::extract_chain_id_hint(&payload);
        if err_msg.starts_with("RPC method not allowed") {
            st.api_metrics
                .rpc_method_blocked
                .fetch_add(1, Ordering::Relaxed);
        } else {
            st.api_metrics
                .rpc_payload_invalid
                .fetch_add(1, Ordering::Relaxed);
        }
        crate::app::inc_reject_reason_counter(&st.api_metrics, "invalid_jsonrpc_request");
        tracing::warn!(
            rpc_method = %method_name,
            chain_id = %chain_id,
            upstream_host = %upstream_host_hint,
            reject_reason = %err_msg,
            "rpc proxy request rejected"
        );
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({
                "error": {
                    "code": "invalid_jsonrpc_request",
                    "message": err_msg,
                    "requestId": request_id
                }
            })),
        )
            .into_response();
    }

    let method_name = payload
        .get("method")
        .and_then(|v| v.as_str())
        .unwrap_or("unknown")
        .to_string();
    let chain_id_hint = crate::app::extract_chain_id_hint(&payload);
    let rpc_id = payload
        .get("id")
        .cloned()
        .unwrap_or(serde_json::Value::Null);
    let method_key = format!("{}:/api/proxy/rpc:{}", client, method_name);
    if crate::app::consume_rate_limit_bucket(
        &st,
        &method_key,
        now,
        st.cfg.rpc_method_rate_limit_window_ms,
        st.cfg.rpc_method_rate_limit_max_requests,
    )
    .await
    {
        st.api_metrics
            .rpc_method_rate_limited_single
            .fetch_add(1, Ordering::Relaxed);
        crate::app::inc_reject_reason_counter(&st.api_metrics, "rpc_method_rate_limited_single");
        tracing::warn!(
            rpc_method = %method_name,
            chain_id = %chain_id_hint,
            upstream_host = %upstream_host_hint,
            reject_reason = "method_rate_limited_single",
            "rpc proxy request rejected"
        );
        return (
            StatusCode::TOO_MANY_REQUESTS,
            Json(serde_json::json!({
                "error": {
                    "code": "rpc_method_rate_limited_single",
                    "message": "RPC method rate limit exceeded for this client",
                    "requestId": request_id
                }
            })),
        )
            .into_response();
    }

    match crate::app::rpc_call_with_cache(&st, &url, payload).await {
        Ok((v, cache_outcome)) => {
            let mut response_headers = HeaderMap::new();
            response_headers.insert(
                crate::app::HEADER_BACKEND_CACHE,
                cache_outcome.as_header_value().parse().unwrap(),
            );
            (StatusCode::OK, response_headers, Json(v)).into_response()
        }
        Err(e) => {
            st.api_metrics.record_upstream_error("rpc_proxy");
            tracing::warn!(
                rpc_method = %method_name,
                chain_id = %chain_id_hint,
                upstream_host = %upstream_host_hint,
                reject_reason = %e,
                "rpc proxy upstream call failed"
            );
            let details = if crate::app::should_expose_proxy_error_details(&st) {
                e.clone()
            } else {
                "upstream request failed".to_string()
            };
            let body = crate::app::rpc_error_envelope(
                &serde_json::json!({
                    "jsonrpc": "2.0",
                    "id": rpc_id
                }),
                -32000,
                &format!("RPC upstream request failed: {}", details),
            );
            (StatusCode::OK, Json(body)).into_response()
        }
    }
}
