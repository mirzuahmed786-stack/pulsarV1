package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.cache.ChainStaticData
import com.elementa.wallet.data.cache.DynamicChainDataHolder
import com.elementa.wallet.data.cache.StaticDataCache
import com.elementa.wallet.data.repository.PriceRepository
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.engine.WalletEngine
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.ui.state.HomeUiState
import com.elementa.wallet.ui.state.ChainMarketUi
import com.elementa.wallet.util.WalletLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val priceRepository: PriceRepository,
    private val walletEngine: WalletEngine,
    private val staticDataCache: StaticDataCache,
    private val dynamicChainDataHolder: DynamicChainDataHolder
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state
    
    private var updateJob: Job? = null
    private var updateIntervalMs: Long = 8_000L
    private var sessionStaticDataLoaded = false

    init {
        viewModelScope.launch {
            // Load static data first (from cache or API)
            loadStaticData()
            // Then load dynamic data
            refreshPortfolioWithPrices(isManualRefresh = false)
            startPeriodicUpdates()
        }
    }
    
    private suspend fun loadStaticData() {
        val supportedChains = listOf(
            Chain.BITCOIN, Chain.ETHEREUM, Chain.SOLANA,
            Chain.AVALANCHE, Chain.POLYGON, Chain.BSC
        )
        
        // Try to load from cache first
        val cachedData = supportedChains.mapNotNull { chain ->
            staticDataCache.getChainStaticData(chain)?.let { chain to it }
        }.toMap()
        
        // If all chains have valid cache, use it and mark as loaded
        if (cachedData.size == supportedChains.size) {
            sessionStaticDataLoaded = true
            state.update { it.copy(staticDataLoaded = true) }
            return
        }
        
        // Otherwise fetch from API and cache
        try {
            val coinIds = supportedChains.map { priceRepository.mapChainToCoinId(it) }
            val marketData = priceRepository.getMarketData(coinIds)
            val marketMap = marketData.associateBy { it.id }
            
            supportedChains.forEach { chain ->
                val coinId = priceRepository.mapChainToCoinId(chain)
                val market = marketMap[coinId]
                if (market != null) {
                    val staticData = ChainStaticData(
                        chainId = chain.name,
                        name = market.name,
                        symbol = market.symbol.uppercase(),
                        logoUrl = market.image,
                        marketCapUsd = market.marketCap ?: 0.0,
                        volume24hUsd = market.totalVolume ?: 0.0,
                        networkType = "MAINNET",
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                    staticDataCache.saveChainStaticData(chain, staticData)
                }
            }
            sessionStaticDataLoaded = true
            state.update { it.copy(staticDataLoaded = true) }
        } catch (e: Exception) {
            // Failed to load static data, will use fallback values
            WalletLogger.logViewModelError("HomeViewModel", "loadStaticData", e)
        }
    }
    
    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                delay(updateIntervalMs)
                // Only refresh dynamic data (prices, balances, sparklines)
                refreshPortfolioWithPrices(isManualRefresh = false)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }

    fun refreshPortfolioWithPrices(isManualRefresh: Boolean = true) {
        viewModelScope.launch {
            // Don't clear existing data - preserve it during refresh
            state.update { it.copy(isLoading = true, isManualRefreshing = isManualRefresh) }
            try {
                val session = sessionRepository.observe().value
                val currentChainMarkets = state.value.chainMarkets

                val supportedChains = listOf(
                    Chain.BITCOIN, Chain.ETHEREUM, Chain.SOLANA,
                    Chain.AVALANCHE, Chain.POLYGON, Chain.BSC
                )
                
                // Fetch only dynamic data: prices and sparklines
                val coinIds = supportedChains.map { priceRepository.mapChainToCoinId(it) }
                val marketData = priceRepository.getMarketData(coinIds)
                val marketMap = marketData.associateBy { it.id }

                val chainLatencies = mutableListOf<Long>()
                val chainMarkets = supportedChains.map { chain ->
                    // Get static data from cache or use fallback
                    val cachedStatic = staticDataCache.getChainStaticData(chain)
                    val existingData = currentChainMarkets.find { it.chain == chain }
                    
                    val coinId = priceRepository.mapChainToCoinId(chain)
                    val market = marketMap[coinId]
                    val scope = if (chain.isEvm) session.evmScope() else session.solanaScope()
                    
                    // Fetch portfolio (dynamic data)
                    var latencyMs = 0L
                    var portfolio: com.elementa.wallet.domain.model.PortfolioResult? = null
                    latencyMs = measureTimeMillis {
                        portfolio = runCatching {
                            walletEngine.fetchPortfolio(scope, chain, com.elementa.wallet.domain.model.NetworkType.MAINNET)
                        }.getOrNull()
                    }
                    chainLatencies.add(latencyMs)
                    
                    val chainValueUsd = portfolio?.tokens?.sumOf { it.balanceInUsd } ?: 0.0
                    val nativeBalance = portfolio
                        ?.tokens
                        ?.firstOrNull { it.symbol.equals(nativeSymbolFor(chain), ignoreCase = true) }
                        ?.balance
                        ?: (existingData?.balanceAmount ?: "0")
                    
                    // Build ChainMarketUi with static data from cache and dynamic data from API
                    ChainMarketUi(
                        chain = chain,
                        // Static data - from cache or fallback
                        name = cachedStatic?.name ?: existingData?.name ?: chainDisplayName(chain),
                        symbol = cachedStatic?.symbol ?: existingData?.symbol ?: nativeSymbolFor(chain),
                        logoUrl = cachedStatic?.logoUrl ?: existingData?.logoUrl ?: "",
                        marketCapUsd = cachedStatic?.marketCapUsd ?: existingData?.marketCapUsd ?: 0.0,
                        volume24hUsd = cachedStatic?.volume24hUsd ?: existingData?.volume24hUsd ?: 0.0,
                        // Dynamic data - always from latest API call
                        balanceAmount = nativeBalance,
                        holdingsUsd = chainValueUsd,
                        currentPriceUsd = market?.currentPrice ?: (existingData?.currentPriceUsd ?: 0.0),
                        priceChangePct24h = market?.priceChange24h ?: (existingData?.priceChangePct24h ?: 0.0),
                        networkLatencyMs = latencyMs,
                        networkSpeedLabel = networkSpeedLabel(latencyMs),
                        sparkline = market?.sparkline7d?.price ?: (existingData?.sparkline ?: emptyList()),
                        // Wallet address for this chain
                        walletAddress = session.getAddressForChain(chain)
                    )
                }

                val avgLatency = if (chainLatencies.isNotEmpty()) {
                    chainLatencies.average().toLong().coerceAtLeast(1L)
                } else {
                    8_000L
                }
                updateIntervalMs = (avgLatency * 3).coerceIn(5_000L, 15_000L)

                val averageSparkline = averageSparkline(chainMarkets.map { it.sparkline })
                val totalBalance = chainMarkets.sumOf { it.holdingsUsd }
                val firstValue = averageSparkline.firstOrNull() ?: totalBalance
                val lastValue = averageSparkline.lastOrNull() ?: totalBalance
                val delta = lastValue - firstValue
                val deltaPct = if (firstValue > 0) (delta / firstValue) * 100.0 else 0.0

                // Update shared dynamic data holder for detail screens
                dynamicChainDataHolder.updateAllChainData(chainMarkets)

                state.update {
                    it.copy(
                        totalBalanceUsd = totalBalance,
                        balanceDeltaUsd24h = delta,
                        balanceDeltaPct24h = deltaPct,
                        sparkline = averageSparkline,
                        chainMarkets = chainMarkets,
                        isLoading = false,
                        isManualRefreshing = false
                    )
                }
            } catch (e: Throwable) {
                // Don't lose data on error - just clear loading flags                WalletLogger.logViewModelError("HomeViewModel", "refreshPortfolioWithPrices", e)                state.update { it.copy(isLoading = false, isManualRefreshing = false) }
            }
        }
    }

    private fun networkSpeedLabel(latencyMs: Long): String = when {
        latencyMs <= 350 -> "Fast"
        latencyMs <= 900 -> "Normal"
        else -> "Slow"
    }

    private fun nativeSymbolFor(chain: Chain): String = when (chain) {
        Chain.BITCOIN -> "BTC"
        Chain.ETHEREUM -> "ETH"
        Chain.SOLANA -> "SOL"
        Chain.BSC -> "BNB"
        Chain.AVALANCHE -> "AVAX"
        Chain.POLYGON -> "POL"
        Chain.LOCALHOST -> "ETH"
    }

    private fun chainDisplayName(chain: Chain): String = when (chain) {
        Chain.BITCOIN -> "Bitcoin"
        Chain.ETHEREUM -> "Ethereum"
        Chain.SOLANA -> "Solana"
        Chain.BSC -> "BNB Chain"
        Chain.AVALANCHE -> "Avalanche"
        Chain.POLYGON -> "Polygon"
        Chain.LOCALHOST -> "Localhost"
    }

    // Builds a simple average sparkline for the global portfolio card.
    private fun averageSparkline(series: List<List<Double>>): List<Double> {
        if (series.isEmpty()) return emptyList()
        val minSize = series.minOf { it.size }
        if (minSize == 0) return emptyList()
        return (0 until minSize).map { index ->
            series.map { it[index] }.average()
        }
    }
}
