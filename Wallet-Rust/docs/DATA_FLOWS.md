# Data Flows

## Balance Fetch
UI -> Chain adapter -> RPC/indexer -> normalized balances -> UI

## Send (EVM/Solana/BTC)
UI -> Send adapter -> WASM sign -> RPC broadcast -> receipt/status -> UI

## Swap (EVM)
UI -> Swap service -> Backend `/api/evm/quote|price` -> user confirm -> WASM sign -> RPC broadcast

## Swap (Solana)
UI -> Swap service -> Backend `/api/jupiter/*` or `/api/solana/amm/*` -> user confirm -> WASM sign -> Solana RPC

## Swap (Earth AMM)
UI -> Earth Swap Adapter -> Local AMM RPC -> WASM sign -> RPC broadcast

## Wallet Session and CSRF
UI -> `/api/auth/*` prepare/exchange -> session cookie + CSRF token -> protected state-changing endpoints

## Cloud Recovery Blob
UI (authenticated) -> `/api/wallet/blob` PUT/GET -> backend validation + rate-limit bucket -> cloud store (file/sqlite)

## RPC Proxy
UI/service -> `/api/proxy/rpc` -> URL/method/payload policy checks -> per-host concurrency + cache -> upstream RPC

## Observability and Correlation
Request -> request context middleware -> request ID + route latency metrics -> `/health` and `/metrics` export
