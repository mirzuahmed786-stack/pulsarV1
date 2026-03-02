package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.storage.FailureAttemptTracker
import com.elementa.wallet.domain.repository.VaultRepository
import com.elementa.wallet.domain.usecase.HasConfiguredPinUseCase
import com.elementa.wallet.domain.usecase.LockVaultUseCase
import com.elementa.wallet.domain.usecase.SetPinUseCase
import com.elementa.wallet.domain.usecase.UnlockVaultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val hasConfiguredPinUseCase: HasConfiguredPinUseCase,
    private val setPinUseCase: SetPinUseCase,
    private val unlockVaultUseCase: UnlockVaultUseCase,
    private val lockVaultUseCase: LockVaultUseCase,
    private val failureTracker: FailureAttemptTracker  // NEW: Lockout tracking
) : ViewModel() {

    private val _uiState = MutableStateFlow<VaultUiState>(VaultUiState.Locked)
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()
    
    private val _isPinConfigured = MutableStateFlow(false)
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured.asStateFlow()
    
    val bioMetricsEnabled: StateFlow<Boolean> = repository.bioMetricsEnabled
    val isLocked: StateFlow<Boolean> = repository.isLocked

    init {
        viewModelScope.launch {
            _isPinConfigured.value = hasConfiguredPinUseCase.execute()
            
            // Check if user is currently locked out
            val failureState = failureTracker.getFailureState()
            if (failureState.attemptCount >= 10) {
                _uiState.value = VaultUiState.FactoryResetRequired
            } else if (failureState.isLockedByTimeout) {
                _uiState.value = VaultUiState.Locked
            } else {
                _uiState.value = VaultUiState.Locked
            }
        }
    }

    // ✅ ENHANCED: Attempts to unlock with a Password, tracking failures and enforcing lockouts
    fun attemptUnlock(password: String) {
        viewModelScope.launch {
            // 1. Check if currently locked out
            val currentState = failureTracker.getFailureState()
            if (currentState.attemptCount >= 10) {
                _uiState.value = VaultUiState.FactoryResetRequired
                return@launch
            }
            if (currentState.isLockedByTimeout) {
                val minutesLeft = failureTracker.getMinutesUntilRetry()
                _uiState.value = VaultUiState.Error("Wallet locked. Try again in $minutesLeft minutes.")
                return@launch
            }
            
            // 2. Attempt unlock
            _uiState.value = VaultUiState.Authenticating
            val result = unlockVaultUseCase.execute(password)
            
            if (result.isSuccess) {
                // SUCCESS: Clear failure counter
                failureTracker.clearFailureAttempts()
                _uiState.value = VaultUiState.Unlocked
            } else {
                // FAILURE: Record attempt and check for lockout
                val failureRecord = failureTracker.recordFailureAttempt()
                
                // Check if we should factory reset (10 attempts)
                if (failureRecord.attemptCount >= 10) {
                    _uiState.value = VaultUiState.FactoryResetRequired
                } else {
                    _uiState.value = VaultUiState.Error(failureRecord.getDisplayMessage())
                }
            }
        }
    }

    // Attempts to unlock using biometrics when enabled.
    fun authenticateBiometrically() {
        viewModelScope.launch {
            _uiState.value = VaultUiState.Authenticating
            val result = repository.authenticateBiometrically()
            if (result.isSuccess) {
                _uiState.value = VaultUiState.Unlocked
            } else {
                _uiState.value = VaultUiState.Error("Biometric Authentication Failed")
            }
        }
    }

    // Saves a new password and updates configuration state.
    fun configurePassword(password: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = setPinUseCase.execute(password)
            _isPinConfigured.value = result.isSuccess
            onComplete(result.isSuccess)
        }
    }

    // Updates biometrics enabled preference.
    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricsEnabled(enabled)
        }
    }

    // Preloads password payload for faster verification.
    fun preloadPinCache() {
        viewModelScope.launch {
            repository.preloadPinCache()
        }
    }

    // Locks the vault and updates UI state.
    fun lock() {
        viewModelScope.launch {
            lockVaultUseCase.execute()
            _uiState.value = VaultUiState.Locked
        }
    }

    // Deletes vault data after verifying the password.
    fun deleteVault(password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = unlockVaultUseCase.execute(password)
            if (result.isSuccess) {
                repository.clearVault()
                failureTracker.forceReset()
                _isPinConfigured.value = false
                _uiState.value = VaultUiState.Locked
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }
    
    // ✅ NEW: Factory reset flow (triggered after 10 failed attempts or user choice)
    fun factoryReset() {
        viewModelScope.launch {
            repository.clearVault()
            failureTracker.forceReset()
            _isPinConfigured.value = false
            _uiState.value = VaultUiState.Locked
        }
    }
    
    // ✅ NEW: Get current lockout state for UI display
    suspend fun getLockoutMinutes(): Long {
        return failureTracker.getMinutesUntilRetry()
    }
    
    // ✅ NEW: Check if user is currently locked out (for UI disable)
    suspend fun isCurrentlyLocked(): Boolean {
        return failureTracker.isCurrentlyLocked()
    }
}

sealed class VaultUiState {
    object Locked : VaultUiState()
    object Authenticating : VaultUiState()
    object Unlocked : VaultUiState()
    data class Error(val message: String) : VaultUiState()
    object FactoryResetRequired : VaultUiState()  // NEW: Requires factory reset
}
