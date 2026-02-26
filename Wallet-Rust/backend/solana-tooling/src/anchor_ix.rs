use sha2::{Digest, Sha256};
use solana_instruction::{AccountMeta, Instruction};
use solana_pubkey::Pubkey;
use std::str::FromStr;

/// Anchor "global" instruction discriminator: first 8 bytes of sha256("global:<name>").
pub fn global_discriminator(ix_name: &str) -> [u8; 8] {
    let mut h = Sha256::new();
    h.update(b"global:");
    h.update(ix_name.as_bytes());
    let out = h.finalize();
    let mut disc = [0u8; 8];
    disc.copy_from_slice(&out[..8]);
    disc
}

#[derive(Clone, Copy, Debug)]
pub struct InitializePoolAccounts {
    pub pool: Pubkey,
    pub token_a_mint: Pubkey,
    pub token_b_mint: Pubkey,
    pub lp_mint: Pubkey,
    pub payer: Pubkey,
}

#[derive(Clone, Copy, Debug)]
pub struct InitializeVaultsAccounts {
    pub pool: Pubkey,
    pub token_a_mint: Pubkey,
    pub token_b_mint: Pubkey,
    pub vault_a: Pubkey,
    pub vault_b: Pubkey,
    pub payer: Pubkey,
}

#[derive(Clone, Copy, Debug)]
pub struct AddLiquidityAccounts {
    pub pool: Pubkey,
    pub token_a_mint: Pubkey,
    pub token_b_mint: Pubkey,
    pub vault_a: Pubkey,
    pub vault_b: Pubkey,
    pub user_token_a: Pubkey,
    pub user_token_b: Pubkey,
    pub lp_mint: Pubkey,
    pub user_lp: Pubkey,
    pub user: Pubkey,
}

#[derive(Clone, Copy, Debug)]
pub struct SwapAccounts {
    pub pool: Pubkey,
    pub user_source: Pubkey,
    pub user_destination: Pubkey,
    pub vault_in: Pubkey,
    pub vault_out: Pubkey,
    pub user: Pubkey,
}

pub fn initialize_pool_ix(
    program_id: Pubkey,
    accts: InitializePoolAccounts,
    fee_bps: u16,
) -> Instruction {
    let mut data = Vec::with_capacity(10);
    data.extend_from_slice(&global_discriminator("initialize_pool"));
    data.extend_from_slice(&fee_bps.to_le_bytes());

    Instruction {
        program_id,
        accounts: vec![
            AccountMeta::new(accts.pool, false),
            AccountMeta::new_readonly(accts.token_a_mint, false),
            AccountMeta::new_readonly(accts.token_b_mint, false),
            AccountMeta::new(accts.lp_mint, true),
            AccountMeta::new(accts.payer, true),
            AccountMeta::new_readonly(system_program_id(), false),
            AccountMeta::new_readonly(spl_token::id(), false),
        ],
        data,
    }
}

pub fn initialize_vaults_ix(program_id: Pubkey, accts: InitializeVaultsAccounts) -> Instruction {
    let data = global_discriminator("initialize_vaults").to_vec();
    Instruction {
        program_id,
        accounts: vec![
            AccountMeta::new(accts.pool, false),
            AccountMeta::new_readonly(accts.token_a_mint, false),
            AccountMeta::new_readonly(accts.token_b_mint, false),
            AccountMeta::new(accts.vault_a, true),
            AccountMeta::new(accts.vault_b, true),
            AccountMeta::new(accts.payer, true),
            AccountMeta::new_readonly(system_program_id(), false),
            AccountMeta::new_readonly(spl_token::id(), false),
        ],
        data,
    }
}

pub fn add_liquidity_ix(
    program_id: Pubkey,
    accts: AddLiquidityAccounts,
    amount_a: u64,
    amount_b: u64,
) -> Instruction {
    let mut data = Vec::with_capacity(24);
    data.extend_from_slice(&global_discriminator("add_liquidity"));
    data.extend_from_slice(&amount_a.to_le_bytes());
    data.extend_from_slice(&amount_b.to_le_bytes());

    Instruction {
        program_id,
        accounts: vec![
            AccountMeta::new(accts.pool, false),
            AccountMeta::new_readonly(accts.token_a_mint, false),
            AccountMeta::new_readonly(accts.token_b_mint, false),
            AccountMeta::new(accts.vault_a, false),
            AccountMeta::new(accts.vault_b, false),
            AccountMeta::new(accts.user_token_a, false),
            AccountMeta::new(accts.user_token_b, false),
            AccountMeta::new(accts.lp_mint, false),
            AccountMeta::new(accts.user_lp, false),
            AccountMeta::new(accts.user, true),
            AccountMeta::new_readonly(spl_token::id(), false),
        ],
        data,
    }
}

fn system_program_id() -> Pubkey {
    Pubkey::from_str("11111111111111111111111111111111").expect("valid system program id")
}

pub fn swap_ix(
    program_id: Pubkey,
    accts: SwapAccounts,
    amount_in: u64,
    min_out: u64,
) -> Instruction {
    let mut data = Vec::with_capacity(24);
    data.extend_from_slice(&global_discriminator("swap"));
    data.extend_from_slice(&amount_in.to_le_bytes());
    data.extend_from_slice(&min_out.to_le_bytes());

    Instruction {
        program_id,
        accounts: vec![
            AccountMeta::new(accts.pool, false),
            AccountMeta::new(accts.user_source, false),
            AccountMeta::new(accts.user_destination, false),
            AccountMeta::new(accts.vault_in, false),
            AccountMeta::new(accts.vault_out, false),
            AccountMeta::new(accts.user, true),
            AccountMeta::new_readonly(spl_token::id(), false),
        ],
        data,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn pk(n: u8) -> Pubkey {
        Pubkey::new_from_array([n; 32])
    }

    #[test]
    fn discriminator_matches_idl() {
        assert_eq!(
            global_discriminator("initialize_pool"),
            [95, 180, 10, 172, 84, 174, 232, 40]
        );
        assert_eq!(
            global_discriminator("initialize_vaults"),
            [234, 84, 214, 3, 128, 195, 81, 125]
        );
        assert_eq!(
            global_discriminator("add_liquidity"),
            [181, 157, 89, 67, 143, 182, 52, 72]
        );
        assert_eq!(
            global_discriminator("swap"),
            [248, 198, 158, 145, 225, 117, 135, 200]
        );
    }

    #[test]
    fn initialize_pool_data_is_discriminator_plus_u16_le() {
        let ix = initialize_pool_ix(
            pk(1),
            InitializePoolAccounts {
                pool: pk(2),
                token_a_mint: pk(3),
                token_b_mint: pk(4),
                lp_mint: pk(5),
                payer: pk(6),
            },
            30,
        );
        let mut expected = global_discriminator("initialize_pool").to_vec();
        expected.extend_from_slice(&30u16.to_le_bytes());
        assert_eq!(ix.data, expected);
    }

    #[test]
    fn add_liquidity_data_is_discriminator_plus_two_u64_le() {
        let ix = add_liquidity_ix(
            pk(1),
            AddLiquidityAccounts {
                pool: pk(2),
                token_a_mint: pk(3),
                token_b_mint: pk(4),
                vault_a: pk(5),
                vault_b: pk(6),
                user_token_a: pk(7),
                user_token_b: pk(8),
                lp_mint: pk(9),
                user_lp: pk(10),
                user: pk(11),
            },
            1_000_000,
            2_000_000,
        );
        let mut expected = global_discriminator("add_liquidity").to_vec();
        expected.extend_from_slice(&1_000_000u64.to_le_bytes());
        expected.extend_from_slice(&2_000_000u64.to_le_bytes());
        assert_eq!(ix.data, expected);
    }

    #[test]
    fn swap_data_is_discriminator_plus_two_u64_le() {
        let ix = swap_ix(
            pk(1),
            SwapAccounts {
                pool: pk(2),
                user_source: pk(3),
                user_destination: pk(4),
                vault_in: pk(5),
                vault_out: pk(6),
                user: pk(7),
            },
            42,
            7,
        );
        let mut expected = global_discriminator("swap").to_vec();
        expected.extend_from_slice(&42u64.to_le_bytes());
        expected.extend_from_slice(&7u64.to_le_bytes());
        assert_eq!(ix.data, expected);
    }
}
