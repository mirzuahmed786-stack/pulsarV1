use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeployedTestnetAmmAddresses {
    #[serde(rename = "UniswapV2Factory")]
    pub uniswap_v2_factory: String,
    #[serde(rename = "UniswapV2Router")]
    pub uniswap_v2_router: String,
    #[serde(default)]
    pub tokens: std::collections::HashMap<String, String>,
    #[serde(rename = "tokensMeta")]
    #[serde(default)]
    pub tokens_meta: std::collections::HashMap<String, TokenMeta>,
    #[serde(rename = "baseToken")]
    pub base_token: String,
    pub network: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenMeta {
    pub address: String,
    pub symbol: String,
    pub decimals: u8,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeployParams {
    pub chain: String,
    #[serde(rename = "rpcUrl")]
    pub rpc_url: String,
    #[serde(rename = "targetAddress")]
    pub target_address: Option<String>,
    pub tokens: Option<String>,
    #[serde(rename = "mintAmount")]
    pub mint_amount: Option<String>,
    #[serde(rename = "approveAmount")]
    pub approve_amount: Option<String>,
    #[serde(rename = "liquidityAmount")]
    pub liquidity_amount: Option<String>,
    #[serde(rename = "liquidityMin")]
    pub liquidity_min: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MintParams {
    pub chain: String,
    #[serde(rename = "rpcUrl")]
    pub rpc_url: String,
    #[serde(rename = "targetAddress")]
    pub target_address: String,
    #[serde(rename = "tokenSymbol")]
    pub token_symbol: String,
    pub amount: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LiquidityParams {
    pub chain: String,
    #[serde(rename = "rpcUrl")]
    pub rpc_url: String,
    #[serde(rename = "targetAddress")]
    pub target_address: Option<String>,
    #[serde(rename = "tokenSymbol")]
    pub token_symbol: String,
    #[serde(rename = "baseAmount")]
    pub base_amount: String,
    #[serde(rename = "tokenAmount")]
    pub token_amount: String,
    #[serde(rename = "minBaseAmount")]
    pub min_base_amount: Option<String>,
    #[serde(rename = "minTokenAmount")]
    pub min_token_amount: Option<String>,
}
