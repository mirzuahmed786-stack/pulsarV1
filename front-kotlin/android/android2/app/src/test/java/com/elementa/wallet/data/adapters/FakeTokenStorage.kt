package com.elementa.wallet.data.adapters

import com.elementa.wallet.data.storage.StoredEvmToken
import com.elementa.wallet.data.storage.StoredSolanaToken
import com.elementa.wallet.data.storage.TokenStorage
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType

class FakeTokenStorage : TokenStorage {
    private val evmTokens = mutableMapOf<String, MutableList<StoredEvmToken>>()
    private val watched = mutableMapOf<String, MutableList<String>>()
    private val solanaTokens = mutableMapOf<String, MutableList<StoredSolanaToken>>()

    override suspend fun getCustomEvmTokens(scope: String, chain: Chain, network: NetworkType): List<StoredEvmToken> {
        return evmTokens[key(scope, chain, network)]?.toList().orEmpty()
    }

    override suspend fun saveCustomEvmTokens(scope: String, chain: Chain, network: NetworkType, tokens: List<StoredEvmToken>) {
        evmTokens[key(scope, chain, network)] = tokens.toMutableList()
    }

    override suspend fun getWatchedDefaultTokens(scope: String, chain: Chain, network: NetworkType): List<String> {
        return watched[key(scope, chain, network)]?.toList().orEmpty()
    }

    override suspend fun saveWatchedDefaultTokens(scope: String, chain: Chain, network: NetworkType, addresses: List<String>) {
        watched[key(scope, chain, network)] = addresses.toMutableList()
    }

    override suspend fun getCustomSolanaTokens(scope: String, network: NetworkType): List<StoredSolanaToken> {
        return solanaTokens[key(scope, Chain.SOLANA, network)]?.toList().orEmpty()
    }

    override suspend fun saveCustomSolanaTokens(scope: String, network: NetworkType, tokens: List<StoredSolanaToken>) {
        solanaTokens[key(scope, Chain.SOLANA, network)] = tokens.toMutableList()
    }

    private fun key(scope: String, chain: Chain, network: NetworkType): String {
        return "${scope.lowercase()}_${chain.name}_${network.name}"
    }
}
