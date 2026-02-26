# Migration Phase Gate

## Phase 0: Baseline, Freeze, and Safety Nets

### Hardening Branch
- Active hardening branch: `hardening/phase-0-baseline-freeze`
- Change control rule: only migration/hardening commits are allowed while freeze is active.

### Release Freeze Checklist
- [x] Release freeze declared for migration workstream.
- [x] No net-new product features merged during freeze window.
- [x] Emergency-only production changes require rollback command validation before merge.
- [x] CI architecture/size guards added and wired.
- [x] Baseline metrics file created and versioned.

### Freeze Rules (Enforced)
- Allowed:
  - Security fixes
  - Regression fixes
  - Migration decomposition changes
  - Test and observability hardening
- Blocked:
  - Net-new user-facing features
  - Non-critical refactors not tied to migration gates
  - Dependency churn without security/migration rationale
- Merge contract:
  - At least one reviewer signs off against this gate file.
  - Any exception must include rollback plan and owner.

### Rollback Ownership
- Rollback owner (primary): `Backend on-call lead`
- Rollback owner (secondary): `Frontend on-call lead`
- Decision authority: `Incident commander on duty`

### Emergency Rollback Commands (Validated)
Validation timestamp: 2026-02-13 (local workspace)

- Branch state check:
  - `git branch --show-current`
  - Validation: passed
- Inspect candidates for rollback:
  - `git log --oneline -n 20`
  - Validation: passed
- Restore deployment branch to previous good commit:
  - `git switch main`
  - `git reset --hard <LAST_GOOD_COMMIT_SHA>`
  - `git push --force-with-lease origin main`
  - Validation: command syntax validated; execution restricted to emergency owner approval.
- Fast revert without history rewrite:
  - `git revert <BAD_COMMIT_SHA>`
  - `git push origin main`
  - Validation: command syntax validated.
- Verify after rollback:
  - `cargo test -p earth_backend`
  - `npm run build --prefix frontend`
  - Validation: command list validated for recovery workflow.

### Phase Sign-off
- [ ] Phase 0 approved by backend owner
- [ ] Phase 0 approved by frontend owner
- [ ] Phase 0 approved by security owner

## Phase 2: Backend Decomposition and Route Hardening

Validation timestamp: 2026-02-13 (local workspace)

### Gate Checklist
- [x] `app.rs` reduced to composition-focused router wiring and shared constants/re-exports.
- [x] Financial route DTO validation rejects malformed or unknown fields with stable 4xx error shape.
- [x] Production admin insecure mode is blocked and test-covered.
- [x] RPC policy enforceable with explicit method allowlist, per-host concurrency control, and response-size limits.
- [x] Full backend test suite passes after extraction and hardening changes.

### Evidence
- Composition-only app wiring:
  - `backend/server/src/app.rs`
  - `backend/server/src/routes/system.rs`
- Shared DTO/contracts extracted:
  - `backend/server/src/infra/contracts.rs`
- Route decomposition modules:
  - `backend/server/src/routes/proxy.rs`
  - `backend/server/src/routes/evm.rs`
  - `backend/server/src/routes/jupiter.rs`
  - `backend/server/src/routes/solana_amm.rs`
  - `backend/server/src/routes/testnet_amm.rs`
  - `backend/server/src/routes/auth.rs`
  - `backend/server/src/routes/wallet.rs`
- RPC policy and tests:
  - `backend/server/src/policy.rs`
- Phase-2 malformed payload regression matrix tests:
  - `backend/server/src/app_phase2_regression_matrix.rs`
- Production admin lockdown tests:
  - `backend/server/src/config.rs`
  - `backend/server/src/app.rs` (test module)

### Phase 2 Sign-off
- [ ] Phase 2 approved by backend owner
- [ ] Phase 2 approved by security owner

## Phase 6: Scale, Audit Readiness, and Launch Gate (Infra Abstraction Pass)

Validation timestamp: 2026-02-13 (local workspace)

### Gate Checklist
- [x] Cache and rate-limit infra abstractions added under `backend/server/src/infra/*` with Redis-compatible interfaces.
- [x] Backend startup supports `memory` and `redis` backend selection with no API route contract changes.
- [x] Redis unavailable fallback path is deterministic when `INFRA_REDIS_REQUIRED=0`.
- [x] Redis required mode fails startup when backend cannot connect.
- [x] Health/metrics expose backend mode and Redis ping health/latency telemetry.
- [x] Backend test suite passes after abstraction wiring.
- [x] Audit packet and model documents updated with trust boundaries, custody matrix, endpoint inventory, and test evidence.

### Evidence
- Infra abstraction modules:
  - `backend/server/src/infra/cache.rs`
  - `backend/server/src/infra/rate_limit_store.rs`
  - `backend/server/src/infra/redis_store.rs`
  - `backend/server/src/infra/state.rs`
- Route + middleware wiring preserved:
  - `backend/server/src/infra/proxy.rs`
  - `backend/server/src/infra/rpc.rs`
  - `backend/server/src/infra/middleware.rs`
  - `backend/server/src/routes/proxy.rs`
  - `backend/server/src/routes/wallet.rs`
- Config and deploy contract:
  - `backend/server/src/config.rs`
  - `backend/.env.example`
- Audit pack and supporting architecture/security docs:
  - `docs/AUDIT_PACKET.md`
  - `docs/SECURITY_MODEL.md`
  - `docs/THREAT_MODEL.md`
  - `docs/ARCHITECTURE.md`
  - `docs/DATA_FLOWS.md`
- Observability contract:
  - `backend/server/src/infra/observability.rs`
  - `docs/OBSERVABILITY.md`
- Test evidence:
  - `cargo test -p earth_backend` -> `78 passed; 0 failed`
  - `redis_backend_unavailable_falls_back_to_memory_when_not_required`
  - `redis_backend_unavailable_fails_when_required`

### Deterministic Build Hash Record
Validation timestamp: 2026-02-14 (local machine time)

- Backend release build:
  - Command: `cd backend && cargo build -p earth_backend --release`
  - Artifact: `backend/target/release/earth_backend.exe`
  - SHA-256: `7DBD07EEFC5DB0B3557AE4B83EDEFAB8B968699F2DBD7A3FA9606B4C0DCDE380`
- Frontend production build:
  - Command: `cd frontend && npm run build`
  - Artifact set: `frontend/dist/**` (96 files)
  - Tree SHA-256 (sorted path+file hash aggregate): `9c6973cab5adba155dae4d3d09cec3d86eab7911eb482592ce4de3b5ece12ccd`
  - File hash list: `frontend/dist-hashes.txt`
- Reproducibility replay:
  - Rebuilt backend and frontend a second time.
  - Hashes matched exactly on replay.

### Vulnerability Scan Summary
Validation timestamp: 2026-02-14 (local machine time)

- Rust audit:
  - Command: `cd backend && cargo audit --json`
  - Result: `3` vulnerabilities, `6` unmaintained warnings, `1` yanked warning.
  - Vulnerability IDs:
    - `RUSTSEC-2024-0344` (`curve25519-dalek@3.2.0`)
    - `RUSTSEC-2022-0093` (`ed25519-dalek@1.0.1`)
    - `RUSTSEC-2025-0009` (`ring@0.16.20`)
- Frontend prod dependency audit:
  - Command: `cd frontend && npm audit --omit=dev --json`
  - Result: total `10`; critical `0`; high `3`; moderate `0`; low `7`.
- Gate condition:
  - Critical vulnerabilities open: `0` (pass).
  - High-severity and maintenance warnings tracked in `docs/AUDIT_PACKET.md`.

### Staging Rollback Rehearsal Evidence
Validation timestamp: 2026-02-14 (local machine time)

- Rehearsal executed in isolated clone to avoid mutating active workspace.
- Rehearsal log file: `docs/rollback-rehearsal.log`
- Command path and outputs:
  - Clone path: `C:\Users\HP\AppData\Local\Temp\wallet-rollback-rehearsal-20260214-003238`
  - Branch: `main`
  - Head before rollback rehearsal: `5ed38ae`
  - Last good candidate: `3e4f46d`
  - `git reset --hard 3e4f46d` applied successfully
  - `git revert --no-edit 5ed38ae` applied successfully in rehearsal clone (`8df0b71`)
  - Rehearsal-only commit kept in temp clone, not pushed.

### Phase 6 Sign-off
- [x] Phase 6 approved by backend owner
- [x] Phase 6 approved by security owner
- [x] Phase 6 approved by release manager
