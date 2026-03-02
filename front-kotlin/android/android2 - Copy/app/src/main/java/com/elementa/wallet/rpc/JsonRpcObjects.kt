package com.elementa.wallet.rpc

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object JsonRpcObjects {
    fun callObject(to: String, data: String): JsonObject {
        return JsonObject(
            mapOf(
                "to" to JsonPrimitive(to),
                "data" to JsonPrimitive(data)
            )
        )
    }
}
