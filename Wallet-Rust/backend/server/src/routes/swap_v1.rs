use axum::{
    body::to_bytes,
    extract::{rejection::JsonRejection, Query, State},
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Response},
    routing::post,
    Json, Router,
};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use sha2::{Digest, Sha256};
use std::collections::HashMap;

use crate::app::{chrono_ms_now, random_base64url, AppState};

const QUOTE_TTL_MS: i64 = 15_000;

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct QuoteRequest {
    chain_family: ChainFamily,
    chain_id: Option<u64>,
    cluster: Option<String>,
    sell_token: String,
    buy_token: String,
    sell_amount: String,
    user_address: Option<String>,
    slippage: SlippageInput,
    preferences: Option<Value>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct BuildRequest {
    quote_id: String,
    user_address: String,
    confirm_slippage_bps: Option<u32>,
    fee_preference: Option<String>,
    idempotency_key: Option<String>,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "lowercase")]
enum ChainFamily {
    Evm,
    Solana,
    Btc,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(tag = "mode", rename_all = "lowercase", deny_unknown_fields)]
enum SlippageInput {
    Auto,
    Fixed { bps: u32 },
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct QuoteResponse {
    quote_id: String,
    expires_at: i64,
    chain_family: ChainFamily,
    chain_id: Option<u64>,
    cluster: Option<String>,
    sell_token: String,
    buy_token: String,
    sell_amount: String,
    buy_amount: String,
    min_buy_amount: String,
    estimated_network_fee: Option<String>,
    price_impact_bps: Option<u32>,
    provider: String,
    actions_required: Vec<String>,
    allowance_target: Option<String>,
    route_summary: String,
    alternatives: Vec<String>,
    warnings: Vec<String>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct BuildResponse {
    build_id: String,
    expires_at: i64,
    chain_family: ChainFamily,
    signable: BuildSignable,
    integrity: BuildIntegrity,
    warnings: Vec<String>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct BuildIntegrity {
    quote_hash: String,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct BuildSignable {
    tx_request: Option<EvmTxRequest>,
    base64_tx: Option<String>,
    recent_blockhash: Option<String>,
    btc_payload: Option<Value>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct EvmTxRequest {
    to: String,
    data: String,
    value: String,
    chain_id: u64,
    gas: Option<String>,
    max_fee_per_gas: Option<String>,
    max_priority_fee_per_gas: Option<String>,
}

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/quote", post(quote_handler))
        .route("/build", post(build_handler))
}

async fn quote_handler(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<QuoteRequest>, JsonRejection>,
) -> Response {
    let Json(req) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/v1/swap/quote",
                rejection,
                Some(&headers),
            )
        }
    };

    if let Err(message) = validate_quote_request(&req) {
        return error_response(StatusCode::BAD_REQUEST, "INVALID_QUOTE", message);
    }

    let dispatch = dispatch_quote(st.clone(), &req).await;
    let (provider_payload, mut quote) = match dispatch {
        Ok(v) => v,
        Err((status, code, msg)) => return error_response(status, code, msg),
    };

    let now = chrono_ms_now();
    let quote_id = random_base64url(12);
    let expires_at = now + QUOTE_TTL_MS;
    quote.quote_id = quote_id.clone();
    quote.expires_at = expires_at;

    let request_value = serde_json::to_value(&req).unwrap_or_else(|_| json!({}));
    let response_value = serde_json::to_value(&quote).unwrap_or_else(|_| json!({}));
    let quote_hash = compute_quote_hash(&request_value, &response_value);

    st.swap_quotes.insert(
        quote_id,
        crate::infra::state::SwapQuoteSnapshot {
            expires_at_ms: expires_at,
            request: request_value,
            response: response_value,
            provider_payload,
            quote_hash,
        },
    );

    (StatusCode::OK, Json(quote)).into_response()
}

async fn build_handler(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<BuildRequest>, JsonRejection>,
) -> Response {
    let Json(req) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/v1/swap/build",
                rejection,
                Some(&headers),
            )
        }
    };

    if req.user_address.trim().is_empty() {
        return error_response(
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "userAddress is required",
        );
    }

    let snapshot = match st.swap_quotes.get(&req.quote_id) {
        Some(v) => v.clone(),
        None => {
            return error_response(
                StatusCode::NOT_FOUND,
                "INVALID_QUOTE",
                "quoteId was not found",
            )
        }
    };

    if chrono_ms_now() > snapshot.expires_at_ms {
        st.swap_quotes.remove(&req.quote_id);
        return error_response(
            StatusCode::GONE,
            "QUOTE_EXPIRED",
            "Quote expired. Request a new quote.",
        );
    }

    let request_value = &snapshot.request;
    let response_value = &snapshot.response;
    if let Err((status, code, message)) = validate_snapshot_consistency(request_value, response_value)
    {
        return error_response(status, code, message);
    }

    let chain_family = request_value
        .get("chainFamily")
        .and_then(|v| v.as_str())
        .unwrap_or_default();

    let build_resp = match chain_family {
        "evm" => build_evm(
            response_value,
            &snapshot.provider_payload,
            request_value,
            &snapshot.quote_hash,
        ),
        "solana" => {
            build_solana(
                st.clone(),
                request_value,
                &snapshot.provider_payload,
                &req.user_address,
                &snapshot.quote_hash,
            )
            .await
        }
        "btc" => build_btc(
            response_value,
            &snapshot.provider_payload,
            &snapshot.quote_hash,
        ),
        _ => Err((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Unsupported chain family".to_string(),
        )),
    };

    let _ = (
        &req.confirm_slippage_bps,
        &req.fee_preference,
        &req.idempotency_key,
    );

    match build_resp {
        Ok(v) => (StatusCode::OK, Json(v)).into_response(),
        Err((status, code, message)) => error_response(status, code, &message),
    }
}

fn validate_quote_request(req: &QuoteRequest) -> Result<(), &'static str> {
    if req.sell_token.trim().is_empty() || req.buy_token.trim().is_empty() {
        return Err("sellToken and buyToken are required");
    }
    if req.sell_amount.trim().is_empty() || req.sell_amount == "0" {
        return Err("sellAmount must be a positive base-unit string");
    }
    if let SlippageInput::Fixed { bps } = &req.slippage {
        if *bps == 0 || *bps > 5000 {
            return Err("slippage.fixed.bps must be in [1, 5000]");
        }
    }
    match req.chain_family {
        ChainFamily::Evm => {
            if req.chain_id.is_none() {
                return Err("chainId is required for EVM quotes");
            }
        }
        ChainFamily::Solana => {
            if req.cluster.is_none() {
                return Err("cluster is required for Solana quotes");
            }
        }
        ChainFamily::Btc => {
            if req.cluster.is_none() {
                return Err("cluster is required for BTC quotes");
            }
            if req
                .user_address
                .as_deref()
                .unwrap_or_default()
                .trim()
                .is_empty()
            {
                return Err("userAddress is required for BTC quotes");
            }
        }
    }
    Ok(())
}

fn validate_snapshot_consistency(
    request_value: &Value,
    response_value: &Value,
) -> Result<(), (StatusCode, &'static str, String)> {
    let req_sell_token = request_value
        .get("sellToken")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|v| !v.is_empty())
        .ok_or((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Corrupted quote snapshot: missing request sellToken".to_string(),
        ))?;
    let req_buy_token = request_value
        .get("buyToken")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|v| !v.is_empty())
        .ok_or((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Corrupted quote snapshot: missing request buyToken".to_string(),
        ))?;
    let req_sell_amount = request_value
        .get("sellAmount")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|v| !v.is_empty())
        .ok_or((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Corrupted quote snapshot: missing request sellAmount".to_string(),
        ))?;

    let resp_sell_token = response_value
        .get("sellToken")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|v| !v.is_empty())
        .ok_or((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Corrupted quote snapshot: missing response sellToken".to_string(),
        ))?;
    let resp_buy_token = response_value
        .get("buyToken")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|v| !v.is_empty())
        .ok_or((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Corrupted quote snapshot: missing response buyToken".to_string(),
        ))?;
    let resp_sell_amount = response_value
        .get("sellAmount")
        .and_then(|v| v.as_str())
        .map(str::trim)
        .filter(|v| !v.is_empty())
        .ok_or((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Corrupted quote snapshot: missing response sellAmount".to_string(),
        ))?;

    if !req_sell_token.eq_ignore_ascii_case(resp_sell_token)
        || !req_buy_token.eq_ignore_ascii_case(resp_buy_token)
        || req_sell_amount != resp_sell_amount
    {
        return Err((
            StatusCode::CONFLICT,
            "PRICE_MOVED",
            "Quote snapshot no longer matches the executable route".to_string(),
        ));
    }

    Ok(())
}

async fn dispatch_quote(
    st: AppState,
    req: &QuoteRequest,
) -> Result<(Value, QuoteResponse), (StatusCode, &'static str, String)> {
    match req.chain_family {
        ChainFamily::Evm => quote_evm(st, req).await,
        ChainFamily::Solana => {
            let cluster = req.cluster.clone().unwrap_or_else(|| "mainnet".to_string());
            if cluster.eq_ignore_ascii_case("mainnet") {
                quote_solana_mainnet(st, req).await
            } else {
                quote_solana_testnet(st, req).await
            }
        }
        ChainFamily::Btc => quote_btc(st, req).await,
    }
}

async fn quote_evm(
    st: AppState,
    req: &QuoteRequest,
) -> Result<(Value, QuoteResponse), (StatusCode, &'static str, String)> {
    let chain_id = req.chain_id.unwrap_or_default().to_string();
    let mut map = HashMap::new();
    map.insert("chainId".to_string(), chain_id.clone());
    map.insert("sellToken".to_string(), req.sell_token.clone());
    map.insert("buyToken".to_string(), req.buy_token.clone());
    map.insert("sellAmount".to_string(), req.sell_amount.clone());
    map.insert(
        "taker".to_string(),
        req.user_address
            .clone()
            // 0x v2 rejects the zero address; use a deterministic non-zero fallback for quotes.
            .unwrap_or_else(|| "0x0000000000000000000000000000000000010000".to_string()),
    );
    map.insert(
        "slippageBps".to_string(),
        quote_slippage_bps(req).to_string(),
    );

    let resp = crate::routes::evm::evm_quote(
        State(st.clone()),
        HeaderMap::new(),
        Query::<HashMap<String, String>>(map),
    )
    .await;

    let data = parse_json_response(resp).await.map_err(|(status, msg)| {
        (
            if status == StatusCode::UNPROCESSABLE_ENTITY {
                StatusCode::UNPROCESSABLE_ENTITY
            } else {
                StatusCode::BAD_GATEWAY
            },
            if status == StatusCode::UNPROCESSABLE_ENTITY {
                "INSUFFICIENT_LIQUIDITY"
            } else {
                "UPSTREAM_DOWN"
            },
            msg,
        )
    })?;

    let buy_amount =
        as_string_path(&data, &[&["buyAmount"], &["quote", "buyAmount"]]).unwrap_or_default();
    let sell_amount = as_string_path(&data, &[&["sellAmount"], &["quote", "sellAmount"]])
        .unwrap_or_else(|| req.sell_amount.clone());
    let min_buy_amount = compute_min_buy_amount(&buy_amount, quote_slippage_bps(req));
    let allowance_target = as_string_path(
        &data,
        &[
            &["allowanceTarget"],
            &["approvalTarget"],
            &["spender"],
            &["issues", "allowance", "spender"],
            &["quote", "allowanceTarget"],
        ],
    );

    let source = data
        .get("source")
        .or_else(|| data.get("_source"))
        .and_then(|v| v.as_str())
        .unwrap_or("zeroex")
        .to_ascii_lowercase();
    let provider = if source.contains("oneinch") {
        "oneinch"
    } else {
        "zeroex"
    };

    let actions_required = if allowance_target.is_some() {
        if source.contains("permit2") {
            vec!["PERMIT2".to_string()]
        } else {
            vec!["APPROVE".to_string()]
        }
    } else {
        Vec::new()
    };

    let quote = QuoteResponse {
        quote_id: String::new(),
        expires_at: 0,
        chain_family: ChainFamily::Evm,
        chain_id: req.chain_id,
        cluster: None,
        sell_token: req.sell_token.clone(),
        buy_token: req.buy_token.clone(),
        sell_amount,
        buy_amount: buy_amount.clone(),
        min_buy_amount,
        estimated_network_fee: as_string_path(
            &data,
            &[&["totalNetworkFee"], &["estimatedGas"], &["gas"]],
        ),
        price_impact_bps: parse_price_impact_bps(&data),
        provider: provider.to_string(),
        actions_required,
        allowance_target,
        route_summary: summarize_evm_route(&data),
        alternatives: Vec::new(),
        warnings: Vec::new(),
    };
    Ok((data, quote))
}

async fn quote_solana_mainnet(
    st: AppState,
    req: &QuoteRequest,
) -> Result<(Value, QuoteResponse), (StatusCode, &'static str, String)> {
    let mut map = HashMap::new();
    map.insert("inputMint".to_string(), req.sell_token.clone());
    map.insert("outputMint".to_string(), req.buy_token.clone());
    map.insert("amount".to_string(), req.sell_amount.clone());
    map.insert(
        "slippageBps".to_string(),
        quote_slippage_bps(req).to_string(),
    );

    let resp = crate::routes::jupiter::jupiter_quote(
        State(st.clone()),
        HeaderMap::new(),
        Query::<HashMap<String, String>>(map),
    )
    .await
    .into_response();

    let data = parse_json_response(resp)
        .await
        .map_err(|(_, msg)| (StatusCode::BAD_GATEWAY, "UPSTREAM_DOWN", msg))?;

    let buy_amount = data
        .get("outAmount")
        .and_then(|v| v.as_str())
        .unwrap_or_default()
        .to_string();
    let min_buy_amount = data
        .get("otherAmountThreshold")
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
        .unwrap_or_else(|| compute_min_buy_amount(&buy_amount, quote_slippage_bps(req)));

    let route_summary = data
        .get("routePlan")
        .and_then(|v| v.as_array())
        .map(|steps| format!("Jupiter route ({} hops)", steps.len()))
        .unwrap_or_else(|| "Jupiter route".to_string());

    let quote = QuoteResponse {
        quote_id: String::new(),
        expires_at: 0,
        chain_family: ChainFamily::Solana,
        chain_id: None,
        cluster: req.cluster.clone(),
        sell_token: req.sell_token.clone(),
        buy_token: req.buy_token.clone(),
        sell_amount: data
            .get("inAmount")
            .and_then(|v| v.as_str())
            .unwrap_or(&req.sell_amount)
            .to_string(),
        buy_amount,
        min_buy_amount,
        estimated_network_fee: None,
        price_impact_bps: parse_jupiter_price_impact_bps(&data),
        provider: "jupiter".to_string(),
        actions_required: Vec::new(),
        allowance_target: None,
        route_summary,
        alternatives: Vec::new(),
        warnings: Vec::new(),
    };
    Ok((data, quote))
}

async fn quote_solana_testnet(
    st: AppState,
    req: &QuoteRequest,
) -> Result<(Value, QuoteResponse), (StatusCode, &'static str, String)> {
    let cfg_path = crate::app::backend_state_file("solana_amm_testnet.json");
    let cfg_raw = std::fs::read_to_string(&cfg_path).map_err(|_| {
        (
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Solana testnet AMM is not initialized".to_string(),
        )
    })?;
    let cfg: Value = serde_json::from_str(&cfg_raw).map_err(|_| {
        (
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Invalid Solana AMM config".to_string(),
        )
    })?;

    let token_a = cfg
        .get("tokenA")
        .and_then(|v| v.get("mint"))
        .and_then(|v| v.as_str())
        .unwrap_or_default()
        .to_ascii_lowercase();
    let token_b = cfg
        .get("tokenB")
        .and_then(|v| v.get("mint"))
        .and_then(|v| v.as_str())
        .unwrap_or_default()
        .to_ascii_lowercase();

    let direction_pref = req
        .preferences
        .as_ref()
        .and_then(|v| v.get("direction"))
        .and_then(|v| v.as_str());
    let sell = req.sell_token.to_ascii_lowercase();
    let buy = req.buy_token.to_ascii_lowercase();
    let direction = if let Some(d) = direction_pref {
        if d == "AtoB" || d == "BtoA" {
            d
        } else {
            return Err((
                StatusCode::BAD_REQUEST,
                "INVALID_QUOTE",
                "preferences.direction must be AtoB or BtoA".to_string(),
            ));
        }
    } else if sell == token_a && buy == token_b {
        "AtoB"
    } else if sell == token_b && buy == token_a {
        "BtoA"
    } else {
        return Err((
            StatusCode::BAD_REQUEST,
            "INVALID_QUOTE",
            "Token pair is not supported by Solana testnet AMM".to_string(),
        ));
    };

    let resp = crate::routes::solana_amm::solana_amm_quote(
        State(st),
        Ok(Json(crate::routes::solana_amm::SolQuoteIn {
            amount_in: req.sell_amount.clone(),
            direction: direction.to_string(),
            slippage_bps: Some(quote_slippage_bps(req) as u64),
        })),
    )
    .await
    .into_response();

    let data = parse_json_response(resp)
        .await
        .map_err(|(_, msg)| (StatusCode::BAD_GATEWAY, "UPSTREAM_DOWN", msg))?;

    let quote = QuoteResponse {
        quote_id: String::new(),
        expires_at: 0,
        chain_family: ChainFamily::Solana,
        chain_id: None,
        cluster: req.cluster.clone(),
        sell_token: req.sell_token.clone(),
        buy_token: req.buy_token.clone(),
        sell_amount: req.sell_amount.clone(),
        buy_amount: data
            .get("amountOut")
            .and_then(|v| v.as_str())
            .unwrap_or_default()
            .to_string(),
        min_buy_amount: data
            .get("minOut")
            .and_then(|v| v.as_str())
            .unwrap_or_default()
            .to_string(),
        estimated_network_fee: None,
        price_impact_bps: None,
        provider: "inhouse_amm".to_string(),
        actions_required: Vec::new(),
        allowance_target: None,
        route_summary: "Solana testnet AMM".to_string(),
        alternatives: Vec::new(),
        warnings: Vec::new(),
    };

    let provider_payload = json!({
        "quote": data,
        "direction": direction,
    });
    Ok((provider_payload, quote))
}

async fn quote_btc(
    st: AppState,
    req: &QuoteRequest,
) -> Result<(Value, QuoteResponse), (StatusCode, &'static str, String)> {
    let cluster = req.cluster.clone().unwrap_or_else(|| "mainnet".to_string());
    let destination = req.user_address.clone().unwrap_or_default();

    let base_urls = thorchain_urls(&st, &cluster);
    if base_urls.is_empty() {
        return Err((
            StatusCode::SERVICE_UNAVAILABLE,
            "PROVIDER_UNAVAILABLE_CHAIN",
            "THORChain endpoint blocked by backend allowlist. Add thornode.ninerealms.com and stagenet-thornode.ninerealms.com to ALLOWED_EXTERNAL_HOSTS/EXTERNAL_API_ALLOWLIST."
                .to_string(),
        ));
    }
    let mut last_error = String::new();

    for base in base_urls {
        let mut url = url::Url::parse(&base).map_err(|_| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "INVALID_QUOTE",
                "Invalid THORChain URL".to_string(),
            )
        })?;
        url.set_path("/thorchain/quote/swap");
        url.query_pairs_mut()
            .append_pair("from_asset", &req.sell_token)
            .append_pair("to_asset", &req.buy_token)
            .append_pair("amount", &req.sell_amount)
            .append_pair("destination", &destination)
            .append_pair("streaming_interval", "1")
            .append_pair("streaming_quantity", "0")
            .append_pair(
                "liquidity_tolerance_bps",
                &quote_slippage_bps(req).to_string(),
            );

        let url = match crate::app::validate_external_base_url(&st, &base)
            .ok()
            .and_then(|_| Some(url))
        {
            Some(v) => v,
            None => continue,
        };

        match crate::infra::rpc::fetch_with_host_policy(
            &st,
            reqwest::Method::GET,
            &url,
            None,
            None,
            std::time::Duration::from_secs(10),
            "thorchain_quote",
        )
        .await
        {
            Ok(data) => {
                let buy_amount = data
                    .get("expected_amount_out")
                    .and_then(|v| v.as_str())
                    .unwrap_or_default()
                    .to_string();
                let quote = QuoteResponse {
                    quote_id: String::new(),
                    expires_at: 0,
                    chain_family: ChainFamily::Btc,
                    chain_id: None,
                    cluster: Some(cluster.clone()),
                    sell_token: req.sell_token.clone(),
                    buy_token: req.buy_token.clone(),
                    sell_amount: req.sell_amount.clone(),
                    buy_amount: buy_amount.clone(),
                    min_buy_amount: buy_amount,
                    estimated_network_fee: data
                        .get("fees")
                        .and_then(|v| v.get("outbound"))
                        .and_then(|v| v.as_str())
                        .map(|s| s.to_string()),
                    price_impact_bps: None,
                    provider: "thorchain".to_string(),
                    actions_required: Vec::new(),
                    allowance_target: None,
                    route_summary: "THORChain".to_string(),
                    alternatives: Vec::new(),
                    warnings: data
                        .get("warning")
                        .and_then(|v| v.as_str())
                        .map(|w| vec![w.to_string()])
                        .unwrap_or_default(),
                };
                return Ok((data, quote));
            }
            Err(e) => {
                last_error = e;
            }
        }
    }

    Err((
        StatusCode::BAD_GATEWAY,
        "UPSTREAM_DOWN",
        if last_error.is_empty() {
            "THORChain quote failed".to_string()
        } else {
            last_error
        },
    ))
}

fn build_evm(
    response_value: &Value,
    provider_payload: &Value,
    request_value: &Value,
    quote_hash: &str,
) -> Result<BuildResponse, (StatusCode, &'static str, String)> {
    let tx = provider_payload
        .get("transaction")
        .or_else(|| provider_payload.get("tx"))
        .or_else(|| {
            provider_payload
                .get("quote")
                .and_then(|v| v.get("transaction"))
        })
        .or_else(|| response_value.get("transaction"))
        .or_else(|| response_value.get("tx"))
        .or_else(|| response_value.get("quote").and_then(|v| v.get("transaction")))
        .ok_or((
            StatusCode::CONFLICT,
            "PRICE_MOVED",
            "EVM route is no longer executable".to_string(),
        ))?;

    let to = tx
        .get("to")
        .or_else(|| tx.get("toAddress"))
        .and_then(|v| v.as_str())
        .unwrap_or_default();
    let data = tx
        .get("data")
        .or_else(|| tx.get("input"))
        .and_then(|v| v.as_str())
        .unwrap_or_default();
    if to.is_empty() || data.is_empty() {
        return Err((
            StatusCode::CONFLICT,
            "PRICE_MOVED",
            "EVM route payload missing transaction data".to_string(),
        ));
    }

    let value = tx
        .get("value")
        .and_then(|v| v.as_str())
        .unwrap_or("0")
        .to_string();

    let chain_id = request_value
        .get("chainId")
        .and_then(|v| v.as_u64())
        .unwrap_or_default();

    Ok(BuildResponse {
        build_id: random_base64url(12),
        expires_at: chrono_ms_now() + QUOTE_TTL_MS,
        chain_family: ChainFamily::Evm,
        signable: BuildSignable {
            tx_request: Some(EvmTxRequest {
                to: to.to_string(),
                data: data.to_string(),
                value,
                chain_id,
                gas: tx
                    .get("gas")
                    .and_then(|v| v.as_str())
                    .map(|s| s.to_string()),
                max_fee_per_gas: tx
                    .get("maxFeePerGas")
                    .and_then(|v| v.as_str())
                    .map(|s| s.to_string()),
                max_priority_fee_per_gas: tx
                    .get("maxPriorityFeePerGas")
                    .and_then(|v| v.as_str())
                    .map(|s| s.to_string()),
            }),
            base64_tx: None,
            recent_blockhash: None,
            btc_payload: None,
        },
        integrity: BuildIntegrity {
            quote_hash: quote_hash.to_string(),
        },
        warnings: Vec::new(),
    })
}

async fn build_solana(
    st: AppState,
    request_value: &Value,
    provider_payload: &Value,
    user_address: &str,
    quote_hash: &str,
) -> Result<BuildResponse, (StatusCode, &'static str, String)> {
    let cluster = request_value
        .get("cluster")
        .and_then(|v| v.as_str())
        .unwrap_or("mainnet");

    if cluster.eq_ignore_ascii_case("mainnet") {
        let quote_response = provider_payload.clone();
        let resp = crate::routes::jupiter::jupiter_swap(
            State(st),
            HeaderMap::new(),
            Ok(Json(json!({
                "quoteResponse": quote_response,
                "userPublicKey": user_address,
                "wrapAndUnwrapSol": true,
                "dynamicComputeUnitLimit": true
            }))),
        )
        .await
        .into_response();

        let data = parse_json_response(resp)
            .await
            .map_err(|(_, msg)| (StatusCode::BAD_GATEWAY, "UPSTREAM_DOWN", msg))?;

        return Ok(BuildResponse {
            build_id: random_base64url(12),
            expires_at: chrono_ms_now() + QUOTE_TTL_MS,
            chain_family: ChainFamily::Solana,
            signable: BuildSignable {
                tx_request: None,
                base64_tx: data
                    .get("swapTransaction")
                    .and_then(|v| v.as_str())
                    .map(|s| s.to_string()),
                recent_blockhash: None,
                btc_payload: None,
            },
            integrity: BuildIntegrity {
                quote_hash: quote_hash.to_string(),
            },
            warnings: Vec::new(),
        });
    }

    let direction = provider_payload
        .get("direction")
        .and_then(|v| v.as_str())
        .unwrap_or_default();
    let min_out = provider_payload
        .get("quote")
        .and_then(|v| v.get("minOut"))
        .and_then(|v| v.as_str())
        .unwrap_or("0")
        .to_string();
    let amount_in = request_value
        .get("sellAmount")
        .and_then(|v| v.as_str())
        .unwrap_or("0")
        .to_string();

    let resp = crate::routes::solana_amm::solana_amm_swap_tx(
        State(st),
        Ok(Json(crate::routes::solana_amm::SolSwapTxIn {
            amount_in: Some(amount_in),
            min_out: Some(min_out),
            direction: Some(direction.to_string()),
            user_public_key: Some(user_address.to_string()),
        })),
    )
    .await
    .into_response();

    let data = parse_json_response(resp)
        .await
        .map_err(|(_, msg)| (StatusCode::BAD_GATEWAY, "UPSTREAM_DOWN", msg))?;

    Ok(BuildResponse {
        build_id: random_base64url(12),
        expires_at: chrono_ms_now() + QUOTE_TTL_MS,
        chain_family: ChainFamily::Solana,
        signable: BuildSignable {
            tx_request: None,
            base64_tx: data
                .get("swap_transaction")
                .or_else(|| data.get("swapTransaction"))
                .and_then(|v| v.as_str())
                .map(|s| s.to_string()),
            recent_blockhash: None,
            btc_payload: None,
        },
        integrity: BuildIntegrity {
            quote_hash: quote_hash.to_string(),
        },
        warnings: Vec::new(),
    })
}

fn build_btc(
    response_value: &Value,
    provider_payload: &Value,
    quote_hash: &str,
) -> Result<BuildResponse, (StatusCode, &'static str, String)> {
    let _ = response_value;
    Ok(BuildResponse {
        build_id: random_base64url(12),
        expires_at: chrono_ms_now() + QUOTE_TTL_MS,
        chain_family: ChainFamily::Btc,
        signable: BuildSignable {
            tx_request: None,
            base64_tx: None,
            recent_blockhash: None,
            btc_payload: Some(json!({
                "inbound_address": provider_payload.get("inbound_address").and_then(|v| v.as_str()).unwrap_or_default(),
                "memo": provider_payload.get("memo").and_then(|v| v.as_str()).unwrap_or_default(),
                "expected_amount_out": provider_payload.get("expected_amount_out").and_then(|v| v.as_str()).unwrap_or_default(),
                "recommended_min_amount_in": provider_payload.get("recommended_min_amount_in").and_then(|v| v.as_str()),
                "expiry": provider_payload.get("expiry").and_then(|v| v.as_i64()),
            })),
        },
        integrity: BuildIntegrity {
            quote_hash: quote_hash.to_string(),
        },
        warnings: Vec::new(),
    })
}

fn quote_slippage_bps(req: &QuoteRequest) -> u32 {
    match &req.slippage {
        SlippageInput::Auto => 50,
        SlippageInput::Fixed { bps } => *bps,
    }
}

fn compute_min_buy_amount(buy_amount: &str, slippage_bps: u32) -> String {
    let buy = buy_amount.parse::<u128>().unwrap_or(0);
    let min = buy.saturating_mul((10_000_u128).saturating_sub(slippage_bps as u128)) / 10_000;
    min.to_string()
}

fn parse_price_impact_bps(data: &Value) -> Option<u32> {
    let candidates = [
        "priceImpact",
        "estimatedPriceImpact",
        "priceImpactPercentage",
    ];
    for key in candidates {
        if let Some(v) = data.get(key) {
            if let Some(s) = v.as_str() {
                if let Ok(parsed) = s.parse::<f64>() {
                    return Some((parsed * 100.0).max(0.0) as u32);
                }
            }
            if let Some(n) = v.as_f64() {
                return Some((n * 100.0).max(0.0) as u32);
            }
        }
    }
    None
}

fn parse_jupiter_price_impact_bps(data: &Value) -> Option<u32> {
    let raw = data.get("priceImpactPct")?;
    let pct = if let Some(s) = raw.as_str() {
        s.parse::<f64>().ok()?
    } else {
        raw.as_f64()?
    };
    Some((pct * 10_000.0).max(0.0) as u32)
}

fn summarize_evm_route(data: &Value) -> String {
    if let Some(len) = data
        .get("route")
        .and_then(|v| v.get("tokenPath"))
        .and_then(|v| v.as_array())
        .map(|a| a.len())
    {
        return format!("{} hops", len.saturating_sub(1));
    }
    "Aggregated route".to_string()
}

fn thorchain_urls(st: &AppState, cluster: &str) -> Vec<String> {
    let testnet =
        cluster.eq_ignore_ascii_case("testnet") || cluster.eq_ignore_ascii_case("stagenet");
    let override_key = if testnet {
        std::env::var("THORCHAIN_API_URL_TESTNET").ok()
    } else {
        std::env::var("THORCHAIN_API_URL").ok()
    };

    let mut urls = Vec::new();
    if let Some(value) = override_key {
        if !value.trim().is_empty() {
            urls.push(value);
        }
    }
    let default = if testnet {
        "https://stagenet-thornode.ninerealms.com"
    } else {
        "https://thornode.ninerealms.com"
    };
    if !urls.iter().any(|u| u == default) {
        urls.push(default.to_string());
    }

    let mut filtered = Vec::new();
    for url in urls {
        if crate::app::validate_external_base_url(st, &url).is_ok() {
            filtered.push(url);
        }
    }
    filtered
}

fn compute_quote_hash(request: &Value, response: &Value) -> String {
    let mut hasher = Sha256::new();
    hasher.update(request.to_string().as_bytes());
    hasher.update(response.to_string().as_bytes());
    hex::encode(hasher.finalize())
}

fn as_string_path(value: &Value, paths: &[&[&str]]) -> Option<String> {
    for path in paths {
        let mut current = value;
        let mut ok = true;
        for key in *path {
            if let Some(next) = current.get(*key) {
                current = next;
            } else {
                ok = false;
                break;
            }
        }
        if ok {
            if let Some(s) = current.as_str() {
                if !s.trim().is_empty() {
                    return Some(s.to_string());
                }
            }
        }
    }
    None
}

async fn parse_json_response(resp: Response) -> Result<Value, (StatusCode, String)> {
    let status = resp.status();
    let bytes = to_bytes(resp.into_body(), usize::MAX)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    let json = serde_json::from_slice::<Value>(&bytes).unwrap_or_else(|_| json!({}));

    if status.is_success() {
        Ok(json)
    } else {
        let message = json
            .get("error")
            .and_then(|v| v.get("message").or(Some(v)))
            .and_then(|v| v.as_str())
            .map(|s| s.to_string())
            .unwrap_or_else(|| "upstream request failed".to_string());
        Err((status, message))
    }
}

fn error_response(status: StatusCode, code: &'static str, message: impl AsRef<str>) -> Response {
    (
        status,
        Json(json!({
            "error": {
                "code": code,
                "message": message.as_ref(),
            }
        })),
    )
        .into_response()
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::{
        body::{to_bytes, Body},
        http::{Method, Request},
    };
    use tower::util::ServiceExt;

    fn test_config() -> crate::config::Config {
        crate::config::Config {
            port: 0,
            node_env: "test".to_string(),
            cors_origins: "http://localhost:5173".to_string(),
            cookie_samesite: "Lax".to_string(),
            session_jwt_secret: "test_session_secret_1234567890".to_string(),
            cloud_kek_secret: "test_cloud_kek_secret_1234567890".to_string(),
            google_oauth_client_id: String::new(),
            google_oauth_client_secret: String::new(),
            google_oauth_redirect_uri: "postmessage".to_string(),
            apple_client_id: String::new(),
            apple_team_id: String::new(),
            apple_key_id: String::new(),
            apple_private_key_pem: String::new(),
            apple_oauth_redirect_uri: String::new(),
            external_api_allowlist: "thornode.ninerealms.com,stagenet-thornode.ninerealms.com,api.0x.org,sepolia.api.0x.org,bsc.api.0x.org,polygon.api.0x.org,avalanche.api.0x.org,api.jup.ag".to_string(),
            proxy_host_allowlist: String::new(),
            external_host_allowlist: "thornode.ninerealms.com,stagenet-thornode.ninerealms.com,api.0x.org,sepolia.api.0x.org,bsc.api.0x.org,polygon.api.0x.org,avalanche.api.0x.org,api.jup.ag".to_string(),
            rpc_url_allowlist: "rpc.ankr.com,api.mainnet-beta.solana.com,api.devnet.solana.com,api.testnet.solana.com".to_string(),
            zerox_api_key: String::new(),
            fallback_aggregator_url: String::new(),
            fallback_aggregator_provider: "zeroex_compat".to_string(),
            fallback_aggregator_api_key: String::new(),
            fallback_aggregator_api_header: "x-api-key".to_string(),
            fallback_aggregator_quote_path: "/swap/allowance-holder/quote".to_string(),
            fallback_aggregator_price_path: "/swap/allowance-holder/price".to_string(),
            fallback_aggregator_version: String::new(),
            jupiter_api_url: "https://api.jup.ag/swap/v1".to_string(),
            jupiter_api_key: String::new(),
            admin_token: "test-admin-token".to_string(),
            allow_insecure_admin_dev: false,
            disable_admin_scripts: false,
            solana_admin_strict: false,
            enable_server_kek: false,
            rpc_max_request_body_bytes: 256 * 1024,
            rpc_max_response_body_bytes: 2 * 1024 * 1024,
            rpc_max_batch_items: 25,
            rpc_per_host_max_concurrency: 8,
            rpc_method_rate_limit_max_requests: 60,
            rpc_method_rate_limit_window_ms: 60_000,
            infra_store_backend: "memory".to_string(),
            infra_redis_url: "redis://127.0.0.1:6379/".to_string(),
            infra_redis_key_prefix: "elementa-test".to_string(),
            infra_redis_required: false,
            infra_redis_connect_timeout_ms: 50,
            infra_redis_command_timeout_ms: 500,
        }
    }

    async fn build_test_state() -> crate::infra::state::AppState {
        crate::infra::state::AppState::new_with_solana(
            test_config(),
            std::sync::Arc::new(crate::infra::state::RealSolanaAmmApi),
        )
        .await
        .expect("test state")
    }

    async fn json_request(
        app: axum::Router,
        method: Method,
        path: &str,
        body: serde_json::Value,
    ) -> (StatusCode, serde_json::Value) {
        let req = Request::builder()
            .method(method)
            .uri(path)
            .header("content-type", "application/json")
            .body(Body::from(body.to_string()))
            .expect("request");
        let resp = app.oneshot(req).await.expect("response");
        let status = resp.status();
        let bytes = to_bytes(resp.into_body(), usize::MAX)
            .await
            .expect("read body");
        let json =
            serde_json::from_slice::<serde_json::Value>(&bytes).unwrap_or_else(|_| json!({}));
        (status, json)
    }

    #[tokio::test]
    async fn quote_rejects_unknown_fields() {
        let st = build_test_state().await;
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/quote",
            json!({
                "chainFamily":"evm",
                "chainId":1,
                "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                "sellAmount":"1000000000000000",
                "slippage":{"mode":"fixed","bps":50},
                "unexpected":"field"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("invalid_json_body")
        );
    }

    #[tokio::test]
    async fn build_rejects_unknown_fields() {
        let st = build_test_state().await;
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/build",
            json!({
                "quoteId":"abc",
                "userAddress":"0x1111111111111111111111111111111111111111",
                "unexpected":"field"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("invalid_json_body")
        );
    }

    #[tokio::test]
    async fn build_returns_quote_expired_for_expired_snapshot() {
        let st = build_test_state().await;
        st.swap_quotes.insert(
            "expired-quote".to_string(),
            crate::infra::state::SwapQuoteSnapshot {
                expires_at_ms: chrono_ms_now() - 1,
                request: json!({"chainFamily":"evm","chainId":1}),
                response: json!({"sellAmount":"1","buyAmount":"1"}),
                provider_payload: json!({}),
                quote_hash: "deadbeef".to_string(),
            },
        );
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/build",
            json!({
                "quoteId":"expired-quote",
                "userAddress":"0x1111111111111111111111111111111111111111"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::GONE);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("QUOTE_EXPIRED")
        );
    }

    #[tokio::test]
    async fn build_returns_invalid_quote_for_missing_snapshot() {
        let st = build_test_state().await;
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/build",
            json!({
                "quoteId":"missing",
                "userAddress":"0x1111111111111111111111111111111111111111"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::NOT_FOUND);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("INVALID_QUOTE")
        );
    }

    #[tokio::test]
    async fn build_returns_price_moved_when_evm_tx_missing() {
        let st = build_test_state().await;
        st.swap_quotes.insert(
            "evm-no-tx".to_string(),
            crate::infra::state::SwapQuoteSnapshot {
                expires_at_ms: chrono_ms_now() + 10_000,
                request: json!({
                    "chainFamily":"evm",
                    "chainId":1,
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    "sellAmount":"1"
                }),
                response: json!({
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    "sellAmount":"1",
                    "buyAmount":"1"
                }),
                provider_payload: json!({}),
                quote_hash: "deadbeef".to_string(),
            },
        );
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/build",
            json!({
                "quoteId":"evm-no-tx",
                "userAddress":"0x1111111111111111111111111111111111111111"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::CONFLICT);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("PRICE_MOVED")
        );
    }

    #[tokio::test]
    async fn build_uses_evm_provider_payload_transaction() {
        let st = build_test_state().await;
        st.swap_quotes.insert(
            "evm-provider-tx".to_string(),
            crate::infra::state::SwapQuoteSnapshot {
                expires_at_ms: chrono_ms_now() + 10_000,
                request: json!({
                    "chainFamily":"evm",
                    "chainId":1,
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    "sellAmount":"1"
                }),
                response: json!({
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    "sellAmount":"1",
                    "buyAmount":"1"
                }),
                provider_payload: json!({
                    "transaction":{
                        "to":"0x1111111111111111111111111111111111111111",
                        "data":"0x01",
                        "value":"0"
                    }
                }),
                quote_hash: "deadbeef".to_string(),
            },
        );
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/build",
            json!({
                "quoteId":"evm-provider-tx",
                "userAddress":"0x1111111111111111111111111111111111111111"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::OK);
        assert_eq!(
            body.get("signable")
                .and_then(|v| v.get("txRequest"))
                .and_then(|v| v.get("to"))
                .and_then(|v| v.as_str()),
            Some("0x1111111111111111111111111111111111111111")
        );
    }

    #[tokio::test]
    async fn build_returns_invalid_quote_for_corrupted_snapshot() {
        let st = build_test_state().await;
        st.swap_quotes.insert(
            "evm-corrupted".to_string(),
            crate::infra::state::SwapQuoteSnapshot {
                expires_at_ms: chrono_ms_now() + 10_000,
                request: json!({
                    "chainFamily":"evm",
                    "chainId":1,
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
                }),
                response: json!({
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    "sellAmount":"1",
                    "buyAmount":"1",
                    "transaction":{"to":"0x1111111111111111111111111111111111111111","data":"0x01"}
                }),
                provider_payload: json!({}),
                quote_hash: "deadbeef".to_string(),
            },
        );
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/build",
            json!({
                "quoteId":"evm-corrupted",
                "userAddress":"0x1111111111111111111111111111111111111111"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("INVALID_QUOTE")
        );
    }

    #[tokio::test]
    async fn build_returns_price_moved_for_snapshot_token_amount_mismatch() {
        let st = build_test_state().await;
        st.swap_quotes.insert(
            "evm-mismatch".to_string(),
            crate::infra::state::SwapQuoteSnapshot {
                expires_at_ms: chrono_ms_now() + 10_000,
                request: json!({
                    "chainFamily":"evm",
                    "chainId":1,
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    "sellAmount":"100"
                }),
                response: json!({
                    "sellToken":"0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE",
                    "buyToken":"0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    "sellAmount":"101",
                    "buyAmount":"1",
                    "transaction":{"to":"0x1111111111111111111111111111111111111111","data":"0x01"}
                }),
                provider_payload: json!({}),
                quote_hash: "deadbeef".to_string(),
            },
        );
        let app = axum::Router::new()
            .nest("/api/v1/swap", router())
            .with_state(st);
        let (status, body) = json_request(
            app,
            Method::POST,
            "/api/v1/swap/build",
            json!({
                "quoteId":"evm-mismatch",
                "userAddress":"0x1111111111111111111111111111111111111111"
            }),
        )
        .await;
        assert_eq!(status, StatusCode::CONFLICT);
        assert_eq!(
            body.get("error")
                .and_then(|e| e.get("code"))
                .and_then(|v| v.as_str()),
            Some("PRICE_MOVED")
        );
    }
}
