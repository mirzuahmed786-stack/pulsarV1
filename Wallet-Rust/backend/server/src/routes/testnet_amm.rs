use crate::app::AppState;
use axum::{
    extract::rejection::JsonRejection,
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    response::IntoResponse,
    routing::{get, post},
    Json, Router,
};

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/:chain", get(testnet_amm_by_chain))
        .route("/deploy", post(testnet_amm_deploy))
        .route("/mint", post(testnet_amm_mint))
        .route("/liquidity", post(testnet_amm_liquidity))
}

#[derive(Debug, Clone, serde::Deserialize)]
#[serde(deny_unknown_fields)]
struct DeployIn {
    chain: String,
    #[serde(rename = "rpcUrl")]
    rpc_url: String,
    #[serde(rename = "targetAddress")]
    target_address: Option<String>,
    tokens: Option<String>,
    #[serde(rename = "mintAmount")]
    mint_amount: Option<String>,
    #[serde(rename = "approveAmount")]
    approve_amount: Option<String>,
    #[serde(rename = "liquidityAmount")]
    liquidity_amount: Option<String>,
    #[serde(rename = "liquidityMin")]
    liquidity_min: Option<String>,
}

impl From<DeployIn> for earth_evm_tooling::types::DeployParams {
    fn from(value: DeployIn) -> Self {
        Self {
            chain: value.chain,
            rpc_url: value.rpc_url,
            target_address: value.target_address,
            tokens: value.tokens,
            mint_amount: value.mint_amount,
            approve_amount: value.approve_amount,
            liquidity_amount: value.liquidity_amount,
            liquidity_min: value.liquidity_min,
        }
    }
}

#[derive(Debug, Clone, serde::Deserialize)]
#[serde(deny_unknown_fields)]
struct MintIn {
    chain: String,
    #[serde(rename = "rpcUrl")]
    rpc_url: String,
    #[serde(rename = "targetAddress")]
    target_address: String,
    #[serde(rename = "tokenSymbol")]
    token_symbol: String,
    amount: String,
}

impl From<MintIn> for earth_evm_tooling::types::MintParams {
    fn from(value: MintIn) -> Self {
        Self {
            chain: value.chain,
            rpc_url: value.rpc_url,
            target_address: value.target_address,
            token_symbol: value.token_symbol,
            amount: value.amount,
        }
    }
}

#[derive(Debug, Clone, serde::Deserialize)]
#[serde(deny_unknown_fields)]
struct LiquidityIn {
    chain: String,
    #[serde(rename = "rpcUrl")]
    rpc_url: String,
    #[serde(rename = "targetAddress")]
    target_address: Option<String>,
    #[serde(rename = "tokenSymbol")]
    token_symbol: String,
    #[serde(rename = "baseAmount")]
    base_amount: String,
    #[serde(rename = "tokenAmount")]
    token_amount: String,
    #[serde(rename = "minBaseAmount")]
    min_base_amount: Option<String>,
    #[serde(rename = "minTokenAmount")]
    min_token_amount: Option<String>,
}

impl From<LiquidityIn> for earth_evm_tooling::types::LiquidityParams {
    fn from(value: LiquidityIn) -> Self {
        Self {
            chain: value.chain,
            rpc_url: value.rpc_url,
            target_address: value.target_address,
            token_symbol: value.token_symbol,
            base_amount: value.base_amount,
            token_amount: value.token_amount,
            min_base_amount: value.min_base_amount,
            min_token_amount: value.min_token_amount,
        }
    }
}

async fn testnet_amm_by_chain(Path(chain): Path<String>) -> impl IntoResponse {
    let chain = chain.to_lowercase();
    if !["sepolia", "amoy", "fuji", "bsc"].contains(&chain.as_str()) {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Unsupported testnet chain" })),
        )
            .into_response();
    }
    let file = crate::app::backend_state_file(&format!("deployed_testnet_amm_{chain}.json"));
    if !file.exists() {
        return (
            StatusCode::OK,
            Json(serde_json::json!({ "notDeployed": true, "chain": chain })),
        )
            .into_response();
    }
    match std::fs::read_to_string(&file)
        .ok()
        .and_then(|s| serde_json::from_str::<serde_json::Value>(&s).ok())
    {
        Some(v) => (StatusCode::OK, Json(v)).into_response(),
        None => (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "Failed to read testnet AMM addresses" })),
        )
            .into_response(),
    }
}

async fn testnet_amm_deploy(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<DeployIn>, JsonRejection>,
) -> impl IntoResponse {
    let Json(input) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/testnet-amm/deploy",
                rejection,
                Some(&headers),
            );
        }
    };
    if !crate::app::is_admin_allowed(&st, &headers) {
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "Forbidden" })),
        )
            .into_response();
    }
    if !["sepolia", "amoy", "fuji", "bsc"].contains(&input.chain.as_str()) {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Unsupported testnet chain" })),
        )
            .into_response();
    }
    if crate::app::validate_rpc_url(&st, &input.rpc_url).is_err() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({ "error": "Blocked rpcUrl: RPC host is not allowlisted" })),
        )
            .into_response();
    }
    let Some(pk) = crate::app::get_private_key() else {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "Missing DEPLOYER_PRIVATE_KEY/PRIVATE_KEY on backend" })),
        )
            .into_response();
    };
    let repo = crate::app::repo_root_from_server_crate();
    match earth_evm_tooling::service::deploy_testnet_amm(&repo, &pk, input.into()).await {
        Ok(v) => (
            StatusCode::OK,
            Json(serde_json::json!({ "ok": true, "addresses": v })),
        )
            .into_response(),
        Err(e) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "AMM deploy failed", "details": e.to_string() })),
        )
            .into_response(),
    }
}

async fn testnet_amm_mint(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<MintIn>, JsonRejection>,
) -> impl IntoResponse {
    let Json(input) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/testnet-amm/mint",
                rejection,
                Some(&headers),
            );
        }
    };
    if !crate::app::is_admin_allowed(&st, &headers) {
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "Forbidden" })),
        )
            .into_response();
    }
    let Some(pk) = crate::app::get_private_key() else {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "Missing DEPLOYER_PRIVATE_KEY/PRIVATE_KEY on backend" })),
        )
            .into_response();
    };
    let repo = crate::app::repo_root_from_server_crate();
    match earth_evm_tooling::service::mint_testnet_token(&repo, &pk, input.into()).await {
        Ok(_) => (StatusCode::OK, Json(serde_json::json!({ "ok": true }))).into_response(),
        Err(e) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "AMM mint failed", "details": e.to_string() })),
        )
            .into_response(),
    }
}

async fn testnet_amm_liquidity(
    State(st): State<AppState>,
    headers: HeaderMap,
    input: Result<Json<LiquidityIn>, JsonRejection>,
) -> impl IntoResponse {
    let Json(input) = match input {
        Ok(v) => v,
        Err(rejection) => {
            return crate::app::invalid_json_response(
                &st.api_metrics,
                "/api/testnet-amm/liquidity",
                rejection,
                Some(&headers),
            );
        }
    };
    if !crate::app::is_admin_allowed(&st, &headers) {
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({ "error": "Forbidden" })),
        )
            .into_response();
    }
    let Some(pk) = crate::app::get_private_key() else {
        return (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "Missing DEPLOYER_PRIVATE_KEY/PRIVATE_KEY on backend" })),
        )
            .into_response();
    };
    let repo = crate::app::repo_root_from_server_crate();
    match earth_evm_tooling::service::add_liquidity(&repo, &pk, input.into()).await {
        Ok(_) => (StatusCode::OK, Json(serde_json::json!({ "ok": true }))).into_response(),
        Err(e) => (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(serde_json::json!({ "error": "AMM liquidity failed", "details": e.to_string() })),
        )
            .into_response(),
    }
}
