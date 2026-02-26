# Logging and Telemetry

## Client
- Log only non-sensitive events.
- Never log keys, mnemonic, or raw entropy.
- Use structured logs for swap and send outcomes.

## Backend
- Log request IDs, route, status code, duration.
- Redact API keys and secrets.
- Avoid logging full request payloads when they contain addresses.

## Privacy
- No PII collection required.
- User addresses are sensitive; minimize retention.
