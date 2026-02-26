package com.elementa.wallet.util

/**
 * Address validation utilities for different blockchain formats
 */
object AddressValidator {
    
    /**
     * Validates Ethereum/EVM address (0x + 40 hex chars)
     */
    fun isValidEvmAddress(address: String): Boolean {
        if (!address.startsWith("0x")) return false
        if (address.length != 42) return false
        return address.substring(2).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }
    
    /**
     * Validates Solana address (base58, 32-44 chars)
     */
    fun isValidSolanaAddress(address: String): Boolean {
        if (address.length !in 32..44) return false
        val base58Chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        return address.all { it in base58Chars }
    }
    
    /**
     * Validates Bitcoin address (legacy: 1/3, segwit: bc1)
     */
    fun isValidBitcoinAddress(address: String): Boolean {
        // Basic validation - full validation would require base58check
        if (address.isEmpty()) return false
        
        return when {
            address.startsWith("1") || address.startsWith("3") -> {
                address.length in 26..35 && address.all { it.isDigit() || it.isLetter() }
            }
            address.startsWith("bc1") -> {
                address.length in 42..62
            }
            else -> false
        }
    }
    
    /**
     * Sanitizes and normalizes addresses
     */
    fun normalizeAddress(address: String, isEvm: Boolean = false): String {
        val trimmed = address.trim()
        return if (isEvm) {
            trimmed.lowercase()
        } else {
            trimmed
        }
    }
    
    /**
     * Validates amount string is a valid positive number
     */
    fun isValidAmount(amount: String): Boolean {
        if (amount.isBlank()) return false
        val parsed = amount.toDoubleOrNull() ?: return false
        return parsed > 0.0 && !parsed.isNaN() && !parsed.isInfinite()
    }
    
    /**
     * Checks if amount is within reasonable bounds
     */
    fun isAmountWithinBounds(amount: String, maxValue: Double = 1_000_000_000.0): Boolean {
        val parsed = amount.toDoubleOrNull() ?: return false
        return parsed > 0.0 && parsed <= maxValue
    }
}
