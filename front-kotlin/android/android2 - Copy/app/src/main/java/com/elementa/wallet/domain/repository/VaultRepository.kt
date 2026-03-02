package com.elementa.wallet.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {
    // Emits current lock state for the vault.
    val isLocked: StateFlow<Boolean>
    // Emits whether biometrics are enabled.
    val bioMetricsEnabled: StateFlow<Boolean>
    // Returns true if a PIN has been configured.
    suspend fun hasConfiguredPin(): Boolean
    // Attempts to unlock using the provided PIN.
    suspend fun unlock(pin: String): Result<Unit>
    // Attempts to unlock using biometrics.
    suspend fun authenticateBiometrically(): Result<Unit>
    // Locks the vault and persists the state.
    suspend fun lock()
    // Sets a new PIN and persists its hash.
    suspend fun setPin(pin: String): Result<Unit>
    // Updates biometrics enabled state.
    suspend fun setBiometricsEnabled(enabled: Boolean)
    // Preloads and caches PIN payload for faster verification.
    suspend fun preloadPinCache()
    // Clears all vault-related persisted data.
    suspend fun clearVault()
}
