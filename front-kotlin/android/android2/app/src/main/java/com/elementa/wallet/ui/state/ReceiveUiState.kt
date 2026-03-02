package com.elementa.wallet.ui.state

data class ReceiveUiState(
    val selectedChainCode: String = "ETH",
    val receiveAddress: String = "",
    val qrPayload: String = "",
    val explorerUrl: String = "",
    val isLoading: Boolean = false
)
