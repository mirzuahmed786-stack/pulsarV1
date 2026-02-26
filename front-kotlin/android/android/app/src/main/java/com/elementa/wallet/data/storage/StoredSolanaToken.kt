package com.elementa.wallet.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class StoredSolanaToken(
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val network: String
)
