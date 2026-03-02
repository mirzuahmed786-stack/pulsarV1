package com.elementa.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementa.wallet.data.cache.ChainStaticData
import com.elementa.wallet.data.cache.DynamicChainDataHolder
import com.elementa.wallet.data.cache.StaticDataCache
import com.elementa.wallet.data.session.SessionRepository
import com.elementa.wallet.domain.engine.WalletEngine
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.RemoveTokenRequest
import com.elementa.wallet.ui.state.ChainDetailUiState
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
class ChainDetailViewModel @Inject constructor(
    private val walletEngine: WalletEngine,
    private val sessionRepository: SessionRepository,
    private val priceRepository: com.elementa.wallet.data.repository.PriceRepository,
    private val staticDataCache: StaticDataCache,
    private val dynamicChainDataHolder: DynamicChainDataHolder
) : ViewModel() {
    private val state = MutableStateFlow(ChainDetailUiState())
    val uiState: StateFlow<ChainDetailUiState> = state
    
    private var updateJob: Job? = null
    private var updateIntervalMs: Long = 8_000L

    fun load(chain: Chain, network: NetworkType) {
        val session = sessionRepository.observe().value
        val walletAddress = session.getAddressForChain(chain)
        state.update { it.copy(chain = chain, network = network, walletAddress = walletAddress) }
        // Cancel previous update job if any
        updateJob?.cancel()
        // Load static data from cache first
        loadStaticData(chain)
        
        // Check if we have fresh dynamic data from dashboard
        val cachedDynamicData = dynamicChainDataHolder.getChainData(chain)
        if (cachedDynamicData != null && dynamicChainDataHolder.isDataFresh()) {
            // Use cached dynamic data - no network fetch needed
            state.update {
                it.copy(
                    currentPriceUsd = cachedDynamicData.currentPriceUsd,
                    priceChangePct24h = cachedDynamicData.priceChangePct24h,
                    networkLatencyMs = cachedDynamicData.networkLatencyMs,
                    networkSpeedLabel = cachedDynamicData.networkSpeedLabel,
                    sparkline = cachedDynamicData.sparkline,
                    logoUrl = cachedDynamicData.logoUrl,
                    marketCapUsd = cachedDynamicData.marketCapUsd,
                    volume24hUsd = cachedDynamicData.volume24hUsd
                )
            }
            // Still need to fetch token balances
            refreshChainData(skipPriceData = true)
        } else {
            // Start initial load of dynamic data
            refreshChainData()
        }
        // Start periodic updates (dynamic data only)
        startPeriodicUpdates()
    }
    
    private fun loadStaticData(chain: Chain) {
        val cached = staticDataCache.getChainStaticData(chain)
        if (cached != null) {
            // Use cached static data
            state.update {
                it.copy(
                    marketCapUsd = cached.marketCapUsd,
                    volume24hUsd = cached.volume24hUsd,
                    logoUrl = cached.logoUrl
                )
            }
        } else {
            // Will fetch from API on first refresh
            viewModelScope.launch {
                try {
                    val coinId = priceRepository.mapChainToCoinId(chain)
                    val market = priceRepository.getMarketData(listOf(coinId)).firstOrNull()
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
                        state.update {
                            it.copy(
                                marketCapUsd = staticData.marketCapUsd,
                                volume24hUsd = staticData.volume24hUsd,
                                logoUrl = staticData.logoUrl
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Failed to load static data
                }
            }
        }
    }
    
    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                delay(updateIntervalMs)
                refreshChainData(isManualRefresh = false)
            }
        }
    }
    
    private fun refreshChainData(isManualRefresh: Boolean = false, skipPriceData: Boolean = false) {
        val currentChain = state.value.chain
        val currentNetwork = state.value.network
        val currentState = state.value
        val session = sessionRepository.observe().value
        val scope = if (currentChain.isEvm) session.evmScope() else session.solanaScope()
        viewModelScope.launch {
            // Don't clear existing data - preserve it during refresh
            state.update { it.copy(isLoading = true, isManualRefreshing = isManualRefresh, error = null) }
            try {
                // Fetch portfolio (dynamic data: balances, tokens)
                var latencyMs = 0L
                var portfolioResult: com.elementa.wallet.domain.model.PortfolioResult? = null
                latencyMs = measureTimeMillis {
                    portfolioResult = walletEngine.fetchPortfolio(scope, currentChain, currentNetwork)
                }
                
                // Fetch market data only if not skipped (price, change, sparkline)
                val sparkline: List<Double>
                val price: Double
                val priceChange: Double
                
                if (skipPriceData) {
                    // Use existing state values (already populated from DynamicChainDataHolder)
                    sparkline = currentState.sparkline
                    price = currentState.currentPriceUsd
                    priceChange = currentState.priceChangePct24h
                } else {
                    val coinId = priceRepository.mapChainToCoinId(currentChain)
                    val market = priceRepository.getMarketData(listOf(coinId)).firstOrNull()
                    sparkline = market?.sparkline7d?.price ?: currentState.sparkline
                    price = market?.currentPrice ?: currentState.currentPriceUsd
                    priceChange = market?.priceChange24h ?: currentState.priceChangePct24h
                }

                updateIntervalMs = (latencyMs * 3).coerceIn(5_000L, 15_000L)

                state.update {
                    it.copy(
                        // Dynamic data - always updated
                        tokens = portfolioResult?.tokens ?: it.tokens,
                        currentPriceUsd = price,
                        priceChangePct24h = priceChange,
                        networkLatencyMs = latencyMs,
                        networkSpeedLabel = networkSpeedLabel(latencyMs),
                        sparkline = sparkline,
                        // Static data - keep existing (from cache)
                        // marketCapUsd, volume24hUsd, logoUrl not updated here
                        isLoading = false,
                        isManualRefreshing = false
                    )
                }
            } catch (error: Throwable) {
                // Don't lose data on error - just clear loading flags
                state.update { it.copy(isLoading = false, isManualRefreshing = false, error = error.message) }
            }
        }
    }

    fun refresh() {
        refreshChainData(isManualRefresh = true)
    }

    private fun networkSpeedLabel(latencyMs: Long): String = when {
        latencyMs <= 350 -> "Fast"
        latencyMs <= 900 -> "Normal"
        else -> "Slow"
    }
    
    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }

    fun removeToken(address: String) {
        val current = state.value
        val session = sessionRepository.observe().value
        val scope = if (current.chain.isEvm) session.evmScope() else session.solanaScope()
        viewModelScope.launch {
            try {
                walletEngine.removeCustomToken(
                    RemoveTokenRequest(current.chain, current.network, address, scope)
                )
                load(current.chain, current.network)
            } catch (error: Throwable) {
                state.update { it.copy(error = error.message) }
            }
        }
    }
}
