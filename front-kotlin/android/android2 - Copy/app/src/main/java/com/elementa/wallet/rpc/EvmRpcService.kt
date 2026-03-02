package com.elementa.wallet.rpc

import com.elementa.wallet.data.config.RpcConfig
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.util.WalletLogger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvmRpcService @Inject constructor(
    private val jsonRpcClient: JsonRpcClient
) : EvmRpc {
    override suspend fun getBytecode(chain: Chain, network: NetworkType, address: String): String {
        return callFirst(chain, network) { url ->
            jsonRpcClient.call(url, "eth_getCode", JsonArray(listOf(JsonPrimitive(address), JsonPrimitive("latest"))))
        }.toString().trim('"')
    }

    override suspend fun readContract(chain: Chain, network: NetworkType, to: String, data: String): String {
        return callFirst(chain, network) { url ->
            jsonRpcClient.call(
                url,
                "eth_call",
                JsonArray(listOf(
                    JsonRpcObjects.callObject(to, data),
                    JsonPrimitive("latest")
                ))
            )
        }.toString().trim('"')
    }

    override suspend fun getChainId(chain: Chain, network: NetworkType): String {
        return callFirst(chain, network) { url ->
            jsonRpcClient.call(url, "eth_chainId", JsonArray(emptyList()))
        }.toString().trim('"')
    }

    override suspend fun erc20Name(chain: Chain, network: NetworkType, address: String): String {
        val data = "0x06fdde03"
        val result = readContract(chain, network, address, data)
        return AbiEncoder.decodeString(result)
    }

    override suspend fun erc20Symbol(chain: Chain, network: NetworkType, address: String): String {
        val data = "0x95d89b41"
        val result = readContract(chain, network, address, data)
        return AbiEncoder.decodeString(result)
    }

    override suspend fun erc20Decimals(chain: Chain, network: NetworkType, address: String): Int {
        val data = "0x313ce567"
        val result = readContract(chain, network, address, data)
        return AbiEncoder.decodeUint256(result).toInt()
    }

    override suspend fun erc20BalanceOf(chain: Chain, network: NetworkType, address: String, owner: String): BigInteger {
        val data = "0x70a08231" + AbiEncoder.encodeAddress(owner)
        val result = readContract(chain, network, address, data)
        return AbiEncoder.decodeUint256(result)
    }

    override suspend fun erc1155BalanceOf(chain: Chain, network: NetworkType, address: String, owner: String, id: Long): BigInteger {
        val data = "0x00fdd58e" + AbiEncoder.encodeAddress(owner) + AbiEncoder.encodeUint256(id)
        val result = readContract(chain, network, address, data)
        return AbiEncoder.decodeUint256(result)
    }

    override suspend fun validateErc20Interface(chain: Chain, network: NetworkType, address: String): Boolean {
        return try {
            val owner = ZERO_ADDRESS
            erc20Decimals(chain, network, address)
            erc20Symbol(chain, network, address)
            erc20Name(chain, network, address)
            erc20BalanceOf(chain, network, address, owner)
            true
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun getNativeBalance(chain: Chain, network: NetworkType, owner: String): java.math.BigInteger {
        val result = callFirst(chain, network) { url ->
            jsonRpcClient.call(url, "eth_getBalance", JsonArray(listOf(JsonPrimitive(owner), JsonPrimitive("latest"))))
        }.toString().trim('"')
        return AbiEncoder.decodeUint256(result)
    }

    private suspend fun callFirst(
        chain: Chain,
        network: NetworkType,
        block: suspend (String) -> JsonElement
    ): JsonElement {
        val urls = RpcConfig.getRpcUrls(chain, network)
        if (urls.isEmpty()) {
            throw RpcException("No RPC endpoints configured for ${chain.name} ${network.name}")
        }
        
        val errors = mutableListOf<Pair<String, Throwable>>()
        for (url in urls) {
            try {
                return block(url)
            } catch (error: Throwable) {
                WalletLogger.logRpcError("${chain.name}_${network.name}", url, error)
                errors.add(url to error)
            }
        }
        
        // All URLs failed - provide detailed error message
        val errorSummary = errors.joinToString("; ") { (url, error) ->
            "${url.substringAfter("://").take(30)}: ${error.message?.take(50)}"
        }
        throw RpcException("All RPC endpoints failed for ${chain.name}: $errorSummary")
    }

    companion object {
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}
