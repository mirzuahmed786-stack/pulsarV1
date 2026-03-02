package com.elementa.wallet.ui.screens

// Shared asset data model used across multiple screens
// Made public so other packages can reference if necessary

data class AssetItem(
    val name: String,
    val symbol: String,
    val balance: Double,
    val usdValue: Double,
    val icon: Int
)
