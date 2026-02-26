package com.elementa.wallet.domain.model

import java.time.Instant

/**
 * Transaction domain model - normalized across all blockchains
 */
data class Transaction(
    val id: String,                    // tx hash/signature
    val chain: Chain,                  // blockchain
    val type: TransactionType,         // send, receive, swap, etc
    val status: TransactionStatus,     // pending, completed, failed
    val from: String,                  // sender address
    val to: String,                    // recipient address
    val amount: String,                // amount in native units
    val amountUSD: Double?,            // USD value at time of tx
    val symbol: String,                // token symbol (ETH, BTC, SOL)
    val timestamp: Long,               // unix timestamp
    val blockNumber: Long?,            // block height
    val gasUsed: String?,              // gas/fee info
    val fee: String?,                  // transaction fee
    val feeUSD: Double?,               // fee in USD
    val confirmations: Int = 0,        // number of confirmations
    val isIncoming: Boolean = false,   // true if received
    val contractAddress: String? = null // for token transfers
)

enum class TransactionType {
    SEND, RECEIVE, SWAP, STAKE, UNSTAKE, MINT, BURN
}

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED
}

/**
 * Token holding - represents a user's balance of a specific asset
 */
data class TokenHolding(
    val address: String,           // wallet address
    val chain: Chain,              // blockchain
    val symbol: String,            // token symbol
    val name: String,              // token name
    val contractAddress: String?,  // token contract (null for native)
    val balance: String,           // balance in token units
    val decimals: Int,             // token decimals
    val priceUSD: Double,          // current price in USD
    val valueUSD: Double,          // total value in USD
    val change24h: Double,         // 24h price change %
    val image: String? = null,     // token image URL
    val sparkline: List<Double> = emptyList() // 7-day price sparkline
) {
    fun getFormattedBalance(): String {
        return try {
            val balanceValue = balance.toDoubleOrNull() ?: return "$balance $symbol"
            val decimalsValue = decimals.toDouble()
            val amount = balanceValue / Math.pow(10.0, decimalsValue)
            String.format("%.4f %s", amount, symbol)
        } catch (e: Exception) {
            "$balance $symbol"
        }
    }

    fun getFormattedValue(): String = String.format("$%.2f", valueUSD)
}

/**
 * Wallet holdings summary
 */
data class WalletHoldingsSummary(
    val walletAddress: String,
    val totalValueUSD: Double,
    val change24hUSD: Double,
    val change24hPercent: Double,
    val holdings: List<TokenHolding>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Activity item for UI display
 */
sealed class ActivityItem {
    abstract val id: String
    abstract val timestamp: Long
    
    data class TransactionItem(
        override val id: String,
        override val timestamp: Long,
        val transaction: Transaction,
        val icon: String = when (transaction.type) {
            TransactionType.SEND -> "➤"
            TransactionType.RECEIVE -> "⬅"
            TransactionType.SWAP -> "⇄"
            TransactionType.STAKE -> "📌"
            TransactionType.UNSTAKE -> "📌"
            else -> "•"
        }
    ) : ActivityItem()
}
