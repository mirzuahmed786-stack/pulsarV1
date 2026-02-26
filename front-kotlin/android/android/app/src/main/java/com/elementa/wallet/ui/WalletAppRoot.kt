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
import com.elementa.wallet.ui.screens.AddTokenScreen
import com.elementa.wallet.ui.screens.AnalyticsDashboardScreen
import com.elementa.wallet.ui.screens.AssetsScreen
import com.elementa.wallet.ui.screens.AssetDetailsScreen
import com.elementa.wallet.ui.screens.CreateWalletScreen
import com.elementa.wallet.ui.screens.GoogleOAuthCallbackScreen
import com.elementa.wallet.ui.screens.GovernanceDashboardScreen
import com.elementa.wallet.ui.screens.HomeScreen
import com.elementa.wallet.ui.screens.ImportWalletScreen
import com.elementa.wallet.ui.screens.MineralsVaultScreen
import com.elementa.wallet.ui.screens.MiningInterfaceScreen
import com.elementa.wallet.ui.screens.MintingInterfaceScreen
import com.elementa.wallet.ui.screens.ProtocolHubScreen
import com.elementa.wallet.ui.screens.ReceiveScreen
import com.elementa.wallet.ui.screens.SendScreen
import com.elementa.wallet.ui.screens.SettingsScreen
import com.elementa.wallet.ui.screens.SwapScreen
import com.elementa.wallet.ui.screens.QrScannerScreen
import com.elementa.wallet.ui.screens.StakingInterfaceScreen
import com.elementa.wallet.ui.screens.WalletAccountsScreen
import com.elementa.wallet.ui.screens.WelcomeScreen
import com.elementa.wallet.ui.screens.ActivityScreen
import com.elementa.wallet.ui.screens.ChainDetailScreen
import com.elementa.wallet.ui.screens.ChainNetworkDetailScreen
import com.elementa.wallet.ui.screens.NotificationsScreen
import com.elementa.wallet.ui.screens.OnboardingScreen
import com.elementa.wallet.ui.screens.PinSetupScreen
import com.elementa.wallet.ui.screens.SplashScreen
import com.elementa.wallet.ui.screens.UnlockScreen
import com.elementa.wallet.ui.screens.BiometricsSetupScreen
import com.elementa.wallet.viewmodel.AddTokenViewModel
import com.elementa.wallet.viewmodel.AssetsViewModel
import com.elementa.wallet.viewmodel.ChainDetailViewModel
import com.elementa.wallet.viewmodel.VaultViewModel
import com.elementa.wallet.ui.theme.WalletTheme

@Composable
fun WalletAppRoot() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Navigation helper using type-safe Screen sealed class
    fun navigateTo(screen: Screen, clearBackStack: Boolean = false, popUpTo: Screen? = null) {
        navController.navigate(screen.route) {
            popUpTo?.let { 
                popUpTo(it.route) { inclusive = clearBackStack }
            }
        }
    }

    WalletTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                com.elementa.wallet.ui.components.PulsarSidebar(
                    onNavigate = { route -> navController.navigate(route) },
                    onClose = { scope.launch { drawerState.close() } }
                )
            },
            gesturesEnabled = drawerState.isOpen
        ) {
            Surface {
                NavHost(navController = navController, startDestination = Screen.Splash.route) {
                    
                    // ─────────────────────────────────────────────────────────────
                    // Auth & Onboarding Routes
                    // ─────────────────────────────────────────────────────────────
                    
                    composable(Screen.Splash.route) {
                        val vaultVm = hiltViewModel<VaultViewModel>()
                        val isLocked by vaultVm.isLocked.collectAsState()
                        val hasPin by vaultVm.isPinConfigured.collectAsState()
                        SplashScreen(
                            onTimeout = { 
                                val next = if (hasPin) {
                                    if (isLocked) Screen.Unlock else Screen.Home
                                } else {
                                    Screen.Welcome
                                }
                                navController.navigate(next.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    composable(Screen.Welcome.route) {
                        WelcomeScreen(
                            onCreateWallet = { navigateTo(Screen.PinSetup) },
                            onImportWallet = { navigateTo(Screen.ImportWallet) },
                            onGoogleLogin = { navigateTo(Screen.PinSetup) },
                            onAppleLogin = { navigateTo(Screen.PinSetup) }
                        )
                    }
                    
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            onComplete = { navigateTo(Screen.PinSetup) }
                        )
                    }
                    
                    composable(Screen.Unlock.route) {
                        val vaultVm = hiltViewModel<VaultViewModel>()
                        UnlockScreen(
                            viewModel = vaultVm,
                            onUnlocked = { 
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Unlock.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    composable(Screen.CreateWallet.route) {
                        CreateWalletScreen(
                            onBack = { navController.popBackStack() },
                            onContinue = { navigateTo(Screen.PinSetup) }
                        )
                    }
                    
                    composable(Screen.ImportWallet.route) {
                        ImportWalletScreen(
                            onBack = { navController.popBackStack() },
                            onContinue = { navigateTo(Screen.PinSetup) }
                        )
                    }
                    
                    composable(Screen.Mnemonic.route) {
                        com.elementa.wallet.ui.screens.MnemonicDisplayScreen(
                            onBack = { navController.popBackStack() },
                            onMnemonicSaved = { navigateTo(Screen.Passphrase) }
                        )
                    }
                    
                    composable(Screen.Passphrase.route) {
                        com.elementa.wallet.ui.screens.PassphraseEntryScreen(
                            onBack = { navController.popBackStack() },
                            onConfirm = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    composable(Screen.PinSetup.route) {
                        val vaultVm = hiltViewModel<VaultViewModel>()
                        PinSetupScreen(
                            onPinEntered = { pin ->
                                vaultVm.configurePin(pin) { success ->
                                    if (success) {
                                        navController.navigate(Screen.Mnemonic.route)
                                    }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable(Screen.BiometricsSetup.route) {
                        val vaultVm = hiltViewModel<VaultViewModel>()
                        BiometricsSetupScreen(
                            viewModel = vaultVm,
                            onContinue = { navigateTo(Screen.Mnemonic) },
                            onSkip = { navigateTo(Screen.Mnemonic) }
                        )
                    }
                    
                    composable(Screen.OAuthGoogleCallback.route) {
                        GoogleOAuthCallbackScreen(
                            onGoHome = { navigateTo(Screen.Home) }
                        )
                    }

                    // ─────────────────────────────────────────────────────────────
                    // Main Dashboard Routes
                    // ─────────────────────────────────────────────────────────────
                    
                    composable(Screen.Home.route) {
                        val vaultVm = hiltViewModel<VaultViewModel>()
                        HomeScreen(
                            onOpenAssets = { navigateTo(Screen.Assets) },
                            onSend = { navigateTo(Screen.Send) },
                            onReceive = { navigateTo(Screen.Receive) },
                            onSwap = { navigateTo(Screen.Swap) },
                            onOpenActivity = { navigateTo(Screen.Activity) },
                            onOpenSettings = { navigateTo(Screen.Settings) },
                            onOpenEcosystem = { navigateTo(Screen.Ecosystem) },
                            onChainDetailClicked = { chain, network ->
                                navController.navigate(Screen.ChainDetail(chain, network).route)
                            },
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onLockWallet = {
                                // Phase 5: Lock functionality - clear session and navigate to Unlock
                                vaultVm.lock()
                                navController.navigate(Screen.Unlock.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            onNotificationsClick = { navigateTo(Screen.Notifications) }
                        )
                    }
                    
                    composable(Screen.Notifications.route) {
                        NotificationsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable(Screen.Assets.route) {
                        val vm = hiltViewModel<AssetsViewModel>()
                        AssetsScreen(
                            viewModel = vm,
                            onAddToken = { navigateTo(Screen.AddToken) },
                            onChainDetail = { chain, network ->
                                navController.navigate(Screen.ChainDetail(chain, network).route)
                            },
                            onAssetDetail = { chainId, assetId ->
                                navController.navigate(Screen.AssetDetails(chainId, assetId).route)
                            },
                            onSend = { navigateTo(Screen.Send) },
                            onReceive = { navigateTo(Screen.Receive) },
                            onBack = { navController.popBackStack() },
                            onSwap = { navigateTo(Screen.Swap) },
                            onActivity = { navigateTo(Screen.Activity) },
                            onNotifications = { navigateTo(Screen.Notifications) }
                        )
                    }
                    
                    composable(
                        route = Screen.AssetDetails.ROUTE_TEMPLATE,
                        arguments = listOf(
                            navArgument(Screen.AssetDetails.ARG_CHAIN) { type = NavType.StringType },
                            navArgument(Screen.AssetDetails.ARG_ID) { type = NavType.StringType }
                        )
                    ) { entry ->
                        AssetDetailsScreen(
                            chainId = entry.arguments?.getString(Screen.AssetDetails.ARG_CHAIN) ?: "ethereum",
                            assetId = entry.arguments?.getString(Screen.AssetDetails.ARG_ID) ?: "token",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable(
                        route = Screen.ChainDetail.ROUTE_TEMPLATE,
                        arguments = listOf(
                            navArgument(Screen.ChainDetail.ARG_CHAIN) { type = NavType.StringType },
                            navArgument(Screen.ChainDetail.ARG_NETWORK) { type = NavType.StringType }
                        )
                    ) { entry ->
                        val chain = Chain.fromWire(entry.arguments?.getString(Screen.ChainDetail.ARG_CHAIN) ?: "ethereum")
                        val network = NetworkType.fromWire(entry.arguments?.getString(Screen.ChainDetail.ARG_NETWORK) ?: "testnet")
                        // Unified design: All chains use ChainDetailScreen
                        val vm = hiltViewModel<ChainDetailViewModel>()
                        LaunchedEffect(chain, network) { vm.load(chain, network) }
                        ChainDetailScreen(
                            viewModel = vm,
                            onAddToken = { navigateTo(Screen.AddToken) },
                            onBack = { navController.popBackStack() },
                            onSend = { navigateTo(Screen.Send) },
                            onReceive = { navigateTo(Screen.Receive) },
                            onQrScan = { navigateTo(Screen.QrScanner) }
                        )
                    }
                    
                    composable(Screen.AddToken.route) {
                        val vm = hiltViewModel<AddTokenViewModel>()
                        AddTokenScreen(
                            viewModel = vm,
                            onDone = { navController.popBackStack() }
                        )
                    }
                    
                    composable(Screen.Activity.route) {
                        ActivityScreen(
                            onBack = { navController.popBackStack() },
                            onDashboard = { navigateTo(Screen.Home) },
                            onAssets = { navigateTo(Screen.Assets) },
                            onTransfers = { navigateTo(Screen.Send) },
                            onSwap = { navigateTo(Screen.Swap) },
                            onActivity = {}
                        )
                    }
                    
                    composable(Screen.Accounts.route) {
                        WalletAccountsScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable(Screen.Settings.route) {
                        val vaultVm = hiltViewModel<VaultViewModel>()
                        SettingsScreen(
                            viewModel = vaultVm,
                            onBack = { navController.popBackStack() },
                            onSignOut = {
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ─────────────────────────────────────────────────────────────
                    // Transfer Routes
                    // ─────────────────────────────────────────────────────────────
                    
                    composable(Screen.Send.route) { entry ->
                        // Get scanned address from QR scanner if available
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
                        com.elementa.wallet.ui.screens.SendConfirmationScreen(
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
                    
                    composable(Screen.Receive.route) {
                        ReceiveScreen(
                            onBack = { navController.popBackStack() },
                            onDashboard = { navigateTo(Screen.Home) },
                            onAssets = { navigateTo(Screen.Assets) },
                            onTransfers = { navigateTo(Screen.Send) },
                            onSwap = { navigateTo(Screen.Swap) },
                            onActivity = { navigateTo(Screen.Activity) }
                        )
                    }
                    
                    composable(Screen.QrScanner.route) {
                        QrScannerScreen(
                            onBack = { navController.popBackStack() },
                            onScanned = { scannedValue ->
                                // Navigate back and let caller handle result
                                // For now, go to Send screen (address can be passed via savedStateHandle)
                                navController.previousBackStackEntry?.savedStateHandle?.set("scannedAddress", scannedValue)
                                navController.popBackStack()
                            }
                        )
                    }
                    
                    composable(Screen.Swap.route) {
                        SwapScreen(
                            onBack = { navController.popBackStack() },
                            onDashboard = { navigateTo(Screen.Home) },
                            onAssets = { navigateTo(Screen.Assets) },
                            onTransfers = { navigateTo(Screen.Send) },
                            onSwap = {},
                            onActivity = { navigateTo(Screen.Activity) }
                        )
                    }

                    // ─────────────────────────────────────────────────────────────
                    // Ecosystem Routes
                    // ─────────────────────────────────────────────────────────────
                    
                    composable(Screen.Ecosystem.route) {
                        ProtocolHubScreen(
                            onBack = { navController.popBackStack() },
                            onMining = { navigateTo(Screen.EcosystemMining) },
                            onMinting = { navigateTo(Screen.EcosystemMint) },
                            onStaking = { navigateTo(Screen.EcosystemStake) },
                            onGovernance = { navigateTo(Screen.EcosystemGovernance) },
                            onAnalytics = { navigateTo(Screen.EcosystemAnalytics) },
                            onMinerals = { navigateTo(Screen.EcosystemMinerals) }
                        )
                    }
                    
                    composable(Screen.EcosystemMining.route) {
                        MiningInterfaceScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable(Screen.EcosystemMint.route) {
                        MintingInterfaceScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable(Screen.EcosystemStake.route) {
                        StakingInterfaceScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable(Screen.EcosystemGovernance.route) {
                        GovernanceDashboardScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable(Screen.EcosystemAnalytics.route) {
                        AnalyticsDashboardScreen(onBack = { navController.popBackStack() })
                    }
                    
                    composable(Screen.EcosystemMinerals.route) {
                        MineralsVaultScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

