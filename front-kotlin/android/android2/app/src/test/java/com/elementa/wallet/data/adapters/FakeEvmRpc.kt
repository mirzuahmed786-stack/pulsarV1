package com.elementa.wallet.data.adapters

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.rpc.EvmRpc
import java.math.BigInteger

class FakeEvmRpc : EvmRpc {
    var bytecode: String = "0x1234"
    var erc20Valid: Boolean = true
    var name: String = "Token"
    var symbol: String = "TKN"
    var decimals: Int = 18
    var metadataError: Throwable? = null
    var balance: BigInteger = BigInteger.ZERO
    var erc1155Balance: BigInteger = BigInteger.ZERO

    override suspend fun getBytecode(chain: Chain, network: NetworkType, address: String): String = bytecode

    override suspend fun readContract(chain: Chain, network: NetworkType, to: String, data: String): String {
        return "0x"
    }

    override suspend fun getChainId(chain: Chain, network: NetworkType): String = "0x1"

    override suspend fun erc20Name(chain: Chain, network: NetworkType, address: String): String {
        metadataError?.let { throw it }
        return name
    }

    override suspend fun erc20Symbol(chain: Chain, network: NetworkType, address: String): String {
        metadataError?.let { throw it }
        return symbol
    }

    override suspend fun erc20Decimals(chain: Chain, network: NetworkType, address: String): Int {
        metadataError?.let { throw it }
        return decimals
    }

    override suspend fun erc20BalanceOf(chain: Chain, network: NetworkType, address: String, owner: String): BigInteger {
        return balance
    }

    override suspend fun erc1155BalanceOf(chain: Chain, network: NetworkType, address: String, owner: String, id: Long): BigInteger {
        return erc1155Balance
    }

    override suspend fun validateErc20Interface(chain: Chain, network: NetworkType, address: String): Boolean = erc20Valid
}
