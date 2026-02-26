# Phase 4 Testing Execution Guide

**Status**: READY FOR TESTING ✓  
**Date**: February 25, 2026  
**Java Environment**: OpenJDK 21 (C:\Program Files\Android\Android Studio\jbr)

---

## Pre-Test Verification ✓

All Phase 4 components confirmed to exist:

```
✓ wallet_core.dll             → core-rust/target/release/wallet_core.dll
✓ build.gradle.kts            → kotlin-bindings/build.gradle.kts  
✓ VaultApi.kt (JSON)          → kotlin-bindings/src/main/kotlin/.../VaultApi.kt (878 lines)
✓ VaultApiTest.kt             → kotlin-bindings/src/test/kotlin/.../VaultApiTest.kt (636 lines)
✓ Java 21 (OpenJDK)           → C:\Program Files\Android\Android Studio\jbr
```

---

## To Run Phase 4 Tests

### Option 1: Using Gradle Wrapper (Recommended)

```bash
cd d:\last\Wallet-Rust\kotlin-bindings

# First time setup (skip if gradlew already exists)
gradle wrapper --gradle-version 8.5

# Then run tests
.\gradlew test --info
```

### Option 2: Using System Gradle (if installed)

```bash
cd d:\last\Wallet-Rust\kotlin-bindings
gradle test --info
```

### Option 3: Manual Download and Setup

```bash
# Download Gradle 8.5
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -OutFile gradle-8.5.zip
Expand-Archive gradle-8.5.zip
$env:Path = "$pwd\gradle-8.5\bin;$env:Path"
gradle test --info
```

---

## What the Tests Will Do

### Test Execution Flow

1. **Gradle Initialization** (first run): 
   - Downloads dependencies from Maven Central
   - Compiles Kotlin source code with Moshi
   - Compiles test code

2. **Library Loading**:
   - JVM sets `-Djava.library.path=d:\last\Wallet-Rust\core-rust\target\release`
   - wallet_core.dll is loaded via JNI
   - All 27 FFI functions become callable

3. **Test Execution** (27 tests):

| Category | Tests | Functions |
|----------|-------|-----------|
| Vault Management | 7 | generate_mnemonic, create_vault, create_vault_from_*, verify_pin, rotate_pin, migrate_vault |
| Transaction Signing | 6 | sign_transaction (legacy, EIP1559, with chain validation), sign_solana, sign_bitcoin |
| Key Derivation | 2 | derive_btc_address, derive_sol_address |
| Addresses | 2 | get_multichain_addresses, get_multichain_addresses_by_index |
| Recovery | 3 | create_recovery_backup, restore_vault_from_recovery_backup, verify_recovery_backup |
| Cloud Recovery | 2 | create_cloud_recovery_blob, restore_vault_from_cloud_recovery_blob |
| Web3Auth | 2 | create_wallet_from_web3auth_key, restore_wallet_from_web3auth_key |
| Key Export | 1 | export_eth_private_key |
| **TOTAL** | **27** | |

4. **Validation During Tests**:
   - Each test verifies JSON serialization works
   - VaultRecord → JSON → (FFI) → JSON → VaultRecord
   - Error handling tested (InvalidPin, SerializationError, etc.)
   - Signature formats validated
   - Address derivation tested

### Expected Test Results

**Success Output**:
```
BUILD SUCCESSFUL
27 tests completed, 27 passed
```

**Each Test Pattern**:
```
testCreateVault PASSED
testVerifyPin PASSED  
testSignTransaction PASSED
... (27 total)
```

---

## Test Details: What's Being Verified

### JSON Marshaling Tests
Each method validates:
- ✓ User passes typed Kotlin object (e.g., VaultRecord)
- ✓ Internal JSON serialization works (vaultToJson)
- ✓ JSON sent to Rust FFI correctly
- ✓ JSON response received from Rust
- ✓ JSON deserialized back to typed Kotlin object
- ✓ Object properties match expectations

### Example: testCreateVault
```kotlin
fun testCreateVault() {
    // Call high-level Kotlin API
    val vault = VaultApi.createVault("testpin")
    
    // This internally:
    // 1. Calls createVaultFfi("testpin") → returns JSON String
    // 2. Calls jsonToVault(jsonString) → deserializes to VaultRecord
    // 3. Returns typed VaultRecord to user
    
    assert(vault.version > 0)           // ✓ Structure correct
    assert(vault.public_address.startsWith("0x"))  // ✓ Address format correct
}
```

### Example: testSignTransaction
```kotlin
fun testSignTransaction() {
    val vault = VaultApi.createVault("testpin")
    val tx = UnsignedLegacyTx(...)
    
    // Call high-level Kotlin API with two typed objects
    val signature = VaultApi.signTransaction("testpin", vault, tx)
    
    // This internally:
    // 1. Serializes: vaultToJson(vault) → JSON String
    // 2. Serializes: legacyTxToJson(tx) → JSON String
    // 3. Calls FFI: signTransactionFfi(pin, vaultJson, txJson)
    // 4. Receives: signature String (already JSON primitive)
    // 5. Returns: String to user
    
    assert(signature.startsWith("0x"))   // ✓ Signature format
    assert(signature.length > 100)       // ✓ Signature size reasonable
}
```

### Example: testGetMultichainAddresses
```kotlin
fun testGetMultichainAddresses() {
    val vault = VaultApi.createVault("testpin")
    
    // Call high-level API
    val addresses = VaultApi.getMultichainAddresses("testpin", vault, false)
    
    // This internally:
    // 1. Serializes: vaultToJson(vault)
    // 2. Calls FFI: getMultichainAddressesFfi(pin, vaultJson)
    // 3. Receives: addresses JSON String from Rust
    // 4. Deserializes: jsonToAddresses(json) → MultichainAddresses
    // 5. Returns: typed MultichainAddresses object
    
    assert(addresses.eth.startsWith("0x"))   // ✓ ETH address format
    assert(addresses.btc.length > 20)        // ✓ BTC address exists
    assert(addresses.sol.length > 20)        // ✓ SOL address exists
}
```

---

## Configuration Summary

### build.gradle.kts Changes
```gradle
dependencies {
    // Added for Phase 4:
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
}

tasks.test {
    // Added for Phase 4:
    val nativeLibPath = "${projectDir.parent}/core-rust/target/release"
    jvmArgs = listOf("-Djava.library.path=$nativeLibPath")
}
```

### VaultApi.kt Changes (878 lines → from 745)
- Added: Moshi imports
- Added: Private moshi instance
- Added: 10 JSON serialization helpers
- Modified: All 27 methods to use JSON marshaling

### Example: Before and After

**Before (Phase 2)**:
```kotlin
fun signTransaction(pin: String, record: VaultRecord, tx: UnsignedLegacyTx): String {
    return signTransactionFfi(pin, record, tx)  // ✗ Can't pass VaultRecord directly
}
```

**After (Phase 4)**:
```kotlin
fun signTransaction(pin: String, record: VaultRecord, tx: UnsignedLegacyTx): String {
    val vaultJson = vaultToJson(record)        // ✓ Serialize to JSON
    val txJson = legacyTxToJson(tx)            // ✓ Serialize to JSON
    return signTransactionFfi(pin, vaultJson, txJson)  // ✓ All String parameters
}
```

---

## Troubleshooting

### Issue: "gradle: command not found"
**Solution**: Download Gradle 8.5 or use gradlew wrapper
```bash
gradle wrapper --gradle-version 8.5
```

### Issue: "Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain"
**Solution**: Download gradle directory properly or install system gradle

### Issue: "UnsatisfiedLinkError: wallet_core"
**Solution**: Verify wallet_core.dll location and that java.library.path is set:
```bash
# Check DLL exists
Test-Path "D:\last\Wallet-Rust\core-rust\target\release\wallet_core.dll"
```

### Issue: "COMPILATION ERROR in VaultApi.kt"
**Solution**: Ensure Moshi dependency is installed (gradle should do this)

### Issue: "Cannot deserialize JSON to VaultRecord"
**Solution**: Verify all Kotlin data classes match Rust type definitions (they do - Phase 2)

---

## Test Success Indicators

When tests pass, you'll see:

```
> Task :test
20XX-XX-XX XX:XX:XX,XXX [main] INFO org.gradle.test.suite.TestSuite

BUILD SUCCESSFUL in Xs

> Task :test
testGenerateMnemonic PASSED
testCreateVault PASSED
testCreateVaultFromMnemonic PASSED
testCreateVaultFromPrivateKey PASSED
testVerifyPin PASSED
testRotatePin PASSED
testMigrateVault PASSED
testSignTransaction PASSED
testSignTransactionEip1559 PASSED
testSignTransactionWithChain PASSED
testSignTransactionEip1559WithChain PASSED
testSignSolanaTransaction PASSED
testSignBitcoinTransaction PASSED
testDeriveBtcAddress PASSED
testDeriveSolAddress PASSED
testGetMultichainAddresses PASSED
testGetMultichainAddressesByIndex PASSED
testCreateRecoveryBackup PASSED
testRestoreVaultFromRecoveryBackup PASSED
testVerifyRecoveryBackup PASSED
testCreateCloudRecoveryBlob PASSED
testRestoreVaultFromCloudRecoveryBlob PASSED
testVerifyCloudRecoveryBlob PASSED
testCreateWalletFromWeb3authKey PASSED
testRestoreWalletFromWeb3authKey PASSED
testExportEthPrivateKey PASSED

27 successful tests
```

---

## Next Step: Phase 5

Once all 27 tests pass:
- ✓ Kotlin-Rust FFI integration VALIDATED
- ✓ JSON marshaling WORKING
- ✓ All 27 functions OPERATIONAL
- ✓ Error handling CORRECT

Then proceed to Phase 5: Advanced Integration & Android

---

## Commands Reference

```bash
# Setup
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd d:\last\Wallet-Rust\kotlin-bindings

# Create gradle wrapper (one-time)
gradle wrapper --gradle-version 8.5

# Run tests
.\gradlew test --info

# Run specific test
.\gradlew test --tests "*VaultApiTest.testCreateVault*"

# Run with stacktrace for failures
.\gradlew test --stacktrace

# Clean rebuild
.\gradlew clean test
```

---

## Phase 4 Status

| Component | Status | Verified |
|-----------|--------|----------|
| wallet_core.dll | ✓ Built | Yes |
| build.gradle.kts | ✓ Updated | Yes |
| VaultApi.kt | ✓ Modified | Yes |
| VaultApiTest.kt | ✓ Ready | Yes |
| Moshi Dependency | ✓ Added | Yes |
| JSON Helpers | ✓ Implemented | Yes |
| Settings | ✓ Configured | Yes |
| Java/JAVA_HOME | ✓ Available | Yes |
| **OVERALL** | **✓ READY** | **YES** |

**Ready to execute tests and validate Phase 4 integration.**
