package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val lockVaultUseCase: LockVaultUseCase
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
            _uiState.value = VaultUiState.Locked
        }
    }

    // Attempts to unlock with a PIN.
    fun attemptUnlock(pin: String) {
        viewModelScope.launch {
            _uiState.value = VaultUiState.Authenticating
            val result = unlockVaultUseCase.execute(pin)
            if (result.isSuccess) {
                _uiState.value = VaultUiState.Unlocked
            } else {
                _uiState.value = VaultUiState.Error("Invalid PIN")
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

    // Saves a new PIN and updates configuration state.
    fun configurePin(pin: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = setPinUseCase.execute(pin)
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

    // Preloads PIN payload for faster verification.
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

    // Deletes vault data after verifying the PIN.
    fun deleteVault(pin: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = unlockVaultUseCase.execute(pin)
            if (result.isSuccess) {
                repository.clearVault()
                _isPinConfigured.value = false
                _uiState.value = VaultUiState.Locked
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }
}

sealed class VaultUiState {
    object Locked : VaultUiState()
    object Authenticating : VaultUiState()
    object Unlocked : VaultUiState()
    data class Error(val message: String) : VaultUiState()
}
