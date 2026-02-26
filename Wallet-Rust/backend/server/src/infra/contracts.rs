use axum::{
    extract::rejection::JsonRejection,
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Response},
    Json,
};
use serde::de::DeserializeOwned;

use crate::app::ApiMetrics;

const HEADER_REQUEST_ID: &str = "x-request-id";

pub(crate) fn incoming_request_id(headers: Option<&HeaderMap>) -> Option<String> {
    headers
        .and_then(|h| h.get(HEADER_REQUEST_ID))
        .and_then(|v| v.to_str().ok())
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
}

pub(crate) fn inc_reject_reason_counter(metrics: &ApiMetrics, reason: &str) {
    match reason {
        "invalid_query" => {
            metrics
                .reject_invalid_query
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "invalid_json_body" => {
            metrics
                .reject_invalid_json_body
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "validation_failed" => {
            metrics
                .reject_validation_failed
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "proxy_url_blocked" => {
            metrics
                .reject_proxy_url_blocked
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "blocked_rpc_url" => {
            metrics
                .reject_blocked_rpc_url
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "method_not_allowed" => {
            metrics
                .reject_method_not_allowed
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "rpc_payload_too_large" => {
            metrics
                .reject_rpc_payload_too_large
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "invalid_jsonrpc_payload" => {
            metrics
                .reject_invalid_jsonrpc_payload
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "empty_rpc_batch" => {
            metrics
                .reject_empty_rpc_batch
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "rpc_batch_too_large" => {
            metrics
                .reject_rpc_batch_too_large
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "invalid_jsonrpc_request" => {
            metrics
                .reject_invalid_jsonrpc_request
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "rpc_method_rate_limited_single" => {
            metrics
                .reject_rpc_method_rate_limited_single
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "rpc_method_rate_limited_batch" => {
            metrics
                .reject_rpc_method_rate_limited_batch
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "cloud_blob_read_rate_limited" => {
            metrics
                .reject_cloud_blob_read_rate_limited
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "cloud_blob_write_rate_limited" => {
            metrics
                .reject_cloud_blob_write_rate_limited
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        "cloud_blob_validation_failed" => {
            metrics
                .reject_cloud_blob_validation_failed
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        }
        _ => {}
    }
}

pub(crate) fn api_error_response(
    status: StatusCode,
    code: &str,
    message: &str,
    details: Option<String>,
    request_id: Option<String>,
) -> Response {
    let mut err = serde_json::json!({
        "code": code,
        "message": message,
    });
    if let Some(d) = details {
        err["details"] = serde_json::Value::String(d);
    }
    if let Some(rid) = request_id {
        err["requestId"] = serde_json::Value::String(rid);
    }
    (status, Json(serde_json::json!({ "error": err }))).into_response()
}

pub(crate) fn parse_strict_query<T>(
    metrics: &ApiMetrics,
    path: &str,
    params: std::collections::HashMap<String, String>,
    headers: Option<&HeaderMap>,
) -> Result<T, Response>
where
    T: DeserializeOwned,
{
    let value = match serde_json::to_value(params) {
        Ok(v) => v,
        Err(e) => {
            return Err(validation_error_response(
                metrics,
                path,
                &format!("Invalid query parameters: {e}"),
                headers,
            ));
        }
    };

    serde_json::from_value::<T>(value).map_err(|e| {
        let msg = e.to_string();
        inc_reject_reason_counter(metrics, "invalid_query");
        tracing::warn!(path, reject_reason = "invalid_query", details = %msg, "request rejected");
        api_error_response(
            StatusCode::BAD_REQUEST,
            "invalid_query",
            "Invalid query parameters",
            Some(msg),
            incoming_request_id(headers),
        )
    })
}

pub(crate) fn invalid_json_response(
    metrics: &ApiMetrics,
    path: &str,
    rejection: JsonRejection,
    headers: Option<&HeaderMap>,
) -> Response {
    let details = rejection.to_string();
    inc_reject_reason_counter(metrics, "invalid_json_body");
    tracing::warn!(path, reject_reason = "invalid_json_body", details = %details, "request rejected");
    api_error_response(
        StatusCode::BAD_REQUEST,
        "invalid_json_body",
        "Invalid JSON body",
        Some(details),
        incoming_request_id(headers),
    )
}

pub(crate) fn validation_error_response(
    metrics: &ApiMetrics,
    path: &str,
    message: &str,
    headers: Option<&HeaderMap>,
) -> Response {
    inc_reject_reason_counter(metrics, "validation_failed");
    tracing::warn!(path, reject_reason = "validation_failed", details = %message, "request rejected");
    api_error_response(
        StatusCode::BAD_REQUEST,
        "validation_failed",
        message,
        None,
        incoming_request_id(headers),
    )
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
pub(crate) struct CacheEntry {
    pub(crate) value: serde_json::Value,
    pub(crate) expires_at_ms: i64,
    pub(crate) is_error: bool,
}

#[derive(Debug, serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct BatchEnvelope {
    pub(crate) requests: Vec<BatchReq>,
}

#[derive(Debug, serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct BatchReq {
    #[serde(default)]
    pub(crate) r#type: String,
    pub(crate) url: String,
    #[serde(default = "default_ttl")]
    pub(crate) ttl: u64,
}

fn default_ttl() -> u64 {
    60
}

#[derive(Debug, serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct ProxyUrlQuery {
    pub(crate) url: String,
}

#[derive(Debug, serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct EvmQuoteQuery {
    #[serde(rename = "chainId")]
    pub(crate) chain_id: String,
    #[serde(rename = "sellToken")]
    pub(crate) sell_token: String,
    #[serde(rename = "buyToken")]
    pub(crate) buy_token: String,
    #[serde(rename = "sellAmount")]
    pub(crate) sell_amount: String,
    pub(crate) taker: String,
    #[serde(rename = "slippageBps")]
    pub(crate) slippage_bps: String,
    #[serde(rename = "buyAmount", default)]
    pub(crate) buy_amount: Option<String>,
    #[serde(rename = "txOrigin", default)]
    pub(crate) tx_origin: Option<String>,
    #[serde(rename = "gasPrice", default)]
    pub(crate) gas_price: Option<String>,
    #[serde(rename = "excludedSources", default)]
    pub(crate) excluded_sources: Option<String>,
    #[serde(rename = "includedSources", default)]
    pub(crate) included_sources: Option<String>,
    #[serde(rename = "feeRecipient", default)]
    pub(crate) fee_recipient: Option<String>,
    #[serde(rename = "buyTokenPercentageFee", default)]
    pub(crate) buy_token_percentage_fee: Option<String>,
    #[serde(rename = "swapFeeRecipient", default)]
    pub(crate) swap_fee_recipient: Option<String>,
    #[serde(rename = "swapFeeBps", default)]
    pub(crate) swap_fee_bps: Option<String>,
    #[serde(rename = "swapFeeToken", default)]
    pub(crate) swap_fee_token: Option<String>,
    #[serde(rename = "sellEntireBalance", default)]
    pub(crate) sell_entire_balance: Option<String>,
    #[serde(rename = "skipValidation", default)]
    pub(crate) skip_validation: Option<String>,
    #[serde(rename = "enableSlippageProtection", default)]
    pub(crate) enable_slippage_protection: Option<String>,
}

#[derive(Debug, serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct JupiterQuoteQuery {
    #[serde(rename = "inputMint")]
    pub(crate) input_mint: String,
    #[serde(rename = "outputMint")]
    pub(crate) output_mint: String,
    pub(crate) amount: String,
    #[serde(rename = "slippageBps", default)]
    pub(crate) slippage_bps: Option<String>,
    #[serde(rename = "swapMode", default)]
    pub(crate) swap_mode: Option<String>,
    #[serde(rename = "restrictIntermediateTokens", default)]
    pub(crate) restrict_intermediate_tokens: Option<String>,
    #[serde(rename = "onlyDirectRoutes", default)]
    pub(crate) only_direct_routes: Option<String>,
    #[serde(rename = "asLegacyTransaction", default)]
    pub(crate) as_legacy_transaction: Option<String>,
    #[serde(rename = "platformFeeBps", default)]
    pub(crate) platform_fee_bps: Option<String>,
    #[serde(rename = "maxAccounts", default)]
    pub(crate) max_accounts: Option<String>,
}

fn validate_required_non_empty(value: &str, field: &str) -> Result<(), String> {
    if value.trim().is_empty() {
        return Err(format!("Missing or empty {field}"));
    }
    Ok(())
}

pub(crate) fn validate_evm_quote_query(params: &EvmQuoteQuery) -> Result<(), String> {
    validate_required_non_empty(&params.chain_id, "chainId")?;
    validate_required_non_empty(&params.sell_token, "sellToken")?;
    validate_required_non_empty(&params.buy_token, "buyToken")?;
    validate_required_non_empty(&params.sell_amount, "sellAmount")?;
    validate_required_non_empty(&params.taker, "taker")?;
    validate_required_non_empty(&params.slippage_bps, "slippageBps")?;
    Ok(())
}

pub(crate) fn validate_jupiter_quote_query(params: &JupiterQuoteQuery) -> Result<(), String> {
    validate_required_non_empty(&params.input_mint, "inputMint")?;
    validate_required_non_empty(&params.output_mint, "outputMint")?;
    validate_required_non_empty(&params.amount, "amount")?;
    Ok(())
}

#[derive(Clone, Copy, Debug)]
pub(crate) enum CacheOutcome {
    Hit,
    Stale,
    Miss,
}

impl CacheOutcome {
    pub(crate) fn as_header_value(self) -> &'static str {
        match self {
            CacheOutcome::Hit => "HIT",
            CacheOutcome::Stale => "STALE",
            CacheOutcome::Miss => "MISS",
        }
    }
}

#[derive(Debug)]
pub(crate) struct ProxyFetchError {
    pub(crate) message: String,
    pub(crate) from_cache: bool,
}

fn is_hex_address_string(value: &str) -> bool {
    let trimmed = value.trim();
    if trimmed.len() != 42 {
        return false;
    }
    if !trimmed.starts_with("0x") {
        return false;
    }
    let hex = &trimmed[2..];
    !hex.eq_ignore_ascii_case("0000000000000000000000000000000000000000")
        && hex.chars().all(|c| c.is_ascii_hexdigit())
}

pub(crate) fn sanitize_deployed_contracts(value: &serde_json::Value) -> serde_json::Value {
    let mut out = serde_json::Map::new();
    let Some(obj) = value.as_object() else {
        return serde_json::Value::Object(out);
    };

    let keys = [
        "ElementaToken",
        "MineralToken",
        "ElementMinter",
        "StakingContract",
        "UniswapV2Factory",
        "UniswapV2Router",
        "MockUSD",
    ];
    for key in keys {
        let Some(raw) = obj.get(key).and_then(|v| v.as_str()) else {
            continue;
        };
        if is_hex_address_string(raw) {
            out.insert(key.to_string(), serde_json::Value::String(raw.to_string()));
        }
    }

    serde_json::Value::Object(out)
}

pub(crate) fn parse_u64_str(value: &str, field: &str) -> Result<u64, String> {
    value
        .trim()
        .parse::<u64>()
        .map_err(|_| format!("Invalid {field}"))
        .and_then(|v| {
            if v > 0 {
                Ok(v)
            } else {
                Err(format!("Invalid {field}"))
            }
        })
}
