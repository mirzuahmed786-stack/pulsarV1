package com.elementa.wallet.ui.state

import com.elementa.wallet.domain.model.Chain

data class SwapUiState(
    val fromChain: Chain = Chain.ETHEREUM,
    val fromSymbol: String = "ETH",
    val fromAmount: String = "",
    val toSymbol: String = "USDC",
    val toAmount: String = "",
    val rate: String = "0.00",
    val slippageTolerance: Double = 0.5,
    val reviewSummary: String = "",
    val reviewReady: Boolean = false,
    val signingDigest: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
