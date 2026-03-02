package com.elementa.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.domain.model.Chain
import com.elementa.wallet.domain.model.NetworkType
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.ui.components.PulsarBottomNav

@Composable
fun AnalyticsDashboardScreen(onBack: () -> Unit) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Analytics Dashboard Coming Soon", color = PulsarColors.TextPrimaryDark)
            }
        }
    }
}

@Composable
fun ChainDetailRouteScreen(
    chain: Chain,
    onOpenChainNetwork: (Chain, NetworkType) -> Unit,
    onBack: () -> Unit
) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "${chain.name} Network",
                    style = PulsarTypography.Typography.headlineLarge,
                    color = PulsarColors.TextPrimaryDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                NetworkOptionCard("Mainnet", "Production network", chain, NetworkType.MAINNET, onOpenChainNetwork)
                NetworkOptionCard("Testnet", "Developer testing network", chain, NetworkType.TESTNET, onOpenChainNetwork)
            }
        }
    }
}

@Composable
private fun NetworkOptionCard(
    label: String,
    desc: String,
    chain: Chain,
    network: NetworkType,
    onOpenChainNetwork: (Chain, NetworkType) -> Unit
) {
    PulsarComponents.PulsarCard(
        onClick = { onOpenChainNetwork(chain, network) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(if (network == NetworkType.MAINNET) PulsarColors.SuccessGreen else PulsarColors.PrimaryDark, CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = PulsarTypography.Typography.titleMedium, color = PulsarColors.TextPrimaryDark)
                Text(desc, style = PulsarTypography.Typography.bodySmall, color = PulsarColors.TextSecondaryDark)
            }
        }
    }
}

@Composable
fun GoogleOAuthCallbackScreen(onGoHome: () -> Unit) {
    LaunchedEffect(Unit) {
        onGoHome()
    }
    PulsarBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PulsarColors.PrimaryDark)
        }
    }
}

@Composable
fun GovernanceDashboardScreen(onBack: () -> Unit) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Governance Dashboard Coming Soon", color = PulsarColors.TextPrimaryDark)
            }
        }
    }
}

@Composable
fun MineralsVaultScreen(onBack: () -> Unit) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Minerals Vault Coming Soon", color = PulsarColors.TextPrimaryDark)
            }
        }
    }
}

@Composable
fun MiningInterfaceScreen(onBack: () -> Unit) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Mining Interface Coming Soon", color = PulsarColors.TextPrimaryDark)
            }
        }
    }
}

@Composable
fun MintingInterfaceScreen(onBack: () -> Unit) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Minting Interface Coming Soon", color = PulsarColors.TextPrimaryDark)
            }
        }
    }
}

@Composable
fun ProtocolHubScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onActivity: () -> Unit,
    onSettings: () -> Unit,
    onMining: () -> Unit,
    onMinting: () -> Unit,
    onStaking: () -> Unit,
    onGovernance: () -> Unit,
    onAnalytics: () -> Unit,
    onMinerals: () -> Unit
) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) },
            bottomBar = {
                PulsarBottomNav(
                    onHome = onDashboard,
                    onAssets = onAssets,
                    onHub = {},
                    onActivity = onActivity,
                    onSettings = onSettings,
                    currentRoute = "ecosystem"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("ECOSYSTEM HUB", style = PulsarTypography.CyberLabel, color = PulsarColors.PrimaryDark)
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HubItem("⛏️", "Mining", onMining, Modifier.weight(1f))
                    HubItem("🎨", "Minting", onMinting, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HubItem("🥩", "Staking", onStaking, Modifier.weight(1f))
                    HubItem("⚖️", "Governance", onGovernance, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HubItem("📊", "Analytics", onAnalytics, Modifier.weight(1f))
                    HubItem("💎", "Minerals", onMinerals, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HubItem(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PulsarComponents.PulsarCard(
        modifier = modifier.height(140.dp),
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Text(icon, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = PulsarTypography.Typography.titleMedium, color = PulsarColors.TextPrimaryDark)
        }
    }
}

@Composable
fun StakingInterfaceScreen(onBack: () -> Unit) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Staking Interface Coming Soon", color = PulsarColors.TextPrimaryDark)
            }
        }
    }
}

@Composable
fun WalletAccountsScreen(onBack: () -> Unit) {
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { PulsarTopBarWithBack(onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Wallet Accounts Coming Soon", color = PulsarColors.TextPrimaryDark)
            }
        }
    }
}

@Composable
fun PulsarTopBarWithBack(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp).statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}