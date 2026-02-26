# Phase 4: Kotlin-Rust Integration & Testing

**Objective**: Integrate the Rust native library with Kotlin bindings and validate full end-to-end functionality.

**Current State**:
- ✅ Phase 1: 27 FFI functions exported (Rust scaffolding)
- ✅ Phase 2: Kotlin module with convenience wrappers (1,877 lines)
- ✅ Phase 3: Native library built (wallet_core.dll)
- 🔄 **Phase 4 (THIS): Kotlin-Rust integration & JNI testing**

---

## Phase 4 Steps (Detailed Breakdown)

### **Step 1: Update Native Library Path in Gradle**
**Goal**: Configure gradle to use the built wallet_core.dll library

**Files to Modify**:
- `kotlin-bindings/build.gradle.kts`

**What Will Happen**:
- Add JNI library path mapping to point to `core-rust/target/release/wallet_core.dll`
- Configure native library directory in JVM options
- Setup system property for DLL loading on Windows

**Outcome**: Gradle knows where to find the native library when running tests

---

### **Step 2: Update VaultApi.kt JSON Serialization**
**Goal**: Make convenience methods handle JSON string serialization/deserialization

**Files to Modify**:
- `kotlin-bindings/src/main/kotlin/com/wallet_rust/VaultApi.kt`

**What Will Happen**:
- Add moshi JSON serializer dependency
- Update all methods that take VaultRecord parameters to serialize to JSON first
- Update all methods that return VaultRecord to deserialize from JSON
- Create helper functions: `vaultToJson()` and `jsonToVault()`
- Same for MultichainAddresses, RecoveryBackup, CloudRecoveryBlob, Web3AuthWalletResult

**Pattern**:
```kotlin
// Before (direct struct passing - won't work with JSON FFI)
external fun verify_pin_ffi(pin: String, record: VaultRecord): Result<String>

// After (JSON marshaling)
external fun verify_pin_ffi(pin: String, record: String): Result<String>

fun verifyPin(pin: String, vault: VaultRecord): String {
    val vaultJson = moshi.adapter(VaultRecord::class.java).toJson(vault)
    return verify_pin_ffi(pin, vaultJson).getOrThrow()
}
```

**Outcome**: VaultApi methods work with JSON strings under the hood, user gets typed objects

---

### **Step 3: Add Moshi Dependency**
**Goal**: Add JSON serialization library to Gradle

**Files to Modify**:
- `kotlin-bindings/build.gradle.kts`

**What Will Happen**:
- Add moshi dependency (latest version)
- Configure code generation for Kotlin data classes
- Ensure android/java compatibility

**Outcome**: Kotlin can serialize/deserialize Vault objects to/from JSON

---

### **Step 4: Run Unit Tests Against Real Library**
**Goal**: Execute VaultApiTest.kt against the actual native library

**What Will Happen**:
- Run: `gradle test` in kotlin-bindings/
- Tests will load wallet_core.dll from classpath
- All 27 test cases execute against real Rust code
- Verify success/error scenarios work correctly

**Expected Results**:
- ✅ Mnemonic generation tests pass
- ✅ Vault creation tests pass
- ✅ PIN verification tests pass
- ✅ Transaction signing tests pass
- ✅ Recovery backup tests pass
- ✅ All error cases handled correctly

**Outcome**: Validated that Kotlin methods work with real Rust implementation

---

### **Step 5: Integration Test - Full Vault Lifecycle**
**Goal**: Test a complete vault operation sequence

**What Will Happen**:
- Create new integration test file: `VaultIntegrationTest.kt`
- Test sequence:
  1. Generate mnemonic
  2. Create vault from mnemonic
  3. Verify PIN multiple times
  4. Derive multichain addresses
  5. Create recovery backup
  6. Restore vault from backup
  7. Verify restored vault works
  8. Sign sample transactions

**Expected Results**:
- All operations complete without errors
- Data persists through serialization/deserialization
- Addresses derived correctly
- Signatures valid

**Outcome**: End-to-end integration proven

---

### **Step 6: Performance Baseline**
**Goal**: Establish baseline performance metrics

**What Will Happen**:
- Create performance test file: `VaultPerformanceTest.kt`
- Measure times for:
  - Vault creation
  - PIN verification
  - Multichain address derivation
  - Transaction signing (EVM)
  - Bitcoin signature generation
  - Recovery backup creation
  
**Success Criteria**:
- Vault creation: < 500ms
- PIN verification: < 100ms
- Address derivation: < 50ms
- Transaction signing: < 200ms
- Backup creation: < 300ms

**Outcome**: Performance metrics documented

---

### **Step 7: Update Documentation**
**Goal**: Document integration findings

**Files to Create**:
- `PHASE_4_COMPLETION_REPORT.md` (after all steps complete)
- Test results summary
- Performance baseline data
- Integration examples

**Outcome**: Complete documentation of Phase 4 work

---

## Phase 4 Success Criteria

✅ Native library loads successfully in Gradle  
✅ All 27 FFI functions callable from Kotlin  
✅ JSON serialization/deserialization transparent to user  
✅ 27/27 unit tests pass with real library  
✅ Integration test succeeds (full vault lifecycle)  
✅ Performance baseline established and acceptable  
✅ No crashes or memory leaks  
✅ Documentation complete  

---

## Current Directory Structure

```
Wallet-Rust/
├── core-rust/
│   ├── target/release/
│   │   ├── wallet_core.dll       ← Native library (ready)
│   │   ├── wallet_core.dll.lib
│   │   └── wallet_core.pdb
│   └── src/
│       ├── lib.rs (27 FFI exports)
│       └── types.rs
├── kotlin-bindings/
│   ├── build.gradle.kts          ← NEEDS UPDATE (Step 1)
│   └── src/
│       ├── main/kotlin/com/wallet_rust/
│       │   ├── VaultApi.kt       ← NEEDS UPDATE (Step 2)
│       │   ├── VaultData.kt      ← Already has data classes
│       │   ├── VaultError.kt
│       │   └── walletrustlib.kt  ← FFI declarations
│       └── test/kotlin/...
│           └── VaultApiTest.kt   ← Existing tests (27 test methods)
├── wallet.udl                    ← FFI definitions
└── PHASE_3_BUILD_SUCCESS.md
```

---

## Ready to Proceed?

**Question**: Shall I start with **Step 1: Update Native Library Path in Gradle**?

Once you approve, I will:
1. Examine current build.gradle.kts
2. Add JNI configuration for wallet_core.dll
3. Test that gradle recognizes the library
4. Report results
5. **Wait for your permission** before proceeding to Step 2

---

**Enter**: `yes, step 1` or similar to proceed.
