# Elementa Wallet

Rust-first multi-chain wallet platform with a pure Rust backend and React frontend.

## Project Layout
- `backend/` Rust backend workspace (server, core, evm-tooling, solana-tooling)
- `frontend/` React + Vite app
- `core-rust/` wallet cryptography crate
- `wasm/` WASM bindings used by frontend
- `docs/` all project documentation

## Quick Start
1. Backend env
- Copy `backend/.env.example` to `backend/.env`
- Fill required secrets (`SESSION_JWT_SECRET`, `CLOUD_KEK_SECRET`, OAuth keys, API keys)

2. Run backend
```powershell
cd Wallet-Rust/backend
cargo run -p earth_backend
```
Backend: `http://127.0.0.1:3001`

3. Run frontend
```powershell
cd Wallet-Rust/frontend
npm install
npm run dev
```


cd "c:\Stellix\rust\New folder\trae\latest-encryption-design\rust-backend\Wallet-Rust\wasm" ; cargo build --target wasm32-unknown-unknown --release

Frontend: `http://localhost:5173`

## Validation Commands
- Backend tests: `cd backend && cargo test --workspace`
- Backend lint: `cd backend && cargo clippy --workspace --all-targets -- -D warnings`
- Frontend lint: `cd frontend && npm run lint`
- Frontend unit: `cd frontend && npm run test:unit`
- Frontend e2e: `cd frontend && npm run test:e2e`

## Solana/EVM Notes
- Solana AMM endpoints: `/api/solana/amm/*`
- EVM AMM endpoints: `/api/testnet-amm/*`
- State files are under `backend/state/`

## Documentation Index
- `docs/CHANGELOG_PHASE4.md`
- `docs/CHANGELOG_PHASE5.md`
- `docs/CHANGELOG_PHASE6.md`
- `docs/ARCHITECTURE.md`
- `docs/RUNBOOK.md`
- `docs/SECURITY_MODEL.md`
- `docs/THREAT_MODEL.md`
- `docs/KEY_MANAGEMENT.md`
- `docs/DATA_FLOWS.md`
- `docs/LOGGING.md`
- `docs/INCIDENT_RESPONSE.md`
- `docs/DEPENDENCY_INVENTORY.md`
- `docs/AUDIT_CHECKLIST.md`
- `docs/AUDIT_PACKET.md`
- `docs/PROJECT_FUNCTIONALITIES.md`


