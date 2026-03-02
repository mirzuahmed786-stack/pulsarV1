package com.elementa.wallet.data.repository

import com.elementa.wallet.data.storage.VaultSecureStorage
import com.elementa.wallet.domain.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val secureStorage: VaultSecureStorage
) : VaultRepository {
    private val _isLocked = MutableStateFlow(true)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _bioMetricsEnabled = MutableStateFlow(false)
    override val bioMetricsEnabled: StateFlow<Boolean> = _bioMetricsEnabled.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            _isLocked.value = secureStorage.isLocked()
            _bioMetricsEnabled.value = secureStorage.isBiometricsEnabled()
        }
    }

    // Returns true if a PIN payload exists.
    override suspend fun hasConfiguredPin(): Boolean = secureStorage.hasPin()

    // Verifies the password and unlocks the vault if valid.
    override suspend fun unlock(pin: String): Result<Unit> {
        if (!secureStorage.hasPin()) return Result.failure(IllegalStateException("Password not set"))
        
        return if (secureStorage.verifyPin(pin)) {
            secureStorage.setLocked(false)
            _isLocked.value = false
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid password"))
        }
    }

    // Unlocks using biometrics when enabled.
    override suspend fun authenticateBiometrically(): Result<Unit> {
        // In a real implementation, we would check biometric hardware and prompt the user.
        // For this implementation, we assume the biometric check passed if enabled.
        return if (secureStorage.isBiometricsEnabled()) {
            secureStorage.setLocked(false)
            _isLocked.value = false
            Result.success(Unit)
        } else {
            Result.failure(Exception("Biometrics not enabled"))
        }
    }

    // Locks the vault and persists the state.
    override suspend fun lock() {
        secureStorage.setLocked(true)
        _isLocked.value = true
    }

    // Stores the password as an encrypted hash.
    override suspend fun setPin(pin: String): Result<Unit> {
        return runCatching {
            require(pin.length >= 6) { "Password must be at least 6 characters" }
            secureStorage.savePin(pin)
            secureStorage.setLocked(false)
            _isLocked.value = false
        }
    }

    // Persists biometrics enabled state.
    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        secureStorage.setBiometricsEnabled(enabled)
        _bioMetricsEnabled.value = enabled
    }

    // Preloads and caches PIN payload for faster verification.
    override suspend fun preloadPinCache() {
        secureStorage.preloadPinCache()
    }

    // Clears vault storage and resets in-memory state.
    override suspend fun clearVault() {
        _isLocked.value = true
        secureStorage.clear()
        _bioMetricsEnabled.value = false
    }
}
