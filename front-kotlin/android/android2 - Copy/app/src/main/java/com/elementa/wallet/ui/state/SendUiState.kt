package com.elementa.wallet.ui.state

import com.elementa.wallet.domain.model.Chain

data class SendUiState(
    val chain: Chain = Chain.ETHEREUM,
    val toAddress: String = "",
    val amount: String = "",
    val assetSymbol: String = "ETH",
    val estimatedFee: String = "0.0005 ETH",
    val reviewReady: Boolean = false,
    val reviewSummary: String = "",
    val signingDigest: String? = null,
    val isSigning: Boolean = false,
    val error: String? = null
)
