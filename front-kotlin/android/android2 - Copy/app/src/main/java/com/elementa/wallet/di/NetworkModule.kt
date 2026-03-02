package com.elementa.wallet.di

import com.elementa.wallet.data.remote.BlockchainApi
import com.elementa.wallet.data.remote.coingecko.CoinGeckoApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideCoinGeckoApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): CoinGeckoApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(CoinGeckoApi::class.java)
    }

    /**
     * Etherscan API - provides Ethereum transaction data
     */
    @Provides
    @Singleton
    @BlockchainApiQualifier("etherscan")
    fun provideEtherscanApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): BlockchainApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.etherscan.io/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BlockchainApi::class.java)
    }

    /**
     * Blockchair API - provides Bitcoin and other UTXO chain data
     */
    @Provides
    @Singleton
    @BlockchainApiQualifier("blockchair")
    fun provideBlockchairApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): BlockchainApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.blockchair.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BlockchainApi::class.java)
    }

    /**
     * Solana RPC endpoint - provides Solana transaction data
     */
    @Provides
    @Singleton
    @BlockchainApiQualifier("solana")
    fun provideSolanaApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): BlockchainApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.mainnet-beta.solana.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BlockchainApi::class.java)
    }
}

/**
 * Qualifier to distinguish between different blockchain API implementations
 */
@Qualifier
annotation class BlockchainApiQualifier(val value: String)
