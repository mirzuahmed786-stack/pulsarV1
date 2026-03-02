package com.elementa.wallet.data.cache

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.ui.state.ChainMarketUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory holder for dynamic chain data that can be shared between
 * Dashboard (HomeViewModel) and Detail screens (ChainDetailViewModel).
 * 
 * This prevents re-fetching data when navigating from dashboard to detail,
 * while still allowing manual refresh to update both views.
 */
@Singleton
class DynamicChainDataHolder @Inject constructor() {
    
    private val _chainData = MutableStateFlow<Map<Chain, ChainMarketUi>>(emptyMap())
    val chainData: StateFlow<Map<Chain, ChainMarketUi>> = _chainData
    
    private val _lastRefreshTimeMs = MutableStateFlow(0L)
    val lastRefreshTimeMs: StateFlow<Long> = _lastRefreshTimeMs
    
    /**
     * Update data for a single chain
     */
    fun updateChainData(chain: Chain, data: ChainMarketUi) {
        _chainData.update { current ->
            current + (chain to data)
        }
        _lastRefreshTimeMs.value = System.currentTimeMillis()
    }
    
    /**
     * Update data for multiple chains at once (used by HomeViewModel)
     */
    fun updateAllChainData(dataList: List<ChainMarketUi>) {
        _chainData.update { _ ->
            dataList.associateBy { it.chain }
        }
        _lastRefreshTimeMs.value = System.currentTimeMillis()
    }
    
    /**
     * Get cached data for a specific chain
     */
    fun getChainData(chain: Chain): ChainMarketUi? {
        return _chainData.value[chain]
    }
    
    /**
     * Check if we have cached data for a chain
     */
    fun hasDataForChain(chain: Chain): Boolean {
        return _chainData.value.containsKey(chain)
    }
    
    /**
     * Check if cached data is fresh (within threshold)
     */
    fun isDataFresh(maxAgeMs: Long = 30_000L): Boolean {
        val age = System.currentTimeMillis() - _lastRefreshTimeMs.value
        return age < maxAgeMs
    }
    
    /**
     * Clear all cached data
     */
    fun clearAll() {
        _chainData.value = emptyMap()
        _lastRefreshTimeMs.value = 0L
    }
}
