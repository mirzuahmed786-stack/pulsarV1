# Phase 1 Build Verification Guide

## Quick Validation Checklist

Before proceeding to Phase 2, verify that Phase 1 was implemented correctly:

### 1. File Existence Check ✅
```bash
# Core-rust changes
ls -la core-rust/Cargo.toml         # Updated ✓
ls -la core-rust/build.rs           # New file ✓
ls -la core-rust/src/types.rs       # Updated ✓
ls -la core-rust/src/lib.rs         # Updated ✓

# Root UDL file
ls -la wallet.udl                   # New file ✓

# Report files
ls -la PHASE_1_COMPLETION_REPORT.md  # New file ✓
```

### 2. Code Content Verification

#### Check Cargo.toml Updated
```bash
cd core-rust
grep -n "uniffi" Cargo.toml         # Should show: uniffi version "0.28"
grep -n "thiserror" Cargo.toml      # Should show: thiserror version "2"
grep -n "build-dependencies" Cargo.toml  # Should exist
```

#### Check build.rs Created
```bash
cd core-rust
head -5 build.rs                    # Should show: "hassam dev" comment
grep "generate_scaffolding" build.rs # Should exist
```

#### Check VaultError in types.rs
```bash
cd core-rust/src
grep -n "uniffi::Error" types.rs    # Should show VaultError derive
grep -n "InvalidMnemonic" types.rs  # Should exist
grep -n "InvalidPin" types.rs       # Should exist
```

#### Check lib.rs Markers
```bash
cd core-rust/src
grep -c "#\[uniffi::export\]" lib.rs    # Should show: ~27 (one per function)
grep "uniffi::setup_scaffolding" lib.rs # Should exist
grep "VaultError" lib.rs                 # Should be exported
```

#### Check wallet.udl File
```bash
wc -l wallet.udl                    # Should be ~450 lines
grep "enum VaultError" wallet.udl   # Should exist
grep "\[Throws=VaultError\]" wallet.udl  # Should show ~27 functions
grep "namespace wallet" wallet.udl  # Should exist
```

### 3. Syntax Validation

#### Check for "hassam dev" Comments (Audit Trail)
```bash
# Count all hassam dev comments
grep -r "hassam dev" core-rust/ wallet.udl | wc -l
# Should show 40+ occurrences
```

#### Verify No Files Were Removed
```bash
# All original files should still exist
ls core-rust/src/crypto.rs      # ✓
ls core-rust/src/keys.rs        # ✓
ls core-rust/src/evm.rs         # ✓
ls core-rust/src/solana.rs      # ✓
ls wasm/src/lib.rs              # ✓ WASM untouched
```

### 4. Rust Syntax Check (No Compilation Required Yet)

```bash
# Check for basic Rust syntax with rustfmt
cd core-rust
rustfmt --check src/lib.rs      # Should produce no errors
rustfmt --check src/types.rs    # Should produce no errors

# Check for obvious lint issues
cd core-rust
cargo clippy --message-format=short 2>&1 | head -20
# May have warnings, but no hard errors expected
```

### 5. Cargo.toml Validation
```bash
cd core-rust
cargo metadata --format-version 1 > /dev/null 2>&1 && echo "✓ Cargo.toml is valid"
```

### 6. UDL Syntax Validation

```bash
# Check if uniffi-bindgen can parse the UDL (requires installation)
uniffi-bindgen validate wallet.udl
# Should output: "✓ wallet.udl is valid"

# Or check manually for common issues:
grep "dictionary\|enum\|interface" wallet.udl | head -20
```

### 7. Function Count Validation

```bash
# Count FFI functions in lib.rs
grep -c "#\[uniffi::export\]" core-rust/src/lib.rs
# Expected: 27

# Count UDL function declarations
grep -c "^\s*\[Throws=VaultError\]" wallet.udl
# Expected: 27 (or 26 if export excluded from one)
```

### 8. Type Compatibility Check

```bash
# Check VaultRecord in types.rs
grep -A 10 "pub struct VaultRecord" core-rust/src/types.rs | grep -E "serialize|derive"
# Should show: #[derive(Debug, Clone, Serialize, Deserialize)]

# Check MultichainAddresses in lib.rs
grep -A 5 "pub struct MultichainAddresses" core-rust/src/lib.rs
# Should show: #[derive(serde::Serialize)]

# Check Web3AuthWalletResult in lib.rs
grep -A 10 "pub struct Web3AuthWalletResult" core-rust/src/lib.rs
# Should exist and be Serialize
```

### 9. Implementation Completeness

```bash
# Verify all 27 functions have FFI wrappers:
# 7 Vault Management
grep "create_vault_ffi\|verify_pin_ffi\|rotate_pin_ffi\|migrate_vault_ffi" core-rust/src/lib.rs | wc -l
# Expected: 4 (at minimum)

# 6 Transaction Signing  
grep "sign_transaction_ffi\|sign_solana_ffi\|sign_bitcoin_ffi" core-rust/src/lib.rs | wc -l
# Expected: 3+ (multiple variants)

# 5 Key Derivation
grep "derive_btc\|derive_sol\|get_btc_public_key\|get_multichain_addresses" core-rust/src/lib.rs | grep "ffi" | wc -l
# Expected: 4+

# 6 Recovery & Backup
grep "recovery_backup\|cloud_recovery" core-rust/src/lib.rs | grep "ffi" | wc -l
# Expected: 6

# 2+ Web3Auth
grep "web3auth" core-rust/src/lib.rs | grep "ffi" | wc -l
# Expected: 2+
```

### 10. No Breaking Changes

```bash
# Check that key internal functions still have &str signatures
grep "pub fn create_vault(" core-rust/src/lib.rs
# Should show: pub fn create_vault(pin: &str)
# NOT: pub fn create_vault(pin: String)  <- Would be breaking change

# Check wrapper exists
grep "pub fn create_vault_ffi(" core-rust/src/lib.rs
# Should show: pub fn create_vault_ffi(pin: String)
```

---

## What Should Pass ✅

After implementing Phase 1, the following should be TRUE:

| Check | Expected | Verify Command |
|-------|----------|-----------------|
| Files Created | 3 new files | `ls core-rust/build.rs wallet.udl PHASE_1_COMPLETION_REPORT.md` |
| Files Modified | 3 modified | `ls core-rust/Cargo.toml core-rust/src/lib.rs core-rust/src/types.rs` |
| Total Lines Added | ~673 | `wc -l` on all changed/new files |
| FFI Exports | 27 functions | `grep -c "#\[uniffi::export\]" core-rust/src/lib.rs` |
| Error Types | 8 variants | `grep "^    " core-rust/src/types.rs \| grep -c "^    [A-Z]"` |
| Comments | 40+ "hassam dev" | `grep -r "hassam dev" . \| wc -l` |
| Breaking Changes | ZERO | `grep "pub fn create_vault(pin: String)" core-rust/src/lib.rs` |
| Compilation | Ready | Should have no hard errors |

---

## What Should NOT Happen ❌

- **No files deleted** - All original code should remain
- **No function logic changed** - Only wrappers added
- **No WASM breakage** - The wasm/ layer should remain untouched
- **No circular dependencies** - Rust compilation should work
- **No missing imports** - uniffi and thiserror must be available

---

## Build Test (Final Validation)

```bash
cd core-rust

# Try a dry build (doesn't produce executable, just checks syntax)
cargo check 2>&1 | head -20

# Look for errors in output
# Expected: Should complete without "error[E" lines
# Warnings are OK, errors are NOT OK
```

---

## Sign-Off Template

```
Phase 1 Validation Checklist 

Files Verified:
  [✓] core-rust/Cargo.toml - Updated with uniffi dependencies
  [✓] core-rust/build.rs - Created with scaffolding logic
  [✓] wallet.udl - Created with 27 function definitions
  [✓] core-rust/src/types.rs - VaultError enum added
  [✓] core-rust/src/lib.rs - FFI wrappers added (27 functions)
  
Code Quality:
  [✓] All changes marked with "hassam dev" comments
  [✓] No existing code removed
  [✓] All ~673 new lines accounted for
  [✓] Zero breaking changes to internal API
  [✓] WASM layer remains untouched

Syntax Check:
  [✓] Rust syntax valid (rustfmt passes)
  [✓] UDL syntax valid (if uniffi-bindgen available)
  [✓] Cargo.toml structure valid
  [✓] No missing imports or dependencies

Status: ✅ PHASE 1 COMPLETE AND VALIDATED

Next: Proceed to Phase 2 (Kotlin Module Setup)
```

---

## Troubleshooting

### Issue: "uniffi" dependency not found
**Solution**: Update Rust: `rustup update && rustup update stable`

### Issue: build.rs not found
**Solution**: Verify file exists: `ls -la core-rust/build.rs`

### Issue: VaultError not found in types.rs
**Solution**: Check if copy/paste included all variants

### Issue: FFI functions not showing up
**Solution**: Verify `#[uniffi::export]` attribute is directly above each function

### Issue: wallet.udl parse error
**Solution**: Check for:
- Missing semicolons on dictionary entries
- Correct bracket matching
- Valid function signatures matching Rust

---

**Validation Guide Generated**: February 25, 2026  
**For**: Phase 1 UFI Scaffolding Completion  
**Status**: Ready for Automated/Manual Verification
