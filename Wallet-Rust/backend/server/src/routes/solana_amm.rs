use axum::{
    extract::rejection::JsonRejection,
    extract::State,
    http::{HeaderMap, StatusCode},
    response::IntoResponse,
    routing::{get, post},
    Json, Router,
};
use std::sync::atomic::Ordering;
use std::time::Duration;

use earth_solana_tooling::types::{
    InitParams as SolInitParams, MintParams as SolMintParams, MintToken as SolMintToken,
    SwapDirection as SolSwapDirection, SwapTxParams as SolSwapTxParams,
};

use crate::app::{
    backend_state_file, chrono_ms_now, is_admin_allowed, parse_u64_str, random_base64url, AppState,
};

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/status", get(solana_amm_status))
        .route("/quote", post(solana_amm_quote))
        .route("/swap-tx", post(solana_amm_swap_tx))
        .route("/mint", post(solana_amm_mint))
        .route("/init", post(solana_amm_init))
}

pub(crate) async fn solana_amm_status() -> impl IntoResponse {
    let p = backend_state_file("solana_amm_testnet.json");
    if !p.exists() {
        return Json(serde_json::json!({ "ok": false, "config": serde_json::Value::Null }));
    }
    match std::fs::read_to_string(&p)
        .ok()
        .and_then(|s| serde_json::from_str::<serde_json::Value>(&s).ok())
    {
        Some(v) => Json(serde_json::json!({ "ok": true, "config": v })),
        None => Json(serde_json::json!({ "ok": false, "config": serde_json::Value::Null })),
    }
}

#[derive(serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct SolQuoteIn {
    #[serde(rename = "amountIn")]
    pub(crate) amount_in: String,
    pub(crate) direction: String,
    #[serde(rename = "slippageBps")]
    pub(crate) slippage_bps: Option<u64>,
}

pub(crate) async fn solana_amm_quote(
    State(st): State<AppState>,
    input: Result<Json<SolQuoteIn>, JsonRejection>,
) -> impl IntoResponse {
    let Json(input) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/solana/amm/quote",
                rejection,
                None,
            );
        }
    };
    let cfg_path = backend_state_file("solana_amm_testnet.json");
    let cfg: serde_json::Value = match std::fs::read_to_string(&cfg_path)
        .ok()
        .and_then(|s| serde_json::from_str(&s).ok())
    {
        Some(v) => v,
        None => {
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Solana AMM not initialized" })),
            )
                .into_response();
        }
    };
    let rpc_url = cfg
        .get("rpcUrl")
        .and_then(|v| v.as_str())
        .unwrap_or("https://api.testnet.solana.com");
    let vault_a = cfg.get("vaultA").and_then(|v| v.as_str()).unwrap_or("");
    let vault_b = cfg.get("vaultB").and_then(|v| v.as_str()).unwrap_or("");
    if vault_a.is_empty() || vault_b.is_empty() {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "Solana AMM not initialized" })),
        )
            .into_response();
    }
    let (vault_in, vault_out) = if input.direction == "AtoB" {
        (vault_a, vault_b)
    } else if input.direction == "BtoA" {
        (vault_b, vault_a)
    } else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Invalid direction" })),
        )
            .into_response();
    };

    let reserve_in = match solana_token_amount(&st, rpc_url, vault_in).await {
        Ok(v) => v,
        Err(e) => {
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Quote failed", "details": e })),
            )
                .into_response();
        }
    };
    let reserve_out = match solana_token_amount(&st, rpc_url, vault_out).await {
        Ok(v) => v,
        Err(e) => {
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Quote failed", "details": e })),
            )
                .into_response();
        }
    };
    let amount_in: u128 = match input.amount_in.parse() {
        Ok(v) if v > 0 => v,
        _ => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": "Invalid amountIn" })),
            )
                .into_response();
        }
    };
    if reserve_in == 0 || reserve_out == 0 {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "No liquidity" })),
        )
            .into_response();
    }

    let fee_bps: u128 = cfg.get("feeBps").and_then(|v| v.as_u64()).unwrap_or(30) as u128;
    let amount_after_fee = amount_in - ((amount_in * fee_bps) / 10_000);
    let numerator = amount_after_fee.saturating_mul(reserve_out);
    let denominator = reserve_in.saturating_add(amount_after_fee);
    let amount_out = if denominator == 0 {
        0
    } else {
        numerator / denominator
    };
    let slippage = input.slippage_bps.unwrap_or(50).clamp(1, 5000) as u128;
    let min_out = amount_out.saturating_sub((amount_out * slippage) / 10_000);
    (
        StatusCode::OK,
        Json(serde_json::json!({
            "amountOut": amount_out.to_string(),
            "minOut": min_out.to_string(),
            "feeBps": fee_bps as u64
        })),
    )
        .into_response()
}

async fn solana_token_amount(
    st: &AppState,
    rpc_url: &str,
    token_account: &str,
) -> Result<u128, String> {
    let url = crate::infra::rpc::validate_rpc_url(st, rpc_url)?;
    let payload = serde_json::json!({
      "jsonrpc": "2.0",
      "id": 1,
      "method": "getTokenAccountBalance",
      "params": [token_account, {"commitment": "confirmed"}]
    });

    let v = crate::infra::rpc::fetch_with_host_policy(
        st,
        reqwest::Method::POST,
        &url,
        Some(payload),
        None,
        Duration::from_secs(10),
        "solana_amm_status",
    )
    .await?;

    let amount = v
        .get("result")
        .and_then(|v| v.get("value"))
        .and_then(|v| v.get("amount"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| "missing token balance".to_string())?;
    amount.parse::<u128>().map_err(|e| e.to_string())
}

#[derive(serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct SolSwapTxIn {
    #[serde(rename = "amountIn")]
    pub(crate) amount_in: Option<String>,
    #[serde(rename = "minOut")]
    pub(crate) min_out: Option<String>,
    pub(crate) direction: Option<String>,
    #[serde(rename = "userPublicKey")]
    pub(crate) user_public_key: Option<String>,
}

pub(crate) async fn solana_amm_swap_tx(
    State(st): State<AppState>,
    input: Result<Json<SolSwapTxIn>, JsonRejection>,
) -> impl IntoResponse {
    let Json(input) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/solana/amm/swap-tx",
                rejection,
                None,
            );
        }
    };
    let request_id = random_base64url(6);
    tracing::info!(request_id = %request_id, "solana swap-tx request");
    let Some(amount_in_raw) = input.amount_in.as_deref() else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing amountIn, minOut, direction, or userPublicKey" })),
        )
            .into_response();
    };
    let Some(min_out_raw) = input.min_out.as_deref() else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing amountIn, minOut, direction, or userPublicKey" })),
        )
            .into_response();
    };
    let Some(direction_raw) = input.direction.as_deref() else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing amountIn, minOut, direction, or userPublicKey" })),
        )
            .into_response();
    };
    let Some(user_public_key_raw) = input.user_public_key.as_deref() else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing amountIn, minOut, direction, or userPublicKey" })),
        )
            .into_response();
    };

    let amount_in = match parse_u64_str(amount_in_raw, "amountIn") {
        Ok(v) => v,
        Err(e) => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": e })),
            )
                .into_response();
        }
    };
    let min_out = match parse_u64_str(min_out_raw, "minOut") {
        Ok(v) => v,
        Err(e) => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": e })),
            )
                .into_response();
        }
    };
    let direction = match direction_raw {
        "AtoB" => SolSwapDirection::AtoB,
        "BtoA" => SolSwapDirection::BtoA,
        _ => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": "Invalid direction" })),
            )
                .into_response();
        }
    };
    if user_public_key_raw.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing amountIn, minOut, direction, or userPublicKey" })),
        )
            .into_response();
    }
    let user_public_key = user_public_key_raw.to_string();

    let cfg_path = backend_state_file("solana_amm_testnet.json");

    let cfg = match st.solana_api.load_config(&cfg_path) {
        Ok(v) => v,
        Err(_) => {
            st.solana_metrics
                .swap_tx_err
                .fetch_add(1, Ordering::Relaxed);
            tracing::warn!(request_id = %request_id, "solana swap-tx config not initialized");
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Solana AMM not initialized" })),
            )
                .into_response();
        }
    };

    let solana_api = st.solana_api.clone();
    let build = tokio::task::spawn_blocking(move || {
        solana_api.build_swap_tx(
            &cfg,
            SolSwapTxParams {
                amount_in,
                min_out,
                direction,
                user_public_key,
            },
        )
    })
    .await;

    match build {
        Ok(Ok(out)) => {
            st.solana_metrics.swap_tx_ok.fetch_add(1, Ordering::Relaxed);
            tracing::info!(
                request_id = %request_id,
                last_valid_block_height = out.last_valid_block_height,
                "solana swap-tx success"
            );
            (
                StatusCode::OK,
                Json(serde_json::to_value(out).unwrap_or_else(|_| serde_json::json!({}))),
            )
                .into_response()
        }
        Ok(Err(e)) => {
            st.solana_metrics
                .swap_tx_err
                .fetch_add(1, Ordering::Relaxed);
            tracing::warn!(request_id = %request_id, error = %e, "solana swap-tx failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Swap tx failed" })),
            )
                .into_response()
        }
        Err(e) => {
            st.solana_metrics
                .swap_tx_err
                .fetch_add(1, Ordering::Relaxed);
            tracing::warn!(request_id = %request_id, error = %e, "solana swap-tx join failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Swap tx failed" })),
            )
                .into_response()
        }
    }
}

#[derive(serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct SolMintIn {
    #[serde(rename = "userPublicKey")]
    user_public_key: Option<String>,
    token: Option<String>,
    amount: Option<String>,
}

pub(crate) async fn solana_amm_mint(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<SolMintIn>, JsonRejection>,
) -> impl IntoResponse {
    let Json(input) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/solana/amm/mint",
                rejection,
                Some(&headers),
            );
        }
    };
    let request_id = random_base64url(6);
    tracing::info!(request_id = %request_id, "solana mint request");
    if !is_admin_allowed(&st, &headers) {
        st.solana_metrics.mint_err.fetch_add(1, Ordering::Relaxed);
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "Forbidden" })),
        )
            .into_response();
    }
    let Some(user_public_key_raw) = input.user_public_key.as_deref() else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing userPublicKey, token, or amount" })),
        )
            .into_response();
    };
    let Some(token_raw) = input.token.as_deref() else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing userPublicKey, token, or amount" })),
        )
            .into_response();
    };
    let Some(amount_raw) = input.amount.as_deref() else {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing userPublicKey, token, or amount" })),
        )
            .into_response();
    };

    let amount = match parse_u64_str(amount_raw, "amount") {
        Ok(v) => v,
        Err(e) => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": e })),
            )
                .into_response();
        }
    };
    if user_public_key_raw.trim().is_empty() || token_raw.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Missing userPublicKey, token, or amount" })),
        )
            .into_response();
    }
    let user_public_key = user_public_key_raw.to_string();
    let token = match token_raw.to_uppercase().as_str() {
        "A" => SolMintToken::A,
        "B" => SolMintToken::B,
        _ => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({ "error": "Token must be A or B" })),
            )
                .into_response();
        }
    };
    let Some(keypair_path) = std::env::var("SOLANA_DEPLOYER_KEYPAIR")
        .ok()
        .filter(|s| !s.trim().is_empty())
    else {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "Missing SOLANA_DEPLOYER_KEYPAIR" })),
        )
            .into_response();
    };

    let cfg_path = backend_state_file("solana_amm_testnet.json");
    let cfg = match st.solana_api.load_config(&cfg_path) {
        Ok(v) => v,
        Err(_) => {
            st.solana_metrics.mint_err.fetch_add(1, Ordering::Relaxed);
            tracing::warn!(request_id = %request_id, "solana mint config not initialized");
            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Solana AMM not initialized" })),
            )
                .into_response();
        }
    };

    let solana_api = st.solana_api.clone();
    let run = tokio::task::spawn_blocking(move || {
        solana_api.mint_test_tokens(
            &cfg,
            &keypair_path,
            SolMintParams {
                user_public_key,
                token,
                amount,
            },
        )
    })
    .await;

    match run {
        Ok(Ok(out)) => {
            st.solana_metrics.mint_ok.fetch_add(1, Ordering::Relaxed);
            tracing::info!(
                request_id = %request_id,
                tx_signature = %out.tx_signature,
                "solana mint success"
            );
            (
                StatusCode::OK,
                Json(serde_json::json!({
                    "ok": true,
                    "mint": out.mint,
                    "destination": out.destination,
                    "amount": out.amount,
                    "txSignature": out.tx_signature
                })),
            )
                .into_response()
        }
        Ok(Err(e)) => {
            st.solana_metrics.mint_err.fetch_add(1, Ordering::Relaxed);
            tracing::warn!(request_id = %request_id, error = %e, "solana mint failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Mint failed" })),
            )
                .into_response()
        }
        Err(e) => {
            st.solana_metrics.mint_err.fetch_add(1, Ordering::Relaxed);
            tracing::warn!(request_id = %request_id, error = %e, "solana mint join failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Mint failed" })),
            )
                .into_response()
        }
    }
}

#[derive(serde::Deserialize)]
#[serde(deny_unknown_fields)]
pub(crate) struct SolInitIn {}

pub(crate) async fn solana_amm_init(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<SolInitIn>, JsonRejection>,
) -> impl IntoResponse {
    if let Err(rejection) = input {
        return crate::app::invalid_json_response(
            &st.api_metrics,
            "/api/solana/amm/init",
            rejection,
            Some(&headers),
        );
    }
    let request_id = random_base64url(6);
    tracing::info!(request_id = %request_id, "solana init request");
    if !is_admin_allowed(&st, &headers) {
        st.solana_metrics.init_err.fetch_add(1, Ordering::Relaxed);
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "Forbidden" })),
        )
            .into_response();
    }

    let Some(keypair_path) = std::env::var("SOLANA_DEPLOYER_KEYPAIR")
        .ok()
        .filter(|s| !s.trim().is_empty())
    else {
        return (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({ "error": "Missing SOLANA_DEPLOYER_KEYPAIR or SOLANA_AMM_PROGRAM_ID" }))).into_response();
    };
    let Some(program_id) = std::env::var("SOLANA_AMM_PROGRAM_ID")
        .ok()
        .filter(|s| !s.trim().is_empty())
    else {
        return (StatusCode::INTERNAL_SERVER_ERROR, Json(serde_json::json!({ "error": "Missing SOLANA_DEPLOYER_KEYPAIR or SOLANA_AMM_PROGRAM_ID" }))).into_response();
    };

    let rpc_url = std::env::var("SOLANA_RPC_URL")
        .ok()
        .filter(|s| !s.trim().is_empty())
        .unwrap_or_else(|| "https://api.testnet.solana.com".to_string());
    let fee_bps = std::env::var("SOLANA_AMM_FEE_BPS")
        .ok()
        .and_then(|s| s.parse::<u16>().ok())
        .unwrap_or(30);
    let token_a_decimals = std::env::var("SOLANA_TOKEN_A_DECIMALS")
        .ok()
        .and_then(|s| s.parse::<u8>().ok())
        .unwrap_or(6);
    let token_b_decimals = std::env::var("SOLANA_TOKEN_B_DECIMALS")
        .ok()
        .and_then(|s| s.parse::<u8>().ok())
        .unwrap_or(6);
    let token_a_symbol =
        std::env::var("SOLANA_TOKEN_A_SYMBOL").unwrap_or_else(|_| "TSTA".to_string());
    let token_b_symbol =
        std::env::var("SOLANA_TOKEN_B_SYMBOL").unwrap_or_else(|_| "TSTB".to_string());
    let liquidity_a_tokens =
        std::env::var("SOLANA_LIQUIDITY_A").unwrap_or_else(|_| "100000".to_string());
    let liquidity_b_tokens =
        std::env::var("SOLANA_LIQUIDITY_B").unwrap_or_else(|_| "100000".to_string());
    let cfg_path = backend_state_file("solana_amm_testnet.json");

    let solana_api = st.solana_api.clone();
    let run = tokio::task::spawn_blocking(move || {
        solana_api.init_amm(
            &cfg_path,
            SolInitParams {
                rpc_url,
                keypair_path,
                program_id,
                fee_bps,
                token_a_decimals,
                token_b_decimals,
                token_a_symbol,
                token_b_symbol,
                liquidity_a_tokens,
                liquidity_b_tokens,
            },
        )
    })
    .await;

    match run {
        Ok(Ok(out)) => {
            st.solana_metrics.init_ok.fetch_add(1, Ordering::Relaxed);
            let mut runtime = st.solana_runtime.write().await;
            runtime.last_init_success = Some(true);
            runtime.last_init_at_ms = Some(chrono_ms_now());
            runtime.last_init_error = None;
            tracing::info!(
                request_id = %request_id,
                tx_count = out.tx_signatures.len(),
                "solana init success"
            );
            (
                StatusCode::OK,
                Json(serde_json::json!({ "ok": true, "config": out.config, "txSignatures": out.tx_signatures })),
            )
                .into_response()
        }
        Ok(Err(e)) => {
            st.solana_metrics.init_err.fetch_add(1, Ordering::Relaxed);
            let mut runtime = st.solana_runtime.write().await;
            runtime.last_init_success = Some(false);
            runtime.last_init_at_ms = Some(chrono_ms_now());
            runtime.last_init_error = Some("Solana AMM init failed".to_string());
            tracing::warn!(request_id = %request_id, error = %e, "solana init failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Solana AMM init failed" })),
            )
                .into_response()
        }
        Err(e) => {
            st.solana_metrics.init_err.fetch_add(1, Ordering::Relaxed);
            let mut runtime = st.solana_runtime.write().await;
            runtime.last_init_success = Some(false);
            runtime.last_init_at_ms = Some(chrono_ms_now());
            runtime.last_init_error = Some("Solana AMM init failed".to_string());
            tracing::warn!(request_id = %request_id, error = %e, "solana init join failed");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({ "error": "Solana AMM init failed" })),
            )
                .into_response()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::parse_u64_str;

    #[test]
    fn parse_u64_str_accepts_positive_u64() {
        assert_eq!(parse_u64_str("42", "amount"), Ok(42));
    }

    #[test]
    fn parse_u64_str_rejects_zero_and_invalid() {
        assert_eq!(
            parse_u64_str("0", "amount"),
            Err("Invalid amount".to_string())
        );
        assert_eq!(
            parse_u64_str("abc", "amount"),
            Err("Invalid amount".to_string())
        );
    }
}
