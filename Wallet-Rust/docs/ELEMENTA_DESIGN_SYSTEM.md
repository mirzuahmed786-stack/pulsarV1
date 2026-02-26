# Elementa Design System

## Scope
Phase 5 brand and design-system modernization baseline for frontend UI.

## Tokens and Motion Contract
- Tokenized palette, typography, spacing, and radii are centralized in:
  - `frontend/src/styles/index.css`
  - `frontend/tailwind.config.ts`
- Motion/easing contract is centralized in:
  - `frontend/src/design-system/motionRules.ts`
- Reduced-motion behavior is enforced globally and in component-level motion surfaces.

## Primitives
Standardized primitives live under:
- `frontend/src/design-system/primitives/Button.tsx`
- `frontend/src/design-system/primitives/Input.tsx`
- `frontend/src/design-system/primitives/Card.tsx`
- `frontend/src/design-system/primitives/Modal.tsx`
- `frontend/src/design-system/primitives/Badge.tsx`
- `frontend/src/design-system/primitives/Tabs.tsx`

Compatibility facades in `frontend/src/ui/components/*` remain transitional only and should not receive new call sites.

## Patterns
Standardized composition patterns live under:
- `frontend/src/design-system/patterns/FormField.tsx`
- `frontend/src/design-system/patterns/DialogHeader.tsx`
- `frontend/src/design-system/patterns/PanelSection.tsx`

Pattern adoption is required for repeated section headers and panel blocks on high-traffic screens.

## Visual Regression Baseline
- Spec source: `frontend/tests/assets/visual-specs.md`
- Automated snapshots: `frontend/tests/visual.spec.ts`
- Screenshot baseline refresh command:
  - `cd frontend && npm run test:e2e:visual -- --update-snapshots`
- Visual diff threshold:
  - `maxDiffPixelRatio: 0.02`

## Brand Approval Pack
Minimum approved screen set:
- Onboarding (`onboarding-welcome.png`)
- Portfolio (`portfolio-home.png`, `portfolio-assets.png`)
- Swap (`swap-desk.png`)
- Send (`send-flow.png`)
- Settings (`settings-security.png`)

Approval process:
1. Generate/refresh screenshots via visual test command.
2. Review changes against prior snapshots.
3. Record approval notes in `docs/CHANGELOG_PHASE5.md`.
