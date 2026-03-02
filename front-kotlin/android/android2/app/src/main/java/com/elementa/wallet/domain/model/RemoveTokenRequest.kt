package com.elementa.wallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoveTokenRequest(
    val chain: Chain,
    val network: NetworkType,
    val address: String,
    val walletScope: String
)
