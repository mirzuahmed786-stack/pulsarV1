package com.elementa.wallet.domain.model

enum class Chain(val id: String, val isEvm: Boolean) {
    ETHEREUM("ethereum", true),
    BSC("binance-smart-chain", true),
    POLYGON("polygon-pos", true),
    AVALANCHE("avalanche", true),
    SOLANA("solana", false),
    BITCOIN("bitcoin", false),
    LOCALHOST("localhost", true);

    companion object {
        fun fromWire(value: String): Chain = when (value.lowercase()) {
            "ethereum" -> ETHEREUM
            "bsc", "binance-smart-chain", "bnb" -> BSC
            "polygon", "polygon-pos" -> POLYGON
            "avalanche" -> AVALANCHE
            "solana" -> SOLANA
            "bitcoin" -> BITCOIN
            "localhost" -> LOCALHOST
            else -> ETHEREUM
        }

        fun toWire(chain: Chain): String = chain.id
    }
}
