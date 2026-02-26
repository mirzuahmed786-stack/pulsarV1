package com.elementa.wallet.ui.state

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.model.TokenAsset

data class AssetsUiState(
    val selectedChain: Chain? = null,
    val network: NetworkType = NetworkType.MAINNET,
    val tokens: List<TokenAsset> = emptyList(),
    val totalBalanceFormatted: String = "$0.00",
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalValueUsd: Double = 0.0,
    val evmAddress: String = "",
    val solanaAddress: String = ""
) {
    private fun nativeSymbol(chain: Chain): String = when (chain) {
        Chain.BITCOIN -> "BTC"
        Chain.ETHEREUM -> "ETH"
        Chain.SOLANA -> "SOL"
        Chain.BSC -> "BNB"
        Chain.AVALANCHE -> "AVAX"
        Chain.POLYGON -> "POL"
        Chain.LOCALHOST -> "ETH"
    }

    private fun isLikelyCoin(token: TokenAsset): Boolean {
        val symbolMatch = token.symbol.equals(nativeSymbol(token.chain), ignoreCase = true)
        val nameMatch = token.name.equals(token.chain.name, ignoreCase = true)
        return symbolMatch || nameMatch
    }

    val visibleTokens: List<TokenAsset>
        get() = if (selectedChain == null) tokens else tokens.filter { it.chain == selectedChain }

    val visibleCoins: List<TokenAsset>
        get() = visibleTokens.filter { isLikelyCoin(it) }

    val visibleChainTokens: List<TokenAsset>
        get() = visibleTokens.filterNot { isLikelyCoin(it) }
}
