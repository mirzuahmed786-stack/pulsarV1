# Observability

## Scope
Phase 6 backend observability baseline for `backend/server`.

## Request Correlation
- Every request is assigned/propagates `x-request-id`.
- Middleware emits structured request logs including:
  - `request_id`
  - `method`
  - `path`
  - `route` (normalized route family)
  - `status`
  - `latency_ms`
- Request handling is wrapped in a tracing span (`http_request`) so downstream logs can be correlated by request ID.

## Metrics Endpoints
- `GET /health`: JSON health snapshot and internal counters.
- `GET /metrics`: Prometheus exposition format.

## Exported Metric Families
- Rate-limit counters:
  - `elementa_rpc_method_rate_limited_single_total`
  - `elementa_rpc_method_rate_limited_batch_total`
- Reject reason counters:
  - `elementa_api_reject_reason_total{reason="..."}`
- Cache gauges:
  - `elementa_cache_entries{cache="proxy|rpc"}`
- Infra backend and Redis health telemetry:
  - `elementa_infra_backend_info{backend="memory|memory-fallback|redis"}`
  - `elementa_redis_ping_total{result="ok|error"}`
  - `elementa_redis_ping_latency_ms_avg`
- Route latency histogram:
  - `elementa_http_request_latency_ms_bucket{route="...",le="..."}`
  - `elementa_http_request_latency_ms_sum{route="..."}`
  - `elementa_http_request_latency_ms_count{route="..."}`
- Upstream error cardinality:
  - `elementa_upstream_error_total{source="all"}`
  - `elementa_upstream_error_total{source="<upstream_name>"}`

## Route Family Labels
Latency histograms are grouped by normalized route family:
- `/health`
- `/metrics`
- `/api/auth`
- `/api/wallet`
- `/api/proxy`
- `/api/evm`
- `/api/jupiter`
- `/api/testnet-amm`
- `/api/solana/amm`
- `/api/other`
- `/other`

## Upstream Source Labels
Current upstream source labels include:
- `zeroex`
- `evm_fallback_aggregator`
- `jupiter_quote`
- `jupiter_swap`
- `proxy_history`
- `proxy_prices`
- `rpc_proxy`

## Verification
- Backend tests assert that `/metrics` exposes:
  - RPC rate-limit counters
  - Reject reason counters
  - Cache gauges
  - Infra backend and Redis ping series
  - Route latency histogram series
  - Upstream cardinality series
