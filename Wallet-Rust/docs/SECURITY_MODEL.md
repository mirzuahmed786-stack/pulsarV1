# Security Model

## Custody Model
- Non-custodial by default: private keys and seed material remain client-side.
- Backend has zero signing authority and does not hold spend-capable key material.
- Cloud recovery stores encrypted blob data, not plaintext seed/private keys.

## Key Handling Rules
- Key generation and derivation execute in Rust/WASM.
- Signing executes in WASM/worker boundary.
- Key export is restricted by policy/feature gating and not part of normal flows.
- Production default cloud recovery mode is client-passphrase, with server mode treated as exception-only.

## Trust Boundaries
- Browser/device boundary:
  - Untrusted runtime; susceptible to extension/XSS compromise.
- Frontend <-> backend boundary:
  - Backend responses are treated as untrusted input requiring validation.
- Backend <-> upstream boundary:
  - RPC and aggregator responses are untrusted and policy-constrained.
- Secrets boundary:
  - `SESSION_JWT_SECRET` and `CLOUD_KEK_SECRET` remain backend-only operational secrets.

## Security Assumptions
- Public network is hostile.
- Upstream providers may be malicious, unavailable, or rate-limiting.
- End-user devices can be compromised; residual risk cannot be reduced to zero.

## Enforced Controls
- Input and DTO hardening:
  - Strict request DTO parsing with unknown-field rejection on financial endpoints.
- RPC/proxy hardening:
  - Method allowlist, request/response size limits, per-host concurrency caps.
- Admin hardening:
  - Insecure admin mode blocked in production.
  - Explicit admin auth path required for sensitive operations.
- Abuse controls:
  - Route and method-scoped rate limiting.
  - Cloud recovery blob read/write throttling with stable reject codes.
- Browser defenses:
  - Trusted Types + production CSP contract.
- Telemetry and traceability:
  - Request ID correlation, route latency metrics, upstream error cardinality.
