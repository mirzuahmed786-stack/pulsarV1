package com.elementa.wallet.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.elementa.wallet.domain.model.Chain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChainStaticData(
    val chainId: String,
    val name: String,
    val symbol: String,
    val logoUrl: String,
    val marketCapUsd: Double,
    val volume24hUsd: Double,
    val networkType: String,
    val lastUpdatedMs: Long
)

@Singleton
class StaticDataCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "wallet_static_data_cache",
        Context.MODE_PRIVATE
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    fun saveChainStaticData(chain: Chain, data: ChainStaticData) {
        val key = "chain_${chain.name}"
        val jsonString = json.encodeToString(data)
        prefs.edit().putString(key, jsonString).apply()
    }
    
    fun getChainStaticData(chain: Chain): ChainStaticData? {
        val key = "chain_${chain.name}"
        val jsonString = prefs.getString(key, null) ?: return null
        return try {
            val data = json.decodeFromString<ChainStaticData>(jsonString)
            // Check if cache is still valid
            val age = System.currentTimeMillis() - data.lastUpdatedMs
            if (age < CACHE_VALIDITY_MS) data else null
        } catch (e: Exception) {
            null
        }
    }
    
    fun hasValidCache(chain: Chain): Boolean {
        return getChainStaticData(chain) != null
    }
    
    fun getAllCachedChains(): List<ChainStaticData> {
        val allEntries = prefs.all
        return allEntries.entries.mapNotNull { entry ->
            if (entry.key.startsWith("chain_")) {
                try {
                    json.decodeFromString<ChainStaticData>(entry.value as String)
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    }
    
    fun clearCache() {
        prefs.edit().clear().apply()
    }
    
    fun clearChainCache(chain: Chain) {
        val key = "chain_${chain.name}"
        prefs.edit().remove(key).apply()
    }
    
    fun shouldRefreshStaticData(chain: Chain): Boolean {
        val cached = getChainStaticData(chain) ?: return true
        val age = System.currentTimeMillis() - cached.lastUpdatedMs
        // Refresh once per app session or every 24 hours
        return age >= CACHE_VALIDITY_MS
    }
}
