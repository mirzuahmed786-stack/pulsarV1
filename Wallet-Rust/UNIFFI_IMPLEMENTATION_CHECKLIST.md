# UniFFI Implementation Checklist & Quick Start Guide

## Status: AUDIT COMPLETE ✅
**Date**: February 25, 2026  
**Core Finding**: Project is architecturally ready for UniFFI migration

---

## Phase 1: Scaffolding (Start Here)

### 1.1 Create UDL Definition File
```bash
# Location: uniffi/wallet.udl
# Size: ~200 lines
# Time: 2-3 hours
```

**File Structure**:
```idl
namespace wallet {
    // Errors
    [Error]
    enum VaultError {
        "InvalidMnemonic",
        "InvalidPin",
        "CryptoError"
    };
    
    // Data types
    dictionary VaultRecord { ... };
    dictionary KdfParams { ... };
    dictionary CipherBlob { ... };
    
    // 27 functions
    [Throws=VaultError]
    VaultRecord create_vault(string pin);
    // ... etc
};
```

**Checklist**:
- [ ] Copy type definitions from `core-rust/src/types.rs`
- [ ] Define all 27 functions with proper signatures
- [ ] Add `[Throws=VaultError]` to functions that return `Result<T, String>`
- [ ] Validate syntax with `uniffi-bindgen`

---

### 1.2 Update Cargo.toml

**File**: `core-rust/Cargo.toml`

```toml
[dependencies]
uniffi = { version = "0.28", features = ["build"] }
# ... existing deps ...

[lib]
crate-type = ["rlib", "cdylib"]  # ← ADD cdylib

[build-dependencies]
uniffi = { version = "0.28", features = ["build"] }
```

**Checklist**:
- [ ] Add `uniffi` to `[dependencies]`
- [ ] Change `crate-type` to include `"cdylib"`
- [ ] Add to `[build-dependencies]`
- [ ] Run `cargo check` to validate

---

### 1.3 Create build.rs

**File**: `core-rust/build.rs` (NEW)

```rust
use uniffi::cargo_uniffi_root;

fn main() {
    uniffi::cargo_uniffi_root().generate_scaffolding().unwrap();
}
```

**Checklist**:
- [ ] Create new file at project root
- [ ] Copy code above exactly
- [ ] Run `cargo build` to generate scaffolding

---

### 1.4 Mark Functions with #[uniffi::export]

**File**: `core-rust/src/lib.rs`

```rust
// ADD at top
uniffi::setup_scaffolding!();

// CHANGE all public functions:
#[uniffi::export]
pub fn generate_mnemonic() -> Result<String, VaultError> {
    // existing code
}

#[uniffi::export]
pub fn create_vault(pin: String) -> Result<VaultRecord, VaultError> {
    // existing code
}
// ... repeat for all 27 functions
```

**Checklist**:
- [ ] Add `uniffi::setup_scaffolding!();` at top of `lib.rs`
- [ ] Mark all public functions with `#[uniffi::export]`
- [ ] Update error signatures: `String` → `VaultError`
- [ ] Run `cargo build` to validate

---

### 1.5 Create VaultError Enum

**File**: `core-rust/src/lib.rs` (add before functions)

```rust
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum VaultError {
    #[error("Invalid mnemonic: {0}")]
    InvalidMnemonic(String),
    
    #[error("PIN verification failed")]
    InvalidPin,
    
    #[error("Invalid derivation path")]
    InvalidDerivationPath,
    
    #[error("Cryptographic operation failed: {0}")]
    CryptoError(String),
    
    #[error("Invalid key material")]
    InvalidKeyMaterial,
    
    #[error("Serialization error: {0}")]
    SerializationError(String),
    
    #[error("Recovery backup mismatch")]
    RecoveryBackupMismatch,
}
```

**Checklist**:
- [ ] Add to `Cargo.toml`: `thiserror = "2"`
- [ ] Define `VaultError` enum with `#[uniffi::Error]`
- [ ] Replace all `Err("message")` with `Err(VaultError::Variant)`
- [ ] Run `cargo build` and fix compile errors

---

### 1.6 Regression Testing: WASM Still Works

**File**: `wasm/Cargo.toml` (verify)

```toml
[dependencies]
wallet_core = { path = "../core-rust", default-features = false, features = ["wasm"] }
```

**Checklist**:
- [ ] Run `wasm-pack build wasm --target bundler`
- [ ] Verify TypeScript frontend still loads wallet
- [ ] Run `npm test` in frontend
- [ ] Confirm no WASM breaking changes

---

## Phase 2: Kotlin Module Setup

### 2.1 Create Kotlin Project Structure

```bash
mkdir -p kotlin-bindings
cd kotlin-bindings
mkdir -p src/main/kotlin/com/wallet_rust
mkdir -p src/test/kotlin/com/wallet_rust
touch build.gradle.kts
touch settings.gradle.kts
```

**Checklist**:
- [ ] Create directory at `./kotlin-bindings/`
- [ ] Set up Gradle project structure
- [ ] Create `build.gradle.kts`

---

### 2.2 Create build.gradle.kts

**File**: `kotlin-bindings/build.gradle.kts` (NEW)

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

tasks.register<Exec>("generateUniFFI") {
    description = "Generate Kotlin bindings from wallet.udl"
    executable = "uniffi-bindgen"
    args = listOf(
        "generate",
        "../uniffi/wallet.udl",
        "--language", "kotlin",
        "--out-dir", "src/main/kotlin/com/wallet_rust"
    )
}

tasks.getByName<JavaCompile>("compileKotlin") {
    dependsOn("generateUniFFI")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.22")
}

tasks.test {
    useJUnitPlatform()
}
```

**Checklist**:
- [ ] Copy code above
- [ ] Update paths if needed
- [ ] Run `gradle build` to test generation

---

### 2.3 Generate Kotlin Bindings

```bash
cd kotlin-bindings
gradle generateUniFFI
```

**Checklist**:
- [ ] Ensure `uniffi-bindgen` is installed: `cargo install uniffi_bindgen`
- [ ] Run generation command
- [ ] Verify files created in `src/main/kotlin/com/wallet_rust/`
- [ ] Check for `.kt` files with `Vault`, `VaultRecord`, `VaultError` classes

**Expected Output**:
```
src/main/kotlin/com/wallet_rust/
  ├── WalletLib.kt          (auto-generated)
  ├── VaultRecord.kt        (auto-generated)
  ├── VaultError.kt         (auto-generated)
  ├── KdfParams.kt          (auto-generated)
  └── ... (more types)
```

---

### 2.4 Create Kotlin Wrapper (Optional but Recommended)

**File**: `kotlin-bindings/src/main/kotlin/com/wallet_rust/VaultApi.kt` (NEW)

```kotlin
package com.wallet_rust

/**
 * Main API for wallet operations.
 * Wraps auto-generated bindings with convenience methods.
 */
object VaultApi {
    
    @JvmStatic
    fun generateMnemonic(): String = generateMnemonic()
    
    @JvmStatic
    fun createVault(pin: String): VaultRecord = createVault(pin)
    
    @JvmStatic
    fun createVaultFromMnemonic(
        pin: String,
        mnemonic: String,
        path: String = "m/44'/60'/0'/0/0"
    ): VaultRecord = createVaultFromMnemonic(pin, mnemonic, path)
    
    @JvmStatic
    fun signTransaction(
        pin: String,
        record: VaultRecord,
        tx: UnsignedLegacyTx
    ): String = signTransaction(pin, record, tx)
    
    // ... wrap all 27 functions
}
```

**Checklist**:
- [ ] Create wrapper for common operations
- [ ] Add convenience overloads (e.g., default derivation path)
- [ ] Add KDoc comments for all methods
- [ ] Provide clear error messages

---

## Phase 3: Testing

### 3.1 Write Unit Tests

**File**: `kotlin-bindings/src/test/kotlin/com/wallet_rust/VaultApiTest.kt` (NEW)

```kotlin
package com.wallet_rust

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VaultApiTest {
    
    @Test
    fun testGenerateMnemonic() {
        val mnemonic = VaultApi.generateMnemonic()
        assertEquals(12, mnemonic.split(" ").size)  // or 24
        assert(mnemonic.isNotBlank())
    }
    
    @Test
    fun testCreateVault() {
        val pin = "test-pin-12345"
        val vault = VaultApi.createVault(pin)
        assertNotNull(vault.publicAddress)
        assert(vault.publicAddress.startsWith("0x"))
    }
    
    @Test
    fun testVerifyPin() {
        val pin = "correct-pin"
        val vault = VaultApi.createVault(pin)
        
        val address = VaultApi.verifyPin(pin, vault)
        assertEquals(vault.publicAddress, address)
    }
    
    @Test
    fun testInvalidPin() {
        val vault = VaultApi.createVault("correct-pin")
        assertThrows<VaultException> {
            VaultApi.verifyPin("wrong-pin", vault)
        }
    }
    
    // ... more tests for all functions
}
```

**Checklist**:
- [ ] Write tests for all 27 functions
- [ ] Test error cases
- [ ] Test edge cases (empty strings, invalid addresses)
- [ ] Run `gradle test` and verify all pass

---

### 3.2 Performance Benchmarking

**File**: `kotlin-bindings/src/test/kotlin/com/wallet_rust/PerfTest.kt` (NEW)

```kotlin
package com.wallet_rust

import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class PerfTest {
    
    @Test
    fun benchmarkVaultCreation() {
        val pin = "test-pin"
        val time = measureTimeMillis {
            VaultApi.createVault(pin)
        }
        println("Vault creation: ${time}ms")
        assert(time < 100)  // Should be < 100ms
    }
    
    @Test
    fun benchmarkSignTransaction() {
        val pin = "test-pin"
        val vault = VaultApi.createVault(pin)
        val tx = UnsignedLegacyTx(
            nonce = 1L,
            gasPrice = "20000000000",  // 20 Gwei
            gasLimit = 21000L,
            to = "0xRecipientAddress",
            value = "1000000000000000000",  // 1 ETH
            data = "0x",
            chainId = 1L
        )
        
        val time = measureTimeMillis {
            VaultApi.signTransaction(pin, vault, tx)
        }
        println("TX signing: ${time}ms")
        assert(time < 150)  // Should be < 150ms
    }
}
```

**Checklist**:
- [ ] Benchmark core operations
- [ ] Document performance targets (40-45ms for vault ops)
- [ ] Compare against WASM if possible
- [ ] Flag any performance regressions

---

## Phase 4: Android Integration (Optional)

### 4.1 Create Android Module

```bash
mkdir -p android/app
cd android/app
touch build.gradle.kts
mkdir -p src/main/jniLibs/arm64-v8a
```

**Checklist**:
- [ ] Create Android project structure
- [ ] Configure Android SDK & NDK paths

---

### 4.2 Create Native Library

**File**: `android/app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = 34
    ndkVersion = "25.2.9519653"
    
    defaultConfig {
        minSdk = 26
        targetSdk = 34
    }
    
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(project(":kotlin-bindings"))
}
```

**Checklist**:
- [ ] Configure NDK version
- [ ] Set min SDK to 26+ (for full compatibility)
- [ ] Link Kotlin bindings module

---

## Validation Checklist (End-to-End)

- [ ] **Phase 1**: WASM tests still pass
- [ ] **Phase 1**: `cargo build` succeeds with no errors
- [ ] **Phase 2**: `uniffi-bindgen` CLI installed
- [ ] **Phase 2**: Kotlin bindings generated successfully
- [ ] **Phase 2**: Gradle `build` succeeds
- [ ] **Phase 3**: All unit tests pass (100%)
- [ ] **Phase 3**: Performance benchmarks < 100ms
- [ ] **Phase 3**: No memory leaks detected
- [ ] **Phase 4** (optional): Android app compiles and runs
- [ ] **Docs**: Full KDoc coverage (100%)

---

## Quick Build Commands

```bash
# Phase 1: Rust scaffolding
cd core-rust && cargo build --release

# Phase 2: Kotlin generation & testing
cd kotlin-bindings && gradle build

# Test
cd kotlin-bindings && gradle test

# Performance check
cd kotlin-bindings && gradle benchmarkTest

# Android (optional)
cd android/app && gradle assembleRelease
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `uniffi-bindgen not found` | `cargo install uniffi_bindgen` |
| `Compiled with unsupported ABI` | Check NDK version matches `build.gradle.kts` |
| `Kotlin tests fail` | Run `gradle generateUniFFI` before `test` |
| `.so not found` | Build Android module: `gradle build` from `android/app` |
| Linker errors | Verify `core-rust/Cargo.toml` has `crate-type = ["cdylib"]` |

---

## References

- UniFFI Docs: https://mozilla.github.io/uniffi-rs/
- Kotlin FFI: https://kotlinlang.org/docs/jni.html
- Android NDK: https://developer.android.com/ndk/guides
- Gradle: https://gradle.org/

---

## Sign-Off

**Audit Status**: ✅ COMPLETE  
**Go/No-Go**: ✅ **GO** - Proceed with implementation  
**Estimated Effort**: 2-3 weeks for MVP  
**Support**: Full audit report available in [UNIFFI_AUDIT_REPORT.md](./UNIFFI_AUDIT_REPORT.md)

Next step: Begin Phase 1 (create wallet.udl)
