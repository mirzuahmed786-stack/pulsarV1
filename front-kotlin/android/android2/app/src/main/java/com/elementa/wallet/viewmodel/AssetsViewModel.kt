package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.engine.WalletEngine
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.state.AssetsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val walletEngine: WalletEngine,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val state = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = state
    private var updateJob: Job? = null
    private val refreshIntervalMs = 30_000L

    private val supportedChains = listOf(
        Chain.BITCOIN,
        Chain.ETHEREUM,
        Chain.SOLANA,
        Chain.AVALANCHE,
        Chain.POLYGON,
        Chain.BSC
    )

    init {
        viewModelScope.launch {
            sessionRepository.observe().collect { session ->
                state.update { it.copy(evmAddress = session.evmAddress, solanaAddress = session.solanaAddress) }
                loadAllTokens(state.value.network, session.evmScope(), session.solanaScope())
                startPeriodicUpdates()
            }
        }
    }

    fun onSelectChain(chain: Chain?) {
        state.update { it.copy(selectedChain = chain) }
    }

    fun onSelectNetwork(network: NetworkType) {
        state.update { it.copy(network = network) }
        val session = sessionRepository.observe().value
        loadAllTokens(network, session.evmScope(), session.solanaScope())
    }

    fun refresh() {
        val session = sessionRepository.observe().value
        loadAllTokens(state.value.network, session.evmScope(), session.solanaScope())
    }

    fun updateEvmAddress(address: String) {
        sessionRepository.updateEvmAddress(address)
    }

    fun updateSolanaAddress(address: String) {
        sessionRepository.updateSolanaAddress(address)
    }

    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                delay(refreshIntervalMs)
                refresh()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }

    private fun loadAllTokens(network: NetworkType, evmScope: String, solScope: String) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }
            try {
                val tokens = supportedChains.flatMap { chain ->
                    val scope = if (chain.isEvm) evmScope else solScope
                    runCatching { walletEngine.fetchPortfolio(scope, chain, network).tokens }
                        .getOrDefault(emptyList())
                }
                val totalUsd = tokens.sumOf { it.balanceInUsd }
                state.update { 
                    it.copy(
                        tokens = tokens,
                        totalValueUsd = totalUsd,
                        totalBalanceFormatted = String.format("$%.2f", totalUsd),
                        isLoading = false
                    ) 
                }
            } catch (error: Throwable) {
                state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
