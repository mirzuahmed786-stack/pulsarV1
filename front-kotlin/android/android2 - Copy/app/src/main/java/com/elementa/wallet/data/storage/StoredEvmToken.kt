package com.elementa.wallet.data.storage

import com.elementa.wallet.domain.model.TokenSource
import com.elementa.wallet.domain.model.TokenStandard
import kotlinx.serialization.Serializable

@Serializable
data class StoredEvmToken(
    val address: String,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val chain: String,
    val network: String,
    val isVerified: Boolean = false,
    val source: TokenSource? = null,
    val tokenStandard: TokenStandard = TokenStandard.ERC20,
    val tokenId: Long? = null
)
