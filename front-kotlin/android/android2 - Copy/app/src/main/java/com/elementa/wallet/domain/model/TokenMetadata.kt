package com.elementa.wallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenMetadata(
    val name: String,
    val symbol: String,
    val decimals: Int
)
