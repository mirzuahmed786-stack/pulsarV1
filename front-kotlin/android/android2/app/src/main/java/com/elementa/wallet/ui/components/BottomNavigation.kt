package com.elementa.wallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarTypography

/**
 * Redesigned bottom navigation bar with Pulsar theme
 */
@Composable
fun RedesignedBottomNav(
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    currentRoute: String = "dashboard"
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PulsarColors.BackgroundDark.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.GridView,
                label = "Dashboard",
                active = currentRoute == "dashboard",
                onClick = onDashboard
            )
            BottomNavItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Assets",
                active = currentRoute == "assets",
                onClick = onAssets
            )
            BottomNavItem(
                icon = Icons.Default.NearMe,
                label = "Transfers",
                active = currentRoute == "transfers",
                onClick = onTransfers
            )
            BottomNavItem(
                icon = Icons.Default.SwapHoriz,
                label = "Swap",
                active = currentRoute == "swap",
                onClick = onSwap
            )
            BottomNavItem(
                icon = Icons.Default.ShowChart,
                label = "Activity",
                active = currentRoute == "activity",
                onClick = onActivity
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) PulsarColors.PrimaryDark else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = PulsarTypography.Typography.labelSmall,
            color = if (active) PulsarColors.PrimaryDark else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
