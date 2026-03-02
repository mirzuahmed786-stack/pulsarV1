package com.elementa.wallet.data.repository

import com.elementa.wallet.data.remote.BlockchainApi
import com.elementa.wallet.di.BlockchainApiQualifier
import com.elementa.wallet.domain.model.*
import com.elementa.wallet.rpc.SolanaRpc
import com.elementa.wallet.util.WalletLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching and managing live blockchain transaction data
 */
@Singleton
class TransactionRepository @Inject constructor(
    @BlockchainApiQualifier("etherscan")
    private val etherscanApi: BlockchainApi,
    @BlockchainApiQualifier("blockchair")
    private val blockchairApi: BlockchainApi,
    @BlockchainApiQualifier("solana")
    private val solanaApi: BlockchainApi,
    private val priceRepository: PriceRepository,
    private val solanaRpc: SolanaRpc
) {
    
    /**
     * Fetches transactions for an Ethereum/EVM address
     * Updates every refreshInterval milliseconds
     */
    fun getEthereumTransactions(
        address: String,
        apiKey: String,
        chainId: String = "ethereum",
        refreshInterval: Long = 10000  // 10 seconds
    ): Flow<List<Transaction>> = flow {
        while (true) {
            try {
                val response = etherscanApi.getEthereumTransactions(
                    address = address,
                    apiKey = apiKey
                )
                
                if (response.status == "1") {
                    // Get current prices for calculations
                    val prices = priceRepository.getMarketData(listOf(chainId))
                    val currentPrice = prices.firstOrNull()?.currentPrice ?: 0.0
                    
                    val transactions = response.result.map { ethTx ->
                        val amount = weiToEth(ethTx.value)
                        val fee = calculateGasFee(ethTx.gasUsed, ethTx.gasPrice)
                        val timestamp = ethTx.timeStamp.toLongOrNull() ?: 0L
                        
                        Transaction(
                            id = ethTx.hash,
                            chain = Chain.ETHEREUM,
                            type = if (ethTx.to.lowercase() == address.lowercase()) {
                                TransactionType.RECEIVE
                            } else {
                                TransactionType.SEND
                            },
                            status = when {
                                ethTx.isError == "1" -> TransactionStatus.FAILED
                                ethTx.txReceiptStatus == "1" || ethTx.isError == "0" -> TransactionStatus.COMPLETED
                                else -> TransactionStatus.PENDING
                            },
                            from = ethTx.from,
                            to = ethTx.to,
                            amount = amount,
                            amountUSD = amount.toDouble() * currentPrice,
                            symbol = "ETH",
                            timestamp = timestamp,
                            blockNumber = ethTx.blockNumber.toLongOrNull(),
                            gasUsed = ethTx.gasUsed.takeIf { it.isNotEmpty() },
                            fee = fee,
                            feeUSD = try {
                                (fee.toDoubleOrNull() ?: 0.0) * currentPrice
                            } catch (e: Exception) { null },
                            confirmations = ethTx.confirmations.toIntOrNull() ?: 0,
                            isIncoming = ethTx.to.lowercase() == address.lowercase()
                        )
                    }
                    
                    emit(transactions)
                } else {
                    emit(emptyList())
                }
                
                delay(refreshInterval)
            } catch (e: Exception) {
                emit(emptyList())
                delay(refreshInterval)
            }
        }
    }
    
    /**
     * Fetches ERC20 token transfers for an address
     * Updates every refreshInterval milliseconds
     */
    fun getERC20Transactions(
        address: String,
        apiKey: String,
        refreshInterval: Long = 10000
    ): Flow<List<Transaction>> = flow {
        while (true) {
            try {
                val response = etherscanApi.getERC20Tokens(
                    address = address,
                    apiKey = apiKey
                )
                
                if (response.status == "1") {
                    val transactions = response.result.map { token ->
                        val amount = formatTokenAmount(token.value, token.tokenDecimal.toIntOrNull() ?: 18)
                        val timestamp = token.timeStamp.toLongOrNull() ?: 0L
                        
                        Transaction(
                            id = token.hash,
                            chain = Chain.ETHEREUM,
                            type = if (token.to.lowercase() == address.lowercase()) {
                                TransactionType.RECEIVE
                            } else {
                                TransactionType.SEND
                            },
                            status = if (token.isError == "1") TransactionStatus.FAILED 
                                else TransactionStatus.COMPLETED,
                            from = token.from,
                            to = token.to,
                            amount = amount,
                            amountUSD = null, // Would need to fetch token prices
                            symbol = token.tokenSymbol,
                            timestamp = timestamp,
                            blockNumber = token.blockNumber.toLongOrNull(),
                            gasUsed = null,
                            fee = calculateGasFee(token.gasUsed, token.gasPrice),
                            feeUSD = null,
                            confirmations = token.confirmations.toIntOrNull() ?: 0,
                            isIncoming = token.to.lowercase() == address.lowercase(),
                            contractAddress = token.contractAddress
                        )
                    }
                    
                    emit(transactions)
                } else {
                    emit(emptyList())
                }
                
                delay(refreshInterval)
            } catch (e: Exception) {
                emit(emptyList())
                delay(refreshInterval)
            }
        }
    }
    
    /**
     * Fetches Bitcoin transactions for an address
     * Updates every refreshInterval milliseconds
     */
    fun getBitcoinTransactions(
        address: String,
        refreshInterval: Long = 10000
    ): Flow<List<Transaction>> = flow {
        while (true) {
            try {
                val response = blockchairApi.getBitcoinTransactions(address)
                val dashboard = response.data
                val prices = priceRepository.getMarketData(listOf("bitcoin"))
                val currentPrice = prices.firstOrNull()?.currentPrice ?: 0.0
                
                val transactions = dashboard.transactions?.mapNotNull { btcTx ->
                    // Find if our address is in inputs or outputs
                    val inputAddresses = btcTx.inputs.map { it.address }
                    val outputAddresses = btcTx.outputs.map { it.address }
                    
                    val isIncoming = address in outputAddresses
                    val amount = if (isIncoming) {
                        btcTx.outputs.filter { it.address == address }
                            .sumOf { it.value.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                    } else {
                        btcTx.inputs.filter { it.address == address }
                            .sumOf { it.value.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                    }
                    
                    val btcAmount = satoshisToBtc(amount.toDouble())
                    
                    Transaction(
                        id = btcTx.hash,
                        chain = Chain.BITCOIN,
                        type = if (isIncoming) TransactionType.RECEIVE else TransactionType.SEND,
                        status = when (btcTx.status) {
                            "confirmed" -> TransactionStatus.COMPLETED
                            "mempool" -> TransactionStatus.PENDING
                            else -> TransactionStatus.FAILED
                        },
                        from = inputAddresses.firstOrNull() ?: "unknown",
                        to = outputAddresses.firstOrNull() ?: "unknown",
                        amount = btcAmount,
                        amountUSD = btcAmount.toDouble() * currentPrice,
                        symbol = "BTC",
                        timestamp = btcTx.time,
                        blockNumber = btcTx.block_id?.toLong(),
                        gasUsed = null,
                        fee = satoshisToBtc(btcTx.fee?.toDoubleOrNull() ?: 0.0),
                        feeUSD = try {
                            satoshisToBtc(btcTx.fee?.toDoubleOrNull() ?: 0.0).toDouble() * currentPrice
                        } catch (e: Exception) { null },
                        confirmations = 0,
                        isIncoming = isIncoming
                    )
                } ?: emptyList()
                
                emit(transactions)
                delay(refreshInterval)
            } catch (e: Exception) {
                emit(emptyList())
                delay(refreshInterval)
            }
        }
    }
    
    /**
     * Fetches wallet holdings with live prices
     * Updates every refreshInterval milliseconds
     */
    fun getWalletHoldings(
        walletAddresses: Map<Chain, String>,
        refreshInterval: Long = 10000
    ): Flow<WalletHoldingsSummary> = flow {
        while (true) {
            try {
                val allHoldings = mutableListOf<TokenHolding>()
                var totalValueUSD = 0.0
                
                // Fetch Ethereum holdings
                walletAddresses[Chain.ETHEREUM]?.let { address ->
                    try {
                        val balResp = etherscanApi.getEthereumBalance(address = address, apiKey = "")
                        if (balResp.status == "1") {
                            val weiBalance = balResp.result.toBigIntegerOrNull() ?: BigInteger.ZERO
                            val ethBalance = weiBalance.toDouble() / 1e18
                            
                            val prices = priceRepository.getMarketData(listOf("ethereum"))
                            val priceData = prices.firstOrNull()
                            
                            priceData?.let {
                                val valueUSD = ethBalance * it.currentPrice
                                val sparklineData = it.sparkline7d?.price ?: emptyList()
                                val holding = TokenHolding(
                                    address = address,
                                    chain = Chain.ETHEREUM,
                                    symbol = "ETH",
                                    name = "Ethereum",
                                    contractAddress = null,
                                    balance = ethBalance.toString(),
                                    decimals = 18,
                                    priceUSD = it.currentPrice,
                                    valueUSD = valueUSD,
                                    change24h = it.priceChange24h ?: 0.0,
                                    image = it.image,
                                    sparkline = sparklineData
                                )
                                allHoldings.add(holding)
                                totalValueUSD += valueUSD
                            }
                        }
                    } catch (e: Exception) {
                        // Continue with other chains
                    }
                }
                
                // Fetch Bitcoin holdings
                walletAddresses[Chain.BITCOIN]?.let { address ->
                    try {
                        val response = blockchairApi.getBitcoinAddressData(address)
                        response.data[address]?.let { btcAddr ->
                            val satoshis = btcAddr.address.balance.toDoubleOrNull() ?: 0.0
                            val btcBalance = satoshisToBtc(satoshis)
                            
                            val prices = priceRepository.getMarketData(listOf("bitcoin"))
                            val priceData = prices.firstOrNull()
                            
                            priceData?.let {
                                val valueUSD = btcBalance.toDouble() * it.currentPrice
                                val sparklineData = it.sparkline7d?.price ?: emptyList()
                                val holding = TokenHolding(
                                    address = address,
                                    chain = Chain.BITCOIN,
                                    symbol = "BTC",
                                    name = "Bitcoin",
                                    contractAddress = null,
                                    balance = btcBalance.toString(),
                                    decimals = 8,
                                    priceUSD = it.currentPrice,
                                    valueUSD = valueUSD,
                                    change24h = it.priceChange24h ?: 0.0,
                                    image = it.image,
                                    sparkline = sparklineData
                                )
                                allHoldings.add(holding)
                                totalValueUSD += valueUSD
                            }
                        }
                    } catch (e: Exception) {
                        // Continue with other chains
                    }
                }
                
                // Fetch Solana holdings with real balance via RPC
                walletAddresses[Chain.SOLANA]?.let { address ->
                    try {
                        val lamports = solanaRpc.getNativeBalance(NetworkType.MAINNET, address)
                        val solBalance = lamports.toDouble() / 1e9 // Convert lamports to SOL
                        
                        val prices = priceRepository.getMarketData(listOf("solana"))
                        val priceData = prices.firstOrNull()
                        
                        priceData?.let {
                            val valueUSD = solBalance * it.currentPrice
                            val sparklineData = it.sparkline7d?.price ?: emptyList()
                            val holding = TokenHolding(
                                address = address,
                                chain = Chain.SOLANA,
                                symbol = "SOL",
                                name = "Solana",
                                contractAddress = null,
                                balance = solBalance.toString(),
                                decimals = 9,
                                priceUSD = it.currentPrice,
                                valueUSD = valueUSD,
                                change24h = it.priceChange24h ?: 0.0,
                                image = it.image,
                                sparkline = sparklineData
                            )
                            allHoldings.add(holding)
                            totalValueUSD += valueUSD
                        }
                    } catch (e: Exception) {
                        WalletLogger.logRepositoryError("TransactionRepository", "getSolanaBalance", e)
                        // Fallback: show SOL with price data but zero balance
                        try {
                            val prices = priceRepository.getMarketData(listOf("solana"))
                            val priceData = prices.firstOrNull()
                            priceData?.let {
                                val sparklineData = it.sparkline7d?.price ?: emptyList()
                                val holding = TokenHolding(
                                    address = address,
                                    chain = Chain.SOLANA,
                                    symbol = "SOL",
                                    name = "Solana",
                                    contractAddress = null,
                                    balance = "0.0",
                                    decimals = 9,
                                    priceUSD = it.currentPrice,
                                    valueUSD = 0.0,
                                    change24h = it.priceChange24h ?: 0.0,
                                    image = it.image,
                                    sparkline = sparklineData
                                )
                                allHoldings.add(holding)
                            }
                        } catch (_: Exception) {
                            // Silently continue if even price fetch fails
                        }
                    }
                }
                
                // Calculate 24h change
                val change24hUSD = allHoldings.sumOf { 
                    (it.valueUSD * it.change24h) / 100.0
                }
                val change24hPercent = if (totalValueUSD > 0) {
                    (change24hUSD / totalValueUSD) * 100.0
                } else {
                    0.0
                }
                
                emit(WalletHoldingsSummary(
                    walletAddress = walletAddresses.values.firstOrNull() ?: "unknown",
                    totalValueUSD = totalValueUSD,
                    change24hUSD = change24hUSD,
                    change24hPercent = change24hPercent,
                    holdings = allHoldings.filter { it.valueUSD > 0.0 }, // Only show non-zero holdings
                    lastUpdated = System.currentTimeMillis()
                ))
                
                delay(refreshInterval)
            } catch (e: Exception) {
                emit(WalletHoldingsSummary(
                    walletAddress = "unknown",
                    totalValueUSD = 0.0,
                    change24hUSD = 0.0,
                    change24hPercent = 0.0,
                    holdings = emptyList()
                ))
                delay(refreshInterval)
            }
        }
    }
    
    // ─────────────────────────────────────────────────────────────
    // Utility Functions
    // ─────────────────────────────────────────────────────────────
    
    private fun weiToEth(wei: String): String {
        return try {
            val weiAmount = BigDecimal(wei)
            val ethAmount = weiAmount.divide(BigDecimal(1e18))
            ethAmount.toPlainString()
        } catch (e: Exception) {
            "0"
        }
    }
    
    private fun satoshisToBtc(satoshis: Double): String {
        return (satoshis / 1e8).toString()
    }
    
    private fun calculateGasFee(gasUsed: String, gasPrice: String): String {
        return try {
            if (gasUsed.isEmpty() || gasPrice.isEmpty()) return "0"
            val gas = BigDecimal(gasUsed)
            val price = BigDecimal(gasPrice)
            val fee = gas * price
            val eth = fee.divide(BigDecimal(1e18))
            eth.toPlainString()
        } catch (e: Exception) {
            "0"
        }
    }
    
    private fun formatTokenAmount(value: String, decimals: Int): String {
        return try {
            val amount = BigDecimal(value)
            val divisor = BigDecimal(10).pow(decimals)
            val formatted = amount.divide(divisor)
            formatted.toPlainString()
        } catch (e: Exception) {
            value
        }
    }
    
    /**
     * Fetches transactions for Avalanche C-Chain address
     * Uses SnowTrace API (Avalanche Etherscan-compatible)
     */
    fun getAvalancheTransactions(
        address: String,
        apiKey: String = "",
        refreshInterval: Long = 10000
    ): Flow<List<Transaction>> = flow {
        while (true) {
            try {
                // Using etherscanApi since Avalanche uses Etherscan-compatible API
                val response = etherscanApi.getEthereumTransactions(
                    address = address,
                    apiKey = apiKey
                )
                
                if (response.status == "1") {
                    val prices = priceRepository.getMarketData(listOf("avalanche-2"))
                    val currentPrice = prices.firstOrNull()?.currentPrice ?: 0.0
                    
                    val transactions = response.result.map { avaxTx ->
                        val amount = weiToEth(avaxTx.value)
                        val fee = calculateGasFee(avaxTx.gasUsed, avaxTx.gasPrice)
                        val timestamp = avaxTx.timeStamp.toLongOrNull() ?: 0L
                        
                        Transaction(
                            id = avaxTx.hash,
                            chain = Chain.AVALANCHE,
                            type = if (avaxTx.to.lowercase() == address.lowercase()) {
                                TransactionType.RECEIVE
                            } else {
                                TransactionType.SEND
                            },
                            status = when {
                                avaxTx.isError == "1" -> TransactionStatus.FAILED
                                avaxTx.txReceiptStatus == "1" || avaxTx.isError == "0" -> TransactionStatus.COMPLETED
                                else -> TransactionStatus.PENDING
                            },
                            from = avaxTx.from,
                            to = avaxTx.to,
                            amount = amount,
                            amountUSD = amount.toDouble() * currentPrice,
                            symbol = "AVAX",
                            timestamp = timestamp,
                            blockNumber = avaxTx.blockNumber.toLongOrNull(),
                            gasUsed = avaxTx.gasUsed.takeIf { it.isNotEmpty() },
                            fee = fee,
                            feeUSD = try {
                                (fee.toDoubleOrNull() ?: 0.0) * currentPrice
                            } catch (e: Exception) { null },
                            confirmations = avaxTx.confirmations.toIntOrNull() ?: 0,
                            isIncoming = avaxTx.to.lowercase() == address.lowercase()
                        )
                    }
                    
                    emit(transactions)
                } else {
                    emit(emptyList())
                }
                
                delay(refreshInterval)
            } catch (e: Exception) {
                emit(emptyList())
                delay(refreshInterval)
            }
        }
    }
    
    /**
     * Fetches transactions for Polygon address
     * Uses PolygonScan API (Ethereum-compatible)
     */
    fun getPolygonTransactions(
        address: String,
        apiKey: String = "",
        refreshInterval: Long = 10000
    ): Flow<List<Transaction>> = flow {
        while (true) {
            try {
                val response = etherscanApi.getEthereumTransactions(
                    address = address,
                    apiKey = apiKey
                )
                
                if (response.status == "1") {
                    val prices = priceRepository.getMarketData(listOf("polygon-pos"))
                    val currentPrice = prices.firstOrNull()?.currentPrice ?: 0.0
                    
                    val transactions = response.result.map { matTx ->
                        val amount = weiToEth(matTx.value)
                        val fee = calculateGasFee(matTx.gasUsed, matTx.gasPrice)
                        val timestamp = matTx.timeStamp.toLongOrNull() ?: 0L
                        
                        Transaction(
                            id = matTx.hash,
                            chain = Chain.POLYGON,
                            type = if (matTx.to.lowercase() == address.lowercase()) {
                                TransactionType.RECEIVE
                            } else {
                                TransactionType.SEND
                            },
                            status = when {
                                matTx.isError == "1" -> TransactionStatus.FAILED
                                matTx.txReceiptStatus == "1" || matTx.isError == "0" -> TransactionStatus.COMPLETED
                                else -> TransactionStatus.PENDING
                            },
                            from = matTx.from,
                            to = matTx.to,
                            amount = amount,
                            amountUSD = amount.toDouble() * currentPrice,
                            symbol = "MATIC",
                            timestamp = timestamp,
                            blockNumber = matTx.blockNumber.toLongOrNull(),
                            gasUsed = matTx.gasUsed.takeIf { it.isNotEmpty() },
                            fee = fee,
                            feeUSD = try {
                                (fee.toDoubleOrNull() ?: 0.0) * currentPrice
                            } catch (e: Exception) { null },
                            confirmations = matTx.confirmations.toIntOrNull() ?: 0,
                            isIncoming = matTx.to.lowercase() == address.lowercase()
                        )
                    }
                    
                    emit(transactions)
                } else {
                    emit(emptyList())
                }
                
                delay(refreshInterval)
            } catch (e: Exception) {
                emit(emptyList())
                delay(refreshInterval)
            }
        }
    }
}
