package com.elementa.wallet.rpc

import java.math.BigInteger

object SolanaAddressValidator {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun isValidPublicKey(value: String): Boolean {
        return try {
            val bytes = decodeBase58(value)
            bytes.size == 32
        } catch (_: Throwable) {
            false
        }
    }

    private fun decodeBase58(input: String): ByteArray {
        var num = BigInteger.ZERO
        val base = BigInteger.valueOf(58)
        for (char in input) {
            val digit = ALPHABET.indexOf(char)
            require(digit >= 0) { "Invalid base58" }
            num = num.multiply(base).add(BigInteger.valueOf(digit.toLong()))
        }
        val bytes = num.toByteArray()
        val strip = if (bytes.isNotEmpty() && bytes[0].toInt() == 0) bytes.copyOfRange(1, bytes.size) else bytes
        val leadingZeros = input.takeWhile { it == '1' }.count()
        val result = ByteArray(leadingZeros + strip.size)
        System.arraycopy(strip, 0, result, leadingZeros, strip.size)
        return result
    }
}
