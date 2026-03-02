package com.elementa.wallet.ui.state

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenAsset

data class ChainDetailUiState(
    val chain: Chain = Chain.ETHEREUM,
    val network: NetworkType = NetworkType.TESTNET,
    val tokens: List<TokenAsset> = emptyList(),
    val walletAddress: String = "",
    val currentPriceUsd: Double = 0.0,
    val priceChangePct24h: Double = 0.0,
    val marketCapUsd: Double = 0.0,
    val volume24hUsd: Double = 0.0,
    val networkLatencyMs: Long = 0L,
    val networkSpeedLabel: String = "Normal",
    val sparkline: List<Double> = emptyList(),
    val logoUrl: String? = null,
    val circulatingSupply: Double = 0.0,
    val allTimeHigh: Double = 0.0,
    val stakedBalance: String = "0.00",
    val availableBalance: String = "0.00",
    val isActivityVisible: Boolean = false,
    val transactions: List<com.elementa.wallet.domain.model.Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val isManualRefreshing: Boolean = false,
    val error: String? = null
)
