# Phase 3 Kickoff: Rust Build & JNI Integration
**Status**: Ready to Start  
**Date**: February 25, 2026  
**Prerequisite**: Phase 2 Complete ✅

---

## Context Summary: What Was Completed in Phase 2

### ✅ Phase 2 Deliverables
- **kotlin-bindings/** module structure created
- **build.gradle.kts** with uniffi-bindgen integration
- **7 Kotlin source files** created (1,877 lines)
  - walletrustlib.kt - 27 FFI function signatures
  - VaultApi.kt - 27 convenience wrapper methods
  - VaultData.kt - 9 data classes
  - VaultError.kt - Structured error handling
- **VaultApiTest.kt** - 27 unit tests for all functions
- **All changes marked with "hassam dev" comments**

### ✅ Current State
- Kotlin side is 100% complete
- Unit test structure ready
- Gradle build configured
- Awaiting Rust library compilation
- No native library (.so/.dll) yet

### 📊 Phase 2 Statistics
| Item | Count |
|------|-------|
| Kotlin Source Files | 4 |
| Test Files | 1 |
| Test Cases | 27 |
| Functions Implemented | 27 |
| Data Classes | 9 |
| Error Types | 8 |
| Total Lines of Code | 1,877 |

---

## Phase 3 Goal

**Build the Rust backend and generate JNI bindings**

### What Phase 3 Will Deliver
1. ✅ Fix Rust build issues (uniffi configuration)
2. ✅ Successfully compile core-rust library
3. ✅ Generate JNI scaffolding (uniffi-bindgen)
4. ✅ Create native library (walletrustlib.so/dll)
5. ✅ Link native library to Kotlin bindings
6. ✅ Run unit tests against real Rust code
7. ✅ Document build process

---

## Phase 3 Implementation Steps

### Step 1: Fix Rust Build Configuration (1-2 hours)

**Current Issue**: `build.rs` has incorrect uniffi imports

```bash
# Problem:
error: unresolved import `uniffi_build::cargo_uniffi_root`

# Root cause:
- Using wrong API for uniffi 0.28
- build.rs needs correct function signature
```

**Action Items**:
- [ ] Correct build.rs to use proper uniffi_build API
- [ ] Update Cargo.toml if needed
- [ ] Remove wallet.udl from core-rust/ (keep only at project root)
- [ ] Ensure wallet.udl is properly located for build script

### Step 2: Compile Core-Rust Library (30 min - 1 hour)

```bash
cd core-rust
cargo build --release
```

**Expected Output**:
- [ ] Compilation completes without errors
- [ ] target/release/libwallet_core.so (Linux) OR
- [ ] target/release/wallet_core.dll (Windows) OR
- [ ] target/release/libwallet_core.dylib (macOS)
- [ ] Warnings OK, errors NOT OK

### Step 3: Install uniffi-bindgen CLI (10-15 min)

**Option A: Install from Cargo (if available)**
```bash
cargo install uniffi_bindgen --version 0.28
```

**Option B: Use from Rust build**
```bash
cargo run --release -p uniffi_bindgen -- \
  generate ../wallet.udl \
  --language kotlin \
  --out-dir ../kotlin-bindings/src/main/kotlin/com/wallet_rust
```

### Step 4: Generate Kotlin Bindings from UDL (10-15 min)

```bash
# From project root:
uniffi-bindgen generate wallet.udl \
  --language kotlin \
  --out-dir kotlin-bindings/src/main/kotlin/com/wallet_rust
```

**Expected Output**:
- [ ] Generated walletrustlib.kt (complete FFI wrapper)
- [ ] Auto-generated data classes (may override manual versions)
- [ ] Auto-generated exception classes
- [ ] All 27 function signatures

### Step 5: Update Kotlin Bindings if Needed (1-2 hours)

**Possible Actions**:
- [ ] Review auto-generated walletrustlib.kt
- [ ] Merge with manual VaultApi wrapper if needed
- [ ] Update imports and package names
- [ ] Verify data class compatibility
- [ ] Ensure error handling works

### Step 6: Copy Native Library to Test Path (15 min)

```bash
# Linux/Mac:
cp core-rust/target/release/libwallet_core.so \
   kotlin-bindings/src/test/resources/

# Windows:
Copy-Item core-rust\target\release\wallet_core.dll `
  -Destination kotlin-bindings\src\test\resources\
```

**Why**: Java tests need to find the native library

### Step 7: Run Kotlin Unit Tests (30 min - 1 hour)

```bash
cd kotlin-bindings
gradle test
```

**Success Criteria**:
- [ ] Tests compile without errors
- [ ] Native library loads successfully
- [ ] All 27 tests pass OR fail gracefully
- [ ] No JNI errors
- [ ] Performance < 100ms per vault operation

---

## Files to Modify in Phase 3

### MODIFIED Files
```
core-rust/
├── build.rs                    (FIX - incorrect uniffi API)
├── Cargo.toml                  (VERIFY - uniffi_build dependency)
└── wallet.udl                  (REMOVE if copied, keep only root)

kotlin-bindings/
├── build.gradle.kts            (MAY NEED - library path config)
└── src/main/kotlin/...
    └── walletrustlib.kt        (REPLACE with auto-generated)
```

### NO CHANGES TO:
- Phase 1 files (core-rust source, types, etc.)
- Phase 2 Kotlin files (VaultApi.kt, VaultData.kt, VaultError.kt)
- Test suite (keep as-is)

---

## Current Build Issue Analysis

### Error Message
```
error: unresolved import `uniffi_build::cargo_uniffi_root`
  --> build.rs:5:5
```

### Root Cause
UniFI 0.28 API changed. The correct approach for Phase 3 is:

**Option 1: Use uniffi::cargo_uniffi** (in lib.rs, not build.rs)
```rust
// In lib.rs, not build.rs:
uniffi::setup_scaffolding!();
```

**Option 2: Use build.rs correctly**
```rust
// In build.rs:
use uniffi_build::generate_scaffolding;

fn main() {
    generate_scaffolding("..")
        .expect("Failed to generate uniffi scaffolding");
}
```

**Option 3: Let uniffi handle it automatically**
- Remove build.rs entirely
- Add `uniffi::setup_scaffolding!()` in lib.rs
- UniFI will auto-detect wallet.udl

### Phase 3 Action
- Review Phase 1 lib.rs to see if `uniffi::setup_scaffolding!()` was added
- If not, add it to lib.rs directly
- Potentially remove or fix build.rs
- Let uniffi auto-detect wallet.udl at project root

---

## Success Criteria for Phase 3

### Build Success
- [ ] `cargo build --release` completes
- [ ] No "error[E" lines in output
- [ ] Native library (.so/.dll/.dylib) produced
- [ ] Size > 5MB (fully linked)

### JNI Generation
- [ ] uniffi-bindgen completes without errors
- [ ] walletrustlib.kt is properly formatted
- [ ] All 27 function signatures present
- [ ] No missing imports

### Test Execution
- [ ] `gradle test` completes
- [ ] Native library loads successfully
- [ ] Tests run (may fail at function level, but JNI layer works)
- [ ] < 5 seconds for test suite execution

### Integration
- [ ] Kotlin code can call Rust functions via JNI
- [ ] Error handling works both ways
- [ ] Data serialization/deserialization works
- [ ] No memory leaks or segfaults

---

## Troubleshooting Phase 3 Issues

### Issue: "Cannot find wallet.udl"
```bash
Solution:
1. Ensure wallet.udl exists at project root: d:\last\Wallet-Rust\wallet.udl
2. Build script should NOT copy it to core-rust/
3. Use relative path "../wallet.udl" if building from core-rust/
```

### Issue: uniffi-bindgen not found
```bash
Solution:
1. Check Rust version: rustup update
2. Reinstall uniffi: cargo install uniffi_bindgen@0.28
3. Or use: cargo run -p uniffi_bindgen --release -- generate
```

### Issue: "Native library not found" in tests
```bash
Solution:
1. Copy .so/.dll to kotlin-bindings/build/libs/
2. Add to gradle: System.setProperty("java.library.path", "...")
3. Or use: -Djava.library.path=/path/to/lib in test command
```

### Issue: Kotlin compilation fails
```bash
Solution:
1. Re-run generateUniFFI: gradle generateUniFFI
2. Check Kotlin version: kotlinc -version (should be 1.9+)
3. Clear gradle cache: gradle clean build
```

### Issue: JNI errors at runtime
```bash
Solution:
1. Verify library is 64-bit (match JVM architecture)
2. Check symbols: nm -D libwallet_core.so | grep ffi
3. Ensure all 27 functions are exported
```

---

## Key Information for Phase 3

### Paths
```
Project Root: d:\last\Wallet-Rust\
├── wallet.udl                              ← Must exist here
├── core-rust/
│   ├── Cargo.toml
│   ├── build.rs                            ← May need fixing
│   ├── src/lib.rs                          ← uniffi::setup_scaffolding!() here
│   └── target/release/
│       └── libwallet_core.so or .dll       ← Target output
└── kotlin-bindings/
    ├── build.gradle.kts
    └── src/main/kotlin/com/wallet_rust/
        └── walletrustlib.kt                ← Will be replaced
```

### Command Reference
```bash
# Test Rust compilation
cd core-rust && cargo check

# Build release
cd core-rust && cargo build --release

# Generate Kotlin bindings
uniffi-bindgen generate wallet.udl --language kotlin --out-dir kotlin-bindings/src/main/kotlin/com/wallet_rust

# Run tests
cd kotlin-bindings && gradle test

# Debug native library
nm -D target/release/libwallet_core.so | grep -i generate_mnemonic
```

---

## Phase 3 Estimated Effort

| Task | Hours | Notes |
|------|-------|-------|
| Fix build.rs | 0.5 | Quick fix to uniffi API call |
| Rust compilation | 1 | First build slower, includes deps |
| uniffi-bindgen install | 0.25 | Either cargo install or from build |
| Generate Kotlin bindings | 0.25 | One-time generation |
| Gradle integration | 0.5 | Update library paths if needed |
| Test execution | 0.5 | May need JVM library path config |
| Troubleshooting | 2 | Varies by environment |
| **Total** | **~5 hours** | Could be 2-3 with luck |

**Realistic Timeline**: 1 business day for one developer

---

## Phase 3 Checklist

### Pre-Phase 3
- [x] Phase 2 complete
- [x] kotlin-bindings/ fully implemented
- [x] wallet.udl at project root
- [x] Cargo.toml has uniffi dependency
- [ ] Verify Phase 1 source code has uniffi markers

### During Phase 3
- [ ] Diagnose and fix build.rs
- [ ] Compile core-rust successfully
- [ ] Install uniffi-bindgen
- [ ] Generate Kotlin FFI from UDL
- [ ] Review auto-generated code
- [ ] Configure gradle library path
- [ ] Link native library to tests
- [ ] Run test suite

### Post-Phase 3
- [ ] All 27 Rust functions callable from Kotlin
- [ ] No JNI errors or crashes
- [ ] Unit tests pass with real Rust code
- [ ] Performance baseline recorded
- [ ] Ready for Phase 4 (Android Integration)

---

## What NOT to Do in Phase 3

- ❌ Do NOT modify Rust function signatures
- ❌ Do NOT rename FFI functions
- ❌ Do NOT remove "hassam dev" comments
- ❌ Do NOT change wallet.udl structure
- ❌ Do NOT delete Phase 1 or Phase 2 code
- ❌ Do NOT manually edit auto-generated Kotlin files (from uniffi-bindgen)

## What IS Okay in Phase 3

- ✅ Fix build.rs to use correct API
- ✅ Run uniffi-bindgen to generate code
- ✅ Update gradle library paths
- ✅ Copy native library to test resources
- ✅ Update build.gradle.kts for library linking
- ✅ Create test configuration files
- ✅ Add documentation for build process

---

## Phase 3 Start Checklist

When opening new chat to start Phase 3:

1. [ ] Review this document (PHASE_3_KICKOFF.md)
2. [ ] Review PHASE_2_COMPLETION_SUMMARY.md
3. [ ] Look at Phase 1 source (core-rust/src/lib.rs) for uniffi markers
4. [ ] Have ready: Rust toolchain info (`rustup show`)
5. [ ] Verify: wallet.udl exists and is valid
6. [ ] Check: Cargo.toml has correct uniffi dependency
7. [ ] Confirm: JDK 11+ installed for Gradle
8. [ ] Start with: Step 1 - Diagnose build issue

---

## Resources & References

### From Phase 1
- [core-rust/Cargo.toml](../core-rust/Cargo.toml) - Dependency configuration
- [core-rust/build.rs](../core-rust/build.rs) - Build script (needs fixing)
- [core-rust/src/lib.rs](../core-rust/src/lib.rs) - Rust FFI exports
- [wallet.udl](../wallet.udl) - Interface definition

### From Phase 2
- [PHASE_2_COMPLETION_SUMMARY.md](../PHASE_2_COMPLETION_SUMMARY.md) - Kotlin implementation status
- [kotlin-bindings/build.gradle.kts](../kotlin-bindings/build.gradle.kts) - Gradle config
- [kotlin-bindings/src/test/kotlin/com/wallet_rust/VaultApiTest.kt](../kotlin-bindings/src/test/kotlin/com/wallet_rust/VaultApiTest.kt) - Test suite

### External Documentation
- UniFI 0.28 docs: https://mozilla.github.io/uniffi-rs/
- Kotlin/JVM docs: https://kotlinlang.org/docs/jvm-interop.html

---

## Expected Outcomes

### By End of Phase 3

**Build Artifacts**
- Native library: `libwallet_core.so` (Linux) / `wallet_core.dll` (Windows)
- Size: ~10-20 MB
- Contains all 27 exported FFI functions

**Kotlin Integration**
- Auto-generated walletrustlib.kt properly integrated
- All imports resolved
- Type conversions working
- Error handling functional

**Test Results**
- Unit test suite runs
- Tests may pass or fail at business logic level
- No JNI errors or crashes
- Performance baseline established

**Documentation**
- Build process documented
- Known issues recorded
- Performance notes captured

---

## Handoff Summary

### ✅ What Kotlin Team Completed (Phase 2)
- Full Kotlin bindings API
- 27 convenience wrapper methods
- 27 unit tests
- Gradle build configuration
- Error handling framework
- Complete documentation

### 🔧 What Rust Team Needs to Do (Phase 3)
- Fix uniffi build configuration
- Compile Rust backend
- Generate JNI layer
- Create native library
- Integrate with Kotlin tests

### 🚀 Result After Phase 3
- Kotlin can call Rust functions
- Tests validate integration
- Ready for Android Phase 4

---

## Timeline Summary

| Phase | Focus | Duration | Status |
|-------|-------|----------|--------|
| Phase 1 | Rust FFI setup | Complete | ✅ Done |
| Phase 2 | Kotlin module | Complete | ✅ Done |
| **Phase 3** | **Rust build & JNI** | **1 day** | **Starting** |
| Phase 4 | Android integration | 3-4 days | Pending |
| Phase 5 | Hardening & release | 2-3 days | Pending |

---

## Sign-Off: Phase 2 → Phase 3

**Phase 2 Status**: ✅ **COMPLETE**

The Kotlin module is ready with:
- 27 FFI wrapper functions
- 27 unit tests
- Complete build configuration
- Full documentation

**Ready to proceed to Phase 3: Rust Build & JNI Integration**

Next steps:
1. Fix build.rs uniffi API call
2. Compile core-rust library
3. Generate Kotlin JNI bindings
4. Run test suite with real Rust code

---

**Generated**: February 25, 2026  
**For**: Rust Build & JNI Generation  
**Status**: Ready to Start  
**Effort**: ~5 hours (1 business day)  
**Next Phase Timeline**: 1-3 days

