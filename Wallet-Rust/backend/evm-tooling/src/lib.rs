//! EVM testnet tooling (pure Rust) replacing the old Hardhat scripts.
//!
//! Intentionally thin for now; the server crate will call into this crate
//! as we port endpoints like `/api/testnet-amm/deploy`.

pub mod artifacts;
pub mod service;
pub mod types;
