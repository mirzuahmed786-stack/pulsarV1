package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenMetadata
import com.elementa.wallet.domain.service.TokenImportErrorClassifier
import com.elementa.wallet.domain.usecase.TokenImportUseCase
import com.elementa.wallet.rpc.EvmRpc
import com.elementa.wallet.ui.state.AddTokenUiState
import com.elementa.wallet.ui.state.ProviderHealthStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTokenViewModel @Inject constructor(
    private val tokenImportUseCase: TokenImportUseCase,
    private val sessionRepository: SessionRepository,
    private val errorClassifier: TokenImportErrorClassifier,
    private val evmRpcService: EvmRpc
) : ViewModel() {
    private val state = MutableStateFlow(AddTokenUiState())
    val uiState: StateFlow<AddTokenUiState> = state

    fun setChain(chain: Chain) {
        state.update { it.copy(chain = chain, error = null, info = null, tokenPreview = null) }
        checkProviderHealth()
    }

    fun setNetwork(network: NetworkType) {
        state.update { it.copy(network = network, error = null, info = null, tokenPreview = null) }
        checkProviderHealth()
    }

    fun setAddress(value: String) {
        state.update { it.copy(address = value) }
    }

    fun toggleManual(value: Boolean) {
        state.update { it.copy(isManualEntry = value) }
    }

    fun setManualName(value: String) {
        state.update { it.copy(manualName = value) }
    }

    fun setManualSymbol(value: String) {
        state.update { it.copy(manualSymbol = value) }
    }

    fun setManualDecimals(value: String) {
        state.update { it.copy(manualDecimals = value) }
    }

    fun importToken() {
        val current = state.value
        val session = sessionRepository.observe().value
        val scope = if (current.chain.isEvm) session.evmScope() else session.solanaScope()
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null, errorHint = null, info = null, retryableError = false) }
            try {
                val manualRequired = current.manualRequired
                val manual = if (manualRequired || current.isManualEntry) {
                    TokenMetadata(
                        name = current.manualName,
                        symbol = current.manualSymbol,
                        decimals = current.manualDecimals.toIntOrNull() ?: -1
                    )
                } else {
                    null
                }
                val token = tokenImportUseCase.execute(
                    AddTokenRequest(
                        chain = current.chain,
                        network = current.network,
                        address = current.address.trim(),
                        walletScope = scope,
                        manualMetadata = manual
                    )
                )
                state.update {
                    it.copy(
                        isLoading = false,
                        tokenPreview = token,
                        info = if (manualRequired) null else null
                    )
                }
            } catch (error: Throwable) {
                val hint = errorClassifier.userHint(error)
                state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message,
                        errorHint = hint,
                        retryableError = errorClassifier.isRetryable(error)
                    )
                }
            }
        }
    }

    fun checkProviderHealth() {
        val current = state.value
        if (!current.chain.isEvm) {
            state.update { it.copy(providerHealth = ProviderHealthStatus.NA) }
            return
        }
        viewModelScope.launch {
            state.update { it.copy(providerHealth = ProviderHealthStatus.CHECKING) }
            try {
                evmRpcService.getChainId(current.chain, current.network)
                state.update { it.copy(providerHealth = ProviderHealthStatus.HEALTHY) }
            } catch (_: Throwable) {
                state.update { it.copy(providerHealth = ProviderHealthStatus.DOWN) }
            }
        }
    }
}
