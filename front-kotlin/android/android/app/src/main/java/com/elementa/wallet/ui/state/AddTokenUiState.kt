package com.elementa.wallet.ui.state

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenAsset

enum class ProviderHealthStatus {
    IDLE,
    CHECKING,
    HEALTHY,
    DEGRADED,
    DOWN,
    NA
}

data class AddTokenUiState(
    val chain: Chain = Chain.ETHEREUM,
    val network: NetworkType = NetworkType.TESTNET,
    val address: String = "",
    val manualName: String = "",
    val manualSymbol: String = "",
    val manualDecimals: String = "18",
    val isManualEntry: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorHint: String? = null,
    val info: String? = null,
    val retryableError: Boolean = false,
    val providerHealth: ProviderHealthStatus = ProviderHealthStatus.IDLE,
    val tokenPreview: TokenAsset? = null
) {
    val manualRequired: Boolean = network == NetworkType.TESTNET
}
