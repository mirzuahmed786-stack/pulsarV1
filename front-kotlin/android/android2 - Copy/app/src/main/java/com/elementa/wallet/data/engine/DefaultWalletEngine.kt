package com.elementa.wallet.data.engine

import com.elementa.wallet.data.adapters.EvmTokenAdapter
import com.elementa.wallet.data.adapters.SolanaTokenAdapter
import com.elementa.wallet.domain.engine.WalletEngine
import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.PortfolioResult
import com.elementa.wallet.domain.model.RemoveTokenRequest
import com.elementa.wallet.domain.model.TokenAsset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWalletEngine @Inject constructor(
    private val evmAdapter: EvmTokenAdapter,
    private val solanaAdapter: SolanaTokenAdapter
) : WalletEngine {
    override suspend fun addCustomToken(request: AddTokenRequest): TokenAsset {
        return if (request.chain.isEvm) {
            evmAdapter.addCustomToken(request)
        } else {
            solanaAdapter.addCustomToken(request)
        }
    }

    override suspend fun removeCustomToken(request: RemoveTokenRequest) {
        if (request.chain.isEvm) {
            evmAdapter.removeCustomToken(request.walletScope, request.chain, request.network, request.address)
        } else {
            solanaAdapter.removeCustomToken(request.walletScope, request.network, request.address)
        }
    }

    override suspend fun getAllTokens(
        walletScope: String,
        chains: List<Chain>,
        networks: Map<Chain, NetworkType>
    ): List<TokenAsset> {
        val assets = mutableListOf<TokenAsset>()
        for (chain in chains) {
            val network = networks[chain] ?: NetworkType.TESTNET
            val portfolio = fetchPortfolio(walletScope, chain, network)
            assets.addAll(portfolio.tokens)
        }
        return assets
    }

    override suspend fun fetchPortfolio(
        walletScope: String,
        chain: Chain,
        network: NetworkType
    ): PortfolioResult {
        val tokens = if (chain.isEvm) {
            evmAdapter.getPortfolio(walletScope, chain, network, walletScope)
        } else {
            solanaAdapter.getPortfolio(walletScope, network, walletScope)
        }
        return PortfolioResult(chain, network, tokens)
    }
}
