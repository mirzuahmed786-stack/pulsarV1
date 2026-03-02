package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.repository.PriceRepository
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.ui.state.SwapUiState
import com.elementa.wallet.util.WalletLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwapViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val priceRepository: PriceRepository
) : ViewModel() {
    private val state = MutableStateFlow(SwapUiState())
    val uiState: StateFlow<SwapUiState> = state

    fun setFromAmount(amount: String) {
        state.update { it.copy(fromAmount = amount.filter { c -> c.isDigit() || c == '.' }, error = null) }
        estimateToAmount()
    }

    fun setToSymbol(symbol: String) {
        state.update { it.copy(toSymbol = symbol) }
        estimateToAmount()
    }

    fun setSlippage(tolerance: Double) {
        state.update { it.copy(slippageTolerance = tolerance) }
    }

    private fun estimateToAmount() {
        viewModelScope.launch {
            val current = state.value
            val fromAmt = current.fromAmount.toDoubleOrNull() ?: 0.0
            if (fromAmt <= 0) {
                state.update { it.copy(toAmount = "", rate = "0.00") }
                return@launch
            }
            
            try {
                state.update { it.copy(isLoading = true, error = null) }
                
                // Fetch real prices from CoinGecko API
                val fromCoinId = mapSymbolToCoinId(current.fromSymbol)
                val toCoinId = mapSymbolToCoinId(current.toSymbol)
                
                val marketData = priceRepository.getMarketData(listOf(fromCoinId, toCoinId))
                val fromPrice = marketData.find { it.id == fromCoinId }?.currentPrice
                val toPrice = marketData.find { it.id == toCoinId }?.currentPrice
                
                if (fromPrice == null || toPrice == null || toPrice == 0.0) {
                    state.update { it.copy(isLoading = false, error = "Unable to fetch current prices. Please try again.") }
                    return@launch
                }
                
                // Calculate real exchange rate: fromPrice / toPrice
                val realRate = fromPrice / toPrice
                val toAmount = fromAmt * realRate
                val rateStr = String.format("1 %s = %.4f %s", current.fromSymbol, realRate, current.toSymbol)
                
                state.update {
                    it.copy(
                        toAmount = String.format("%.6f", toAmount),
                        rate = rateStr,
                        reviewReady = true,
                        reviewSummary = buildSwapSummary(current.fromAmount, current.fromSymbol, String.format("%.6f", toAmount), current.toSymbol),
                        signingDigest = java.util.UUID.randomUUID().toString(),
                        isLoading = false
                    )
                }
            } catch (e: Throwable) {
                WalletLogger.logViewModelError("SwapViewModel", "estimateToAmount", e)
                state.update { it.copy(isLoading = false, error = "Failed to estimate swap: ${e.message}") }
            }
        }
    }
    
    private fun mapSymbolToCoinId(symbol: String): String = when (symbol.uppercase()) {
        "BTC" -> "bitcoin"
        "ETH" -> "ethereum"
        "SOL" -> "solana"
        "AVAX" -> "avalanche-2"
        "MATIC", "POL" -> "polygon-pos"
        "BNB" -> "binancecoin"
        "USDC" -> "usd-coin"
        "USDT" -> "tether"
        "DAI" -> "dai"
        else -> symbol.lowercase()
    }

    private fun buildSwapSummary(fromAmt: String, fromSym: String, toAmt: String, toSym: String): String {
        return "Swap $fromAmt $fromSym for $toAmt $toSym"
    }

    fun approveAndSign(onSigned: (String) -> Unit) {
        viewModelScope.launch {
            val current = state.value
            if (current.signingDigest != null) {
                // In production: call WalletEngineBridge.signTransaction()
                onSigned(current.signingDigest!!)
            }
        }
    }
}
