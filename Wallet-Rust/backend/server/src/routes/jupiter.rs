use axum::{
    extract::{Query, State},
    http::{HeaderMap, StatusCode},
    response::IntoResponse,
    routing::{get, post},
    Json, Router,
};
use std::{collections::HashMap, time::Duration};

use crate::app::{AppState, JupiterQuoteQuery};

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/status", get(jupiter_status))
        .route("/quote", get(jupiter_quote))
        .route("/swap", post(jupiter_swap))
}

pub(crate) async fn jupiter_status(
    State(st): State<AppState>,
) -> impl axum::response::IntoResponse {
    Json(serde_json::json!({
        "ok": !st.cfg.jupiter_api_key.trim().is_empty(),
        "message": if st.cfg.jupiter_api_key.trim().is_empty() { "JUPITER_API_KEY is not configured" } else { "Jupiter API key configured" }
    }))
}

fn build_jupiter_url(base_url: &str, endpoint_path: &str) -> Result<url::Url, String> {
    let base = url::Url::parse(base_url).map_err(|_| "Invalid URL".to_string())?;
    let base_path = base.path().trim_end_matches('/').to_string();
    if base_path.ends_with("/swap/v1") {
        return base
            .join(&format!("{}/{}", base_path, endpoint_path))
            .map_err(|_| "Invalid URL".to_string());
    }
    base.join(endpoint_path)
        .map_err(|_| "Invalid URL".to_string())
}

pub(crate) async fn jupiter_quote(
    State(st): State<AppState>,
    headers: HeaderMap,
    Query(params): Query<HashMap<String, String>>,
) -> impl IntoResponse {
    let request_id = crate::app::incoming_request_id(Some(&headers));
    let params = match crate::app::parse_strict_query::<JupiterQuoteQuery>(
        &st.api_metrics,
        "/api/jupiter/quote",
        params,
        Some(&headers),
    ) {
        Ok(v) => v,
        Err(resp) => return resp,
    };
    if let Err(e) = crate::app::validate_jupiter_quote_query(&params) {
        return crate::app::validation_error_response(
            &st.api_metrics,
            "/api/jupiter/quote",
            &e,
            Some(&headers),
        );
    }

    if st.cfg.jupiter_api_key.trim().is_empty() {
        return crate::app::api_error_response(
            StatusCode::INTERNAL_SERVER_ERROR,
            "jupiter_api_key_missing",
            "Jupiter API key is not configured",
            None,
            request_id,
        );
    }
    if let Err(e) = crate::app::validate_external_base_url(&st, &st.cfg.jupiter_api_url) {
        return crate::app::api_error_response(
            StatusCode::INTERNAL_SERVER_ERROR,
            "jupiter_url_blocked",
            "Jupiter URL blocked",
            Some(e),
            request_id,
        );
    }
    let mut url = match build_jupiter_url(&st.cfg.jupiter_api_url, "quote") {
        Ok(u) => u,
        Err(e) => {
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": format!("Invalid JUPITER_API_URL: {e}") })),
            )
                .into_response();
        }
    };
    {
        let mut qp = url.query_pairs_mut();
        qp.append_pair("inputMint", &params.input_mint);
        qp.append_pair("outputMint", &params.output_mint);
        qp.append_pair("amount", &params.amount);
        if let Some(v) = params.slippage_bps.as_deref() {
            qp.append_pair("slippageBps", v);
        }
        if let Some(v) = params.swap_mode.as_deref() {
            qp.append_pair("swapMode", v);
        }
        if let Some(v) = params.restrict_intermediate_tokens.as_deref() {
            qp.append_pair("restrictIntermediateTokens", v);
        }
        if let Some(v) = params.only_direct_routes.as_deref() {
            qp.append_pair("onlyDirectRoutes", v);
        }
        if let Some(v) = params.as_legacy_transaction.as_deref() {
            qp.append_pair("asLegacyTransaction", v);
        }
        if let Some(v) = params.platform_fee_bps.as_deref() {
            qp.append_pair("platformFeeBps", v);
        }
        if let Some(v) = params.max_accounts.as_deref() {
            qp.append_pair("maxAccounts", v);
        }
    }

    let mut h = reqwest::header::HeaderMap::new();
    h.insert("x-api-key", st.cfg.jupiter_api_key.parse().unwrap());
    h.insert("accept", "application/json".parse().unwrap());

    match crate::infra::rpc::fetch_with_host_policy(
        &st,
        reqwest::Method::GET,
        &url,
        None,
        Some(h),
        Duration::from_secs(10),
        "jupiter_quote",
    )
    .await
    {
        Ok(v) => (StatusCode::OK, Json(v)).into_response(),
        Err(e) => {
            tracing::warn!(upstream = "jupiter_quote", error = %e, "jupiter quote failed");
            crate::infra::proxy::upstream_error_response(
                &st,
                "jupiter_quote",
                StatusCode::BAD_GATEWAY,
                "Jupiter quote call failed",
                e,
                request_id,
            )
        }
    }
}

pub(crate) async fn jupiter_swap(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<serde_json::Value>, axum::extract::rejection::JsonRejection>,
) -> impl IntoResponse {
    let request_id = crate::app::incoming_request_id(Some(&headers));
    let Json(payload) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/jupiter/swap",
                rejection,
                Some(&headers),
            );
        }
    };
    if st.cfg.jupiter_api_key.trim().is_empty() {
        return crate::app::api_error_response(
            StatusCode::INTERNAL_SERVER_ERROR,
            "jupiter_api_key_missing",
            "Jupiter API key is not configured",
            None,
            request_id,
        );
    }
    if let Err(e) = crate::app::validate_external_base_url(&st, &st.cfg.jupiter_api_url) {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": format!("Jupiter URL blocked: {e}") })),
        )
            .into_response();
    }
    let url = match build_jupiter_url(&st.cfg.jupiter_api_url, "swap") {
        Ok(u) => u,
        Err(e) => {
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": format!("Invalid JUPITER_API_URL: {e}") })),
            )
                .into_response();
        }
    };

    let mut h = reqwest::header::HeaderMap::new();
    h.insert("x-api-key", st.cfg.jupiter_api_key.parse().unwrap());
    h.insert("content-type", "application/json".parse().unwrap());

    match crate::infra::rpc::fetch_with_host_policy(
        &st,
        reqwest::Method::POST,
        &url,
        Some(payload),
        Some(h),
        Duration::from_secs(12),
        "jupiter_swap",
    )
    .await
    {
        Ok(v) => (StatusCode::OK, Json(v)).into_response(),
        Err(e) => {
            tracing::warn!(upstream = "jupiter_swap", error = %e, "jupiter swap failed");
            crate::infra::proxy::upstream_error_response(
                &st,
                "jupiter_swap",
                StatusCode::BAD_GATEWAY,
                "Jupiter swap call failed",
                e,
                request_id,
            )
        }
    }
}
