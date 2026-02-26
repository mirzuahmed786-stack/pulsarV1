# Dependency Inventory

## Rust/WASM
- `core-rust/` uses cryptography crates and BIP standards.
- `wasm/` bindings built with `wasm-pack`.

## Frontend
- React + Zustand
- `ethers` and `viem` for EVM
- `@solana/web3.js` for Solana
- `bitcoinjs-lib` for Bitcoin

## Backend
- Rust (axum)
- Aggregator integrations (0x, Jupiter)

## Notes
Maintain a lockfile review cadence and pin versions for audit readiness.

