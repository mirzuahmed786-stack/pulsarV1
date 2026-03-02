package com.elementa.wallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AddTokenRequest(
    val chain: Chain,
    val network: NetworkType,
    val address: String,
    val walletScope: String,
    val manualMetadata: TokenMetadata? = null
)
