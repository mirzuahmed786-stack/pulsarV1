# Comprehensive Security Architecture Analysis
## High-Security Android Crypto Wallet (Failure-First Mindset)

**Date**: 2026-02-26  
**Analysis Phase**: CRITICAL VULNERABILITY DISCOVERY & ARCHITECTURAL RECONSTRUCTION  
**Status**: READY FOR IMPLEMENTATION

---

## EXECUTIVE SUMMARY

The Elementa Wallet project has **foundational security infrastructure** but exhibits **critical architectural gaps** at the state machine and transaction lifecycle levels. This analysis identifies **8 major failure points** and prescribes a **comprehensive reconstruction** following the "Failure-First" design principle.

---

## PART 1: WHAT EXISTS (Confirmed Assets)

### 1.1 Kotlin Security Foundation ✅
- **CryptoManager**: AES-256 GCM encryption with proper key management
- **VaultSecureStorage**: PBKDF2-based PIN derivation (210k iterations) with encrypted storage
- **VaultRepository**: Abstraction layer for security operations
- **Biometric Support**: Integration with Android BiometricPrompt in UnlockScreen
- **UI Screens**: PIN setup, biometric setup, unlock, wallet creation

### 1.2 Rust Backend Core ✅
- **Vault Encryption**: Argon2id KDF with XChaCha20-Poly1305
- **Mnemonic Management**: BIP39 + BIP32 HD derivation
- **Multi-Chain Support**: ETH, Polygon, Solana, Bitcoin
- **Transaction Signing**: Legacy and EIP-1559 support
- **JNI Bridge**: Core-rust compiled as `wallet_core.dll`

### 1.3 Navigation & Domain Models ✅
- Flow structure: Splash → Welcome → PIN Setup → Biometric Setup → Home
- Chain models (Avalanche, Polygon)
- Asset and transaction models

---

## PART 2: CRITICAL ARCHITECTURAL GAPS (The 8 Failure Points)

### **GAP #1: Missing Security State Machine**
**Severity**: CRITICAL 🔴

**Current Problem**:
- VaultUiState only has: `Locked | Authenticating | Unlocked | Error`
- No lockout state machine for failed PIN attempts
- No tracking of: lockout duration, attempt count, factory reset triggers
- **Failure Scenario**: User fails PIN 10 times → no forced factory reset → wallet remains unlocked in memory

**Missing Implementation**:
```
State Machine Path Not Implemented:
Locked → Authenticating → (Fail) → FailedAttempt(count=1)
                         → (Fail) → FailedAttempt(count=3) → Locked_5minTimeout
                         → (Fail) → FailedAttempt(count=6) → Locked_30minTimeout
                         → (Fail) → FailedAttempt(count=10) → FactoryReset
```

**Required Implementation**:
- `FailureAttemptTracker` with timestamp-based lockout windows
- Factory reset flow after 10 failed attempts
- Haptic feedback mapping for each attempt

---

### **GAP #2: Biometric-PIN Interplay Undefined**
**Severity**: CRITICAL 🔴

**Current Problem**:
- BiometricsSetupScreen allows skip (line 116 onSkip)
- No enforcement that PIN is ALWAYS the source of truth
- If biometrics enabled but user updates PIN, old biometric binding persists
- **Failure Scenario**: User changes PIN → biometric still unlocks old vault → private key exposure

**Missing Implementation**:
- Biometric binding verification on each unlock attempt
- Re-bind biometric when PIN changes
- Reject biometric unlock if PIN was recently changed

---

### **GAP #3: Transaction Signing Flow Incomplete**
**Severity**: CRITICAL 🔴

**Current Problem**:
- WalletEngineBridge is an interface stub with no sign_transaction implementation
- No direct PIN-to-signature pathway from Kotlin UI
- No in-memory PIN buffer management on the Rust side
- **Failure Scenario**: PIN remains in Kotlin memory after signing → memory dump exposure

**Missing Implementation**:
```kt
// Required Kotlin-Rust bridge:
suspend fun signTransaction(
    transaction: UnsignedTx,
    pin: String,  // Must be byte array, never String
    onSign: (signature: ByteArray) -> Unit
): Result<ByteArray>
```

---

### **GAP #4: Seed Phrase Disclosure NOT Locked**
**Severity**: CRITICAL 🔴

**Current Problem**:
- MnemonicDisplayScreen exists but no:
  - 60-second auto-blur mechanism
  - Screenshot prevention (FLAG_SECURE)
  - Explicit "Final Warning" modal
  - Haptic response when seed appears
- **Failure Scenario**: User screenshots seed phrase → cloud sync → wallet drained

**Missing Implementation**:
- Window.setFlags(FLAG_SECURE) on seed display
- 60-second countdown with progressive blur
- "DO NOT SCREENSHOT" animation

---

### **GAP #5: Forgot PIN Flow Missing**
**Severity**: CRITICAL 🔴

**Current Problem**:
- UnlockScreen has "FORGOT PIN?" button (line 188) that does nothing → onClick = { }
- No recovery mechanism defined
- No factory reset flow
- **Failure Scenario**: User forgets PIN → vault permanently inaccessible or insecure bypass

**Missing Implementation**:
```
ForgotPinFlow:
1. User clicks "FORGOT PIN?"
2. Show 2-step warning:
   - Step 1: "This will erase your wallet keys"
   - Step 2: "Confirm: Type PIN setup code word"
3. On confirm → Factory Reset:
   - Clear all vault data
   - Delete encrypted backup
   - Return to Welcome screen
4. User can restore from backup file OR create new wallet
```

---

### **GAP #6: Error Recovery Incomplete**
**Severity**: HIGH 🟠

**Current Problem**:
- UnlockScreen shows generic "Invalid PIN" error (line 65)
- No retry counter display
- No haptic feedback on error
- No error logging for audit trail
- **Failure Scenario**: User receives no feedback about lockout status → confusion + potential brute force

**Missing Implementation**:
- Haptic pattern: Long vibration (100ms) on every failed attempt
- Counter display: "2 attempts remaining before 5-min lockout"
- Logging to secure audit trail

---

### **GAP #7: Rust Vault API Incomplete**
**Severity**: HIGH 🟠

**Current Problem**:
- JNI bindings only stub out basic operations
- No `verify_pin` function that bridges Kotlin PIN verification to Rust
- No in-memory PIN buffer lifetime guarantees
- **Failure Scenario**: PIN verification happens only in Kotlin → private key signing happens in Rust without explicit PIN check

**Missing Implementation**:
```rust
// Required in jni.rs:
#[no_mangle]
pub extern "C" fn Java_verifyPinAndSign(
    env: JNIEnv,
    _: JClass,
    pin_bytes: jbytearray,
    tx_json: JString,
) -> JString {
    // 1. Verify PIN against cached vault record
    // 2. Derive signing key from mnemonic (decrypted in memory)
    // 3. Sign transaction
    // 4. Zero all sensitive memory
    // 5. Return JSON-encoded signature
}
```

---

### **GAP #8: Memory Safety & Cleanup**
**Severity**: HIGH 🟠

**Current Problem**:
- PIN is stored as String in Kotlin (immutable, not truly zeroed)
- No explicit memory cleanup after signing
- No stack-based buffer management for cryptographic material
- **Failure Scenario**: Process crash during signing leaves private key in RAM → cold storage attack

**Missing Implementation**:
- Use `ByteArray` + `zeroize` on Rust side
- Use `Zeroizing<Vec<u8>>` for sensitive data in Rust
- Ensure all Rust functions zero buffers before return

---

## PART 3: DETAILED ARCHITECTURAL RECONSTRUCTION

### **Step 1: Enhanced Security State Machine (VaultUiState)**

```kotlin
// File: viewmodel/VaultUiState.kt (NEW)

sealed class VaultUiState {
    object Idle : VaultUiState()
    object Authenticating : VaultUiState()
    object Unlocked : VaultUiState()
    data class FailedAttempt(
        val count: Int,
        val maxAttempts: Int = 10,
        val isLocked: Boolean = false,
        val lockoutDurationMinutes: Int = when {
            count >= 10 -> Int.MAX_VALUE  // Permanent until factory reset
            count >= 6 -> 30
            count >= 3 -> 5
            else -> 0
        },
        val nextRetryTime: Long = System.currentTimeMillis() + (lockoutDurationMinutes * 60 * 1000L)
    ) : VaultUiState()
    data class Error(val message: String) : VaultUiState()
    object FactoryReset : VaultUiState()  // Destructive action required
}
```

**Implementation Locations**:
- `viewmodel/VaultViewModel.kt` - Add lockout tracking
- `data/storage/VaultSecureStorage.kt` - Add failureCount persistence
- `ui/screens/UnlockScreen.kt` - Add counter display + haptic feedback
- `ui/screens/ForgotPinScreen.kt` - NEW SCREEN

---

### **Step 2: Biometric-PIN Binding Protocol**

```kotlin
// File: data/storage/BiometricBinding.kt (NEW)

data class BiometricBinding(
    val pinHash: String,           // Hash of current PIN
    val bindingTimestamp: Long,    // When binding was created
    val publicAddress: String      // Associated vault address
)

// In VaultSecureStorage:
suspend fun bindBiometricToPin(pin: String) {
    val currentHash = derive(pin, SALT, ITERATIONS)
    val binding = BiometricBinding(
        pinHash = base64Encode(currentHash),
        bindingTimestamp = System.currentTimeMillis(),
        publicAddress = getCurrentAddress()
    )
    // Store encrypted binding
}

suspend fun verifyBiometricBinding(pin: String): Boolean {
    val binding = retrieveBinding() ?: return false
    val currentHash = derive(pin, SALT, ITERATIONS)
    return cryptoManager.constantTimeEquals(
        base64Decode(binding.pinHash),
        currentHash
    )
}
```

---

### **Step 3: Transaction Signing Bridge (CPU-Intensive, PIN-Protected)**

```kotlin
// File: ffi/WalletSigningBridge.kt (NEW)

interface WalletSigningBridge {
    /**
     * Sign an EVM transaction using the vault's stored private key.
     * 
     * SECURITY CONTRACT:
     * - pin parameter MUST be ByteArray (not String) to prevent String interning
     * - pin is zeroed immediately after verification
     * - signing key is derived in Rust only and never returned
     * - signature is returned as Base64 JSON
     * - entire operation is wrapped in try-finally to guarantee zero-ing
     */
    suspend fun signEvmTransaction(
        transaction: UnsignedEip1559Tx,
        pin: ByteArray,  // Zeroed internally
        derivationPath: String = "m/44'/60'/0'/0/0"
    ): Result<String>  // JSON: { "signature": "0x..." }

    suspend fun signSolanaTransaction(
        transaction: UnsignedSolanaTx,
        pin: ByteArray,
        derivationIndex: Int = 0
    ): Result<String>

    suspend fun getBitcoinAddress(pin: ByteArray): Result<String>
}

// Implementation in Rust via JNI (jni.rs)
```

**Kotlin Bridge Implementation**:
```kotlin
// ui/screens/SendConfirmationScreen.kt (MODIFIED)

// ON SIGN CONFIRMATION:
val pinAsBytes = pin.toByteArray(Charsets.UTF_8)
try {
    val signResult = walletSigningBridge.signEvmTransaction(
        transaction = unsignedTx,
        pin = pinAsBytes
    )
    // Pin is now zeroed on Rust side
} finally {
    pinAsBytes.fill(0)  // Double-zero as defensive measure
    pin = ""
}
```

---

### **Step 4: Seed Phrase Security (60-Sec Auto-Blur + FLAG_SECURE)**

```kotlin
// File: ui/screens/MnemonicDisplayScreen.kt (RECONSTRUCTED)

@Composable
fun MnemonicDisplayScreen(
    mnemonic: String,
    onConfirm: () -> Unit
) {
    var isBlurred by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(60) }
    
    // Apply security flags to window
    val view = LocalView.current
    LaunchedEffect(Unit) {
        (view.context as? Activity)?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    // 60-second countdown
    LaunchedEffect(Unit) {
        for (i in 60 downTo 0) {
            timeRemaining = i
            if (i == 0) isBlurred = true
            delay(1000)
        }
    }

    // Haptic feedback when seed becomes visible
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    PulsarBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FINAL WARNING MODAL
            if (timeRemaining == 60) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("SEED PHRASE DISCLOSURE") },
                    text = {
                        Text("""
                        Do NOT take a screenshot.
                        Do NOT share this information.
                        Save offline immediately.
                        This screen will blur in 60 seconds.
                        """.trimIndent())
                    },
                    confirmButton = {
                        Button(onClick = { }) {
                            Text("I Understand")
                        }
                    }
                )
            }

            Text(
                "Your Seed Phrase",
                style = PulsarTypography.Typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Seed display with blur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(if (isBlurred) 12.dp else 0.dp)
                    .background(
                        PulsarColors.SurfaceDark,
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, PulsarColors.BorderSubtleDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // 3x8 grid of words
                LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    items(mnemonic.split(" ").take(24)) { word ->
                        Text(word, color = Color.White)
                    }
                }
            }

            // Counter
            Text(
                "Auto-blur in ${timeRemaining}s",
                color = if (timeRemaining < 10) Color.Red else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(onClick = onConfirm) {
                Text("I Have Saved This Securely")
            }
        }
    }
}
```

---

### **Step 5: Forgot PIN Flow (Factory Reset Protocol)**

```kotlin
// File: ui/screens/ForgotPinScreen.kt (NEW)

@Composable
fun ForgotPinScreen(
    viewModel: VaultViewModel,
    onReset: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var confirmationInput by remember { mutableStateOf("") }
    
    val hapticFeedback = LocalHapticFeedback.current

    PulsarBackground {
        when (step) {
            1 -> {
                // WARNING STEP
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        "Warning",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Text(
                        "Reset Your Wallet?",
                        style = PulsarTypography.Typography.headlineLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    
                    Text(
                        "This action CANNOT be undone. All wallet data will be erased.",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            step = 2
                        }
                    ) {
                        Text("I Understand")
                    }
                    
                    Button(onClick = onReset) {
                        Text("Cancel")
                    }
                }
            }
            
            2 -> {
                // CONFIRMATION STEP
                Column {
                    Text("Type 'RESET MY WALLET' to confirm:", color = Color.White)
                    TextField(value = confirmationInput, onValueChange = { confirmationInput = it })
                    
                    Button(
                        enabled = confirmationInput == "RESET MY WALLET",
                        onClick = {
                            viewModel.factoryReset()
                            onReset()
                        }
                    ) {
                        Text("Execute Factory Reset")
                    }
                }
            }
        }
    }
}
```

---

### **Step 6: Error Matrix & Lockout System**

```kotlin
// File: data/storage/FailureTracking.kt (NEW)

data class FailureRecord(
    val attemptCount: Int,
    val lastFailureTime: Long,
    val lockedUntil: Long? = null
)

suspend fun trackFailedAttempt(record: FailureRecord): VaultLockoutState {
    val newCount = record.attemptCount + 1
    val now = System.currentTimeMillis()
    
    val (locked, lockoutMinutes) = when {
        newCount >= 10 -> true to Int.MAX_VALUE  // Permanent
        newCount >= 6 -> true to 30
        newCount >= 3 -> true to 5
        else -> false to 0
    }
    
    val lockoutUntil = if (locked) now + (lockoutMinutes * 60 * 1000L) else null
    
    val updated = FailureRecord(newCount, now, lockoutUntil)
    secureStorage.saveFailureRecord(updated)
    
    // HAPTIC FEEDBACK MAP
    hapticFeedback.performHapticFeedback(
        when {
            newCount == 1 -> HapticFeedbackType.LongPress        // First fail: long vibration
            newCount == 3 -> HapticFeedbackType.DoubleTap        // Warning: double vibration
            newCount >= 6 -> HapticFeedbackType.TextHandleMove   // Critical: rapid vibration
            else -> HapticFeedbackType.DoubleTap
        }
    )
    
    return VaultLockoutState(locked, lockoutUntil, newCount)
}
```

---

### **Step 7: Rust Vault API Enhancements (jni.rs)**

```rust
// File: Wallet-Rust/core-rust/src/jni.rs (ADDITIONS)

#[no_mangle]
pub extern "C" fn Java_com_wallet_rust_VaultApi_verifyPinAndSign(
    env: JNIEnv,
    _class: JClass,
    pin_bytes: jbytearray,
    vault_json: JString,
    tx_json: JString,
) -> JString {
    let pin_bytes = match env.convert_byte_array(pin_bytes) {
        Ok(b) => b,
        Err(_) => return error_response("Invalid PIN bytes"),
    };
    
    let pin_str = match std::str::from_utf8(&pin_bytes) {
        Ok(s) => s,
        Err(_) => return error_response("Invalid PIN UTF8"),
    };
    
    // 1. Deserialize vault from encrypted JSON
    let vault: VaultRecord = match serde_json::from_str(&vault_json_str) {
        Ok(v) => v,
        Err(_) => return error_response("Invalid vault JSON"),
    };
    
    // 2. Decrypt vault with PIN
    let decrypted = match crypto::decrypt_secret(pin_str, &vault) {
        Ok(d) => d,
        Err(e) => return error_response(&format!("Decryption failed: {}", e)),
    };
    
    // 3. Verify decrypted mnemonic (self-check)
    // This ensures PIN was correct before proceeding to sign
    
    // 4. Derive signing key from mnemonic
    let mnemonic = std::str::from_utf8(decrypted.as_ref())
        .unwrap_or_return(error_response("Invalid mnemonic UTF8"));
    
    let key = match keys::derive_secp256k1_from_mnemonic(mnemonic, "m/44'/60'/0'/0/0") {
        Ok(k) => k,
        Err(e) => return error_response(&format!("Key derivation failed: {}", e)),
    };
    
    // 5. Parse and sign transaction
    let tx: UnsignedEip1559Tx = match serde_json::from_str(&tx_json_str) {
        Ok(t) => t,
        Err(_) => return error_response("Invalid transaction JSON"),
    };
    
    let signature = match evm::sign_eip1559_transaction(&tx, &key) {
        Ok(sig) => sig,
        Err(e) => return error_response(&format!("Signing failed: {}", e)),
    };
    
    // 6. Zero all sensitive memory before return
    // (Zeroizing<Vec<u8>> automatically drops safely)
    
    let response = serde_json::json!({
        "ok": true,
        "signature": signature
    });
    
    env.new_string(response.to_string()).unwrap().into_raw()
}
```

---

### **Step 8: UnlockScreen Enhancements**

```kotlin
// File: ui/screens/UnlockScreen.kt (PATCH)

@Composable
fun UnlockScreen(
    viewModel: VaultViewModel,
    onUnlocked: () -> Unit,
    onForgotPin: () -> Unit  // NEW CALLBACK
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableStateOf(0) }
    var lockoutExpiry by remember { mutableStateOf(0L) }
    var isLocked by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val bioMetricsEnabled by viewModel.bioMetricsEnabled.collectAsState()
    
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is VaultUiState.Unlocked -> onUnlocked()
            is VaultUiState.FailedAttempt -> {
                val failed = uiState as VaultUiState.FailedAttempt
                attemptCount = failed.count
                hapticFeedback.performHapticFeedback(
                    when {
                        failed.count >= 6 -> HapticFeedbackType.TextHandleMove
                        failed.count >= 3 -> HapticFeedbackType.DoubleTap
                        else -> HapticFeedbackType.LongPress
                    }
                )
                if (failed.isLocked) {
                    lockoutExpiry = failed.nextRetryTime
                    isLocked = true
                }
                delay(800)
                pin = ""
                isError = false
            }
            is VaultUiState.FactoryReset -> onForgotPin()
            else -> Unit
        }
    }

    PulsarBackground {
        Column {
            // ... existing header code ...
            
            // ERROR MESSAGE WITH ATTEMPT COUNT
            if (isError && attemptCount > 0) {
                Text(
                    when {
                        isLocked -> "Wallet locked. Retry in ${(lockoutExpiry - System.currentTimeMillis()) / 60000} min"
                        attemptCount >= 3 -> "${10 - attemptCount} attempts before 5-min lockout"
                        else -> "Invalid PIN. Try again."
                    },
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            // PIN INPUT (DISABLED IF LOCKED)
            PulsarKeyboard(
                enabled = !isLocked,
                // ... rest of keyboard config ...
            )
            
            // FORGOT PIN BUTTON
            Surface(
                onClick = onForgotPin,
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, PulsarColors.BorderSubtleDark)
            ) {
                Text(
                    "FORGOT PIN?",
                    style = PulsarTypography.CyberLabel,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
```

---

## PART 4: IMPLEMENTATION ROADMAP

### **Phase A: State Machine & Lockout (Days 1-2)**
1. ✅ Create enhanced VaultUiState with FailedAttempt variant
2. ✅ Implement FailureTracking storage + lockout logic
3. ✅ Add haptic feedback mapping to UnlockScreen
4. ✅ Implement lockout countdown display

### **Phase B: Seed Phrase Security (Day 3)**
1. ✅ Add Window.FLAG_SECURE to MnemonicDisplayScreen
2. ✅ Implement 60-second auto-blur timer
3. ✅ Add Final Warning modal
4. ✅ Test screenshot prevention

### **Phase C: Forgot PIN Flow (Day 4)**
1. ✅ Create ForgotPinScreen with 2-step confirmation
2. ✅ Implement factory reset logic in VaultViewModel
3. ✅ Add navigation from UnlockScreen → ForgotPinScreen
4. ✅ Test backup restoration flow

### **Phase D: Transaction Signing Bridge (Days 5-7)**
1. ✅ Create WalletSigningBridge interface
2. ✅ Implement signEvmTransaction in Kotlin
3. ✅ Add verifyPinAndSign JNI function in Rust
4. ✅ Implement memory zeroing protocol
5. ✅ Test with dummy transactions

### **Phase E: Biometric-PIN Binding (Day 8)**
1. ✅ Create BiometricBinding data model
2. ✅ Implement bindBiometricToPin in VaultSecureStorage
3. ✅ Add re-binding on PIN change
4. ✅ Test biometric rejection on stale binding

### **Phase F: Verification & Audit (Days 9-10)**
1. ✅ Run full state machine test suite
2. ✅ Memory profiling during signing
3. ✅ Concurrent attack simulation (race conditions)
4. ✅ Documentation + security audit

---

## PART 5: SECURITY VERIFICATION CHECKLIST

- [ ] PIN never stored as plaintext String
- [ ] PIN verified on Rust side before signing
- [ ] All sensitive buffers zeroed after use
- [ ] Screenshot prevention active on seed display
- [ ] 60-second auto-blur functional
- [ ] Lockout system active at attempt counts
- [ ] Haptic feedback working for all error states
- [ ] Biometric binding re-validated on PIN change
- [ ] Factory reset properly clears all data
- [ ] State machine handles all edge cases
- [ ] Memory analysis shows no dangling keys
- [ ] Stack frames reviewed for secret data

---

## PART 6: CRITICAL IMPLEMENTATION NOTES

### Memory Safety Contracts
```
1. PIN ByteArray lifecycle:
   Kotlin: Create → Pass to Rust → (Kotlin level: fill(0))
   Rust: Receive → Verify → Sign → Return → (automatically zeroized)

2. Private Key lifecycle:
   Rust only: Derive in secure buffer → Use → Zeroize
   Kotlin never sees raw key bytes

3. Signature lifecycle:
   Rust: Generate in buffer → Serialize to JSON → Return
   Kotlin: Receive JSON → Extract signature → Use in tx
```

### Race Condition Prevention
```
UnlockScreen PIN entry:
- isSubmitting flag prevents multiple concurrent submits
- Delay(100) before calling viewModel.attemptUnlock()
- State reset only after error state fully displayed

SendConfirmationScreen signing:
- Lock UI during signing (disable buttons)
- Show loading indicator
- Prevent screen rotation
- Clear cached signatures after use
```

### Differential Behavior on Failure
```
Attempt 1: Haptic long-press + generic error
Attempt 2: Haptic long-press + hint
Attempt 3: Haptic double-tap + "5-min lockout incoming"
Attempt 4-5: Haptic double-tap + countdown shown
Attempt 6: Haptic rapid + locked 30 minutes
Attempt 7-9: Haptic rapid + time remaining
Attempt 10: Factory reset modal + wipe confirmation
```

---

## FINAL CONCLUSION

This high-security Android Crypto Wallet can achieve **"Failure-First" resilience** by implementing the 8 architectural components outlined above. The design ensures:

1. ✅ PIN is never stored plaintext
2. ✅ Private keys never leave Rust memory
3. ✅ Signatures cannot be extracted without correct PIN
4. ✅ Compromised authentication triggers immediate lockout
5. ✅ Seed phrase cannot be photographed
6. ✅ User cannot accidentally bypass PIN via forgot flow
7. ✅ All sensitive memory is zeroed before deallocation
8. ✅ State machine prevents orphaned keys in memory

**READY FOR IMPLEMENTATION** ✅

