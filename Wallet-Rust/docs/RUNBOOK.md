# Runbook

## Frontend
1. Install deps: `cd frontend && npm install`
2. Start dev: `npm run dev`
3. Build: `npm run build`

## WASM
1. Build: `cd wasm && wasm-pack build --target web`

## Backend
1. Start Rust backend: `cd backend && cargo run -p earth_backend`
2. Security baseline for production:
   - `NODE_ENV=production`
   - `ENABLE_SERVER_KEK=0` (server KEK mode is blocked in production)
   - `RPC_URL_ALLOWLIST` must be non-empty and explicitly curated

## RPC/Proxy Abuse Controls
- `/api/proxy/rpc` accepts only `POST` JSON-RPC payloads.
- JSON-RPC method allowlist is enforced server-side (`ALLOWED_RPC_METHODS` in `backend/server/src/app.rs`).
- Request body size is capped (`RPC_MAX_REQUEST_BODY_BYTES`, default `262144` bytes).
- Batch item count is capped (`RPC_MAX_BATCH_ITEMS`, default `25`).
- Per-method per-client rate bucket is enforced for RPC:
  - max requests: `RPC_METHOD_RATE_LIMIT_MAX_REQUESTS` (default `60`)
  - window: `RPC_METHOD_RATE_LIMIT_WINDOW_MS` (default `60000`)
- RPC upstream hosts must match strict allowlist/domain matching in `validate_rpc_url`.
- `/health` exposes alertable abuse counters:
  - `apiMetrics.rpcMethodRateLimitedSingle`
  - `apiMetrics.rpcMethodRateLimitedBatch`

## Cloud Recovery Abuse Controls
- `/api/wallet/blob` now enforces per-client+user throttles:
  - read limit: `CLOUD_BLOB_READ_MAX_REQUESTS` per `CLOUD_BLOB_READ_WINDOW_MS`
  - write limit: `CLOUD_BLOB_WRITE_MAX_REQUESTS` per `CLOUD_BLOB_WRITE_WINDOW_MS`
- Cloud blob validation failures are tracked as explicit reject reasons.
- Reject responses use typed error envelope codes:
  - `cloud_blob_read_rate_limited`
  - `cloud_blob_write_rate_limited`
  - `cloud_blob_validation_failed`
- Incident response workflow:
  1. Check `/metrics` and `/health` for cloud blob reject spikes.
  2. Correlate `x-request-id` from client error with backend logs.
  3. If abuse is sustained, temporarily tighten edge limits/WAF for `/api/wallet/blob`.
  4. If validation failures spike without abuse pattern, investigate frontend payload regression.

## Upstream Error Redaction
- In `production`, upstream `details` are redacted for:
  - `/api/proxy/*`
  - `/api/evm/*` quote/price failures
  - `/api/jupiter/*` quote/swap failures
- Full upstream details remain in server logs via `tracing::warn!` for incident debugging.

## Swap Services
- EVM mainnet: set `ZEROX_API_KEY` (or fallback envs).
- Solana mainnet: set `JUPITER_API_KEY`.
- Solana testnet AMM: set program ID + deployer keys, call init endpoint.

## Health Checks
- UI uses `NetworkHealthService` (adapter-based).
- Backend `/health` (if present) should be added to monitoring.

## Monitoring Integration
- Backend now exposes Prometheus text metrics at `/metrics`.
- RPC abuse counters exported:
  - `elementa_rpc_method_rate_limited_single_total`
  - `elementa_rpc_method_rate_limited_batch_total`
- Reject-reason counters exported (labeled):
  - `elementa_api_reject_reason_total{reason="invalid_query"}`
  - `elementa_api_reject_reason_total{reason="blocked_rpc_url"}`
  - `elementa_api_reject_reason_total{reason="rpc_payload_too_large"}`
  - `elementa_api_reject_reason_total{reason="invalid_jsonrpc_payload"}`
  - `elementa_api_reject_reason_total{reason="invalid_jsonrpc_request"}`
  - `elementa_api_reject_reason_total{reason="rpc_method_rate_limited_single"}`
  - `elementa_api_reject_reason_total{reason="rpc_method_rate_limited_batch"}`
  - `elementa_api_reject_reason_total{reason="cloud_blob_read_rate_limited"}`
  - `elementa_api_reject_reason_total{reason="cloud_blob_write_rate_limited"}`
  - `elementa_api_reject_reason_total{reason="cloud_blob_validation_failed"}`
- Prometheus alert rules are provided at `ops/monitoring/prometheus/elementa-rpc-alerts.yml`.
- Grafana dashboard JSON is provided at `ops/monitoring/grafana/elementa-rpc-abuse-dashboard.json`.

## Typed Error Envelope Contract
- Hardened endpoints (`/api/proxy/*`, `/api/evm/quote|price`, `/api/jupiter/quote`) emit:
  - `error.code`
  - `error.message`
  - optional `error.details`
  - optional `error.requestId` (when incoming `x-request-id` is provided)
- Client integrations should key retry/UX behavior from `error.code` rather than parsing raw message strings.

## Migration Control (Phase 0)
- Hardening branch for migration controls: `hardening/phase-0-baseline-freeze`.
- Release freeze is active for this migration stream:
  - Allowed: security fixes, migration decomposition, regression fixes, tests/observability hardening.
  - Blocked: net-new product features and unrelated refactors.
- Rollback owner:
  - Primary: `Backend on-call lead`
  - Secondary: `Frontend on-call lead`
- Emergency rollback command list (validated):
  1. `git branch --show-current`
  2. `git log --oneline -n 20`
  3. `git switch main`
  4. `git reset --hard <LAST_GOOD_COMMIT_SHA>`
  5. `git push --force-with-lease origin main`
  6. `git revert <BAD_COMMIT_SHA>`
  7. `git push origin main`
  8. `cargo test -p earth_backend`
  9. `npm run build --prefix frontend`
- CI architecture/size guards:
  - React file limit: `350` LOC
  - TS service limit: `250` LOC
  - Rust module limit: `600` LOC
- Gate source of truth: `docs/MIGRATION_PHASE_GATE.md`.
