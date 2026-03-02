package com.elementa.wallet.ui.navigation

import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType

/**
 * Type-safe sealed class for all navigation routes in the app.
 * Eliminates string-based routing errors and provides compile-time safety.
 */
sealed class Screen(val route: String) {
    
    // Auth & Onboarding
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Onboarding : Screen("onboarding")
    data object Unlock : Screen("unlock")
    data object PinSetup : Screen("pin-setup")
    data object BiometricsSetup : Screen("biometrics-setup")
    data object Mnemonic : Screen("mnemonic")
    data object Passphrase : Screen("passphrase")
    data object CreateWallet : Screen("create-wallet")
    data object CreatePassword : Screen("create-password")
    data object ImportWallet : Screen("import-wallet")
    data object WalletCreated : Screen("wallet-created")
    data object OAuthGoogleCallback : Screen("oauth/google/callback")
    
    // Main Dashboard
    data object Home : Screen("home")
    data object Assets : Screen("assets")
    data object Buy : Screen("buy")
    data object AddToken : Screen("add-token")
    data object Activity : Screen("activity")
    data object Accounts : Screen("accounts")
    data object Settings : Screen("settings")
    data object Notifications : Screen("notifications")
    
    // Asset Details - dynamic route
    data class AssetDetails(val chainId: String, val assetId: String) : Screen("assets/$chainId/$assetId") {
        companion object {
            const val ROUTE_TEMPLATE = "assets/{chain}/{id}"
            const val ARG_CHAIN = "chain"
            const val ARG_ID = "id"
        }
    }
    
    // Chain Detail - dynamic route
    data class ChainDetail(val chain: Chain, val network: NetworkType) : Screen("chain/${Chain.toWire(chain)}/${NetworkType.toWire(network)}") {
        companion object {
            const val ROUTE_TEMPLATE = "chain/{chain}/{network}"
            const val ARG_CHAIN = "chain"
            const val ARG_NETWORK = "network"
        }
    }
    
    // Transfers
    data object Send : Screen("send")
    data class SendConfirm(val amount: String, val recipient: String) : Screen("send-confirm/$amount/$recipient") {
        companion object {
            const val ROUTE_TEMPLATE = "send-confirm/{amount}/{recipient}"
            const val ARG_AMOUNT = "amount"
            const val ARG_RECIPIENT = "recipient"
        }
    }
    data object Receive : Screen("receive")
    data object Swap : Screen("swap")
    data object QrScanner : Screen("qr-scanner")
    
    // Ecosystem
    data object Ecosystem : Screen("ecosystem")
    data object EcosystemMining : Screen("ecosystem/mining")
    data object EcosystemMint : Screen("ecosystem/mint")
    data object EcosystemStake : Screen("ecosystem/stake")
    data object EcosystemGovernance : Screen("ecosystem/governance")
    data object EcosystemAnalytics : Screen("ecosystem/analytics")
    data object EcosystemMinerals : Screen("ecosystem/minerals")
    
    companion object {
        // Helper to get all static routes for NavHost registration
        val allStaticRoutes = listOf(
            Splash, Welcome, Onboarding, Unlock, PinSetup, BiometricsSetup,
            Mnemonic, Passphrase, CreateWallet, CreatePassword, ImportWallet, WalletCreated, OAuthGoogleCallback,
            Home, Assets, Buy, AddToken, Activity, Accounts, Settings, Notifications,
            Send, Receive, Swap, QrScanner,
            Ecosystem, EcosystemMining, EcosystemMint, EcosystemStake,
            EcosystemGovernance, EcosystemAnalytics, EcosystemMinerals
        )
    }
}
