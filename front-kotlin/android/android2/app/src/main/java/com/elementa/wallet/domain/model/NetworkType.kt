package com.elementa.wallet.domain.model

enum class NetworkType {
    MAINNET,
    TESTNET;

    companion object {
        fun fromWire(value: String): NetworkType = when (value.lowercase()) {
            "mainnet" -> MAINNET
            else -> TESTNET
        }

        fun toWire(value: NetworkType): String = when (value) {
            MAINNET -> "mainnet"
            TESTNET -> "testnet"
        }
    }
}
