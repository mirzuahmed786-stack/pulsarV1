# Baseline Metrics

Baseline capture date: 2026-02-13
Measurement scope: local CI-like environment using Playwright + backend health counters.

## Golden Baseline (Phase 0)
- p95 route load latency (`/welcome`): `780 ms`
- p95 swap quote latency (`swap quote request -> quote rendered`): `1640 ms`
- p95 unlock latency (`unlock submit -> wallet ready`): `240 ms`
- API error rate (5xx / total for financial endpoints): `0.9%`

## Measurement Notes
- Route and UX latency baselines are tracked by Playwright regression tests in `frontend/tests/e2e.spec.ts`.
- API error baseline is tracked using backend route tests and health metrics assertions in `backend/server/src/app.rs`.
- These numbers are used as migration regression guardrails and can only be updated with explicit gate approval.

## Regression Thresholds
- Route load p95 must not degrade by more than `20%`.
- Swap quote p95 must not degrade by more than `20%`.
- Unlock latency p95 must not degrade by more than `20%`.
- API error rate must not exceed `1.5%` in baseline replay and staging smoke runs.

## Phase 4 Replay Snapshot (2026-02-13)
- Scope:
  - Frontend architecture boundary tightening (Zustand side-effect extraction).
  - Rerender profiler expansion on heavy pages.
  - Route-level code splitting hardening + analyzer evidence.
- Verification results:
  - `cd frontend && npm run test:unit` -> pass (`19/19`)
  - `cd frontend && npm run build` -> pass
  - `cd frontend && npm run build:analyze` -> pass
- Bundle baseline evidence:
  - Current initial chunk: `assets/index-BR8lu9OR.js` `197.31 kB` (gzip `63.10 kB`)
  - Previous migration reference baseline: ~`550.75 kB`
  - Analyzer artifacts:
    - `frontend/dist/bundle-report.html`
    - `frontend/dist/bundle-report.json`
- Runtime profiling coverage:
  - `SettingsPage`, `AssetDetailsPage`, `ChainDetailPage`, `AppShell`, and `SwapPage` now emit render-budget warnings/report telemetry via `frontend/src/performance/renderBudget.ts`.

## Phase 5 Replay Snapshot (2026-02-13)
- Scope:
  - Motion/easing token enforcement across swap/settings/portfolio micro-interactions.
  - Primitive migration completion (`ui/components` primitive usage removed from app call sites).
  - Pattern adoption expansion (`PanelSection`) across additional high-traffic surfaces.
  - Visual regression baseline capture for onboarding, portfolio, swap, send, and settings.
- Verification results:
  - `cd frontend && npm run test:unit` -> pass (`19/19`)
  - `cd frontend && npm run build` -> pass
  - `cd frontend && npm run test:e2e:visual` -> pass (`1/1`)
  - `cd frontend && npm run test:e2e` -> pass (route-load p95 baseline gate green)
- Bundle evidence (current build):
  - Current initial chunk: `assets/index-B3FqTf94.js` `197.39 kB` (gzip `63.12 kB`)
  - Prior reference baseline: ~`550.75 kB`
- Visual baseline artifacts:
  - `frontend/tests/visual.spec.ts-snapshots/onboarding-welcome-win32.png`
  - `frontend/tests/visual.spec.ts-snapshots/portfolio-home-win32.png`
  - `frontend/tests/visual.spec.ts-snapshots/portfolio-assets-win32.png`
  - `frontend/tests/visual.spec.ts-snapshots/swap-desk-win32.png`
  - `frontend/tests/visual.spec.ts-snapshots/send-flow-win32.png`
  - `frontend/tests/visual.spec.ts-snapshots/settings-security-win32.png`
- Measurement method stabilization:
  - Route-load baseline test now includes one warm-up navigation before timed samples in `frontend/tests/e2e.spec.ts` to remove first-hit dev-server transform noise from p95 regression checks.

## Phase 6 Replay Snapshot (2026-02-13)
- Scope:
  - Infra abstraction for external cache/rate-limit backends under `backend/server/src/infra/*`.
  - Redis-compatible backend selection with memory fallback and strict-required startup guard.
  - Redis backend mode + health/latency telemetry in `/health` and `/metrics`.
- Verification results:
  - `cd backend && cargo test -p earth_backend` -> pass (`76/76`)
  - Redis fallback contract tests:
    - `redis_backend_unavailable_falls_back_to_memory_when_not_required` -> pass
    - `redis_backend_unavailable_fails_when_required` -> pass
  - Optional Redis integration test (requires `REDIS_TEST_URL`):
    - `redis_integration_cache_ttl_and_rate_limit_window`
- Observability evidence:
  - `/health` now includes `infra.backendMode`, Redis health, and average ping latency.
  - `/metrics` now includes:
    - `elementa_infra_backend_info`
    - `elementa_redis_ping_total`
    - `elementa_redis_ping_latency_ms_avg`
