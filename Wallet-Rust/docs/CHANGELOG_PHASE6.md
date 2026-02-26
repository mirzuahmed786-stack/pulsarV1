# Phase 6 Security and Reliability Additions

## 6.2 Cookie and Session Tightening
- Hardened `Set-Cookie` policy normalization in `backend/server/src/http/cookies.rs`.
- Invalid or malformed `SameSite` values normalize to `Lax`.
- `SameSite=None` now enforces `Secure`.
- Production guard (`NODE_ENV=production`) forces `Secure` and disallows `SameSite=None` fallback by converting to `Lax`.
- Added production-guard tests for cookie policy behavior.

## 6.3 Cloud Recovery Blob Abuse Controls
- Added per-user/per-client read and write rate buckets for `/api/wallet/blob`.
- Added typed reject codes for cloud blob abuse and validation failures:
  - `cloud_blob_read_rate_limited`
  - `cloud_blob_write_rate_limited`
  - `cloud_blob_validation_failed`
- Added reject-reason and throttle counters to `/health` and `/metrics`.
- Added targeted backend tests for cloud blob throttling and metric exposure.
- Added Prometheus alerts and Grafana panels for cloud blob abuse signals.

## 6.4 CI and Auditability Closeout
- Added a dedicated PR CI job for cloud-security contract tests:
  - `contract_wallet_blob_write_rate_limited_returns_429`
  - `contract_wallet_blob_read_rate_limited_returns_429`
  - `metrics_exposes_cloud_blob_reject_reason_counters`
- Updated runbook and audit checklist with cloud blob controls, typed errors, and reject-reason monitoring expectations.

## 6.5 Infra Abstraction for External Cache/Rate-Limit Backends
- Added infra abstraction interfaces under `backend/server/src/infra/*`:
  - `cache.rs` (`CacheStore`)
  - `rate_limit_store.rs` (`RateLimitStore`)
  - `redis_store.rs` (Redis-backed implementations)
- `AppState` now selects backend mode (`memory`, `redis`, `memory-fallback`) at startup without route API contract changes.
- Added Redis startup controls in config:
  - `INFRA_STORE_BACKEND`
  - `INFRA_REDIS_URL`
  - `INFRA_REDIS_KEY_PREFIX`
  - `INFRA_REDIS_REQUIRED`
  - `INFRA_REDIS_CONNECT_TIMEOUT_MS`
  - `INFRA_REDIS_COMMAND_TIMEOUT_MS`
- Added fallback/strictness tests:
  - Redis unavailable + required off => fallback to memory.
  - Redis unavailable + required on => startup failure.
- Added Redis-ready observability:
  - Health JSON includes `infra.backendMode` and Redis health/latency snapshot.
  - Prometheus exports backend mode and Redis ping counters/latency gauge.
