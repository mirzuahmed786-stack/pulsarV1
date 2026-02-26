package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.ui.state.ReceiveUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val state = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = state

    init {
        viewModelScope.launch {
            loadReceiveAddress()
        }
    }

    fun selectChain(chainCode: String) {
        state.update { it.copy(selectedChainCode = chainCode) }
        loadReceiveAddress()
    }

    private fun loadReceiveAddress() {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true) }
            try {
                val session = sessionRepository.observe().value
                val chainCode = state.value.selectedChainCode
                
                // Get the correct address based on chain type
                val chain = chainCodeToChain(chainCode)
                val addr = session.getAddressForChain(chain)
                
                // Build QR payload based on chain format
                val qr = buildQrPayload(chainCode, addr)
                
                state.update {
                    it.copy(
                        receiveAddress = addr,
                        qrPayload = qr,
                        explorerUrl = buildExplorerUrl(chainCode, addr),
                        isLoading = false
                    )
                }
            } catch (e: Throwable) {
                state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun chainCodeToChain(chainCode: String): Chain = when (chainCode.uppercase()) {
        "BTC" -> Chain.BITCOIN
        "ETH" -> Chain.ETHEREUM
        "SOL" -> Chain.SOLANA
        "AVAX" -> Chain.AVALANCHE
        "MATIC", "POL" -> Chain.POLYGON
        "BNB" -> Chain.BSC
        else -> Chain.ETHEREUM
    }
    
    private fun buildQrPayload(chainCode: String, address: String): String {
        if (address.isBlank()) return ""
        return when (chainCode.uppercase()) {
            "BTC" -> "bitcoin:$address"
            "ETH" -> "ethereum:$address"
            "SOL" -> "solana:$address"
            else -> address
        }
    }

    private fun buildExplorerUrl(chain: String, address: String): String {
        if (address.isBlank()) return ""
        return when (chain.uppercase()) {
            "ETH" -> "https://etherscan.io/address/$address"
            "SOL" -> "https://explorer.solana.com/address/$address?cluster=mainnet-beta"
            "BTC" -> "https://mempool.space/address/$address"
            "AVAX" -> "https://snowtrace.io/address/$address"
            "MATIC", "POL" -> "https://polygonscan.com/address/$address"
            "BNB" -> "https://bscscan.com/address/$address"
            else -> ""
        }
    }
}
