package com.elementa.wallet.rpc

import java.math.BigInteger

object AbiEncoder {
    private const val ADDRESS_BYTES = 20
    private const val WORD_BYTES = 32

    fun encodeAddress(address: String): String {
        val clean = address.removePrefix("0x").lowercase()
        require(clean.length == ADDRESS_BYTES * 2) { "Invalid address" }
        return clean.padStart(WORD_BYTES * 2, '0')
    }

    fun encodeUint256(value: Long): String {
        return BigInteger.valueOf(value).toString(16).padStart(WORD_BYTES * 2, '0')
    }

    fun encodeUint256(value: BigInteger): String {
        return value.toString(16).padStart(WORD_BYTES * 2, '0')
    }

    fun decodeUint256(data: String): BigInteger {
        val clean = data.removePrefix("0x")
        return BigInteger(clean, 16)
    }

    fun decodeString(data: String): String {
        val clean = data.removePrefix("0x")
        if (clean.length < WORD_BYTES * 2) return ""
        val offset = clean.substring(0, WORD_BYTES * 2).toInt(16) * 2
        if (clean.length < offset + WORD_BYTES * 2) return ""
        val length = clean.substring(offset, offset + WORD_BYTES * 2).toInt(16) * 2
        if (length <= 0 || clean.length < offset + WORD_BYTES * 2 + length) return ""
        val dataHex = clean.substring(offset + WORD_BYTES * 2, offset + WORD_BYTES * 2 + length)
        return hexToString(dataHex)
    }

    private fun hexToString(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < hex.length) {
            val byte = hex.substring(i, i + 2).toInt(16)
            sb.append(byte.toChar())
            i += 2
        }
        return sb.toString().trim()
    }
}
