# Elementa UI System

## Design Intent
Elementa embraces cryptographic clarity: precise geometry, layered surfaces, and energy flows that imply trustless computation. The UI feels sleek and powerful without being aggressive.

## Design System
### Color Tokens
Tokens are CSS variables in `Wallet-Rust/frontend/src/styles/index.css` and mapped in `Wallet-Rust/frontend/tailwind.config.ts`.

- Background: `--ea-bg`, `--ea-surface`, `--ea-panel`, `--ea-elevated`
- Primary energy: `--ea-primary`, `--ea-primary-strong`, `--ea-primary-glow`
- Accent spectrum: `--ea-accent`, `--ea-accent-strong`, `--ea-accent-glow`
- Text hierarchy: `--ea-text`, `--ea-text-secondary`, `--ea-text-muted`, `--ea-text-dim`
- Status: `--ea-success`, `--ea-warning`, `--ea-danger`

### Typography
- Display + UI: `Space Grotesk`
- Mono: `IBM Plex Mono`
- Use `.font-display` for headlines and `.font-mono` for addresses and hashes.

### Spacing + Radius
- `--ea-radius-*` for consistent curvature.
- Use tighter spacing in wallet flows; increase padding for dashboards and cards.

### Elevation + Glow
- `glass-panel` + `shadow-card` for layered surfaces.
- Use `bg-elementa-panel` for energy-panel surfaces.

## Tailwind Theme
- Config at `Wallet-Rust/frontend/tailwind.config.ts`
- Core tokens are mapped to Tailwind colors.

## Component Architecture
Folder layout:
- `Wallet-Rust/frontend/src/ui/components/` for base UI building blocks.
- `Wallet-Rust/frontend/src/ui/layout/` for layout + background primitives.
- `Wallet-Rust/frontend/src/ui/motion/` for motion presets.

Suggested layering:
1. `components` (Button, Card, Modal, PinInput)
2. `composites` (WalletHeader, AssetList, NetworkSelector)
3. `screens` (Dashboard, Send, Receive, Settings)

## Motion Guidelines
Use Framer Motion with minimal movement:
- Page entry: `fadeUp` (low distance, short duration)
- Stagger list items with `stagger`
- Use `easeOut` for key transitions

Guidelines:
- Use opacity + y-translation for most entrances.
- Avoid scaling large surfaces; use subtle light/shadow shifts instead.

## Layout Descriptions (Key Screens)
### Wallet Dashboard
- Top: Balance hero card with accent glow.
- Middle: Action row (Send, Receive, Swap) using `Button` variants.
- Bottom: Asset list in a stacked `Card` layout.

### Send / Receive
- Centered card with stepper on top.
- Address and amount inputs in compact panels.
- Confirm CTA anchored at bottom.

### Settings
- Two-column layout on desktop.
- Left: grouped preference cards.
- Right: security + backup cards.

### Empty States
- Centered icon badge + concise copy + primary CTA.

## Migration Strategy
1. Replace global CSS import with `index.css` (done).
2. Migrate layout wrappers to Tailwind: start with `AppShell` and onboarding screens (done).
3. Convert legacy utilities (`glass-panel`, `input-premium`) to Tailwind classes per screen.
4. Replace inline styles with design tokens (ongoing for remaining screens).
5. Introduce reusable composites for Assets, Transactions, and Network Switcher.

## Notes
- Light mode is supported via `data-theme='light'` tokens.
- New components are in `src/ui/components/`.
