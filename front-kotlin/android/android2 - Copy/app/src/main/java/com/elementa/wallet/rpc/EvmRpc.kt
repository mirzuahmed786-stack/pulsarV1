package com.elementa.wallet.rpc

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import java.math.BigInteger

interface EvmRpc {
    suspend fun getBytecode(chain: Chain, network: NetworkType, address: String): String
    suspend fun readContract(chain: Chain, network: NetworkType, to: String, data: String): String
    suspend fun getChainId(chain: Chain, network: NetworkType): String
    suspend fun erc20Name(chain: Chain, network: NetworkType, address: String): String
    suspend fun erc20Symbol(chain: Chain, network: NetworkType, address: String): String
    suspend fun erc20Decimals(chain: Chain, network: NetworkType, address: String): Int
    suspend fun erc20BalanceOf(chain: Chain, network: NetworkType, address: String, owner: String): BigInteger
    suspend fun erc1155BalanceOf(chain: Chain, network: NetworkType, address: String, owner: String, id: Long): BigInteger
    suspend fun validateErc20Interface(chain: Chain, network: NetworkType, address: String): Boolean
    suspend fun getNativeBalance(chain: Chain, network: NetworkType, owner: String): BigInteger
}
