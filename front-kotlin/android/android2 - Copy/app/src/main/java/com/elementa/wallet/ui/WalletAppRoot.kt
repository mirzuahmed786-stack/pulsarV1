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
import com.elementa.wallet.viewmodel.ChainDetailViewModel

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

    // Helper for main tab navigation to prevent backstack cycles
    fun switchTab(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    WalletTheme {
        Surface {
            NavHost(navController = navController, startDestination = Screen.Welcome.route) {
                
                composable(Screen.Unlock.route) {
                    UnlockScreen(
                        viewModel = hiltViewModel(),
                        onUnlocked = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Unlock.route) { inclusive = true }
                            }
                        }
                    )
                }

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
                        onOpenAssets = { switchTab(Screen.Assets) },
                        onSend = { navigateTo(Screen.Send) },
                        onReceive = { navigateTo(Screen.Receive) },
                        onSwap = { navigateTo(Screen.Swap) },
                        onOpenActivity = { switchTab(Screen.Activity) },
                        onOpenSettings = { switchTab(Screen.Settings) },
                        onOpenEcosystem = { switchTab(Screen.Ecosystem) },
                        onBuy = { navigateTo(Screen.Buy) },
                        onChainDetailClicked = { chain, network ->
                            navController.navigate(Screen.ChainDetail(chain, network).route)
                        }
                    )
                }
                
                composable(Screen.Send.route) { entry ->
                    val scannedAddress = entry.savedStateHandle.get<String>("scannedAddress") ?: ""
                    SendScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { switchTab(Screen.Home) },
                        onAssets = { switchTab(Screen.Assets) },
                        onTransfers = {},
                        onSwap = { navigateTo(Screen.Swap) },
                        onActivity = { switchTab(Screen.Activity) },
                        onHub = { switchTab(Screen.Ecosystem) },
                        onSettings = { switchTab(Screen.Settings) },
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

                composable(Screen.Buy.route) {
                    BuyScreen(
                        onBack = { navController.popBackStack() },
                        onContinue = { amount, chain ->
                            // In real app, proceed to payment
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Swap.route) {
                    SwapScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { switchTab(Screen.Home) },
                        onAssets = { switchTab(Screen.Assets) },
                        onTransfers = { navigateTo(Screen.Send) },
                        onSwap = { /* already here */ },
                        onActivity = { switchTab(Screen.Activity) },
                        onHub = { switchTab(Screen.Ecosystem) },
                        onSettings = { switchTab(Screen.Settings) }
                    )
                }

                composable(Screen.Receive.route) {
                    ReceiveScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { switchTab(Screen.Home) },
                        onAssets = { switchTab(Screen.Assets) },
                        onTransfers = { navigateTo(Screen.Send) },
                        onSwap = { navigateTo(Screen.Swap) },
                        onActivity = { switchTab(Screen.Activity) },
                        onHub = { switchTab(Screen.Ecosystem) },
                        onSettings = { switchTab(Screen.Settings) }
                    )
                }

                composable(Screen.Assets.route) {
                    AssetsScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { switchTab(Screen.Home) },
                        onAssets = { /* already here */ },
                        onTransfers = { navigateTo(Screen.Send) },
                        onSwap = { navigateTo(Screen.Swap) },
                        onActivity = { switchTab(Screen.Activity) },
                        onHub = { switchTab(Screen.Ecosystem) },
                        onSettings = { switchTab(Screen.Settings) },
                        onAddToken = { navigateTo(Screen.AddToken) }
                    )
                }

                composable(Screen.AddToken.route) {
                    AddTokenScreen(
                        viewModel = hiltViewModel(),
                        onDone = { navController.popBackStack() }
                    )
                }

                composable(Screen.Activity.route) {
                    ActivityScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { switchTab(Screen.Home) },
                        onAssets = { switchTab(Screen.Assets) },
                        onTransfers = { navigateTo(Screen.Send) },
                        onSwap = { navigateTo(Screen.Swap) },
                        onActivity = { /* already here */ },
                        onHub = { switchTab(Screen.Ecosystem) },
                        onSettings = { switchTab(Screen.Settings) }
                    )
                }

                composable(Screen.Ecosystem.route) {
                    ProtocolHubScreen(
                        onBack = { navController.popBackStack() },
                        onDashboard = { switchTab(Screen.Home) },
                        onAssets = { switchTab(Screen.Assets) },
                        onActivity = { switchTab(Screen.Activity) },
                        onSettings = { switchTab(Screen.Settings) },
                        onMining = { navigateTo(Screen.EcosystemMining) },
                        onMinting = { navigateTo(Screen.EcosystemMint) },
                        onStaking = { navigateTo(Screen.EcosystemStake) },
                        onGovernance = { navigateTo(Screen.EcosystemGovernance) },
                        onAnalytics = { navigateTo(Screen.EcosystemAnalytics) },
                        onMinerals = { navigateTo(Screen.EcosystemMinerals) }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() },
                        onDashboard = { switchTab(Screen.Home) },
                        onAssets = { switchTab(Screen.Assets) },
                        onHub = { switchTab(Screen.Ecosystem) },
                        onActivity = { switchTab(Screen.Activity) },
                        onSignOut = {
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.ChainDetail.ROUTE_TEMPLATE,
                    arguments = listOf(
                        navArgument(Screen.ChainDetail.ARG_CHAIN) { type = NavType.StringType },
                        navArgument(Screen.ChainDetail.ARG_NETWORK) { type = NavType.StringType }
                    )
                ) { entry ->
                    val chainStr = entry.arguments?.getString(Screen.ChainDetail.ARG_CHAIN) ?: ""
                    val networkStr = entry.arguments?.getString(Screen.ChainDetail.ARG_NETWORK) ?: ""
                    val chain = Chain.fromWire(chainStr)
                    val network = NetworkType.fromWire(networkStr)
                    
                    // explicitly request the correct ViewModel type for Hilt
                    val viewModel: ChainDetailViewModel = hiltViewModel<ChainDetailViewModel>()
                    LaunchedEffect(chain, network) {
                        viewModel.load(chain, network)
                    }
                    
                    ChainDetailScreen(
                        viewModel = viewModel,
                        onAddToken = { navigateTo(Screen.AddToken) },
                        onBack = { navController.popBackStack() },
                        onSend = { navigateTo(Screen.Send) },
                        onReceive = { navigateTo(Screen.Receive) },
                        onQrScan = { navigateTo(Screen.QrScanner) },
                        onActivity = { switchTab(Screen.Activity) }
                    )
                }
            }
        }
    }
}
