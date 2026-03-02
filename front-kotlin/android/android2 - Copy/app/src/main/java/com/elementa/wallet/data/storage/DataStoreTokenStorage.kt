package com.elementa.wallet.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.security.CryptoManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "token_store")

@Singleton
class DataStoreTokenStorage @Inject constructor(
    private val context: Context,
    private val cryptoManager: CryptoManager,
    private val json: Json
) : TokenStorage {

    override suspend fun getCustomEvmTokens(
        scope: String,
        chain: Chain,
        network: NetworkType
    ): List<StoredEvmToken> {
        val scopedKey = customTokenKey(scope, chain, network)
        val legacyKey = legacyCustomTokenKey(chain, network)
        if (!scope.equals("global", true)) {
            migrateLegacyKey(scopedKey, legacyKey)
        }
        return readList(scopedKey, ListSerializer(StoredEvmToken.serializer()))
    }

    override suspend fun saveCustomEvmTokens(
        scope: String,
        chain: Chain,
        network: NetworkType,
        tokens: List<StoredEvmToken>
    ) {
        val scopedKey = customTokenKey(scope, chain, network)
        writeList(scopedKey, tokens, ListSerializer(StoredEvmToken.serializer()))
    }

    override suspend fun getWatchedDefaultTokens(
        scope: String,
        chain: Chain,
        network: NetworkType
    ): List<String> {
        val scopedKey = watchedTokenKey(scope, chain, network)
        val legacyKey = legacyWatchedTokenKey(chain, network)
        if (!scope.equals("global", true)) {
            migrateLegacyKey(scopedKey, legacyKey)
        }
        return readList(scopedKey, ListSerializer(String.serializer()))
            .map { it.lowercase() }
    }

    override suspend fun saveWatchedDefaultTokens(
        scope: String,
        chain: Chain,
        network: NetworkType,
        addresses: List<String>
    ) {
        val scopedKey = watchedTokenKey(scope, chain, network)
        val unique = addresses.map { it.lowercase() }.distinct()
        writeList(scopedKey, unique, ListSerializer(String.serializer()))
    }

    override suspend fun getCustomSolanaTokens(
        scope: String,
        network: NetworkType
    ): List<StoredSolanaToken> {
        val scopedKey = solanaCustomTokenKey(scope, network)
        val legacyKey = solanaLegacyCustomTokenKey(network)
        if (!scope.equals("global", true)) {
            migrateLegacyKey(scopedKey, legacyKey)
        }
        return readList(scopedKey, ListSerializer(StoredSolanaToken.serializer()))
    }

    override suspend fun saveCustomSolanaTokens(
        scope: String,
        network: NetworkType,
        tokens: List<StoredSolanaToken>
    ) {
        val scopedKey = solanaCustomTokenKey(scope, network)
        writeList(scopedKey, tokens, ListSerializer(StoredSolanaToken.serializer()))
    }

    private suspend fun migrateLegacyKey(scoped: Preferences.Key<String>, legacy: Preferences.Key<String>) {
        val prefs = context.tokenDataStore.data.first()
        if (prefs.contains(scoped) || !prefs.contains(legacy)) return
        val legacyValue = prefs[legacy] ?: return
        context.tokenDataStore.edit { it[scoped] = legacyValue }
    }

    private suspend fun <T> readList(
        key: Preferences.Key<String>,
        serializer: kotlinx.serialization.KSerializer<List<T>>
    ): List<T> {
        val prefs = context.tokenDataStore.data.first()
        val payload = prefs[key] ?: return emptyList()
        val decrypted = runCatching { cryptoManager.decrypt(payload) }.getOrNull() ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, decrypted) }.getOrElse { emptyList() }
    }

    private suspend fun <T> writeList(
        key: Preferences.Key<String>,
        value: List<T>,
        serializer: kotlinx.serialization.KSerializer<List<T>>
    ) {
        val encoded = json.encodeToString(serializer, value)
        val encrypted = cryptoManager.encrypt(encoded)
        context.tokenDataStore.edit { it[key] = encrypted }
    }

    private fun customTokenKey(scope: String, chain: Chain, network: NetworkType) =
        stringPreferencesKey("custom_tokens_${scope.lowercase()}_${Chain.toWire(chain)}_${NetworkType.toWire(network)}")

    private fun legacyCustomTokenKey(chain: Chain, network: NetworkType) =
        stringPreferencesKey("custom_tokens_${Chain.toWire(chain)}_${NetworkType.toWire(network)}")

    private fun watchedTokenKey(scope: String, chain: Chain, network: NetworkType) =
        stringPreferencesKey("watched_default_tokens_${scope.lowercase()}_${Chain.toWire(chain)}_${NetworkType.toWire(network)}")

    private fun legacyWatchedTokenKey(chain: Chain, network: NetworkType) =
        stringPreferencesKey("watched_default_tokens_${Chain.toWire(chain)}_${NetworkType.toWire(network)}")

    private fun solanaCustomTokenKey(scope: String, network: NetworkType) =
        stringPreferencesKey("custom_tokens_solana_${scope.lowercase()}_${NetworkType.toWire(network)}")

    private fun solanaLegacyCustomTokenKey(network: NetworkType) =
        stringPreferencesKey("custom_tokens_solana_${NetworkType.toWire(network)}")
}
