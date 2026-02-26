use axum::{
    extract::{rejection::JsonRejection, State},
    http::{HeaderMap, StatusCode},
    response::IntoResponse,
    routing::{get, post},
    Json, Router,
};
use std::{sync::Arc, time::Duration};

use crate::app::{AppState, BATCH_CONCURRENCY_LIMIT};

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/deployed", get(api_deployed_compat))
        .route("/batch", post(api_batch))
}

pub(crate) async fn api_batch(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<crate::app::BatchEnvelope>, JsonRejection>,
) -> impl IntoResponse {
    let Json(input) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/batch",
                rejection,
                Some(&headers),
            )
        }
    };
    st.api_metrics
        .batch_requests
        .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
    if input.requests.is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Invalid batch request format" })),
        )
            .into_response();
    }
    if input.requests.len() > 20 {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Maximum 20 requests per batch" })),
        )
            .into_response();
    }

    let req_len = input.requests.len();
    let semaphore = Arc::new(tokio::sync::Semaphore::new(BATCH_CONCURRENCY_LIMIT));
    let mut tasks = Vec::with_capacity(req_len);
    for (idx, req) in input.requests.into_iter().enumerate() {
        let st_cloned = st.clone();
        let sem = semaphore.clone();
        tasks.push(tokio::spawn(async move {
            let _permit = match sem.acquire_owned().await {
                Ok(p) => p,
                Err(_) => {
                    return (
                        idx,
                        serde_json::json!({ "success": false, "error": "Batch scheduler unavailable" }),
                    )
                }
            };
            let response = match crate::app::validate_proxy_url(&st_cloned, &req.url) {
                Ok(url) => match crate::app::cached_json_fetch(
                    &st_cloned,
                    crate::app::cache_key(&req.r#type, &req.url),
                    Duration::from_secs(req.ttl),
                    None,
                    url,
                    crate::app::proxy_timeout_for_type(&req.r#type),
                )
                .await
                {
                    Ok((data, cache_outcome)) => serde_json::json!({
                        "success": true,
                        "data": data,
                        "fromCache": matches!(cache_outcome, crate::app::CacheOutcome::Hit | crate::app::CacheOutcome::Stale)
                    }),
                    Err(e) => serde_json::json!({ "success": false, "error": e.message }),
                },
                Err(e) => {
                    serde_json::json!({ "success": false, "error": format!("Blocked URL: {}", e) })
                }
            };
            (idx, response)
        }));
    }

    let mut out = vec![serde_json::Value::Null; req_len];
    for task in tasks {
        if let Ok((idx, value)) = task.await {
            out[idx] = value;
        }
    }
    (StatusCode::OK, Json(serde_json::json!({ "results": out }))).into_response()
}

pub(crate) async fn api_deployed_compat() -> impl IntoResponse {
    // Back-compat endpoint used by older frontend address refresh code.
    // We return the first available deployed contract map sanitized to known keys.
    let candidates = [
        "protocol_contracts_sepolia.json",
        "protocol_contracts_amoy.json",
        "protocol_contracts_bsc.json",
        "protocol_contracts_fuji.json",
        "deployed_testnet_amm_sepolia.json",
        "deployed_testnet_amm_amoy.json",
        "deployed_testnet_amm_bsc.json",
        "deployed_testnet_amm_fuji.json",
    ];
    for file in candidates {
        let path = crate::app::backend_state_file(file);
        if !path.exists() {
            continue;
        }
        if let Some(v) = std::fs::read_to_string(&path)
            .ok()
            .and_then(|s| serde_json::from_str::<serde_json::Value>(&s).ok())
        {
            let sanitized = crate::app::sanitize_deployed_contracts(&v);
            if sanitized.as_object().is_some_and(|o| !o.is_empty()) {
                return (StatusCode::OK, Json(sanitized)).into_response();
            }
        }
    }
    (StatusCode::OK, Json(serde_json::json!({}))).into_response()
}
