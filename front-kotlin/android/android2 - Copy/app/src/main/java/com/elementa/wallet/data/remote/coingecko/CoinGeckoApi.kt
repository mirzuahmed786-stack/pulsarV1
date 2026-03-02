package com.elementa.wallet.data.remote.coingecko

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoApi {
    @GET("coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") currency: String = "usd",
        @Query("ids") ids: String?,
        @Query("sparkline") sparkline: Boolean = true
    ): List<CoinMarketResponse>

    @GET("simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_24hr_change") includeChange: Boolean = true
    ): Map<String, Map<String, Double>>
}

@Serializable
data class CoinMarketResponse(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    @kotlinx.serialization.SerialName("current_price") val currentPrice: Double,
    @kotlinx.serialization.SerialName("price_change_percentage_24h") val priceChange24h: Double,
    @kotlinx.serialization.SerialName("market_cap") val marketCap: Double? = null,
    @kotlinx.serialization.SerialName("total_volume") val totalVolume: Double? = null,
    @kotlinx.serialization.SerialName("circulating_supply") val circulatingSupply: Double? = null,
    @kotlinx.serialization.SerialName("ath") val ath: Double? = null,
    @kotlinx.serialization.SerialName("sparkline_in_7d") val sparkline7d: SparklineData?
)

@Serializable
data class SparklineData(
    val price: List<Double>
)
