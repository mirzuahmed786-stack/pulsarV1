package com.elementa.wallet.domain.model

/**
 * Chain-specific blockchain data for displaying network information and statistics
 */
data class ChainData(
    val chain: Chain,
    val nativeSymbol: String,
    val nativeBalance: String,
    val nativePrice: Double,
    val nativeChange24h: Double,
    val totalBalance: String,
    val totalBalanceUSD: Double,
    val change24h: Double,
    val change24hPercent: Double,
    val transactions: List<Transaction> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val gasPrice: String? = null,  // Current gas/fee price
    val blockNumber: Long? = null, // Latest block number
    val networkStatus: NetworkStatus = NetworkStatus.HEALTHY,
    val sparkline: List<Double> = emptyList() // 7-day price chart
)

enum class NetworkStatus {
    HEALTHY, SLOW, CONGESTED, OFFLINE
}

/**
 * Represents a single blockchain with its current state
 */
data class BlockchainChainState(
    val chain: Chain,
    val name: String,
    val symbol: String,
    val balance: String,
    val balanceUSD: Double,
    val price: Double,
    val change24h: Double,
    val marketCap: Double? = null,
    val volume: Double? = null,
    val transactions: List<Transaction> = emptyList(),
    val image: String? = null
)
