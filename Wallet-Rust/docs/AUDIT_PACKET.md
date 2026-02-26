# Audit Packet

## Scope
- Product: Elementa Wallet (`Wallet-Rust` monorepo)
- Review target: Phase 6 launch readiness artifacts
- Last updated: 2026-02-13

## System Snapshot
- Frontend: React + Vite (`frontend/`)
- Wallet core/signing: Rust + WASM (`core-rust/`, `wasm/`)
- Backend: Rust Axum service (`backend/server/`)
- Supported chain domains: EVM, Solana, Bitcoin

## Trust Boundaries
- Boundary 1: User browser/device vs internet
  - Risk: XSS, extension injection, phishing.
  - Primary controls: CSP, Trusted Types, strict route DTO validation, CSRF on state-changing endpoints.
- Boundary 2: Frontend vs backend API
  - Risk: malformed payloads, privilege abuse.
  - Primary controls: deny-unknown DTOs, typed error envelopes, admin token checks, production insecure-admin block.
- Boundary 3: Backend vs upstream RPC/aggregators
  - Risk: hostile upstream data, rate-limit abuse, large payload attacks.
  - Primary controls: RPC method allowlist, response-size limits, per-host concurrency, cache/rate-limit guards.
- Boundary 4: Local vault/session vs cloud recovery blob
  - Risk: recovery blob abuse, key exposure.
  - Primary controls: client-passphrase default, validated blob schema, read/write throttle buckets, no server signing authority.

## Key Custody Matrix
| Asset | Owner | Stored Where | Backend Access | Control |
|---|---|---|---|---|
| Seed/private key material | Client | Local encrypted vault / WASM memory | No | Non-custodial model |
| Session JWT secret | Backend ops | Backend env | Yes | Production length requirement |
| Cloud KEK secret | Backend ops | Backend env | Yes | Production length requirement |
| Cloud recovery blob | User + backend store | File/SQLite cloud store | Yes (blob only) | Validation + rate limits |
| Aggregator/API keys | Backend ops | Backend env | Yes | Secret not returned to client |
| Admin token | Backend ops | Backend env | Yes | Explicit admin auth path |

## Incident Playbook
- Canonical runbooks:
  - `docs/INCIDENT_RESPONSE.md`
  - `docs/RUNBOOK.md`
- Rollback ownership:
  - Primary: Backend on-call lead
  - Secondary: Frontend on-call lead
  - Authority: Incident commander on duty
- Emergency rollback commands and validation:
  - `docs/MIGRATION_PHASE_GATE.md` (Phase 0 rollback section)

## Endpoint Inventory (Backend)
- System:
  - `GET /health`
  - `GET /metrics`
  - `GET /api/deployed`
  - `POST /api/batch`
- Auth:
  - `GET /api/auth/apple/prepare`
  - `GET /api/auth/google/prepare`
  - `POST /api/auth/google/exchange`
  - `POST /api/auth/apple/exchange`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
  - `GET /api/auth/kek`
- Wallet:
  - `GET /api/wallet/blob`
  - `PUT /api/wallet/blob`
- Proxy/RPC:
  - `GET /api/proxy/history`
  - `GET /api/proxy/prices`
  - `ANY /api/proxy/rpc` (POST enforced)
- EVM/Jupiter:
  - `GET /api/evm/quote`
  - `GET /api/evm/price`
  - `GET /api/evm/status`
  - `GET /api/jupiter/status`
  - `GET /api/jupiter/quote`
  - `POST /api/jupiter/swap`
- AMM admin/testnet:
  - `GET /api/testnet-amm/:chain`
  - `POST /api/testnet-amm/deploy`
  - `POST /api/testnet-amm/mint`
  - `POST /api/testnet-amm/liquidity`
  - `GET /api/solana/amm/status`
  - `POST /api/solana/amm/quote`
  - `POST /api/solana/amm/swap-tx`
  - `POST /api/solana/amm/mint`
  - `POST /api/solana/amm/init`

## Control Evidence
- Security model: `docs/SECURITY_MODEL.md`
- Threat model: `docs/THREAT_MODEL.md`
- Architecture boundaries: `docs/ARCHITECTURE.md`
- Data-flow references: `docs/DATA_FLOWS.md`
- Observability: `docs/OBSERVABILITY.md`
- Operational controls: `docs/RUNBOOK.md`
- Config controls: `backend/server/src/config.rs`

## Test Evidence
- Backend suite:
  - Command: `cd backend && cargo test -p earth_backend`
  - Latest result: `78 passed; 0 failed`
- Key Phase 6 coverage:
  - Redis backend fallback and required-fail startup tests
  - Infra metrics series export test
  - RPC policy and response-size tests
  - Cloud recovery read/write throttling tests
  - Financial route malformed-payload regression matrix

## Deterministic Build Evidence
Validation timestamp: 2026-02-14 (local machine time)

- Backend release artifact:
  - `backend/target/release/earth_backend.exe`
  - SHA-256: `7DBD07EEFC5DB0B3557AE4B83EDEFAB8B968699F2DBD7A3FA9606B4C0DCDE380`
- Frontend dist tree aggregate:
  - `frontend/dist/**` (96 files)
  - Tree SHA-256: `9c6973cab5adba155dae4d3d09cec3d86eab7911eb482592ce4de3b5ece12ccd`
  - Per-file hash manifest: `frontend/dist-hashes.txt`
- Replay check:
  - Second clean build run produced identical hashes for both backend and frontend artifacts.

## Vulnerability Scan Results
Validation timestamp: 2026-02-14 (local machine time)

- Rust:
  - Command: `cd backend && cargo audit --json`
  - Result: vulnerabilities `3`, unmaintained warnings `6`, yanked warnings `1`
  - Findings:
    - `RUSTSEC-2024-0344` (`curve25519-dalek@3.2.0`)
    - `RUSTSEC-2022-0093` (`ed25519-dalek@1.0.1`)
    - `RUSTSEC-2025-0009` (`ring@0.16.20`)
- Frontend (production deps):
  - Command: `cd frontend && npm audit --omit=dev --json`
  - Result: total `10`; critical `0`; high `3`; moderate `0`; low `7`
  - High findings are rooted in Solana/crypto dependency chain and tracked for upgrade planning.
- Launch-gate status:
  - Critical vulnerabilities open: `0` (meets final gate condition).

## Staging Rollback Rehearsal
Validation timestamp: 2026-02-14 (local machine time)

- Rehearsal run in isolated clone:
  - `C:\Users\HP\AppData\Local\Temp\wallet-rollback-rehearsal-20260214-003238`
- Executed workflow:
  - branch check
  - commit log inspection
  - hard reset to prior commit
  - revert of candidate bad commit
- Evidence file:
  - `docs/rollback-rehearsal.log`
- Result:
  - rollback command sequence executed successfully in rehearsal environment.
