package com.elementa.wallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PortfolioResult(
    val chain: Chain,
    val network: NetworkType,
    val tokens: List<TokenAsset>
)
