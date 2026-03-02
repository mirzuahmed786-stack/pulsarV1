package com.elementa.wallet.data.adapters

import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.rpc.SolanaRpc
import com.elementa.wallet.rpc.SolanaRpcService

class FakeSolanaRpc : SolanaRpc {
    var decimals: Int = 6
    var accounts: List<SolanaRpcService.SolanaTokenAccount> = emptyList()

    override suspend fun getTokenSupply(network: NetworkType, mint: String): SolanaRpcService.TokenSupplyResult {
        return SolanaRpcService.TokenSupplyResult(decimals)
    }

    override suspend fun getHealth(network: NetworkType): Boolean = true

    override suspend fun getTokenAccountsByOwner(
        network: NetworkType,
        owner: String
    ): List<SolanaRpcService.SolanaTokenAccount> = accounts
}
