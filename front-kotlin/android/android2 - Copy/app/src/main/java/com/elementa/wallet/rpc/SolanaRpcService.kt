package com.elementa.wallet.rpc

import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.util.WalletLogger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaRpcService @Inject constructor(
    private val jsonRpcClient: JsonRpcClient
) : SolanaRpc {
    override suspend fun getTokenSupply(network: NetworkType, mint: String): TokenSupplyResult {
        val url = rpcUrl(network)
        val result = jsonRpcClient.call(
            url,
            "getTokenSupply",
            JsonArray(listOf(JsonPrimitive(mint)))
        )
        val obj = result as? JsonObject ?: throw RpcException("Invalid token supply")
        val value = obj["value"] as? JsonObject ?: throw RpcException("Invalid token supply")
        val decimals = (value["decimals"] as? JsonPrimitive)?.contentOrNull?.toInt() ?: throw RpcException("Invalid decimals")
        return TokenSupplyResult(decimals)
    }

    override suspend fun getHealth(network: NetworkType): Boolean {
        val url = rpcUrl(network)
        val result = jsonRpcClient.call(
            url,
            "getLatestBlockhash",
            JsonArray(emptyList())
        )
        return result is JsonObject
    }

    override suspend fun getTokenAccountsByOwner(network: NetworkType, owner: String): List<SolanaTokenAccount> {
        val url = rpcUrl(network)
        val result = jsonRpcClient.call(
            url,
            "getTokenAccountsByOwner",
            JsonArray(
                listOf(
                    JsonPrimitive(owner),
                    JsonObject(mapOf("programId" to JsonPrimitive("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"))),
                    JsonObject(mapOf("encoding" to JsonPrimitive("jsonParsed")))
                )
            )
        )
        val root = result as? JsonObject ?: return emptyList()
        val accounts = root["value"] as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return accounts.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val account = obj["account"] as? JsonObject ?: return@mapNotNull null
            val data = account["data"] as? JsonObject ?: return@mapNotNull null
            val parsed = data["parsed"] as? JsonObject ?: return@mapNotNull null
            val info = parsed["info"] as? JsonObject ?: return@mapNotNull null
            val tokenAmount = info["tokenAmount"] as? JsonObject ?: return@mapNotNull null
            val mint = (info["mint"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val uiAmount = (tokenAmount["uiAmountString"] as? JsonPrimitive)?.contentOrNull ?: "0"
            val decimals = (tokenAmount["decimals"] as? JsonPrimitive)?.jsonPrimitive?.contentOrNull?.toInt() ?: 0
            SolanaTokenAccount(mint, uiAmount, decimals)
        }
    }

    /**
     * Fetch native SOL balance for an address using getBalance RPC
     * @return balance in lamports (1 SOL = 1e9 lamports)
     */
    override suspend fun getNativeBalance(network: NetworkType, address: String): BigInteger {
        return try {
            val url = rpcUrl(network)
            val result = jsonRpcClient.call(
                url,
                "getBalance",
                JsonArray(listOf(JsonPrimitive(address)))
            )
            val obj = result as? JsonObject
                ?: throw RpcException("Invalid getBalance response")
            val value = obj["value"]
            when (value) {
                is JsonPrimitive -> {
                    val lamports = value.contentOrNull?.toLongOrNull()
                        ?: throw RpcException("Invalid balance value")
                    BigInteger.valueOf(lamports)
                }
                else -> throw RpcException("Unexpected balance format")
            }
        } catch (e: RpcException) {
            throw e
        } catch (e: Exception) {
            WalletLogger.logRpcError("Solana_${network.name}", rpcUrl(network), e)
            throw RpcException("Failed to fetch Solana balance: ${e.message}")
        }
    }

    private fun rpcUrl(network: NetworkType): String {
        return if (network == NetworkType.MAINNET) {
            "https://api.mainnet-beta.solana.com"
        } else {
            "https://api.devnet.solana.com"
        }
    }

    data class TokenSupplyResult(val decimals: Int)

    data class SolanaTokenAccount(
        val mint: String,
        val uiAmountString: String,
        val decimals: Int
    )
}
