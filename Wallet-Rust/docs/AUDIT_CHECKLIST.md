# Audit Checklist

## Key Management
- [ ] Export disabled by default (feature gated)
- [ ] WASM memory zeroing verified
- [ ] Derivation paths allowlisted

## Signing
- [ ] All signing is client-side WASM
- [ ] Chain ID and nonce included
- [ ] Replay protections per chain

## RPC Safety
- [ ] Health checks enabled
- [ ] `/metrics` scrape enabled for backend
- [ ] Fallback RPCs configured
- [ ] Malicious response handling
- [ ] JSON-RPC strict method allowlist enforced (`ALLOWED_RPC_METHODS`)
- [ ] `/api/proxy/rpc` rejects non-POST requests
- [ ] RPC URL validation blocks localhost/private IPs and enforces allowlist/domain boundary checks
- [ ] RPC request body size cap enforced (`RPC_MAX_REQUEST_BODY_BYTES`)
- [ ] RPC batch item cap enforced (`RPC_MAX_BATCH_ITEMS`)
- [ ] Per-method per-client RPC rate bucket enforced (`RPC_METHOD_RATE_LIMIT_MAX_REQUESTS`)
- [ ] RPC method rate window configured (`RPC_METHOD_RATE_LIMIT_WINDOW_MS`)
- [ ] Reject-reason counters exported via Prometheus (`elementa_api_reject_reason_total{reason=...}`)
- [ ] Alert rules configured for reject-reason spikes (invalid query, blocked rpcUrl, payload abuse, aggregate critical)

## Upstream Error Handling
- [ ] Production error redaction enabled for `/api/proxy/*`
- [ ] Production error redaction enabled for `/api/evm/*` upstream failures
- [ ] Production error redaction enabled for `/api/jupiter/*` upstream failures
- [ ] Detailed upstream failures logged server-side with structured fields
- [ ] Error response envelope standardized to `error.code/message/details?/requestId?` on hardened endpoints

## Production Guard Rails
- [ ] `ENABLE_SERVER_KEK=1` rejected in production
- [ ] Production startup fails when `RPC_URL_ALLOWLIST` is empty

## Swap Integrity
- [ ] Quote and execution consistency
- [ ] Allowance checks and limits
- [ ] Slippage bounds applied

## UI Security
- [ ] CSP enforced
- [ ] XSS protections verified
- [ ] No secrets in logs
- [ ] Frontend API clients handle typed backend errors (`error.code`) without brittle string-matching
