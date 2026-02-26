package com.elementa.wallet.data.config

import com.elementa.wallet.domain.model.TokenSource

object SolanaConfig {
    data class KnownMint(
        val address: String,
        val symbol: String,
        val name: String,
        val decimals: Int,
        val source: TokenSource = TokenSource.KNOWN_ADDRESS
    )

    val knownMints: Map<String, KnownMint> = mapOf(
        "epjfwdd5aufqssqem2qn1xzybapc8g4weggkzwytdt1v" to KnownMint(
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", "USDC", "USD Coin", 6
        ),
        "es9vmfrzacermjfrf4h2fyd4kconky11mcce8benwnyb" to KnownMint(
            "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB", "USDT", "Tether USD", 6
        )
    )

    fun knownMint(address: String): KnownMint? {
        return knownMints[address.lowercase()]
    }
}
