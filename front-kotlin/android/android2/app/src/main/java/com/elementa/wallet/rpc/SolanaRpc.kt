package com.elementa.wallet.rpc

import com.elementa.wallet.domain.model.NetworkType
import java.math.BigInteger

interface SolanaRpc {
    suspend fun getTokenSupply(network: NetworkType, mint: String): SolanaRpcService.TokenSupplyResult
    suspend fun getHealth(network: NetworkType): Boolean
    suspend fun getTokenAccountsByOwner(network: NetworkType, owner: String): List<SolanaRpcService.SolanaTokenAccount>
    
    /**
     * Get native SOL balance for an address
     * @return balance in lamports (1 SOL = 1e9 lamports)
     */
    suspend fun getNativeBalance(network: NetworkType, address: String): BigInteger
}
