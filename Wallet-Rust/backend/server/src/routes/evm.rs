use axum::{
    extract::{Query, State},
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Response},
    routing::get,
    Json, Router,
};
use std::{collections::HashMap, time::Duration};

use crate::app::{AppState, EvmQuoteQuery};
use serde_json::Value;

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/quote", get(evm_quote))
        .route("/price", get(evm_price))
        .route("/status", get(evm_status))
}

pub(crate) async fn evm_status(State(st): State<AppState>) -> impl axum::response::IntoResponse {
    let primary = !st.cfg.zerox_api_key.trim().is_empty();
    let fallback = fallback_enabled(&st);
    let mode = if primary {
        "zeroex"
    } else if fallback {
        "fallback"
    } else {
        "none"
    };
    Json(serde_json::json!({
        "primary": primary,
        "fallback": fallback,
        "mode": mode,
        "fallbackProvider": fallback_provider(&st),
        "fallbackBaseConfigured": !fallback_base_url(&st).trim().is_empty()
    }))
}

fn zeroex_chain_specific_base_url(chain_id: Option<&str>) -> &'static str {
    match chain_id.unwrap_or("").parse::<u64>().unwrap_or(0) {
        11155111 => "https://sepolia.api.0x.org",
        56 => "https://bsc.api.0x.org",
        137 => "https://polygon.api.0x.org",
        43114 => "https://avalanche.api.0x.org",
        _ => "https://api.0x.org",
    }
}

fn zeroex_base_candidates(chain_id: Option<&str>) -> Vec<&'static str> {
    // 0x v2 is reliably served from the generic host with chainId in query params.
    // Keep chain-specific hosts as optional fallback only.
    let chain_specific = zeroex_chain_specific_base_url(chain_id);
    if chain_specific == "https://api.0x.org" {
        vec!["https://api.0x.org"]
    } else {
        vec!["https://api.0x.org", chain_specific]
    }
}

fn is_no_route_error(message: &str) -> bool {
    let lower = message.to_ascii_lowercase();
    lower.contains("no route matched")
        || lower.contains("no route found")
        || lower.contains("no_route")
        || lower.contains("insufficient liquidity")
        || lower.contains("cannot estimate")
        || (lower.contains(" returned 404") && lower.contains("route"))
}

fn get_nested_value<'a>(value: &'a Value, path: &[&str]) -> Option<&'a Value> {
    let mut current = value;
    for key in path {
        current = current.get(*key)?;
    }
    Some(current)
}

fn extract_string_from_paths(value: &Value, paths: &[&[&str]]) -> Option<String> {
    for path in paths {
        if let Some(v) = get_nested_value(value, path).and_then(|v| v.as_str()) {
            if !v.trim().is_empty() {
                return Some(v.to_string());
            }
        }
    }
    None
}

fn canonicalize_swap_payload(source: &str, data: &mut Value) {
    let sell_amount = extract_string_from_paths(
        data,
        &[
            &["sellAmount"],
            &["quote", "sellAmount"],
            &["tx", "sellAmount"],
        ],
    );
    let buy_amount = extract_string_from_paths(
        data,
        &[
            &["buyAmount"],
            &["quote", "buyAmount"],
            &["tx", "buyAmount"],
        ],
    );
    let allowance_target = extract_string_from_paths(
        data,
        &[
            &["allowanceTarget"],
            &["approvalTarget"],
            &["spender"],
            &["quote", "allowanceTarget"],
            &["quote", "approvalTarget"],
            &["quote", "spender"],
            &["issues", "allowance", "spender"],
            &["quote", "issues", "allowance", "spender"],
        ],
    );
    let tx_to = extract_string_from_paths(
        data,
        &[
            &["transaction", "to"],
            &["tx", "to"],
            &["tx", "toAddress"],
            &["to"],
            &["quote", "transaction", "to"],
            &["quote", "tx", "to"],
            &["quote", "to"],
        ],
    );
    let tx_data = extract_string_from_paths(
        data,
        &[
            &["transaction", "data"],
            &["transaction", "input"],
            &["tx", "data"],
            &["tx", "input"],
            &["tx", "callData"],
            &["data"],
            &["input"],
            &["quote", "transaction", "data"],
            &["quote", "tx", "data"],
            &["quote", "data"],
        ],
    );
    let tx_value = extract_string_from_paths(
        data,
        &[
            &["transaction", "value"],
            &["tx", "value"],
            &["value"],
            &["quote", "transaction", "value"],
            &["quote", "tx", "value"],
            &["quote", "value"],
        ],
    );

    if let Some(obj) = data.as_object_mut() {
        obj.insert("_source".to_string(), serde_json::json!(source));
        obj.insert("source".to_string(), serde_json::json!(source));
        if let Some(v) = sell_amount {
            obj.insert("sellAmount".to_string(), serde_json::json!(v));
        }
        if let Some(v) = buy_amount {
            obj.insert("buyAmount".to_string(), serde_json::json!(v));
        }
        if let Some(v) = allowance_target {
            obj.insert("allowanceTarget".to_string(), serde_json::json!(v));
        }
        if let (Some(to), Some(data_field)) = (tx_to, tx_data) {
            let mut tx = serde_json::Map::new();
            tx.insert("to".to_string(), serde_json::json!(to));
            tx.insert("data".to_string(), serde_json::json!(data_field));
            if let Some(value) = tx_value {
                tx.insert("value".to_string(), serde_json::json!(value));
            }
            obj.insert("transaction".to_string(), Value::Object(tx));
        }

        if let Some(route) = obj.get("route") {
            if route.get("tokenPath").is_none() && route.get("tokenAddressPath").is_none() {
                if let Some(tokens) = route.get("tokens").and_then(|v| v.as_array()) {
                    let token_path: Vec<String> = tokens
                        .iter()
                        .filter_map(|item| {
                            if let Some(s) = item.as_str() {
                                Some(s.to_string())
                            } else {
                                item.get("address")
                                    .and_then(|a| a.as_str())
                                    .map(|s| s.to_string())
                            }
                        })
                        .collect();
                    if !token_path.is_empty() {
                        let mut route_obj = route.as_object().cloned().unwrap_or_default();
                        route_obj.insert("tokenPath".to_string(), serde_json::json!(token_path));
                        obj.insert("route".to_string(), Value::Object(route_obj));
                    }
                }
            }
        }
    }
}

fn fallback_provider(st: &AppState) -> &str {
    let raw = st.cfg.fallback_aggregator_provider.trim();
    if raw.is_empty() {
        "zeroex_compat"
    } else {
        raw
    }
}

fn fallback_base_url(st: &AppState) -> String {
    let raw = st.cfg.fallback_aggregator_url.trim();
    if !raw.is_empty() {
        return raw.to_string();
    }
    if fallback_provider(st) == "oneinch" {
        return "https://api.1inch.dev/swap/v6.0".to_string();
    }
    String::new()
}

fn fallback_enabled(st: &AppState) -> bool {
    !fallback_base_url(st).trim().is_empty()
}

fn canonicalize_oneinch_payload(source: &str, data: &mut Value, sell_amount: &str) {
    if let Some(obj) = data.as_object_mut() {
        if obj.get("sellAmount").is_none() {
            if let Some(v) = obj
                .get("fromTokenAmount")
                .and_then(|v| v.as_str())
                .filter(|v| !v.trim().is_empty())
            {
                obj.insert("sellAmount".to_string(), serde_json::json!(v));
            } else if !sell_amount.trim().is_empty() {
                obj.insert("sellAmount".to_string(), serde_json::json!(sell_amount));
            }
        }
        if obj.get("buyAmount").is_none() {
            if let Some(v) = obj
                .get("toTokenAmount")
                .and_then(|v| v.as_str())
                .filter(|v| !v.trim().is_empty())
            {
                obj.insert("buyAmount".to_string(), serde_json::json!(v));
            } else if let Some(v) = obj
                .get("dstAmount")
                .and_then(|v| v.as_str())
                .filter(|v| !v.trim().is_empty())
            {
                obj.insert("buyAmount".to_string(), serde_json::json!(v));
            }
        }
        if obj.get("allowanceTarget").is_none() {
            if let Some(v) = obj
                .get("tx")
                .and_then(|tx| tx.get("to"))
                .and_then(|v| v.as_str())
                .filter(|v| !v.trim().is_empty())
            {
                obj.insert("allowanceTarget".to_string(), serde_json::json!(v));
            }
        }
    }
    canonicalize_swap_payload(source, data);
}

pub(crate) async fn evm_quote(
    State(st): State<AppState>,
    headers: HeaderMap,
    Query(params): Query<HashMap<String, String>>,
) -> Response {
    let params = match crate::app::parse_strict_query::<EvmQuoteQuery>(
        &st.api_metrics,
        "/api/evm/quote",
        params,
        Some(&headers),
    ) {
        Ok(v) => v,
        Err(resp) => return resp,
    };
    if let Err(e) = crate::app::validate_evm_quote_query(&params) {
        return crate::app::validation_error_response(
            &st.api_metrics,
            "/api/evm/quote",
            &e,
            Some(&headers),
        );
    }
    evm_quote_or_price(
        st,
        params,
        true,
        crate::app::incoming_request_id(Some(&headers)),
    )
    .await
}

pub(crate) async fn evm_price(
    State(st): State<AppState>,
    headers: HeaderMap,
    Query(params): Query<HashMap<String, String>>,
) -> Response {
    let params = match crate::app::parse_strict_query::<EvmQuoteQuery>(
        &st.api_metrics,
        "/api/evm/price",
        params,
        Some(&headers),
    ) {
        Ok(v) => v,
        Err(resp) => return resp,
    };
    if let Err(e) = crate::app::validate_evm_quote_query(&params) {
        return crate::app::validation_error_response(
            &st.api_metrics,
            "/api/evm/price",
            &e,
            Some(&headers),
        );
    }
    evm_quote_or_price(
        st,
        params,
        false,
        crate::app::incoming_request_id(Some(&headers)),
    )
    .await
}

async fn evm_quote_or_price(
    st: AppState,
    params: EvmQuoteQuery,
    is_quote: bool,
    request_id: Option<String>,
) -> Response {
    let mut primary_no_route_error: Option<String> = None;
    let mut primary_last_error: Option<String> = None;
    let q = {
        let mut serializer = url::form_urlencoded::Serializer::new(String::new());
        serializer.append_pair("chainId", &params.chain_id);
        serializer.append_pair("sellToken", &params.sell_token);
        serializer.append_pair("buyToken", &params.buy_token);
        serializer.append_pair("sellAmount", &params.sell_amount);
        serializer.append_pair("taker", &params.taker);
        serializer.append_pair("slippageBps", &params.slippage_bps);
        if let Some(v) = params.buy_amount.as_deref() {
            serializer.append_pair("buyAmount", v);
        }
        if let Some(v) = params.tx_origin.as_deref() {
            serializer.append_pair("txOrigin", v);
        }
        if let Some(v) = params.gas_price.as_deref() {
            serializer.append_pair("gasPrice", v);
        }
        if let Some(v) = params.excluded_sources.as_deref() {
            serializer.append_pair("excludedSources", v);
        }
        if let Some(v) = params.included_sources.as_deref() {
            serializer.append_pair("includedSources", v);
        }
        if let Some(v) = params.fee_recipient.as_deref() {
            serializer.append_pair("feeRecipient", v);
        }
        if let Some(v) = params.buy_token_percentage_fee.as_deref() {
            serializer.append_pair("buyTokenPercentageFee", v);
        }
        if let Some(v) = params.swap_fee_recipient.as_deref() {
            serializer.append_pair("swapFeeRecipient", v);
        }
        if let Some(v) = params.swap_fee_bps.as_deref() {
            serializer.append_pair("swapFeeBps", v);
        }
        if let Some(v) = params.swap_fee_token.as_deref() {
            serializer.append_pair("swapFeeToken", v);
        }
        if let Some(v) = params.sell_entire_balance.as_deref() {
            serializer.append_pair("sellEntireBalance", v);
        }
        if let Some(v) = params.skip_validation.as_deref() {
            serializer.append_pair("skipValidation", v);
        }
        if let Some(v) = params.enable_slippage_protection.as_deref() {
            serializer.append_pair("enableSlippageProtection", v);
        }
        serializer.finish()
    };
    let chain_id = Some(params.chain_id.as_str());
    let zeroex_paths: [&str; 3] = if is_quote {
        [
            "/swap/allowance-holder/quote",
            "/swap/permit2/quote",
            "/swap/v1/quote",
        ]
    } else {
        [
            "/swap/allowance-holder/price",
            "/swap/permit2/price",
            "/swap/v1/price",
        ]
    };

    if !st.cfg.zerox_api_key.trim().is_empty() {
        let base_candidates = zeroex_base_candidates(chain_id);

        let mut headers = reqwest::header::HeaderMap::new();
        headers.insert("0x-api-key", st.cfg.zerox_api_key.parse().unwrap());
        headers.insert("0x-version", "v2".parse().unwrap());

        'zeroex_attempts: for base in base_candidates {
            for path in zeroex_paths {
                let mut url = url::Url::parse(base).unwrap();
                url.set_path(path);
                url.set_query(Some(&q));
                let upstream_label = format!("zeroex:{}{path}", base.replace("https://", ""));

                match crate::infra::rpc::fetch_with_host_policy(
                    &st,
                    reqwest::Method::GET,
                    &url,
                    None,
                    Some(headers.clone()),
                    Duration::from_secs(10),
                    "zeroex",
                )
                .await
                {
                    Ok(mut data) => {
                        canonicalize_swap_payload(&upstream_label, &mut data);
                        return (StatusCode::OK, Json(data)).into_response();
                    }
                    Err(e) => {
                        tracing::warn!(
                            upstream = "0x",
                            base = %base,
                            path = %path,
                            error = %e,
                            "evm quote/price request to 0x failed"
                        );
                        primary_last_error = Some(e.clone());
                        if is_no_route_error(&e) {
                            primary_no_route_error = Some(match primary_no_route_error {
                                Some(prev) => format!("{prev} | {base}{path}: {e}"),
                                None => format!("{base}{path}: {e}"),
                            });
                            continue;
                        }
                        // For transient/provider issues, try next host/path before surfacing.
                        let lower = e.to_ascii_lowercase();
                        if lower.contains("provider_unavailable_chain")
                            || lower.contains("status code: 503")
                            || lower.contains(" returned 503")
                            || lower.contains(" returned 502")
                            || lower.contains("timeout")
                        {
                            continue;
                        }
                        if !fallback_enabled(&st) {
                            return crate::infra::proxy::upstream_error_response(
                                &st,
                                "zeroex",
                                StatusCode::BAD_GATEWAY,
                                "0x API error",
                                e,
                                request_id,
                            );
                        }
                        break 'zeroex_attempts;
                    }
                }
            }
        }

        if !fallback_enabled(&st) {
            if let Some(details) = primary_no_route_error {
                return crate::app::api_error_response(
                    StatusCode::UNPROCESSABLE_ENTITY,
                    "no_route",
                    "No swap route found for selected pair/amount",
                    Some(details),
                    request_id,
                );
            }
            if let Some(err) = primary_last_error {
                return crate::infra::proxy::upstream_error_response(
                    &st,
                    "zeroex",
                    StatusCode::BAD_GATEWAY,
                    "0x API error",
                    err,
                    request_id,
                );
            }
        }
    }

    if !fallback_enabled(&st) {
        return crate::app::api_error_response(
            StatusCode::INTERNAL_SERVER_ERROR,
            "no_aggregator_configured",
            "No aggregator configured",
            None,
            request_id,
        );
    }

    let fallback_base = fallback_base_url(&st);
    let base = match crate::app::validate_external_base_url(&st, &fallback_base) {
        Ok(v) => v,
        Err(e) => {
            return crate::app::api_error_response(
                StatusCode::INTERNAL_SERVER_ERROR,
                "fallback_aggregator_blocked",
                "Fallback aggregator blocked",
                Some(e),
                request_id,
            );
        }
    };
    let provider = fallback_provider(&st);
    let mut url = url::Url::parse(&base).unwrap();

    let mut headers = reqwest::header::HeaderMap::new();
    if !st.cfg.fallback_aggregator_api_key.trim().is_empty() {
        if provider == "oneinch"
            && st
                .cfg
                .fallback_aggregator_api_header
                .eq_ignore_ascii_case("x-api-key")
        {
            if let Ok(v) = format!("Bearer {}", st.cfg.fallback_aggregator_api_key.trim()).parse() {
                headers.insert(reqwest::header::AUTHORIZATION, v);
            }
        } else {
            headers.insert(
                st.cfg
                    .fallback_aggregator_api_header
                    .parse::<reqwest::header::HeaderName>()
                    .unwrap_or(reqwest::header::HeaderName::from_static("x-api-key")),
                st.cfg.fallback_aggregator_api_key.parse().unwrap(),
            );
        }
    }
    if !st.cfg.fallback_aggregator_version.trim().is_empty() {
        headers.insert(
            "x-api-version",
            st.cfg.fallback_aggregator_version.parse().unwrap(),
        );
    }
    if provider == "oneinch" {
        let path = if is_quote { "swap" } else { "quote" };
        url.set_path(&format!("{}/{path}", params.chain_id));
        let mut serializer = url::form_urlencoded::Serializer::new(String::new());
        serializer.append_pair("src", &params.sell_token);
        serializer.append_pair("dst", &params.buy_token);
        serializer.append_pair("amount", &params.sell_amount);
        serializer.append_pair("from", &params.taker);
        if is_quote {
            serializer.append_pair("origin", &params.taker);
            let slippage = params.slippage_bps.parse::<f64>().unwrap_or(50.0) / 100.0;
            serializer.append_pair("slippage", &slippage.to_string());
            serializer.append_pair("disableEstimate", "true");
            serializer.append_pair("allowPartialFill", "false");
        }
        url.set_query(Some(&serializer.finish()));
    } else {
        let sub = if is_quote {
            &st.cfg.fallback_aggregator_quote_path
        } else {
            &st.cfg.fallback_aggregator_price_path
        };
        url.set_path(sub);
        url.set_query(Some(&q));
    }

    match crate::infra::rpc::fetch_with_host_policy(
        &st,
        reqwest::Method::GET,
        &url,
        None,
        if headers.is_empty() {
            None
        } else {
            Some(headers)
        },
        Duration::from_secs(10),
        "evm_fallback",
    )
    .await
    {
        Ok(mut data) => {
            if provider == "oneinch" {
                canonicalize_oneinch_payload("fallback:oneinch", &mut data, &params.sell_amount);
            } else {
                canonicalize_swap_payload("fallback:zeroex_compat", &mut data);
            }
            (StatusCode::OK, Json(data)).into_response()
        }
        Err(e) => {
            tracing::warn!(upstream = "evm_fallback", provider = %provider, error = %e, "evm quote/price fallback failed");
            if is_no_route_error(&e) {
                let details = match primary_no_route_error {
                    Some(primary) => format!("0x: {}; fallback: {}", primary, e),
                    None => e,
                };
                return crate::app::api_error_response(
                    StatusCode::UNPROCESSABLE_ENTITY,
                    "no_route",
                    "No swap route found for selected pair/amount",
                    Some(details),
                    request_id,
                );
            }
            if let Some(primary) = primary_no_route_error {
                return crate::infra::proxy::upstream_error_response(
                    &st,
                    "evm_fallback",
                    StatusCode::BAD_GATEWAY,
                    "Fallback aggregator error after 0x reported no route",
                    format!("0x no_route: {}; fallback error: {}", primary, e),
                    request_id,
                );
            }
            crate::infra::proxy::upstream_error_response(
                &st,
                "evm_fallback",
                StatusCode::BAD_GATEWAY,
                "Fallback aggregator error",
                e,
                request_id,
            )
        }
    }
}
