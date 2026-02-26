use axum::{
    extract::State,
    http::{HeaderMap, HeaderValue, StatusCode},
    response::IntoResponse,
    Json,
};
use std::{
    sync::atomic::Ordering,
    time::{Duration, SystemTime},
};

use crate::app::{backend_state_file, chrono_ms_now, AppState};

pub(crate) async fn health(State(st): State<AppState>) -> impl IntoResponse {
    let now = SystemTime::now()
        .duration_since(SystemTime::UNIX_EPOCH)
        .unwrap_or(Duration::from_secs(0))
        .as_secs();
    let solana_state = st.solana_runtime.read().await.clone();
    let solana_cfg_exists = backend_state_file("solana_amm_testnet.json").exists();
    let solana_deployer_set = std::env::var("SOLANA_DEPLOYER_KEYPAIR")
        .ok()
        .map(|s| !s.trim().is_empty())
        .unwrap_or(false);
    let solana_program_set = std::env::var("SOLANA_AMM_PROGRAM_ID")
        .ok()
        .map(|s| !s.trim().is_empty())
        .unwrap_or(false);

    let proxy_entries = st.proxy_cache.len().await;
    let rpc_entries = st.rpc_cache.len().await;

    let mut redis_health = serde_json::json!({
        "enabled": st.redis_client.is_some(),
        "healthy": false
    });
    if let Some(client) = &st.redis_client {
        let started = std::time::Instant::now();
        let ping_result = tokio::time::timeout(
            Duration::from_millis(st.cfg.infra_redis_command_timeout_ms),
            async {
                let mut conn = client.get_multiplexed_async_connection().await?;
                redis::cmd("PING").query_async::<String>(&mut conn).await
            },
        )
        .await;
        match ping_result {
            Ok(Ok(_)) => {
                let latency_ms = started.elapsed().as_millis() as u64;
                st.infra_metrics
                    .redis_ping_ok
                    .fetch_add(1, Ordering::Relaxed);
                st.infra_metrics
                    .redis_ping_latency_sum_ms
                    .fetch_add(latency_ms, Ordering::Relaxed);
                st.infra_metrics
                    .redis_ping_latency_samples
                    .fetch_add(1, Ordering::Relaxed);
                st.infra_metrics
                    .redis_last_ping_ok_ms
                    .store(chrono_ms_now(), Ordering::Relaxed);
                redis_health = serde_json::json!({
                    "enabled": true,
                    "healthy": true,
                    "latencyMs": latency_ms
                });
            }
            Ok(Err(err)) => {
                st.infra_metrics
                    .redis_ping_err
                    .fetch_add(1, Ordering::Relaxed);
                redis_health = serde_json::json!({
                    "enabled": true,
                    "healthy": false,
                    "error": err.to_string()
                });
            }
            Err(_) => {
                st.infra_metrics
                    .redis_ping_err
                    .fetch_add(1, Ordering::Relaxed);
                redis_health = serde_json::json!({
                    "enabled": true,
                    "healthy": false,
                    "error": "redis ping timeout"
                });
            }
        }
    }
    let redis_samples = st
        .infra_metrics
        .redis_ping_latency_samples
        .load(Ordering::Relaxed);
    let redis_avg_ms = if redis_samples == 0 {
        0
    } else {
        st.infra_metrics
            .redis_ping_latency_sum_ms
            .load(Ordering::Relaxed)
            / redis_samples
    };

    Json(serde_json::json!({
        "status": "healthy",
        "timestamp": now,
        "uptime": st.started_at.elapsed().as_secs_f64(),
        "infra": {
            "backendMode": st.infra_backend_mode.clone(),
            "redis": redis_health,
            "redisPingOk": st.infra_metrics.redis_ping_ok.load(Ordering::Relaxed),
            "redisPingErr": st.infra_metrics.redis_ping_err.load(Ordering::Relaxed),
            "redisAvgPingMs": redis_avg_ms,
            "redisLastPingOkMs": st.infra_metrics.redis_last_ping_ok_ms.load(Ordering::Relaxed)
        },
        "cache": {
            "proxy_entries": proxy_entries,
            "rpc_entries": rpc_entries,
        },
        "apiMetrics": {
            "requestsTotal": st.api_metrics.requests_total.load(Ordering::Relaxed),
            "batchRequests": st.api_metrics.batch_requests.load(Ordering::Relaxed),
            "proxyRequests": st.api_metrics.proxy_requests.load(Ordering::Relaxed),
            "proxyCacheHit": st.api_metrics.proxy_cache_hit.load(Ordering::Relaxed),
            "proxyCacheMiss": st.api_metrics.proxy_cache_miss.load(Ordering::Relaxed),
            "proxyErrorCacheHit": st.api_metrics.proxy_error_cache_hit.load(Ordering::Relaxed),
            "rpcRequests": st.api_metrics.rpc_requests.load(Ordering::Relaxed),
            "rpcCacheHit": st.api_metrics.rpc_cache_hit.load(Ordering::Relaxed),
            "rpcCacheMiss": st.api_metrics.rpc_cache_miss.load(Ordering::Relaxed),
            "rpcCooldownHits": st.api_metrics.rpc_cooldown_hits.load(Ordering::Relaxed),
            "rpcMethodBlocked": st.api_metrics.rpc_method_blocked.load(Ordering::Relaxed),
            "rpcPayloadInvalid": st.api_metrics.rpc_payload_invalid.load(Ordering::Relaxed),
            "rpcHostBlocked": st.api_metrics.rpc_host_blocked.load(Ordering::Relaxed),
            "rpcMethodRateLimitedSingle": st.api_metrics.rpc_method_rate_limited_single.load(Ordering::Relaxed),
            "rpcMethodRateLimitedBatch": st.api_metrics.rpc_method_rate_limited_batch.load(Ordering::Relaxed),
            "rejectReasons": {
                "invalidQuery": st.api_metrics.reject_invalid_query.load(Ordering::Relaxed),
                "invalidJsonBody": st.api_metrics.reject_invalid_json_body.load(Ordering::Relaxed),
                "validationFailed": st.api_metrics.reject_validation_failed.load(Ordering::Relaxed),
                "proxyUrlBlocked": st.api_metrics.reject_proxy_url_blocked.load(Ordering::Relaxed),
                "blockedRpcUrl": st.api_metrics.reject_blocked_rpc_url.load(Ordering::Relaxed),
                "methodNotAllowed": st.api_metrics.reject_method_not_allowed.load(Ordering::Relaxed),
                "rpcPayloadTooLarge": st.api_metrics.reject_rpc_payload_too_large.load(Ordering::Relaxed),
                "invalidJsonRpcPayload": st.api_metrics.reject_invalid_jsonrpc_payload.load(Ordering::Relaxed),
                "emptyRpcBatch": st.api_metrics.reject_empty_rpc_batch.load(Ordering::Relaxed),
                "rpcBatchTooLarge": st.api_metrics.reject_rpc_batch_too_large.load(Ordering::Relaxed),
                "invalidJsonRpcRequest": st.api_metrics.reject_invalid_jsonrpc_request.load(Ordering::Relaxed),
                "rpcMethodRateLimitedSingle": st.api_metrics.reject_rpc_method_rate_limited_single.load(Ordering::Relaxed),
                "rpcMethodRateLimitedBatch": st.api_metrics.reject_rpc_method_rate_limited_batch.load(Ordering::Relaxed),
                "cloudBlobReadRateLimited": st.api_metrics.reject_cloud_blob_read_rate_limited.load(Ordering::Relaxed),
                "cloudBlobWriteRateLimited": st.api_metrics.reject_cloud_blob_write_rate_limited.load(Ordering::Relaxed),
                "cloudBlobValidationFailed": st.api_metrics.reject_cloud_blob_validation_failed.load(Ordering::Relaxed)
            },
            "cloudBlobReadRateLimited": st.api_metrics.cloud_blob_read_rate_limited.load(Ordering::Relaxed),
            "cloudBlobWriteRateLimited": st.api_metrics.cloud_blob_write_rate_limited.load(Ordering::Relaxed)
        },
        "solana": {
            "configLoaded": solana_cfg_exists,
            "deployerKeypairSet": solana_deployer_set,
            "programIdSet": solana_program_set,
            "adminStrictMode": st.cfg.solana_admin_strict,
            "metrics": {
                "swapTxOk": st.solana_metrics.swap_tx_ok.load(Ordering::Relaxed),
                "swapTxErr": st.solana_metrics.swap_tx_err.load(Ordering::Relaxed),
                "mintOk": st.solana_metrics.mint_ok.load(Ordering::Relaxed),
                "mintErr": st.solana_metrics.mint_err.load(Ordering::Relaxed),
                "initOk": st.solana_metrics.init_ok.load(Ordering::Relaxed),
                "initErr": st.solana_metrics.init_err.load(Ordering::Relaxed)
            },
            "lastInit": solana_state
        }
    }))
}

pub(crate) async fn metrics(State(st): State<AppState>) -> impl IntoResponse {
    let mut body = format!(
        concat!(
            "# HELP elementa_rpc_method_rate_limited_single_total Number of single JSON-RPC requests rejected by method-rate limiting.\n",
            "# TYPE elementa_rpc_method_rate_limited_single_total counter\n",
            "elementa_rpc_method_rate_limited_single_total {}\n",
            "# HELP elementa_rpc_method_rate_limited_batch_total Number of JSON-RPC batch items rejected by method-rate limiting.\n",
            "# TYPE elementa_rpc_method_rate_limited_batch_total counter\n",
            "elementa_rpc_method_rate_limited_batch_total {}\n"
        ),
        st.api_metrics.rpc_method_rate_limited_single.load(Ordering::Relaxed),
        st.api_metrics.rpc_method_rate_limited_batch.load(Ordering::Relaxed)
    );

    body.push_str("# HELP elementa_cache_entries Current cache entry counts by cache type.\n");
    body.push_str("# TYPE elementa_cache_entries gauge\n");
    body.push_str(&format!(
        "elementa_cache_entries{{cache=\"proxy\"}} {}\n",
        st.proxy_cache.len().await
    ));
    body.push_str(&format!(
        "elementa_cache_entries{{cache=\"rpc\"}} {}\n",
        st.rpc_cache.len().await
    ));

    body.push_str("# HELP elementa_infra_backend_info Selected infra backend mode.\n");
    body.push_str("# TYPE elementa_infra_backend_info gauge\n");
    body.push_str(&format!(
        "elementa_infra_backend_info{{backend=\"{}\"}} 1\n",
        st.infra_backend_mode
    ));
    body.push_str("# HELP elementa_redis_ping_total Redis health ping outcomes.\n");
    body.push_str("# TYPE elementa_redis_ping_total counter\n");
    body.push_str(&format!(
        "elementa_redis_ping_total{{result=\"ok\"}} {}\n",
        st.infra_metrics.redis_ping_ok.load(Ordering::Relaxed)
    ));
    body.push_str(&format!(
        "elementa_redis_ping_total{{result=\"error\"}} {}\n",
        st.infra_metrics.redis_ping_err.load(Ordering::Relaxed)
    ));
    body.push_str(
        "# HELP elementa_redis_ping_latency_ms_avg Average Redis ping latency in milliseconds.\n",
    );
    body.push_str("# TYPE elementa_redis_ping_latency_ms_avg gauge\n");
    let ping_samples = st
        .infra_metrics
        .redis_ping_latency_samples
        .load(Ordering::Relaxed);
    let ping_avg = if ping_samples == 0 {
        0
    } else {
        st.infra_metrics
            .redis_ping_latency_sum_ms
            .load(Ordering::Relaxed)
            / ping_samples
    };
    body.push_str(&format!(
        "elementa_redis_ping_latency_ms_avg {}\n",
        ping_avg
    ));

    body.push_str(
        "# HELP elementa_upstream_error_total Number of upstream call failures by source.\n",
    );
    body.push_str("# TYPE elementa_upstream_error_total counter\n");
    body.push_str(&format!(
        "elementa_upstream_error_total{{source=\"all\"}} {}\n",
        st.api_metrics.upstream_error_total.load(Ordering::Relaxed)
    ));
    let mut upstream_sources: Vec<(String, u64)> = st
        .api_metrics
        .upstream_error_by_source
        .iter()
        .map(|entry| (entry.key().clone(), entry.value().load(Ordering::Relaxed)))
        .collect();
    upstream_sources.sort_by(|a, b| a.0.cmp(&b.0));
    for (source, count) in upstream_sources {
        body.push_str(&format!(
            "elementa_upstream_error_total{{source=\"{}\"}} {}\n",
            source, count
        ));
    }

    body.push_str(
        "# HELP elementa_http_request_latency_ms Route latency histogram in milliseconds.\n",
    );
    body.push_str("# TYPE elementa_http_request_latency_ms histogram\n");
    let mut latency_routes: Vec<_> = st
        .api_metrics
        .route_latency_histograms
        .iter()
        .map(|entry| {
            let route = entry.key().clone();
            let value = entry.value();
            (
                route,
                value.bucket_le_25_ms.load(Ordering::Relaxed),
                value.bucket_le_50_ms.load(Ordering::Relaxed),
                value.bucket_le_100_ms.load(Ordering::Relaxed),
                value.bucket_le_250_ms.load(Ordering::Relaxed),
                value.bucket_le_500_ms.load(Ordering::Relaxed),
                value.bucket_le_1000_ms.load(Ordering::Relaxed),
                value.bucket_le_2500_ms.load(Ordering::Relaxed),
                value.bucket_le_5000_ms.load(Ordering::Relaxed),
                value.bucket_inf.load(Ordering::Relaxed),
                value.sum_ms.load(Ordering::Relaxed),
                value.count.load(Ordering::Relaxed),
            )
        })
        .collect();
    latency_routes.sort_by(|a, b| a.0.cmp(&b.0));
    for (
        route,
        le_25,
        le_50,
        le_100,
        le_250,
        le_500,
        le_1000,
        le_2500,
        le_5000,
        le_inf,
        sum,
        count,
    ) in latency_routes
    {
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"25\"}} {}\n",
            route, le_25
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"50\"}} {}\n",
            route, le_50
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"100\"}} {}\n",
            route, le_100
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"250\"}} {}\n",
            route, le_250
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"500\"}} {}\n",
            route, le_500
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"1000\"}} {}\n",
            route, le_1000
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"2500\"}} {}\n",
            route, le_2500
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"5000\"}} {}\n",
            route, le_5000
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_bucket{{route=\"{}\",le=\"+Inf\"}} {}\n",
            route, le_inf
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_sum{{route=\"{}\"}} {}\n",
            route, sum
        ));
        body.push_str(&format!(
            "elementa_http_request_latency_ms_count{{route=\"{}\"}} {}\n",
            route, count
        ));
    }

    body.push_str(
        "# HELP elementa_api_reject_reason_total Number of API requests rejected by reason.\n",
    );
    body.push_str("# TYPE elementa_api_reject_reason_total counter\n");

    let reject_reason_metrics = [
        (
            "invalid_query",
            st.api_metrics.reject_invalid_query.load(Ordering::Relaxed),
        ),
        (
            "invalid_json_body",
            st.api_metrics
                .reject_invalid_json_body
                .load(Ordering::Relaxed),
        ),
        (
            "validation_failed",
            st.api_metrics
                .reject_validation_failed
                .load(Ordering::Relaxed),
        ),
        (
            "proxy_url_blocked",
            st.api_metrics
                .reject_proxy_url_blocked
                .load(Ordering::Relaxed),
        ),
        (
            "blocked_rpc_url",
            st.api_metrics
                .reject_blocked_rpc_url
                .load(Ordering::Relaxed),
        ),
        (
            "method_not_allowed",
            st.api_metrics
                .reject_method_not_allowed
                .load(Ordering::Relaxed),
        ),
        (
            "rpc_payload_too_large",
            st.api_metrics
                .reject_rpc_payload_too_large
                .load(Ordering::Relaxed),
        ),
        (
            "invalid_jsonrpc_payload",
            st.api_metrics
                .reject_invalid_jsonrpc_payload
                .load(Ordering::Relaxed),
        ),
        (
            "empty_rpc_batch",
            st.api_metrics
                .reject_empty_rpc_batch
                .load(Ordering::Relaxed),
        ),
        (
            "rpc_batch_too_large",
            st.api_metrics
                .reject_rpc_batch_too_large
                .load(Ordering::Relaxed),
        ),
        (
            "invalid_jsonrpc_request",
            st.api_metrics
                .reject_invalid_jsonrpc_request
                .load(Ordering::Relaxed),
        ),
        (
            "rpc_method_rate_limited_single",
            st.api_metrics
                .reject_rpc_method_rate_limited_single
                .load(Ordering::Relaxed),
        ),
        (
            "rpc_method_rate_limited_batch",
            st.api_metrics
                .reject_rpc_method_rate_limited_batch
                .load(Ordering::Relaxed),
        ),
        (
            "cloud_blob_read_rate_limited",
            st.api_metrics
                .reject_cloud_blob_read_rate_limited
                .load(Ordering::Relaxed),
        ),
        (
            "cloud_blob_write_rate_limited",
            st.api_metrics
                .reject_cloud_blob_write_rate_limited
                .load(Ordering::Relaxed),
        ),
        (
            "cloud_blob_validation_failed",
            st.api_metrics
                .reject_cloud_blob_validation_failed
                .load(Ordering::Relaxed),
        ),
    ];
    for (reason, count) in reject_reason_metrics {
        body.push_str(&format!(
            "elementa_api_reject_reason_total{{reason=\"{}\"}} {}\n",
            reason, count
        ));
    }

    let mut headers = HeaderMap::new();
    headers.insert(
        axum::http::header::CONTENT_TYPE,
        HeaderValue::from_static("text/plain; version=0.0.4; charset=utf-8"),
    );
    (StatusCode::OK, headers, body)
}

pub(crate) async fn handler_404() -> impl IntoResponse {
    (
        StatusCode::NOT_FOUND,
        Json(serde_json::json!({ "error": "Route not found" })),
    )
}
