package com.elementa.wallet.domain.model

data class WalletSession(
    val evmAddress: String = "",
    val solanaAddress: String = "",
    val bitcoinAddress: String = ""
) {
    fun evmScope(): String = evmAddress.lowercase().ifBlank { "global" }
    fun solanaScope(): String = solanaAddress.lowercase().ifBlank { "global" }
    fun bitcoinScope(): String = bitcoinAddress.lowercase().ifBlank { "global" }
    
    /**
     * Get the wallet address for a specific chain
     */
    fun getAddressForChain(chain: Chain): String = when (chain) {
        Chain.BITCOIN -> bitcoinAddress
        Chain.SOLANA -> solanaAddress
        Chain.ETHEREUM, Chain.BSC, Chain.AVALANCHE, Chain.POLYGON, Chain.LOCALHOST -> evmAddress
    }
}
