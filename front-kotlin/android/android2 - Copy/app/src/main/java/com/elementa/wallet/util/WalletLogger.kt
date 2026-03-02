package com.elementa.wallet.util

import android.util.Log

/**
 * Centralized logging utility for the wallet application
 * Provides structured logging with proper error tracking
 */
object WalletLogger {
    private const val TAG = "ElementaWallet"
    
    fun d(message: String, tag: String = TAG) {
        Log.d(tag, message)
    }
    
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }
    
    fun w(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log RPC errors with detailed context
     */
    fun logRpcError(chain: String, method: String, error: Throwable) {
        e("RPC Error: $chain.$method - ${error.message}", "RPC", error)
    }
    
    /**
     * Log API errors with context
     */
    fun logApiError(api: String, endpoint: String, error: Throwable) {
        e("API Error: $api.$endpoint - ${error.message}", "API", error)
    }
    
    /**
     * Log ViewModel errors
     */
    fun logViewModelError(viewModel: String, operation: String, error: Throwable) {
        e("ViewModel Error: $viewModel.$operation - ${error.message}", "ViewModel", error)
    }
    
    /**
     * Log repository errors
     */
    fun logRepositoryError(repository: String, operation: String, error: Throwable) {
        e("Repository Error: $repository.$operation - ${error.message}", "Repository", error)
    }
    
    /**
     * Log info messages with context
     */
    fun logInfo(tag: String, message: String) {
        i(message, tag)
    }
    
    /**
     * Log error messages with context
     */
    fun logError(tag: String, message: String, error: Throwable? = null) {
        e(message, tag, error)
    }
}
