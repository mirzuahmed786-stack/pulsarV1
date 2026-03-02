package com.elementa.wallet.ui.state

import com.elementa.wallet.domain.model.Chain

data class ChainMarketUi(
    val chain: Chain,
    // Static data - loaded once and cached
    val name: String,
    val symbol: String,
    val logoUrl: String,
    val marketCapUsd: Double = 0.0,
    val volume24hUsd: Double = 0.0,
    // Dynamic data - refreshed frequently
    val balanceAmount: String,
    val holdingsUsd: Double,
    val currentPriceUsd: Double,
    val priceChangePct24h: Double,
    val networkLatencyMs: Long,
    val networkSpeedLabel: String,
    val sparkline: List<Double>,
    // Wallet address for this chain (copyable)
    val walletAddress: String = ""
)

data class HomeUiState(
    val totalBalanceUsd: Double = 0.0,
    val balanceDeltaUsd24h: Double = 0.0,
    val balanceDeltaPct24h: Double = 0.0,
    val sparkline: List<Double> = emptyList(),
    val chainMarkets: List<ChainMarketUi> = emptyList(),
    val isLoading: Boolean = false,
    val isManualRefreshing: Boolean = false,
    val staticDataLoaded: Boolean = false
)
