package com.elementa.wallet.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.navigation.Screen
import com.elementa.wallet.ui.screens.*
import com.elementa.wallet.ui.theme.WalletTheme

@Composable
fun WalletAppRoot() {
    val navController = rememberNavController()
    
    // Navigation helper using type-safe Screen sealed class
    fun navigateTo(screen: Screen, clearBackStack: Boolean = false, popUpTo: Screen? = null) {
        navController.navigate(screen.route) {
            popUpTo?.let { 
                popUpTo(it.route) { inclusive = clearBackStack }
            }
        }
    }

    WalletTheme {
        Surface {
            NavHost(navController = navController, startDestination = Screen.Welcome.route) {
                
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onCreateWallet = { navigateTo(Screen.CreatePassword) },
                        onImportWallet = { navigateTo(Screen.ImportWallet) }
                    )
                }
                
                composable(Screen.CreatePassword.route) {
                    CreatePasswordScreen(
                        onBack = { navController.popBackStack() },
                        onContinue = { pin ->
                            // In real app, we'd save PIN to VaultViewModel
                            navigateTo(Screen.Mnemonic)
                        }
                    )
                }
                
                composable(Screen.ImportWallet.route) {
                    ImportWalletScreen(
                        onBack = { navController.popBackStack() },
                        onContinue = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }
                
                composable(Screen.Mnemonic.route) {
                    MnemonicDisplayScreen(
                        onBack = { navController.popBackStack() },
                        onMnemonicSaved = { navigateTo(Screen.Passphrase) }
                    )
                }
                
                composable(Screen.Passphrase.route) {
                    PassphraseEntryScreen(
                        onBack = { navController.popBackStack() },
                        onConfirmed = {
                            navigateTo(Screen.WalletCreated)
                        }
                    )
                }
                
                composable(Screen.WalletCreated.route) {
                    WalletCreatedScreen(
                        onGetStarted = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }
                
                composable(Screen.Home.route) {
                    HomeScreen(
                        onOpenAssets = { navigateTo(Screen.Assets) },
                        onSend = { navigateTo(Screen.Send) },
                        onReceive = { navigateTo(Screen.Receive) },
                        onSwap = { navigateTo(Screen.Swap) },
                        onOpenActivity = { navigateTo(Screen.Activity) },
                        onOpenSettings = { navigateTo(Screen.Settings) },
                        onOpenEcosystem = { /* navigateTo(Screen.Ecosystem) */ },
                        onChainDetailClicked = { chain, network ->
                            navController.navigate(Screen.ChainDetail(chain, network).route)
                        }
                    )
                }
                
                composable(Screen.Send.route) { entry ->
                    val scannedAddress = entry.savedStateHandle.get<String>("scannedAddress") ?: ""
                    SendScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { navigateTo(Screen.Home) },
                        onAssets = { navigateTo(Screen.Assets) },
                        onTransfers = {},
                        onSwap = { navigateTo(Screen.Swap) },
                        onActivity = { navigateTo(Screen.Activity) },
                        onContinue = { amount, recipient ->
                            navController.navigate(Screen.SendConfirm(amount, recipient).route)
                        },
                        onQrScan = { navigateTo(Screen.QrScanner) },
                        initialRecipient = scannedAddress
                    )
                }
                
                composable(
                    route = Screen.SendConfirm.ROUTE_TEMPLATE,
                    arguments = listOf(
                        navArgument(Screen.SendConfirm.ARG_AMOUNT) { type = NavType.StringType },
                        navArgument(Screen.SendConfirm.ARG_RECIPIENT) { type = NavType.StringType }
                    )
                ) { entry ->
                    val amount = entry.arguments?.getString(Screen.SendConfirm.ARG_AMOUNT) ?: "0.0"
                    val recipient = entry.arguments?.getString(Screen.SendConfirm.ARG_RECIPIENT) ?: ""
                    SendConfirmationScreen(
                        amount = amount,
                        recipient = recipient,
                        onBack = { navController.popBackStack() },
                        onConfirmed = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.QrScanner.route) {
                    QrScannerScreen(
                        onBack = { navController.popBackStack() },
                        onScanned = { scannedValue ->
                            navController.previousBackStackEntry?.savedStateHandle?.set("scannedAddress", scannedValue)
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Assets.route) {
                    AssetsScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { navigateTo(Screen.Home) },
                        onAssets = { /* already here */ },
                        onTransfers = { navigateTo(Screen.Send) },
                        onSwap = { navigateTo(Screen.Swap) },
                        onActivity = { navigateTo(Screen.Activity) }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                        onSignOut = {
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
