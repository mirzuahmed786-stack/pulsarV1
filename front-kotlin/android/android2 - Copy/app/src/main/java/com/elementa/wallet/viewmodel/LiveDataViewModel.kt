package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.repository.PriceRepository
import com.elementa.wallet.data.repository.TransactionRepository
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.engine.WalletEngine
import com.elementa.wallet.domain.model.*
import com.elementa.wallet.util.WalletLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing live market data and transactions
 * Displays real-time token prices, wallet holdings, and transactions
 */
@HiltViewModel
class LiveDataViewModel @Inject constructor(
    private val priceRepository: PriceRepository,
    private val transactionRepository: TransactionRepository,
    private val sessionRepository: SessionRepository,
    private val walletEngine: WalletEngine
) : ViewModel() {

    private val _marketData = MutableStateFlow<List<CoinMarketResponse>>(emptyList())
    val marketData: StateFlow<List<CoinMarketResponse>> = _marketData.asStateFlow()

    private val _walletHoldings = MutableStateFlow<WalletHoldingsSummary?>(null)
    val walletHoldings: StateFlow<WalletHoldingsSummary?> = _walletHoldings.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _chainData = MutableStateFlow<Map<Chain, ChainData>>(emptyMap())
    val chainData: StateFlow<Map<Chain, ChainData>> = _chainData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var holdingsJob: Job? = null
    private var transactionsJob: Job? = null
    private var chainDataJob: Job? = null

    /**
     * Get wallet addresses from session for all chains
     * EVM chains (Ethereum, Avalanche, Polygon, BSC) share the same EVM address
     */
    fun getWalletAddressesFromSession(): Map<Chain, String> {
        val session = sessionRepository.observe().value
        return mapOf(
            Chain.ETHEREUM to session.evmAddress,
            Chain.AVALANCHE to session.evmAddress,
            Chain.POLYGON to session.evmAddress,
            Chain.BSC to session.evmAddress,
            Chain.SOLANA to session.solanaAddress,
            Chain.BITCOIN to session.bitcoinAddress
        ).filterValues { it.isNotBlank() }
    }

    /**
     * Load all live market data including prices, holdings, and transactions
     */
    fun loadLiveData(walletAddresses: Map<Chain, String>, apiKey: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load market prices for major coins
                val coinIds = listOf("bitcoin", "ethereum", "solana", "avalanche-2", "polygon-pos", "binancecoin")
                val markets = priceRepository.getMarketData(coinIds)
                _marketData.value = markets

                // Start continuous price updates
                startPriceUpdates(coinIds)

                // Load wallet holdings
                loadWalletHoldings(walletAddresses)

                // Load transactions
                loadTransactions(walletAddresses, apiKey)
                
                // Load chain-specific data
                loadChainData(walletAddresses, apiKey)

                _isLoading.value = false
            } catch (e: Exception) {
                WalletLogger.logViewModelError("LiveDataViewModel", "loadLiveData", e)
                _error.value = e.message ?: "Failed to load live data"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load chain-specific data for Avalanche and Polygon
     */
    private fun loadChainData(walletAddresses: Map<Chain, String>, apiKey: String) {
        chainDataJob?.cancel()
        chainDataJob = viewModelScope.launch {
            val chainDataMap = mutableMapOf<Chain, ChainData>()
            val session = sessionRepository.observe().value

            suspend fun updateChainData(
                chain: Chain,
                nativeSymbol: String,
                priceId: String,
                txs: List<Transaction>
            ) {
                // Fetch price data
                val markets = priceRepository.getMarketData(listOf(priceId))
                val priceData = markets.firstOrNull()

                val fallbackPrices = if (priceData == null) {
                    priceRepository.getSimplePrices(listOf(priceId))
                } else {
                    emptyMap()
                }

                val price = priceData?.currentPrice ?: fallbackPrices[priceId] ?: 0.0
                val change24h = priceData?.priceChange24h ?: 0.0
                val sparkline = priceData?.sparkline7d?.price ?: emptyList()
                val networkStatus = if (priceData == null && price == 0.0) {
                    NetworkStatus.OFFLINE
                } else {
                    NetworkStatus.HEALTHY
                }
                
                // Fetch actual holdings from WalletEngine
                val scope = if (chain.isEvm) session.evmScope() else session.solanaScope()
                val portfolio = runCatching {
                    walletEngine.fetchPortfolio(scope, chain, NetworkType.MAINNET)
                }.getOrNull()
                
                // Get native balance from portfolio
                val nativeBalance = portfolio
                    ?.tokens
                    ?.firstOrNull { it.symbol.equals(nativeSymbol, ignoreCase = true) }
                    ?.balance
                    ?: "0.0"
                
                // Calculate total balance in USD
                val totalBalanceUSD = portfolio?.tokens?.sumOf { it.balanceInUsd } ?: 0.0

                chainDataMap[chain] = ChainData(
                    chain = chain,
                    nativeSymbol = nativeSymbol,
                    nativeBalance = nativeBalance,
                    nativePrice = price,
                    nativeChange24h = change24h,
                    totalBalance = nativeBalance,
                    totalBalanceUSD = totalBalanceUSD,
                    change24h = (totalBalanceUSD * change24h) / 100.0,
                    change24hPercent = change24h,
                    transactions = txs.filter { it.chain == chain },
                    gasPrice = null,
                    blockNumber = null,
                    networkStatus = networkStatus,
                    sparkline = sparkline
                )
                _chainData.value = chainDataMap.toMap()
            }
            
            // Load Avalanche data
            walletAddresses[Chain.AVALANCHE]?.let { address ->
                try {
                    transactionRepository.getAvalancheTransactions(address, apiKey).collect { txs ->
                        updateChainData(Chain.AVALANCHE, "AVAX", "avalanche-2", txs)
                    }
                } catch (e: Exception) {
                    WalletLogger.logViewModelError("LiveDataViewModel", "loadAvalancheData", e)
                    // Continue loading other chains
                }
            } ?: run {
                // Price-only fallback when no wallet address is available
                updateChainData(Chain.AVALANCHE, "AVAX", "avalanche-2", emptyList())
            }
            
            // Load Polygon data
            walletAddresses[Chain.POLYGON]?.let { address ->
                try {
                    transactionRepository.getPolygonTransactions(address, apiKey).collect { txs ->
                        updateChainData(Chain.POLYGON, "MATIC", "polygon-pos", txs)
                    }
                } catch (e: Exception) {
                    WalletLogger.logViewModelError("LiveDataViewModel", "loadPolygonData", e)
                    // Continue
                }
            } ?: run {
                // Price-only fallback when no wallet address is available
                updateChainData(Chain.POLYGON, "MATIC", "polygon-pos", emptyList())
            }
        }
    }

    /**
     * Load wallet holdings with live prices
     */
    private fun loadWalletHoldings(walletAddresses: Map<Chain, String>) {
        holdingsJob?.cancel()
        holdingsJob = viewModelScope.launch {
            transactionRepository.getWalletHoldings(walletAddresses).collect { holdings ->
                _walletHoldings.value = holdings
            }
        }
    }

    /**
     * Load all transactions from user's wallets
     */
    private fun loadTransactions(walletAddresses: Map<Chain, String>, apiKey: String) {
        transactionsJob?.cancel()
        transactionsJob = viewModelScope.launch {
            val allTransactions = mutableListOf<Transaction>()

            // Load Ethereum transactions
            walletAddresses[Chain.ETHEREUM]?.let { address ->
                try {
                    transactionRepository.getEthereumTransactions(address, apiKey).collect { txs ->
                        allTransactions.clear()
                        allTransactions.addAll(txs)
                        _transactions.value = allTransactions.sortedByDescending { it.timestamp }
                    }
                } catch (e: Exception) {
                    WalletLogger.logViewModelError("LiveDataViewModel", "loadEthereumTransactions", e)
                    // Continue loading other chains
                }
            }

            // Load ERC20 token transfers
            walletAddresses[Chain.ETHEREUM]?.let { address ->
                try {
                    transactionRepository.getERC20Transactions(address, apiKey).collect { txs ->
                        allTransactions.addAll(txs)
                        _transactions.value = allTransactions.sortedByDescending { it.timestamp }
                    }
                } catch (e: Exception) {
                    WalletLogger.logViewModelError("LiveDataViewModel", "loadERC20Transactions", e)
                    // Continue loading other chains
                }
            }

            // Load Bitcoin transactions
            walletAddresses[Chain.BITCOIN]?.let { address ->
                try {
                    transactionRepository.getBitcoinTransactions(address).collect { txs ->
                        allTransactions.addAll(txs)
                        _transactions.value = allTransactions.sortedByDescending { it.timestamp }
                    }
                } catch (e: Exception) {
                    WalletLogger.logViewModelError("LiveDataViewModel", "loadBitcoinTransactions", e)
                    // Continue loading other chains
                }
            }
        }
    }

    /**
     * Start continuous price updates every 10 seconds
     */
    private fun startPriceUpdates(coinIds: List<String>) {
        viewModelScope.launch {
            while (isActive) {
                delay(10_000) // Update every 10 seconds
                try {
                    val updated = priceRepository.getMarketData(coinIds)
                    _marketData.value = updated
                } catch (e: Exception) {
                    WalletLogger.logViewModelError("LiveDataViewModel", "priceUpdate", e)
                    // Continue updating despite errors
                }
            }
        }
    }

    /**
     * Manually refresh all data
     */
    fun refresh(walletAddresses: Map<Chain, String>, apiKey: String = "") {
        loadLiveData(walletAddresses, apiKey)
    }

    /**
     * Get transaction activity for display
     */
    fun getActivityItems(): List<ActivityItem> {
        return transactions.value.map { tx ->
            ActivityItem.TransactionItem(
                id = tx.id,
                timestamp = tx.timestamp,
                transaction = tx
            )
        }.sortedByDescending { it.timestamp }
    }

    override fun onCleared() {
        super.onCleared()
        holdingsJob?.cancel()
        transactionsJob?.cancel()
        chainDataJob?.cancel()
    }
}

/**
 * Import for actual CoinMarketResponse model from PriceRepository
 */
typealias CoinMarketResponse = com.elementa.wallet.data.remote.coingecko.CoinMarketResponse
