use anchor_lang::prelude::*;
use anchor_spl::token::{self, Mint, Token, TokenAccount, Transfer, MintTo};

declare_id!("DHnQfvfUy7Yt92BxZxpj1G8UMWXyKyGGYHvUn2Lrb1xK");

const MAX_FEE_BPS: u16 = 1000; // 10%

#[program]
pub mod amm {
    use super::*;

    pub fn initialize_pool(
        ctx: Context<InitializePool>,
        fee_bps: u16
    ) -> Result<()> {
        require!(fee_bps <= MAX_FEE_BPS, AmmError::InvalidFee);
        let pool = &mut ctx.accounts.pool;
        pool.token_a_mint = ctx.accounts.token_a_mint.key();
        pool.token_b_mint = ctx.accounts.token_b_mint.key();
        pool.vault_a = Pubkey::default();
        pool.vault_b = Pubkey::default();
        pool.lp_mint = ctx.accounts.lp_mint.key();
        pool.fee_bps = fee_bps;
        pool.bump = ctx.bumps.pool;
        Ok(())
    }

    pub fn initialize_vaults(
        ctx: Context<InitializeVaults>
    ) -> Result<()> {
        let pool = &mut ctx.accounts.pool;
        require!(pool.vault_a == Pubkey::default(), AmmError::VaultsAlreadyInitialized);
        require!(pool.vault_b == Pubkey::default(), AmmError::VaultsAlreadyInitialized);

        pool.vault_a = ctx.accounts.vault_a.key();
        pool.vault_b = ctx.accounts.vault_b.key();

        Ok(())
    }

pub fn add_liquidity(
        ctx: Context<AddLiquidity>,
        amount_a: u64,
        amount_b: u64
    ) -> Result<()> {
        require!(amount_a > 0 && amount_b > 0, AmmError::InvalidAmount);
        require!(
            ctx.accounts.user_token_a.mint == ctx.accounts.token_a_mint.key(),
            AmmError::InvalidTokenAccount
        );
        require!(
            ctx.accounts.user_token_b.mint == ctx.accounts.token_b_mint.key(),
            AmmError::InvalidTokenAccount
        );

        let reserve_a = ctx.accounts.vault_a.amount as u128;
        let reserve_b = ctx.accounts.vault_b.amount as u128;
        let total_lp = ctx.accounts.lp_mint.supply as u128;

        token::transfer(ctx.accounts.transfer_to_vault_a_ctx(), amount_a)?;
        token::transfer(ctx.accounts.transfer_to_vault_b_ctx(), amount_b)?;

        let lp_amount = if total_lp == 0 {
            integer_sqrt((amount_a as u128) * (amount_b as u128)) as u64
        } else {
            let lp_from_a = (amount_a as u128)
                .saturating_mul(total_lp)
                .checked_div(reserve_a)
                .unwrap_or(0);
            let lp_from_b = (amount_b as u128)
                .saturating_mul(total_lp)
                .checked_div(reserve_b)
                .unwrap_or(0);
            lp_from_a.min(lp_from_b) as u64
        };
        require!(lp_amount > 0, AmmError::InvalidAmount);

        let token_a_key = ctx.accounts.token_a_mint.key();
        let token_b_key = ctx.accounts.token_b_mint.key();
        let pool_seeds = &[
            b"pool",
            token_a_key.as_ref(),
            token_b_key.as_ref(),
            &[ctx.accounts.pool.bump],
        ];
        token::mint_to(
            ctx.accounts.mint_lp_ctx().with_signer(&[pool_seeds]),
            lp_amount
        )?;

        Ok(())
    }

    pub fn swap(
        ctx: Context<Swap>,
        amount_in: u64,
        min_out: u64
    ) -> Result<()> {
        require!(amount_in > 0, AmmError::InvalidAmount);
        validate_swap_accounts(
            ctx.accounts.pool.vault_a,
            ctx.accounts.pool.vault_b,
            ctx.accounts.vault_in.key(),
            ctx.accounts.vault_out.key(),
            ctx.accounts.user_source.mint,
            ctx.accounts.user_destination.mint,
            ctx.accounts.vault_in.mint,
            ctx.accounts.vault_out.mint
        )?;

        let vault_in = &ctx.accounts.vault_in;
        let vault_out = &ctx.accounts.vault_out;

        let amount_in_after_fee = apply_fee(amount_in, ctx.accounts.pool.fee_bps);

        let reserve_in = vault_in.amount as u128;
        let reserve_out = vault_out.amount as u128;
        require!(reserve_in > 0 && reserve_out > 0, AmmError::NoLiquidity);

        let numerator = amount_in_after_fee as u128 * reserve_out;
        let denominator = reserve_in + amount_in_after_fee as u128;
        let amount_out = (numerator / denominator) as u64;

        require!(amount_out >= min_out, AmmError::SlippageExceeded);

        token::transfer(ctx.accounts.transfer_to_vault_in_ctx(), amount_in)?;

        let pool_seeds = &[
            b"pool",
            ctx.accounts.pool.token_a_mint.as_ref(),
            ctx.accounts.pool.token_b_mint.as_ref(),
            &[ctx.accounts.pool.bump],
        ];
        token::transfer(
            ctx.accounts.transfer_to_user_out_ctx().with_signer(&[pool_seeds]),
            amount_out
        )?;

        Ok(())
    }
}

fn apply_fee(amount: u64, fee_bps: u16) -> u64 {
    let fee = (amount as u128 * fee_bps as u128) / 10_000u128;
    amount.saturating_sub(fee as u64)
}

fn validate_swap_accounts(
    pool_vault_a: Pubkey,
    pool_vault_b: Pubkey,
    vault_in: Pubkey,
    vault_out: Pubkey,
    user_source_mint: Pubkey,
    user_destination_mint: Pubkey,
    vault_in_mint: Pubkey,
    vault_out_mint: Pubkey
) -> Result<()> {
    let valid_pair = (vault_in == pool_vault_a && vault_out == pool_vault_b)
        || (vault_in == pool_vault_b && vault_out == pool_vault_a);
    if !valid_pair {
        return Err(error!(AmmError::InvalidVault));
    }
    if user_source_mint != vault_in_mint {
        return Err(error!(AmmError::InvalidTokenAccount));
    }
    if user_destination_mint != vault_out_mint {
        return Err(error!(AmmError::InvalidTokenAccount));
    }
    Ok(())
}

fn integer_sqrt(value: u128) -> u128 {
    if value == 0 {
        return 0;
    }
    let mut x0 = value;
    let mut x1 = (x0 + 1) >> 1;
    while x1 < x0 {
        x0 = x1;
        x1 = (x1 + value / x1) >> 1;
    }
    x0
}

#[derive(Accounts)]
pub struct InitializePool<'info> {
    #[account(
        init,
        payer = payer,
        space = 8 + Pool::INIT_SPACE,
        seeds = [b"pool", token_a_mint.key().as_ref(), token_b_mint.key().as_ref()],
        bump
    )]
    pub pool: Box<Account<'info, Pool>>,
    pub token_a_mint: Box<Account<'info, Mint>>,
    pub token_b_mint: Box<Account<'info, Mint>>,
    #[account(
        init,
        payer = payer,
        mint::decimals = 6,
        mint::authority = pool
    )]
    pub lp_mint: Box<Account<'info, Mint>>,
    #[account(mut)]
    pub payer: Signer<'info>,
    pub system_program: Program<'info, System>,
    pub token_program: Program<'info, Token>,
}

#[derive(Accounts)]
pub struct InitializeVaults<'info> {
    #[account(
        mut,
        has_one = token_a_mint,
        has_one = token_b_mint
    )]
    pub pool: Box<Account<'info, Pool>>,
    pub token_a_mint: Box<Account<'info, Mint>>,
    pub token_b_mint: Box<Account<'info, Mint>>,
    #[account(
        init,
        payer = payer,
        token::mint = token_a_mint,
        token::authority = pool
    )]
    pub vault_a: Box<Account<'info, TokenAccount>>,
    #[account(
        init,
        payer = payer,
        token::mint = token_b_mint,
        token::authority = pool
    )]
    pub vault_b: Box<Account<'info, TokenAccount>>,
    #[account(mut)]
    pub payer: Signer<'info>,
    pub system_program: Program<'info, System>,
    pub token_program: Program<'info, Token>,
}

#[derive(Accounts)]
pub struct AddLiquidity<'info> {
    #[account(mut)]
    pub pool: Account<'info, Pool>,
    pub token_a_mint: Account<'info, Mint>,
    pub token_b_mint: Account<'info, Mint>,
    #[account(mut, address = pool.vault_a)]
    pub vault_a: Account<'info, TokenAccount>,
    #[account(mut, address = pool.vault_b)]
    pub vault_b: Account<'info, TokenAccount>,
    #[account(mut)]
    pub user_token_a: Account<'info, TokenAccount>,
    #[account(mut)]
    pub user_token_b: Account<'info, TokenAccount>,
    #[account(mut, address = pool.lp_mint)]
    pub lp_mint: Account<'info, Mint>,
    #[account(mut)]
    pub user_lp: Account<'info, TokenAccount>,
    pub user: Signer<'info>,
    pub token_program: Program<'info, Token>,
}

impl<'info> AddLiquidity<'info> {
    fn transfer_to_vault_a_ctx(&self) -> CpiContext<'_, '_, '_, 'info, Transfer<'info>> {
        CpiContext::new(
            self.token_program.to_account_info(),
            Transfer {
                from: self.user_token_a.to_account_info(),
                to: self.vault_a.to_account_info(),
                authority: self.user.to_account_info(),
            }
        )
    }

    fn transfer_to_vault_b_ctx(&self) -> CpiContext<'_, '_, '_, 'info, Transfer<'info>> {
        CpiContext::new(
            self.token_program.to_account_info(),
            Transfer {
                from: self.user_token_b.to_account_info(),
                to: self.vault_b.to_account_info(),
                authority: self.user.to_account_info(),
            }
        )
    }

    fn mint_lp_ctx(&self) -> CpiContext<'_, '_, '_, 'info, MintTo<'info>> {
        CpiContext::new(
            self.token_program.to_account_info(),
            MintTo {
                mint: self.lp_mint.to_account_info(),
                to: self.user_lp.to_account_info(),
                authority: self.pool.to_account_info(),
            }
        )
    }
}

#[derive(Accounts)]
pub struct Swap<'info> {
    #[account(mut)]
    pub pool: Account<'info, Pool>,
    #[account(mut)]
    pub user_source: Account<'info, TokenAccount>,
    #[account(mut)]
    pub user_destination: Account<'info, TokenAccount>,
    #[account(mut)]
    pub vault_in: Account<'info, TokenAccount>,
    #[account(mut)]
    pub vault_out: Account<'info, TokenAccount>,
    pub user: Signer<'info>,
    pub token_program: Program<'info, Token>,
}

impl<'info> Swap<'info> {
    fn transfer_to_vault_in_ctx(&self) -> CpiContext<'_, '_, '_, 'info, Transfer<'info>> {
        CpiContext::new(
            self.token_program.to_account_info(),
            Transfer {
                from: self.user_source.to_account_info(),
                to: self.vault_in.to_account_info(),
                authority: self.user.to_account_info(),
            }
        )
    }

    fn transfer_to_user_out_ctx(&self) -> CpiContext<'_, '_, '_, 'info, Transfer<'info>> {
        CpiContext::new(
            self.token_program.to_account_info(),
            Transfer {
                from: self.vault_out.to_account_info(),
                to: self.user_destination.to_account_info(),
                authority: self.pool.to_account_info(),
            }
        )
    }
}

#[account]
pub struct Pool {
    pub token_a_mint: Pubkey,
    pub token_b_mint: Pubkey,
    pub vault_a: Pubkey,
    pub vault_b: Pubkey,
    pub lp_mint: Pubkey,
    pub fee_bps: u16,
    pub bump: u8,
}

impl Pool {
    pub const INIT_SPACE: usize = 32 + 32 + 32 + 32 + 32 + 2 + 1;
}

#[error_code]
pub enum AmmError {
    #[msg("Invalid fee")]
    InvalidFee,
    #[msg("Invalid amount")]
    InvalidAmount,
    #[msg("No liquidity")]
    NoLiquidity,
    #[msg("Slippage exceeded")]
    SlippageExceeded,
    #[msg("Missing PDA bump")]
    MissingBump,
    #[msg("Vaults already initialized")]
    VaultsAlreadyInitialized,
    #[msg("Invalid vault accounts")]
    InvalidVault,
    #[msg("Invalid token account")]
    InvalidTokenAccount,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validate_swap_accounts_ok() {
        let vault_a = Pubkey::new_unique();
        let vault_b = Pubkey::new_unique();
        let mint_a = Pubkey::new_unique();
        let mint_b = Pubkey::new_unique();

        let result = validate_swap_accounts(
            vault_a,
            vault_b,
            vault_a,
            vault_b,
            mint_a,
            mint_b,
            mint_a,
            mint_b
        );

        assert!(result.is_ok());
    }

    #[test]
    fn validate_swap_accounts_rejects_invalid_vaults() {
        let vault_a = Pubkey::new_unique();
        let vault_b = Pubkey::new_unique();
        let mint_a = Pubkey::new_unique();
        let mint_b = Pubkey::new_unique();
        let wrong_vault = Pubkey::new_unique();

        let result = validate_swap_accounts(
            vault_a,
            vault_b,
            wrong_vault,
            vault_b,
            mint_a,
            mint_b,
            mint_a,
            mint_b
        );

        assert!(result.is_err());
    }

    #[test]
    fn validate_swap_accounts_rejects_mint_mismatch() {
        let vault_a = Pubkey::new_unique();
        let vault_b = Pubkey::new_unique();
        let mint_a = Pubkey::new_unique();
        let mint_b = Pubkey::new_unique();
        let wrong_mint = Pubkey::new_unique();

        let result = validate_swap_accounts(
            vault_a,
            vault_b,
            vault_a,
            vault_b,
            wrong_mint,
            mint_b,
            mint_a,
            mint_b
        );

        assert!(result.is_err());
    }
}
