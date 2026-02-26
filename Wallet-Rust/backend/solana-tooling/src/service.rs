use std::path::Path;
use std::time::Duration;

use anyhow::{anyhow, Context, Result};
use base64::Engine;
use solana_client::rpc_client::RpcClient;
use solana_commitment_config::CommitmentConfig;
use solana_instruction::Instruction;
use solana_keypair::{read_keypair_file, Keypair};
use solana_pubkey::Pubkey;
use solana_signer::Signer;
use solana_transaction::Transaction;
use spl_associated_token_account::{
    get_associated_token_address, instruction::create_associated_token_account_idempotent,
};
use spl_token::solana_program::program_pack::Pack;

use crate::{
    anchor_ix::{
        add_liquidity_ix, initialize_pool_ix, initialize_vaults_ix, swap_ix, AddLiquidityAccounts,
        InitializePoolAccounts, InitializeVaultsAccounts, SwapAccounts,
    },
    types::{
        InitParams, InitResult, MintParams, MintResult, MintToken, SolanaAmmConfig, SwapDirection,
        SwapTxParams, SwapTxResult, TokenConfig,
    },
};

const SOLANA_RPC_TIMEOUT_S: u64 = 20;
const SOLANA_RPC_MAX_ATTEMPTS: usize = 3;
const SOLANA_RPC_RETRY_DELAY_MS: u64 = 150;

pub fn load_config(config_path: &Path) -> Result<SolanaAmmConfig> {
    let s = std::fs::read_to_string(config_path)
        .with_context(|| format!("failed reading {}", config_path.display()))?;
    let cfg: SolanaAmmConfig =
        serde_json::from_str(&s).context("invalid solana amm config json")?;
    Ok(cfg)
}

pub fn init_amm(config_path: &Path, params: InitParams) -> Result<InitResult> {
    if config_path.exists() {
        let existing = load_config(config_path).context("failed reading existing AMM config")?;
        return Ok(InitResult {
            config: existing,
            tx_signatures: Vec::new(),
        });
    }
    let rpc = rpc_client(&params.rpc_url);
    let payer = read_keypair_file(&params.keypair_path)
        .map_err(|e| anyhow!("failed reading keypair {}: {e}", params.keypair_path))?;
    let program_id: Pubkey = params
        .program_id
        .parse()
        .with_context(|| format!("invalid SOLANA_AMM_PROGRAM_ID: {}", params.program_id))?;
    let fee_bps = params.fee_bps;
    if fee_bps > 10_000 {
        return Err(anyhow!("SOLANA_AMM_FEE_BPS must be <= 10000"));
    }

    let mint_a = Keypair::new();
    let mint_b = Keypair::new();
    let token_a_amount = parse_token_amount(&params.liquidity_a_tokens, params.token_a_decimals)?;
    let token_b_amount = parse_token_amount(&params.liquidity_b_tokens, params.token_b_decimals)?;

    let mut signatures = Vec::new();
    signatures.push(create_mint(&rpc, &payer, &mint_a, params.token_a_decimals)?);
    signatures.push(create_mint(&rpc, &payer, &mint_b, params.token_b_decimals)?);

    let payer_a_ata = get_associated_token_address(&payer.pubkey(), &mint_a.pubkey());
    let payer_b_ata = get_associated_token_address(&payer.pubkey(), &mint_b.pubkey());
    if let Some(sig) = create_ata_if_needed(&rpc, &payer, &payer.pubkey(), &mint_a.pubkey())? {
        signatures.push(sig);
    }
    if let Some(sig) = create_ata_if_needed(&rpc, &payer, &payer.pubkey(), &mint_b.pubkey())? {
        signatures.push(sig);
    }
    signatures.push(mint_to(
        &rpc,
        &payer,
        &mint_a.pubkey(),
        &payer_a_ata,
        token_a_amount,
    )?);
    signatures.push(mint_to(
        &rpc,
        &payer,
        &mint_b.pubkey(),
        &payer_b_ata,
        token_b_amount,
    )?);

    let (pool, _) = Pubkey::find_program_address(
        &[b"pool", mint_a.pubkey().as_ref(), mint_b.pubkey().as_ref()],
        &program_id,
    );
    let lp_mint = Keypair::new();
    let vault_a = Keypair::new();
    let vault_b = Keypair::new();

    let init_pool = initialize_pool_ix(
        program_id,
        InitializePoolAccounts {
            pool,
            token_a_mint: mint_a.pubkey(),
            token_b_mint: mint_b.pubkey(),
            lp_mint: lp_mint.pubkey(),
            payer: payer.pubkey(),
        },
        fee_bps,
    );
    signatures.push(send_ix(&rpc, &payer, &[init_pool], &[&payer, &lp_mint])?);

    let init_vaults = initialize_vaults_ix(
        program_id,
        InitializeVaultsAccounts {
            pool,
            token_a_mint: mint_a.pubkey(),
            token_b_mint: mint_b.pubkey(),
            vault_a: vault_a.pubkey(),
            vault_b: vault_b.pubkey(),
            payer: payer.pubkey(),
        },
    );
    signatures.push(send_ix(
        &rpc,
        &payer,
        &[init_vaults],
        &[&payer, &vault_a, &vault_b],
    )?);

    let user_lp = get_associated_token_address(&payer.pubkey(), &lp_mint.pubkey());
    if let Some(sig) = create_ata_if_needed(&rpc, &payer, &payer.pubkey(), &lp_mint.pubkey())? {
        signatures.push(sig);
    }
    let add_liq = add_liquidity_ix(
        program_id,
        AddLiquidityAccounts {
            pool,
            token_a_mint: mint_a.pubkey(),
            token_b_mint: mint_b.pubkey(),
            vault_a: vault_a.pubkey(),
            vault_b: vault_b.pubkey(),
            user_token_a: payer_a_ata,
            user_token_b: payer_b_ata,
            lp_mint: lp_mint.pubkey(),
            user_lp,
            user: payer.pubkey(),
        },
        token_a_amount,
        token_b_amount,
    );
    signatures.push(send_ix(&rpc, &payer, &[add_liq], &[&payer])?);

    let config = SolanaAmmConfig {
        program_id: program_id.to_string(),
        pool: pool.to_string(),
        fee_bps,
        token_a: TokenConfig {
            mint: mint_a.pubkey().to_string(),
            symbol: params.token_a_symbol,
            decimals: params.token_a_decimals,
        },
        token_b: TokenConfig {
            mint: mint_b.pubkey().to_string(),
            symbol: params.token_b_symbol,
            decimals: params.token_b_decimals,
        },
        vault_a: vault_a.pubkey().to_string(),
        vault_b: vault_b.pubkey().to_string(),
        lp_mint: lp_mint.pubkey().to_string(),
        rpc_url: params.rpc_url,
    };

    if let Some(parent) = config_path.parent() {
        std::fs::create_dir_all(parent)
            .with_context(|| format!("failed creating {}", parent.display()))?;
    }
    std::fs::write(
        config_path,
        serde_json::to_string_pretty(&config).context("failed serializing config")?,
    )
    .with_context(|| format!("failed writing {}", config_path.display()))?;

    Ok(InitResult {
        config,
        tx_signatures: signatures,
    })
}

pub fn build_swap_tx(config: &SolanaAmmConfig, params: SwapTxParams) -> Result<SwapTxResult> {
    let rpc = rpc_client(&config.rpc_url);
    let user: Pubkey = params
        .user_public_key
        .parse()
        .with_context(|| format!("invalid userPublicKey: {}", params.user_public_key))?;

    let token_a_mint: Pubkey = config
        .token_a
        .mint
        .parse()
        .context("invalid tokenA.mint in config")?;
    let token_b_mint: Pubkey = config
        .token_b
        .mint
        .parse()
        .context("invalid tokenB.mint in config")?;
    let pool: Pubkey = config.pool.parse().context("invalid pool in config")?;
    let vault_a: Pubkey = config.vault_a.parse().context("invalid vaultA in config")?;
    let vault_b: Pubkey = config.vault_b.parse().context("invalid vaultB in config")?;
    let program_id: Pubkey = config
        .program_id
        .parse()
        .context("invalid programId in config")?;

    let token_a_ata = get_associated_token_address(&user, &token_a_mint);
    let token_b_ata = get_associated_token_address(&user, &token_b_mint);

    let mut ixs: Vec<Instruction> = Vec::new();
    if rpc_call_with_retry("get_account(token_a_ata)", || rpc.get_account(&token_a_ata)).is_err() {
        ixs.push(create_associated_token_account_idempotent(
            &user,
            &user,
            &token_a_mint,
            &spl_token::id(),
        ));
    }
    if rpc_call_with_retry("get_account(token_b_ata)", || rpc.get_account(&token_b_ata)).is_err() {
        ixs.push(create_associated_token_account_idempotent(
            &user,
            &user,
            &token_b_mint,
            &spl_token::id(),
        ));
    }

    let (vault_in, vault_out, user_source, user_destination) = match params.direction {
        SwapDirection::AtoB => (vault_a, vault_b, token_a_ata, token_b_ata),
        SwapDirection::BtoA => (vault_b, vault_a, token_b_ata, token_a_ata),
    };

    ixs.push(swap_ix(
        program_id,
        SwapAccounts {
            pool,
            user_source,
            user_destination,
            vault_in,
            vault_out,
            user,
        },
        params.amount_in,
        params.min_out,
    ));

    let (blockhash, last_valid_block_height) =
        rpc_call_with_retry("get_latest_blockhash_with_commitment", || {
            rpc.get_latest_blockhash_with_commitment(CommitmentConfig::confirmed())
        })?;
    let tx = Transaction::new_unsigned(solana_message::Message::new_with_blockhash(
        &ixs,
        Some(&user),
        &blockhash,
    ));
    let serialized = bincode::serialize(&tx).context("failed to serialize swap tx")?;
    Ok(SwapTxResult {
        swap_transaction: base64::engine::general_purpose::STANDARD.encode(serialized),
        last_valid_block_height,
    })
}

pub fn mint_test_tokens(
    config: &SolanaAmmConfig,
    keypair_path: &str,
    params: MintParams,
) -> Result<MintResult> {
    let rpc = rpc_client(&config.rpc_url);
    let payer = read_keypair_file(keypair_path)
        .map_err(|e| anyhow!("failed reading keypair {}: {e}", keypair_path))?;

    let user: Pubkey = params
        .user_public_key
        .parse()
        .with_context(|| format!("invalid userPublicKey: {}", params.user_public_key))?;

    let mint_str = match params.token {
        MintToken::A => &config.token_a.mint,
        MintToken::B => &config.token_b.mint,
    };
    let mint: Pubkey = mint_str.parse().context("invalid mint in config")?;
    let destination = get_associated_token_address(&user, &mint);

    let mut ixs: Vec<Instruction> = Vec::new();
    if rpc_call_with_retry("get_account(destination)", || rpc.get_account(&destination)).is_err() {
        ixs.push(create_associated_token_account_idempotent(
            &payer.pubkey(),
            &user,
            &mint,
            &spl_token::id(),
        ));
    }
    ixs.push(
        spl_token::instruction::mint_to(
            &spl_token::id(),
            &mint,
            &destination,
            &payer.pubkey(),
            &[],
            params.amount,
        )
        .context("failed building mint_to ix")?,
    );

    let sig = send_ix(&rpc, &payer, &ixs, &[&payer])?;
    Ok(MintResult {
        mint: mint.to_string(),
        destination: destination.to_string(),
        amount: params.amount.to_string(),
        tx_signature: sig,
    })
}

fn create_mint(rpc: &RpcClient, payer: &Keypair, mint: &Keypair, decimals: u8) -> Result<String> {
    let rent = rpc_call_with_retry("get_minimum_balance_for_rent_exemption", || {
        rpc.get_minimum_balance_for_rent_exemption(spl_token::state::Mint::LEN)
    })
    .context("failed getting rent exemption for mint")?;
    let create_ix = spl_token::solana_program::system_instruction::create_account(
        &payer.pubkey(),
        &mint.pubkey(),
        rent,
        spl_token::state::Mint::LEN as u64,
        &spl_token::id(),
    );
    let init_ix = spl_token::instruction::initialize_mint(
        &spl_token::id(),
        &mint.pubkey(),
        &payer.pubkey(),
        None,
        decimals,
    )
    .context("failed building initialize_mint ix")?;
    send_ix(rpc, payer, &[create_ix, init_ix], &[payer, mint])
}

fn mint_to(
    rpc: &RpcClient,
    payer: &Keypair,
    mint: &Pubkey,
    dest_ata: &Pubkey,
    amount: u64,
) -> Result<String> {
    let ix = spl_token::instruction::mint_to(
        &spl_token::id(),
        mint,
        dest_ata,
        &payer.pubkey(),
        &[],
        amount,
    )
    .context("failed building mint_to ix")?;
    send_ix(rpc, payer, &[ix], &[payer])
}

fn create_ata_if_needed(
    rpc: &RpcClient,
    payer: &Keypair,
    owner: &Pubkey,
    mint: &Pubkey,
) -> Result<Option<String>> {
    let ata = get_associated_token_address(owner, mint);
    if rpc_call_with_retry("get_account(ata)", || rpc.get_account(&ata)).is_ok() {
        return Ok(None);
    }
    let ix =
        create_associated_token_account_idempotent(&payer.pubkey(), owner, mint, &spl_token::id());
    Ok(Some(send_ix(rpc, payer, &[ix], &[payer])?))
}

fn send_ix(
    rpc: &RpcClient,
    payer: &Keypair,
    ixs: &[Instruction],
    signers: &[&Keypair],
) -> Result<String> {
    let blockhash = rpc_call_with_retry("get_latest_blockhash", || rpc.get_latest_blockhash())
        .context("failed getting blockhash")?;
    let tx = Transaction::new_signed_with_payer(ixs, Some(&payer.pubkey()), signers, blockhash);
    let sig = rpc_call_with_retry("send_and_confirm_transaction", || {
        rpc.send_and_confirm_transaction(&tx)
    })
    .context("failed sending Solana transaction")?;
    Ok(sig.to_string())
}

fn rpc_client(url: &str) -> RpcClient {
    RpcClient::new_with_timeout_and_commitment(
        url.to_string(),
        Duration::from_secs(SOLANA_RPC_TIMEOUT_S),
        CommitmentConfig::confirmed(),
    )
}

fn rpc_call_with_retry<T, E, F>(op: &str, mut f: F) -> Result<T>
where
    E: std::fmt::Display,
    F: FnMut() -> std::result::Result<T, E>,
{
    let mut last_err = String::new();
    for attempt in 1..=SOLANA_RPC_MAX_ATTEMPTS {
        match f() {
            Ok(v) => return Ok(v),
            Err(e) => {
                last_err = e.to_string();
                if attempt < SOLANA_RPC_MAX_ATTEMPTS {
                    std::thread::sleep(Duration::from_millis(SOLANA_RPC_RETRY_DELAY_MS));
                }
            }
        }
    }
    Err(anyhow!("solana rpc {op} failed after retries: {last_err}"))
}

fn parse_token_amount(tokens: &str, decimals: u8) -> Result<u64> {
    let whole: u128 = tokens
        .trim()
        .parse()
        .with_context(|| format!("invalid token amount: {tokens}"))?;
    let base = 10u128
        .checked_pow(decimals as u32)
        .ok_or_else(|| anyhow!("invalid decimals"))?;
    let amount = whole
        .checked_mul(base)
        .ok_or_else(|| anyhow!("token amount overflow"))?;
    u64::try_from(amount).map_err(|_| anyhow!("token amount exceeds u64"))
}

#[cfg(test)]
mod tests {
    use super::parse_token_amount;

    #[test]
    fn parse_token_amount_scales_by_decimals() {
        let out = parse_token_amount("100000", 6).expect("must parse");
        assert_eq!(out, 100_000_000_000);
    }

    #[test]
    fn parse_token_amount_rejects_non_numeric() {
        let err = parse_token_amount("abc", 6).expect_err("must fail");
        assert!(err.to_string().contains("invalid token amount"));
    }

    #[test]
    fn parse_token_amount_rejects_overflow() {
        // This value overflows u64 after scaling by 10^9.
        let err = parse_token_amount("18446744074", 9).expect_err("must fail");
        assert!(err.to_string().contains("exceeds u64"));
    }
}
