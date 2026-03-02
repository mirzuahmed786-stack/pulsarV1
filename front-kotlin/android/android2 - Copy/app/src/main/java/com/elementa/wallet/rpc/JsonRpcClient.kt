package com.elementa.wallet.rpc

import com.elementa.wallet.util.WalletLogger
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonRpcClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    suspend fun call(url: String, method: String, params: JsonElement): JsonElement {
        return callWithRetry(url, method, params, maxRetries = 3)
    }
    
    private suspend fun callWithRetry(
        url: String,
        method: String,
        params: JsonElement,
        maxRetries: Int = 3,
        retryDelayMs: Long = 1000
    ): JsonElement {
        var lastException: IOException? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return executeRpcCall(url, method, params)
            } catch (e: IOException) {
                lastException = e
                WalletLogger.logRpcError(url, method, e)
                
                // Don't retry on certain errors
                val message = e.message?.lowercase() ?: ""
                if (message.contains("invalid") || message.contains("not found")) {
                    throw e
                }
                
                // Retry with exponential backoff
                if (attempt < maxRetries - 1) {
                    delay(retryDelayMs * (attempt + 1))
                }
            }
        }
        
        throw lastException ?: IOException("RPC call failed after $maxRetries attempts")
    }
    
    private suspend fun executeRpcCall(url: String, method: String, params: JsonElement): JsonElement {
        val payload = JsonObject(
            mapOf(
                "jsonrpc" to json.parseToJsonElement("\"2.0\""),
                "id" to json.parseToJsonElement("1"),
                "method" to json.parseToJsonElement("\"$method\""),
                "params" to params
            )
        )
        val body = json.encodeToString(JsonObject.serializer(), payload)
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message}")
        }
        
        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            throw IOException("Empty RPC response from $url")
        }
        
        val element = json.parseToJsonElement(responseBody)
        val obj = element.jsonObject
        val error = obj["error"]
        if (error != null) {
            val errorMsg = try {
                val errorObj = error.jsonObject
                val errorMessage = errorObj["message"]?.toString()?.trim('\"') ?: error.toString()
                "RPC Error: $errorMessage"
            } catch (e: Exception) {
                "RPC Error: $error"
            }
            throw IOException(errorMsg)
        }
        return obj["result"] ?: throw IOException("Missing result in RPC response")
    }

    companion object {
        private val JSON = "application/json".toMediaType()
    }
}
