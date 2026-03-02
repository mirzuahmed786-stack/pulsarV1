package com.elementa.wallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenAsset(
    val address: String,
    val chain: Chain,
    val network: NetworkType,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val balance: String,
    val balanceInUsd: Double,
    val source: TokenSource? = null,
    val isCustom: Boolean = false,
    val isWatchedDefault: Boolean = false,
    val tokenStandard: TokenStandard = TokenStandard.ERC20,
    val tokenId: Long? = null
)
