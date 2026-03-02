package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.engine.WalletEngine
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.state.SendUiState
import com.elementa.wallet.util.AddressValidator
import com.elementa.wallet.util.WalletLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SendViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val walletEngine: WalletEngine
) : ViewModel() {
    private val state = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = state

    fun setChain(chain: Chain) {
        state.update { it.copy(chain = chain) }
    }

    fun setToAddress(address: String) {
        val normalized = address.trim()
        state.update { it.copy(toAddress = normalized, error = null) }
        validateAndPrepareSendSummary()
    }

    fun setAmount(amount: String) {
        state.update { it.copy(amount = amount.filter { c -> c.isDigit() || c == '.' }, error = null) }
        validateAndPrepareSendSummary()
    }

    fun setAsset(symbol: String) {
        state.update { it.copy(assetSymbol = symbol) }
        validateAndPrepareSendSummary()
    }

    private fun validateAndPrepareSendSummary() {
        val current = state.value
        val amountValue = current.amount.toDoubleOrNull()
        
        // Validate address format
        val isAddressValid = when (current.chain) {
            Chain.ETHEREUM, Chain.BSC, Chain.POLYGON, Chain.AVALANCHE, Chain.LOCALHOST -> 
                AddressValidator.isValidEvmAddress(current.toAddress)
            Chain.SOLANA -> 
                AddressValidator.isValidSolanaAddress(current.toAddress)
            Chain.BITCOIN -> 
                AddressValidator.isValidBitcoinAddress(current.toAddress)
        }
        
        // Validate amount
        val isAmountValid = amountValue != null && 
            amountValue > 0.0 && 
            AddressValidator.isAmountWithinBounds(current.amount)
        
        val isValid = isAddressValid && isAmountValid
        
        if (!isAddressValid && current.toAddress.isNotBlank()) {
            state.update { it.copy(error = "Invalid recipient address format", reviewReady = false) }
            return
        }
        
        if (!isAmountValid && current.amount.isNotBlank()) {
            state.update { it.copy(error = "Invalid amount", reviewReady = false) }
            return
        }
        
        if (isValid) {
            val summary = buildString {
                append("Send ")
                append(current.amount)
                append(" ")
                append(current.assetSymbol)
                append(" to ")
                append(current.toAddress.take(12))
                append("...")
            }
            val digest = java.util.UUID.randomUUID().toString() // Mock digest in real flow
            state.update {
                it.copy(
                    reviewReady = true,
                    reviewSummary = summary,
                    signingDigest = digest,
                    error = null
                )
            }
        } else {
            state.update { it.copy(reviewReady = false, reviewSummary = "", signingDigest = null) }
        }
    }

    fun approveAndSign(onSigned: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val current = state.value
                if (current.signingDigest == null) {
                    state.update { it.copy(error = "Transaction not ready for signing") }
                    return@launch
                }
                
                state.update { it.copy(isSigning = true, error = null) }
                
                // Validate we have sufficient balance before signing
                val session = sessionRepository.observe().value
                val scope = if (current.chain.isEvm) session.evmScope() else session.solanaScope()
                
                val portfolio = walletEngine.fetchPortfolio(scope, current.chain, NetworkType.MAINNET)
                val tokenBalance = portfolio.tokens
                    .firstOrNull { it.symbol.equals(current.assetSymbol, ignoreCase = true) }
                    ?.balance?.toDoubleOrNull() ?: 0.0
                
                val sendAmount = current.amount.toDoubleOrNull() ?: 0.0
                if (sendAmount > tokenBalance) {
                    state.update { 
                        it.copy(
                            isSigning = false, 
                            error = "Insufficient balance. Available: $tokenBalance ${current.assetSymbol}"
                        ) 
                    }
                    return@launch
                }
                
                // Transaction is valid - proceed with signing
                // Note: Actual signing requires Rust FFI bridge (Phase 7)
                // For now, we validate and prepare the transaction
                WalletLogger.i("Transaction validated: ${current.reviewSummary}")
                state.update { it.copy(isSigning = false) }
                onSigned(current.signingDigest!!)
                
            } catch (e: Exception) {
                WalletLogger.logViewModelError("SendViewModel", "approveAndSign", e)
                state.update { it.copy(isSigning = false, error = "Failed to process transaction: ${e.message}") }
            }
        }
    }
}
