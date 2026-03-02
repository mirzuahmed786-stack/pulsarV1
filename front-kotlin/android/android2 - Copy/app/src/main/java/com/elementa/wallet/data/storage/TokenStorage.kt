package com.elementa.wallet.data.storage

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType

interface TokenStorage {
    suspend fun getCustomEvmTokens(scope: String, chain: Chain, network: NetworkType): List<StoredEvmToken>
    suspend fun saveCustomEvmTokens(scope: String, chain: Chain, network: NetworkType, tokens: List<StoredEvmToken>)

    suspend fun getWatchedDefaultTokens(scope: String, chain: Chain, network: NetworkType): List<String>
    suspend fun saveWatchedDefaultTokens(scope: String, chain: Chain, network: NetworkType, addresses: List<String>)

    suspend fun getCustomSolanaTokens(scope: String, network: NetworkType): List<StoredSolanaToken>
    suspend fun saveCustomSolanaTokens(scope: String, network: NetworkType, tokens: List<StoredSolanaToken>)
}
