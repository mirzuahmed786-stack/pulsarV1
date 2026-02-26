# CSP Policy

## Production Contract
Production deploys must ship `frontend/dist/_headers` with this CSP:

`default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; script-src-attr 'none'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: blob:; connect-src 'self' https: wss:; worker-src 'self' blob:; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; frame-src 'none'; require-trusted-types-for 'script'; trusted-types earth-default`

## Security Requirements
- No wildcard sources (`*`) are allowed.
- No `unsafe-inline` or `unsafe-eval` in `script-src`.
- Only required script exception is `'wasm-unsafe-eval'` for WASM execution.
- Trusted Types is required for script sinks.

## Build Integration
- `frontend/vite.config.ts` generates `dist/_headers` during production build.
- `frontend/public/_headers` mirrors the policy for static-host compatibility.

## Deploy Verification
Run after build:

```bash
cd frontend
npm run build
cat dist/_headers
```

Verify the emitted `Content-Security-Policy` exactly matches this contract.
