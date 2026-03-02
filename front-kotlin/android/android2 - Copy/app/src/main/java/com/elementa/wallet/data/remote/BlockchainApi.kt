package com.elementa.wallet.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Blockchain transaction and account data API
 * Supports multiple blockchain explorers (Etherscan, Blockchair, etc.)
 */
interface BlockchainApi {
    
    // ──────────────────────────────────────────────────────────────────
    // Ethereum/EVM Transactions (Etherscan-compatible)
    // ──────────────────────────────────────────────────────────────────
    
    @GET("api")
    suspend fun getEthereumTransactions(
        @Query("module") module: String = "account",
        @Query("action") action: String = "txlist",
        @Query("address") address: String,
        @Query("apiKey") apiKey: String
    ): EtherscanResponse<EthereumTransaction>

    @GET("api")
    suspend fun getEthereumBalance(
        @Query("module") module: String = "account",
        @Query("action") action: String = "balance",
        @Query("address") address: String,
        @Query("tag") tag: String = "latest",
        @Query("apiKey") apiKey: String
    ): EtherscanBalanceResponse

    @GET("api")
    suspend fun getERC20Tokens(
        @Query("module") module: String = "account",
        @Query("action") action: String = "tokentx",
        @Query("address") address: String,
        @Query("apiKey") apiKey: String
    ): EtherscanResponse<ERC20TokenTransfer>
    
    // ──────────────────────────────────────────────────────────────────
    // Bitcoin Transactions (Blockchair-compatible)
    // ──────────────────────────────────────────────────────────────────
    
    @GET("bitcoin/addresses/balances")
    suspend fun getBitcoinAddressData(
        @Query("addresses") addresses: String
    ): BitcoinAddressResponse

    @GET("bitcoin/dashboards/address/{address}")
    suspend fun getBitcoinTransactions(
        @Path("address") address: String
    ): BitcoinTransactionResponse
    
    // ──────────────────────────────────────────────────────────────────
    // Solana Transactions (Solana RPC)
    // ──────────────────────────────────────────────────────────────────
    
    @GET("rpc")
    suspend fun getSolanaSignatures(
        @Query("jsonrpc") jsonRpc: String = "2.0",
        @Query("id") id: Int = 1,
        @Query("method") method: String = "getSignaturesForAddress",
        @Query("params") params: String
    ): SolanaSignatureResponse
}

// ══════════════════════════════════════════════════════════════════
// ETHEREUM / EVM Models
// ══════════════════════════════════════════════════════════════════

@Serializable
data class EtherscanResponse<T>(
    val status: String,
    val message: String,
    val result: List<T>
)

@Serializable
data class EtherscanBalanceResponse(
    val status: String,
    val message: String,
    val result: String // Wei amount as string
)

@Serializable
data class EthereumTransaction(
    val hash: String,
    @SerialName("blockNumber")
    val blockNumber: String,
    @SerialName("timeStamp")
    val timeStamp: String,
    @SerialName("from")
    val from: String,
    @SerialName("to")
    val to: String,
    @SerialName("value")
    val value: String, // Wei
    @SerialName("gas")
    val gas: String,
    @SerialName("gasPrice")
    val gasPrice: String, // Wei
    @SerialName("isError")
    val isError: String, // "0" = success, "1" = failed
    @SerialName("txreceipt_status")
    val txReceiptStatus: String? = null,
    @SerialName("input")
    val input: String,
    @SerialName("contractAddress")
    val contractAddress: String = "",
    @SerialName("gasUsed")
    val gasUsed: String = "",
    @SerialName("cumulativeGasUsed")
    val cumulativeGasUsed: String = "",
    @SerialName("confirmations")
    val confirmations: String = "0"
)

@Serializable
data class ERC20TokenTransfer(
    val hash: String,
    @SerialName("blockNumber")
    val blockNumber: String,
    @SerialName("timeStamp")
    val timeStamp: String,
    @SerialName("from")
    val from: String,
    @SerialName("to")
    val to: String,
    @SerialName("value")
    val value: String,
    @SerialName("tokenName")
    val tokenName: String,
    @SerialName("tokenSymbol")
    val tokenSymbol: String,
    @SerialName("tokenDecimal")
    val tokenDecimal: String,
    @SerialName("transactionIndex")
    val transactionIndex: String,
    @SerialName("gas")
    val gas: String,
    @SerialName("gasPrice")
    val gasPrice: String,
    @SerialName("gasUsed")
    val gasUsed: String,
    @SerialName("cumulativeGasUsed")
    val cumulativeGasUsed: String,
    @SerialName("contractAddress")
    val contractAddress: String,
    @SerialName("isError")
    val isError: String,
    @SerialName("confirmations")
    val confirmations: String
)

// ══════════════════════════════════════════════════════════════════
// BITCOIN Models
// ══════════════════════════════════════════════════════════════════

@Serializable
data class BitcoinAddressResponse(
    val data: Map<String, BitcoinAddress>
)

@Serializable
data class BitcoinAddress(
    val address: BitcoinAddressData
)

@Serializable
data class BitcoinAddressData(
    val balance: String,
    @SerialName("unconfirmed_balance")
    val unconfirmedBalance: String,
    @SerialName("total_received")
    val totalReceived: String,
    @SerialName("total_spent")
    val totalSpent: String,
    @SerialName("tx_count")
    val txCount: Int,
    @SerialName("unconfirmed_tx_count")
    val unconfirmedTxCount: Int
)

@Serializable
data class BitcoinTransactionResponse(
    val data: BitcoinDashboard
)

@Serializable
data class BitcoinDashboard(
    val address: String,
    val balance: String,
    val balance_usd: String? = null,
    val received: String,
    val received_usd: String? = null,
    val spent: String,
    val spent_usd: String? = null,
    val tx_count: Int,
    val unconfirmed_tx_count: Int,
    val transactions: List<BitcoinTransaction>? = null
)

@Serializable
data class BitcoinTransaction(
    val hash: String,
    val time: Long,
    val balance: String,
    val balance_usd: String? = null,
    val inputs: List<BitcoinInput>,
    val outputs: List<BitcoinOutput>,
    val input_count: Int,
    val output_count: Int,
    val input_total: String,
    val input_total_usd: String? = null,
    val output_total: String,
    val output_total_usd: String? = null,
    val fee: String?,
    val fee_usd: String? = null,
    val is_coinbase: Boolean,
    val has_rbf: Boolean,
    val lock_time: Long,
    val size: Int,
    val weight: Int,
    val version: Int,
    val block_id: Long? = null,
    val block_height: Int,
    val block_time: Long? = null,
    val status: String
)

@Serializable
data class BitcoinInput(
    val block_id: Long? = null,
    val transaction_hash: String,
    val index: Int,
    val transaction_index: Int,
    val address: String? = null,
    val type: String,
    val script: String,
    val is_from_coinbase: Boolean,
    val is_spendable: Boolean? = null,
    val value: String
)

@Serializable
data class BitcoinOutput(
    val block_id: Long? = null,
    val transaction_hash: String,
    val index: Int,
    val transaction_index: Int,
    val address: String? = null,
    val type: String,
    val script: String,
    val is_spent: Boolean,
    val is_spendable: Boolean? = null,
    val value: String
)

// ══════════════════════════════════════════════════════════════════
// SOLANA Models
// ══════════════════════════════════════════════════════════════════

@Serializable
data class SolanaSignatureResponse(
    val jsonrpc: String,
    val result: List<SolanaSignature>?,
    val error: SolanaRpcError? = null
)

@Serializable
data class SolanaSignature(
    val signature: String,
    val slot: Long,
    val err: Map<String, String>?,
    val memo: String? = null,
    val blockTime: Long? = null
)

@Serializable
data class SolanaRpcError(
    val code: Int,
    val message: String
)

@Serializable
data class SolanaTransaction(
    val signature: String,
    val slot: Long,
    val blockTime: Long?,
    val error: String?,
    val feePayer: String,
    val status: String,
    val lamports: Long,
    val instructions: List<String>
)
