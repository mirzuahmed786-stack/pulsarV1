package com.elementa.wallet.ffi

import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.PortfolioResult
import com.elementa.wallet.domain.model.RemoveTokenRequest
import com.elementa.wallet.domain.model.TokenAsset
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType

/**
 * Boundary for Rust wallet engine bindings (UniFFI/JNI).
 * Implementations should keep private key material inside the Rust layer and
 * only return encrypted blobs or signing results.
 */
interface WalletEngineBridge {
    suspend fun addCustomToken(request: AddTokenRequest): TokenAsset
    suspend fun removeCustomToken(request: RemoveTokenRequest)
    suspend fun getAllTokens(
        walletScope: String,
        chains: List<Chain>,
        networks: Map<Chain, NetworkType>
    ): List<TokenAsset>
    suspend fun fetchPortfolio(
        walletScope: String,
        chain: Chain,
        network: NetworkType
    ): PortfolioResult
}
