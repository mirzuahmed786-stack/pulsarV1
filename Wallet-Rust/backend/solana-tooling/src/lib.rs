//! Solana AMM tooling (pure Rust) replacing the old Node scripts.
//!
//! Note: we intentionally do not depend on `solana-sdk` (umbrella crate) or `anchor-client`
//! because those currently pull in `openssl-sys` on Windows in this environment.
//! We implement Anchor instruction discriminators + Borsh encoding ourselves.

pub mod anchor_ix;
pub mod service;
pub mod types;
