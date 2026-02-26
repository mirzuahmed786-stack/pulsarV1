# Phase 4 Frontend Refactor + Performance Checklist

Status: Completed
Last updated: 2026-02-13

## Scope
- Frontend architecture isolation for chain adapters and services.
- Route/module-level lazy loading for heavy screens.
- Zustand selector isolation in high-churn components.
- WASM import-path unification for consistent code splitting.

## Implemented
- Adapter registry refactor:
  - `frontend/src/chains/adapters/index.ts` is now a thin registry.
  - EVM adapters are instantiated from `createEvmAdapter` in `frontend/src/chains/adapters/evmAdapter.ts`.
  - Non-EVM adapters (Solana/Bitcoin) are dynamically imported with cached lazy loaders.
- EVM adapter decomposition:
  - `frontend/src/chains/adapters/evmConfig.ts`
  - `frontend/src/chains/adapters/evmContracts.ts`
  - `frontend/src/chains/adapters/evmTokenStorage.ts`
  - `frontend/src/chains/adapters/evmTokenImportService.ts`
  - `frontend/src/chains/adapters/evmTokenPortfolioService.ts`
  - `frontend/src/chains/adapters/evmHistoryService.ts`
- Route-level lazy loading:
  - `frontend/src/app/Router.tsx` uses `React.lazy` + `Suspense` for heavy routes.
  - Idle-time route prefetch for primary views (`Home`, `Assets`, `Swap`, `Activity`).
- Store isolation and selector optimization:
  - Added explicit selector exports in:
    - `frontend/src/stores/sessionStore.ts`
    - `frontend/src/stores/multiChainStore.ts`
    - `frontend/src/stores/networkStore.ts`
  - Switched high-churn components to shallow selector subscriptions:
    - `frontend/src/app/AppShell.tsx`
    - `frontend/src/app/AppRouter.tsx`
    - `frontend/src/features/portfolio/Accounts.tsx`
    - `frontend/src/features/activity/Activity.tsx`
- WASM chunking hardening:
  - Removed static runtime import of `WasmWalletCore` in `frontend/src/services/WasmWalletFacade.ts`.
  - Facade now uses cached dynamic `loadWasmCore()`.
  - `frontend/src/services/SigningService.ts` uses type-only import from `WasmWalletCore`.

## Test Coverage Added
- New focused service tests:
  - `frontend/tests/evm.services.test.ts`
  - Covers:
    - token import validation edge cases (`evmTokenImportService`)
    - token portfolio path (`evmTokenPortfolioService`)
    - EVM history mapping (`evmHistoryService`)
- Existing unit tests remain green.

## Verification
- Typecheck:
  - `cd frontend && npx tsc -p tsconfig.app.json --noEmit`
- Unit tests:
  - `cd frontend && npm run test:unit`
  - Result: pass (11/11)
- Frontend regression sweep:
  - `cd frontend && npm run test:e2e`
  - Result: pass (4/4) after aligning test selectors and route assertions with current router contracts.
- Production build:
  - `cd frontend && npm run build`
  - Result: pass
  - Route chunks split as expected (`Home`, `Swap`, `Accounts`, `Activity`, etc).
  - `WasmWalletCore` emitted as dedicated chunk.

## Finalization Evidence (2026-02-13)
- Remaining high-churn Zustand stores moved to use-case boundaries:
  - `frontend/src/stores/contactStore.ts` -> `frontend/src/use-cases/contactUseCases.ts`
  - `frontend/src/stores/analyticsStore.ts` -> `frontend/src/use-cases/analyticsUseCases.ts`
  - Additional boundary use-cases already active for wallet/portfolio/protocol/minting/staking flows.
- Rerender profiling/reporting expanded:
  - `frontend/src/performance/renderBudget.ts` now records last/max render durations and emits periodic reports.
  - Heavy-page instrumentation active in:
    - `frontend/src/features/security/Settings.tsx`
    - `frontend/src/features/portfolio/AssetDetails.tsx`
    - `frontend/src/features/networks/ChainDetail.tsx`
- Route/code splitting hardened:
  - `frontend/src/app/Router.tsx` now lazy-loads `AppShell` and `Welcome` in addition to feature routes.
- Bundle analyzer artifacts generated:
  - `frontend/dist/bundle-report.html`
  - `frontend/dist/bundle-report.json`
- Initial chunk verification:
  - Current: `assets/index-BR8lu9OR.js` `197.31 kB` (gzip `63.10 kB`)
  - Prior baseline in migration notes: ~`550.75 kB`
- Regression validation:
  - `cd frontend && npm run test:unit` -> pass (`19/19`)
  - `cd frontend && npm run build` -> pass
  - `cd frontend && npm run build:analyze` -> pass

## Notes
- Remaining build warning is from third-party package annotation (`ox`), not from project import topology.
