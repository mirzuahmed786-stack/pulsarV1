# Key Management

## Generation
- Entropy from WebCrypto + Rust RNG.
- BIP39 mnemonic generation, BIP32/BIP44 derivation.

## Storage
- Encrypted vault stored client-side.
- Backend never stores or receives keys.

## Usage
- Keys are used only inside WASM boundary for signing.
- Signatures returned to UI without exposing private material.

## Export Policy
- Private key export is disabled by default.
- Export requires compile-time feature and runtime opt-in flag.

## Recovery
- Recovery relies on user mnemonic.
- Cloud recovery KEK mode policy:
  - Production default: `client_passphrase` (client-held derivation, server cannot derive KEK).
  - `server` mode is controlled/exception-only and must have explicit approval.
  - Frontend exception flag: `VITE_ALLOW_SERVER_KEK_EXCEPTION=1` (temporary, approved window only).
  - Backend guard: `ENABLE_SERVER_KEK=0` by default.
- No server-side key escrow.
