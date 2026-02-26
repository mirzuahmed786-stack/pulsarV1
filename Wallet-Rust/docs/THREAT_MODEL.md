# Threat Model

## Assets
- Seed phrase, private keys, derived keys
- Transaction signing integrity
- User funds and swap execution correctness
- Transaction history and balances
- Session and CSRF integrity
- Admin operation integrity

## Adversaries
- Malicious RPC endpoints
- Malicious browser extensions/XSS payloads
- Backend compromise
- Phishing and social engineering
- Supply chain attacks on dependencies
- Abuse actors attempting proxy/RPC exhaustion

## Entry Points
- Frontend bundle and injected scripts
- RPC responses and aggregator quotes
- Backend APIs for quotes and AMM init
- Local storage for custom tokens
- Cloud recovery blob endpoints (`/api/wallet/blob`)
- Admin-bearing AMM endpoints

## Threats and Mitigations
- Threat: hostile RPC method abuse or oversized payload response
  - Mitigation: RPC method allowlist, payload/body size caps, response-size enforcement, per-host concurrency caps.
- Threat: malformed financial endpoint payloads causing unsafe behavior
  - Mitigation: strict DTO validation and deny-unknown field policy with stable 4xx envelopes.
- Threat: admin path misuse in production
  - Mitigation: production config hard-blocks insecure admin mode; admin token path is explicit and test-covered.
- Threat: cloud recovery blob brute-force/abuse
  - Mitigation: read/write rate-limit buckets and validation failure counters.
- Threat: frontend script injection
  - Mitigation: Trusted Types policy + production CSP contract.
- Threat: quote spoofing / upstream failure masking
  - Mitigation: deterministic upstream error handling and observability on upstream source cardinality.

## Residual Risks
- If browser/device is compromised, funds can be stolen
- Malicious RPCs can censor or delay, or return misleading data
- User approval on malicious UI can still sign harmful transactions
- Third-party outage can degrade quote/fill reliability despite fallback controls
