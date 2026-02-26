# Incident Response

## Severity
- SEV1: Active loss of funds or key exposure
- SEV2: Signing failures or incorrect swaps
- SEV3: UI outages or degraded performance

## Immediate Actions
- Revoke/rotate API keys for aggregators
- Disable swap endpoints if integrity is uncertain
- Publish user advisory if keys or funds at risk

## Investigation
- Identify blast radius by chain/network
- Reproduce in staging
- Audit recent releases and dependencies

## Recovery
- Patch and deploy
- Verify integrity with regression tests
- Restore services, monitor for recurrence
