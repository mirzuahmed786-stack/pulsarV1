# Architecture

## High-Level
Client-custody wallet with Rust/WASM cryptography and a Rust backend for orchestration, policy enforcement, and observability. Private keys do not leave client custody paths.

```
User -> React UI -> Web Workers (WASM) -> Chain RPCs
                      |             |
                      |             +-> Solana RPC / EVM RPC / Bitcoin API
                      |
                      +-> Backend API (policy + quote/proxy + telemetry)
                                   |
                                   +-> Infra stores (memory or Redis-compatible abstraction)
```

## Core Components
- `core-rust/` - cryptographic primitives, derivation, signing.
- `wasm/` - WASM bindings and memory hygiene.
- `frontend/` - React UI, chain adapters, workers, and services.
- `backend/` - APIs for auth, wallet blob, proxy/RPC, swap quotes, AMM controls.

## Chain Adapter Layer
Adapters isolate chain-specific logic:
- `frontend/src/chains/adapters/` for balance/history/tokens/health
- `frontend/src/chains/adapters/send/` for send flows
- `frontend/src/chains/adapters/swaps/` for swap quote/execution flows

## Backend Route Composition
- `app.rs` composes route modules and middleware only.
- Route modules:
  - `routes/auth.rs`
  - `routes/wallet.rs`
  - `routes/proxy.rs`
  - `routes/evm.rs`
  - `routes/jupiter.rs`
  - `routes/testnet_amm.rs`
  - `routes/solana_amm.rs`
  - `routes/system.rs`
- Shared infra:
  - `infra/contracts.rs` for DTO/error contracts
  - `infra/middleware.rs` for request context + rate limiting
  - `infra/rpc.rs` and `infra/proxy.rs` for upstream controls
  - `infra/cache.rs`, `infra/rate_limit_store.rs`, `infra/redis_store.rs` for backend abstraction

## Execution and Trust Boundaries
- Signing is always in WASM (worker) via `WasmWalletFacade`.
- Backend has zero signing authority.
- Backend treats upstream provider data as untrusted and enforces policy checks.
- Chain interactions remain mediated through adapters/services and strict backend contracts.
