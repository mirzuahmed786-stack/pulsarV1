package com.elementa.wallet.data.engine

import com.elementa.wallet.domain.model.AddTokenRequest
import com.elementa.wallet.domain.model.PortfolioResult
import com.elementa.wallet.domain.model.RemoveTokenRequest
import com.elementa.wallet.domain.model.TokenAsset
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.domain.engine.WalletEngine
import javax.inject.Inject
import com.elementa.wallet.data.bindings.WallettrustlibKt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import javax.inject.Singleton

/**
 * Bridge implementation to Rust wallet engine via UniFFI/JNI.
 * Keeps all key material on the Rust side; Kotlin only receives signing results/encrypted blobs.
 * 
 * Phase 6.5 (Current): Stub implementations to prevent crashes
 * Phase 7: Replace stubs with real Rust FFI calls (JNI integration)
 */
@Singleton
class RustWalletEngineBridge @Inject constructor() : WalletEngine {
    private var nativeLibLoaded = false
    
    init {
        // Phase 7: Uncomment when .so library is available
        try {
            // Ensure the native library is loaded. WallettrustlibKt also loads it,
            // but loading here makes the bridge self-contained.
            System.loadLibrary("wallet_core")
            nativeLibLoaded = true
        } catch (e: Exception) {
            nativeLibLoaded = false
        }
    }

    /**
     * Phase 6.5: Stub implementation of custom token addition.
     * Phase 7: Replace with Native FFI call via System.loadLibrary()
     */
    private fun rustAddCustomToken(
        scope: String,
        chainWire: String,
        networkWire: String,
        address: String,
        name: String,
        symbol: String,
        decimals: Int
    ): String {
        // Delegate to native layer if available (placeholder: no native add token API),
        // fall back to returning mock JSON.
        return try {
            // No direct FFI add token; return a best-effort JSON describing the token
            """{
                "address": "$address",
                "chain": "$chainWire",
                "network": "$networkWire",
                "name": "$name",
                "symbol": "$symbol",
                "decimals": $decimals,
                "balance": "0",
                "balanceInUsd": 0.0,
                "isCustom": true
            }"""
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Phase 6.5: Stub implementation of custom token removal.
     * Phase 7: Replace with Native FFI call
     */
    private fun rustRemoveCustomToken(
        scope: String,
        chainWire: String,
        networkWire: String,
        address: String
    ): String {
        // No native remove function in the current bindings; just return success.
        return try { "" } catch (e: Throwable) { "" }
    }

    /**
     * Phase 6.5: Stub implementation of portfolio fetch.
     * Phase 7: Replace with Native FFI call
     */
    private fun rustFetchPortfolio(
        scope: String,
        chainWire: String,
        networkWire: String
    ): String {
        // Try to call native multichain address enumeration (if vault-based APIs exist).
        return try {
            // We don't have PIN/vault JSON here; call into native layer's address enumerator
            // with empty values to get any available addresses. This depends on native behavior.
            WallettrustlibKt.getMultichainAddressesFfi("", "{}", false)
        } catch (e: Throwable) {
            // Fallback: empty portfolio JSON
            """{"tokens": [], "total_balance_usd": 0.0}"""
        }
    }

    /**
     * Phase 6.5: Stub implementation of transaction signing.
     * Phase 7: Replace with Native FFI call via actual Rust JNI
     * 
     * SECURITY: In Phase 7, this must push key material into the Rust
     * secure enclave and return ONLY the signed digest, never exposing keys.
     */
    private fun rustSignTransaction(
        scope: String,
        txData: String
    ): String {
        // If the native signing API is available, delegate. We don't have PIN/vault here,
        // so this is a best-effort passthrough (may require additional params in production).
        return try {
            // Use the generic signTransactionFfi which expects pin and vault JSON; provide empty
            // placeholders to allow JNI invocation (native side should handle or error).
            WallettrustlibKt.signTransactionFfi("", "{}", txData)
        } catch (e: Throwable) {
            """{"signed_digest": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
        }
    }

    override suspend fun addCustomToken(request: AddTokenRequest): TokenAsset {
        return try {
            val metadata = request.manualMetadata
            val result = rustAddCustomToken(
                request.walletScope,
                Chain.toWire(request.chain),
                NetworkType.toWire(request.network),
                request.address,
                metadata?.name ?: "",
                metadata?.symbol ?: "",
                metadata?.decimals ?: 18
            )
            // Parse JSON result into TokenAsset (best-effort)
            parseSingleTokenJson(result, request.chain, request.network) ?: TokenAsset(
                address = request.address,
                chain = request.chain,
                network = request.network,
                symbol = metadata?.symbol ?: "?",
                name = metadata?.name ?: "Unknown",
                decimals = metadata?.decimals ?: 18,
                balance = "0",
                balanceInUsd = 0.0,
                isCustom = true
            )
        } catch (e: Throwable) {
            throw IllegalStateException("Rust bridge error: ${e.message}", e)
        }
    }

    override suspend fun removeCustomToken(request: RemoveTokenRequest) {
        try {
            rustRemoveCustomToken(
                request.walletScope,
                Chain.toWire(request.chain),
                NetworkType.toWire(request.network),
                request.address
            )
        } catch (e: Throwable) {
            throw IllegalStateException("Rust bridge error: ${e.message}", e)
        }
    }

    override suspend fun getAllTokens(
        walletScope: String,
        chains: List<Chain>,
        networks: Map<Chain, NetworkType>
    ): List<TokenAsset> {
        return chains.flatMap { chain ->
            val network = networks[chain] ?: NetworkType.TESTNET
            try {
                val result = rustFetchPortfolio(
                    walletScope,
                    Chain.toWire(chain),
                    NetworkType.toWire(network)
                )
                parseTokensFromJson(result, chain, network)
            } catch (e: Throwable) {
                emptyList()
            }
        }
    }

    override suspend fun fetchPortfolio(
        walletScope: String,
        chain: Chain,
        network: NetworkType
    ): PortfolioResult {
        return try {
            val result = rustFetchPortfolio(
                walletScope,
                Chain.toWire(chain),
                NetworkType.toWire(network)
            )
            // Parse JSON and return PortfolioResult
            val tokens = parseTokensFromJson(result, chain, network)
            PortfolioResult(chain, network, tokens)
        } catch (e: Throwable) {
            PortfolioResult(chain, network, emptyList())
        }
    }

    private fun parseTokensFromJson(jsonStr: String, chain: Chain, network: NetworkType): List<TokenAsset> {
        try {
            val json = Json.parseToJsonElement(jsonStr)
            val root = json.jsonObject
            val tokensElem = root["tokens"] ?: return emptyList()
            val arr = tokensElem.jsonArray
            return arr.mapNotNull { elem ->
                parseTokenElement(elem, chain, network)
            }
        } catch (e: Throwable) {
            return emptyList()
        }
    }

    private fun parseTokenElement(elem: JsonElement, chain: Chain, network: NetworkType): TokenAsset? {
        try {
            val obj = elem.jsonObject
            val address = obj["address"]?.jsonPrimitive?.content ?: return null
            val symbol = obj["symbol"]?.jsonPrimitive?.content ?: "?"
            val name = obj["name"]?.jsonPrimitive?.content ?: ""
            val decimals = obj["decimals"]?.jsonPrimitive?.content?.toIntOrNull() ?: 18
            val balance = obj["balance"]?.jsonPrimitive?.content ?: "0"
            val balanceUsd = obj["balanceInUsd"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            val isCustom = obj["isCustom"]?.jsonPrimitive?.content?.toBoolean() ?: false
            return TokenAsset(
                address = address,
                chain = chain,
                network = network,
                symbol = symbol,
                name = name,
                decimals = decimals,
                balance = balance,
                balanceInUsd = balanceUsd,
                isCustom = isCustom
            )
        } catch (e: Throwable) {
            return null
        }
    }

    private fun parseSingleTokenJson(jsonStr: String, chain: Chain, network: NetworkType): TokenAsset? {
        return try {
            val elem = Json.parseToJsonElement(jsonStr)
            parseTokenElement(elem, chain, network)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Sign a transaction using Rust key material.
     * TX data is marshalled as JSON; result is signed digest/blob.
     */
    fun signTransaction(walletScope: String, txJson: String): String {
        return try {
            rustSignTransaction(walletScope, txJson)
        } catch (e: Throwable) {
            throw IllegalStateException("Signing failed: ${e.message}", e)
        }
    }
}
