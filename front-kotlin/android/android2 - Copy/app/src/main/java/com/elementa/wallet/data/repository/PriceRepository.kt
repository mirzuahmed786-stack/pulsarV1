package com.elementa.wallet.data.repository

import com.elementa.wallet.data.remote.coingecko.CoinGeckoApi
import com.elementa.wallet.data.remote.coingecko.CoinMarketResponse
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.util.WalletLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceRepository @Inject constructor(
    private val api: CoinGeckoApi
) {
    fun getMarketPrices(coinIds: List<String>): Flow<Map<String, Double>> = flow {
        try {
            val response = api.getSimplePrice(ids = coinIds.joinToString(","))
            val prices = response.mapValues { it.value["usd"] ?: 0.0 }
            emit(prices)
        } catch (e: Exception) {
            WalletLogger.logRepositoryError("PriceRepository", "getMarketPrices", e)
            emit(emptyMap())
        }
    }

    // Fetches market data with sparkline and logos for multiple coins.
    suspend fun getMarketData(coinIds: List<String>): List<CoinMarketResponse> {
        return try {
            api.getMarkets(ids = coinIds.joinToString(","))
        } catch (e: Exception) {
            WalletLogger.logRepositoryError("PriceRepository", "getMarketData", e)
            emptyList()
        }
    }

    // Lightweight price-only fallback (no sparkline/image).
    suspend fun getSimplePrices(coinIds: List<String>): Map<String, Double> {
        return try {
            val response = api.getSimplePrice(ids = coinIds.joinToString(","))
            response.mapValues { it.value["usd"] ?: 0.0 }
        } catch (e: Exception) {
            WalletLogger.logRepositoryError("PriceRepository", "getSimplePrices", e)
            emptyMap()
        }
    }

    suspend fun getSparklineData(coinId: String): List<Double> {
        return try {
            val response = api.getMarkets(ids = coinId)
            response.firstOrNull()?.sparkline7d?.price ?: emptyList()
        } catch (e: Exception) {
            WalletLogger.logRepositoryError("PriceRepository", "getSparklineData", e)
            emptyList()
        }
    }

    // Maps app chain to CoinGecko coin id.
    fun mapChainToCoinId(chain: Chain): String = when (chain) {
        Chain.ETHEREUM -> "ethereum"
        Chain.BITCOIN -> "bitcoin"
        Chain.SOLANA -> "solana"
        Chain.AVALANCHE -> "avalanche-2"
        Chain.POLYGON -> "polygon-pos"
        Chain.BSC -> "binancecoin"
        Chain.LOCALHOST -> "ethereum"
    }
}
