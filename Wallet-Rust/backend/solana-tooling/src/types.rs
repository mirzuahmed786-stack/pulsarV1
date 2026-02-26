use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenConfig {
    pub mint: String,
    pub symbol: String,
    pub decimals: u8,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SolanaAmmConfig {
    #[serde(rename = "programId")]
    pub program_id: String,
    pub pool: String,
    #[serde(rename = "feeBps")]
    pub fee_bps: u16,
    #[serde(rename = "tokenA")]
    pub token_a: TokenConfig,
    #[serde(rename = "tokenB")]
    pub token_b: TokenConfig,
    #[serde(rename = "vaultA")]
    pub vault_a: String,
    #[serde(rename = "vaultB")]
    pub vault_b: String,
    #[serde(rename = "lpMint")]
    pub lp_mint: String,
    #[serde(rename = "rpcUrl")]
    pub rpc_url: String,
}

#[derive(Debug, Clone)]
pub struct InitParams {
    pub rpc_url: String,
    pub keypair_path: String,
    pub program_id: String,
    pub fee_bps: u16,
    pub token_a_decimals: u8,
    pub token_b_decimals: u8,
    pub token_a_symbol: String,
    pub token_b_symbol: String,
    pub liquidity_a_tokens: String,
    pub liquidity_b_tokens: String,
}

#[derive(Debug, Clone, Serialize)]
pub struct InitResult {
    pub config: SolanaAmmConfig,
    #[serde(rename = "txSignatures")]
    pub tx_signatures: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct SwapTxParams {
    pub amount_in: u64,
    pub min_out: u64,
    pub direction: SwapDirection,
    pub user_public_key: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SwapDirection {
    AtoB,
    BtoA,
}

#[derive(Debug, Clone, Serialize)]
pub struct SwapTxResult {
    #[serde(rename = "swapTransaction")]
    pub swap_transaction: String,
    #[serde(rename = "lastValidBlockHeight")]
    pub last_valid_block_height: u64,
}

#[derive(Debug, Clone)]
pub struct MintParams {
    pub user_public_key: String,
    pub token: MintToken,
    pub amount: u64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MintToken {
    A,
    B,
}

#[derive(Debug, Clone, Serialize)]
pub struct MintResult {
    pub mint: String,
    pub destination: String,
    pub amount: String,
    #[serde(rename = "txSignature")]
    pub tx_signature: String,
}
