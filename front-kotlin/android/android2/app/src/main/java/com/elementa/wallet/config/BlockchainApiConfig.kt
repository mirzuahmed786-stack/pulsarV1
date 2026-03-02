package com.elementa.wallet.config

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * API Configuration - Centralized management of blockchain API keys
 * 
 * To configure API keys, you can:
 * 1. Set them in BuildConfig (via gradle) - RECOMMENDED for production
 * 2. Use environment variables
 * 3. Load from secure storage (Keystore)
 * 4. Use default/free tier endpoints
 */
data class BlockchainApiConfig(
    val etherscanApiKey: String = BuildConfigKeys.ETHERSCAN_API_KEY,
    val polygonscanApiKey: String = BuildConfigKeys.POLYGONSCAN_API_KEY,
    val blockchainRpcEndpoints: Map<String, String> = mapOf(
        "ethereum" to "https://eth.llamarpc.com",
        "avalanche" to "https://avax.llamarpc.com",
        "polygon" to "https://polygon.llamarpc.com",
        "solana" to "https://api.mainnet-beta.solana.com"
    )
)

/**
 * Build-time configuration for API keys
 * These values should be set in BuildVariants or BuildConfig
 */
object BuildConfigKeys {
    // Get from BuildConfig or set to empty string for free tier
    // To set: Add these to your build.gradle:
    // buildTypes {
    //   release {
    //     buildConfigField "String", "ETHERSCAN_API_KEY", "\"your_key_here\""
    //     buildConfigField "String", "POLYGONSCAN_API_KEY", "\"your_key_here\""
    //   }
    // }
    
    const val ETHERSCAN_API_KEY = ""  // Will use free tier if empty
    const val POLYGONSCAN_API_KEY = ""  // Will use free tier if empty
    
    // Free tier rate limits (as of Feb 2026):
    // - Etherscan: 5 calls/sec, 100,000 calls/day
    // - PolygonScan: 5 calls/sec, 10,000 calls/day
    // - CoinGecko: 50 calls/min (free) or more with Pro API key
    
    // Register your free API keys at:
    // - https://etherscan.io/apis
    // - https://polygonscan.com/apis
    // - https://www.coingecko.com/api
}

/**
 * Dagger-Hilt module for providing API configuration
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiConfigModule {
    
    @Singleton
    @Provides
    fun provideBlockchainApiConfig(): BlockchainApiConfig {
        return BlockchainApiConfig(
            etherscanApiKey = BuildConfigKeys.ETHERSCAN_API_KEY,
            polygonscanApiKey = BuildConfigKeys.POLYGONSCAN_API_KEY
        )
    }
}

/**
 * Extension function to get API key with fallback to free tier
 */
fun BlockchainApiConfig.getEthereumApiKey(): String {
    return if (etherscanApiKey.isNotEmpty()) etherscanApiKey else ""
}

fun BlockchainApiConfig.getPolygonApiKey(): String {
    return if (polygonscanApiKey.isNotEmpty()) polygonscanApiKey else ""
}

/**
 * Get RPC endpoint for a chain
 */
fun BlockchainApiConfig.getRpcEndpoint(chainId: String): String {
    return blockchainRpcEndpoints[chainId] ?: "https://eth.llamarpc.com"
}
