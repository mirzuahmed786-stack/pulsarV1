package com.elementa.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarTypography

data class SidebarItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val route: String,
    val highlighted: Boolean = false
)

@Composable
fun PulsarSidebar(
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    // Updated sidebar items with proper Screen routes
    val primaryItems = listOf(
        SidebarItem("My Vaults", icon = Icons.Default.AccountBalanceWallet, route = "accounts", highlighted = true),
        SidebarItem("Assets", icon = Icons.Default.Wallet, route = "assets"),
        SidebarItem("Notifications", subtitle = "ALERTS", icon = Icons.Default.Notifications, route = "notifications"),
        SidebarItem("Security Center", icon = Icons.Default.Security, route = "settings"),
        SidebarItem("Connected Apps", subtitle = "WALLETCONNECT", icon = Icons.Default.Hub, route = "settings"),
        SidebarItem("Network Settings", icon = Icons.Default.AccountTree, route = "settings")
    )
    val supportItems = listOf(
        SidebarItem("Activity History", icon = Icons.Default.History, route = "activity"),
        SidebarItem("Contact Support", icon = Icons.Default.SupportAgent, route = "settings"),
        SidebarItem("About Pulsar", icon = Icons.Default.Info, route = "settings")
    )

    BoxWithConstraints {
        val compact = maxWidth < 330.dp
        val sidebarWidth = if (compact) 312.dp else 340.dp
        val headerRing = if (compact) 78.dp else 88.dp
        val headerBadge = if (compact) 22.dp else 26.dp
        val titleSize = if (compact) 20.sp else 22.sp
        val addressSize = if (compact) 11.sp else 12.sp
        val itemTitle = if (compact) 13.sp else 14.sp
        val supportLabel = if (compact) 10.sp else 11.sp
        val lockText = if (compact) 13.sp else 14.sp

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(sidebarWidth)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PulsarColors.BackgroundDark,
                            PulsarColors.SurfaceDark,
                            PulsarColors.BackgroundDark
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
                )
                .padding(horizontal = if (compact) 16.dp else 20.dp, vertical = 26.dp)
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable {
                    onNavigate("home")
                    onClose()
                }
        ) {
            Box(
                modifier = Modifier.size(if (compact) 86.dp else 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(headerRing)
                        .border(if (compact) 2.4.dp else 3.dp, PulsarColors.PrimaryDark, CircleShape)
                        .padding(if (compact) 6.dp else 7.dp)
                        .background(PulsarColors.PanelDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PulsarColors.PrimaryDark,
                        modifier = Modifier.size(if (compact) 34.dp else 40.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(headerBadge)
                        .background(PulsarColors.PrimaryDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PulsarColors.BackgroundDark,
                        modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(if (compact) 10.dp else 14.dp))

            Column {
                Text(
                    "PULSAR Wallet",
                    style = PulsarTypography.Typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = titleSize
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(top = if (compact) 8.dp else 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (compact) 12.dp else 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "0x71C...3921",
                            style = PulsarTypography.Typography.labelMedium,
                            color = PulsarColors.PrimaryDark,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            fontSize = addressSize
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = PulsarColors.PrimaryDark,
                            modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = PulsarColors.PrimaryDark.copy(alpha = 0.35f),
            modifier = Modifier.padding(top = if (compact) 20.dp else 24.dp, bottom = if (compact) 16.dp else 22.dp)
        )

        primaryItems.forEach { item ->
            SidebarItemRow(
                item = item,
                onClick = {
                    onNavigate(item.route)
                    onClose()
                },
                modifier = Modifier.padding(bottom = if (item.highlighted) 14.dp else 10.dp),
                titleFontSize = itemTitle,
                subtitleFontSize = if (compact) 7.sp else 8.sp,
                iconSize = if (compact) 22.dp else 24.dp,
                highlightedIconSize = if (compact) 26.dp else 28.dp,
                rowVerticalPadding = if (compact) 10.dp else 12.dp
            )
        }

        Text(
            text = "SUPPORT & INFO",
            style = PulsarTypography.CyberLabel,
            color = PulsarColors.TextSecondary,
            fontSize = supportLabel,
            letterSpacing = 2.4.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = if (compact) 14.dp else 20.dp, start = 4.dp)
        )

        supportItems.forEach { item ->
            SidebarItemRow(
                item = item,
                onClick = {
                    onNavigate(item.route)
                    onClose()
                },
                modifier = Modifier.padding(bottom = 10.dp),
                titleFontSize = itemTitle,
                subtitleFontSize = if (compact) 7.sp else 8.sp,
                iconSize = if (compact) 22.dp else 24.dp,
                highlightedIconSize = if (compact) 26.dp else 28.dp,
                rowVerticalPadding = if (compact) 10.dp else 12.dp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                onNavigate("unlock")
                onClose()
            },
            shape = RoundedCornerShape(30.dp),
            border = androidx.compose.foundation.BorderStroke(1.8.dp, PulsarColors.PrimaryDark.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = PulsarColors.PrimaryDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 56.dp else 58.dp)
                .padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 20.dp else 22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Lock Vault",
                style = PulsarTypography.Typography.titleMedium,
                fontSize = lockText,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onNavigate("home")
                    onClose()
                }
                .padding(top = 28.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PULSAR WALLET",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.TextSecondary,
                letterSpacing = 2.sp
            )
            Text(
                text = "v2.4.0-stable • Build 842",
                style = PulsarTypography.Typography.bodySmall,
                color = PulsarColors.TextSecondary
            )
        }
    }
    }
}

@Composable
private fun SidebarItemRow(
    item: SidebarItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    subtitleFontSize: androidx.compose.ui.unit.TextUnit,
    iconSize: androidx.compose.ui.unit.Dp,
    highlightedIconSize: androidx.compose.ui.unit.Dp,
    rowVerticalPadding: androidx.compose.ui.unit.Dp
) {
    val shape = RoundedCornerShape(28.dp)

    Surface(
        onClick = onClick,
        color = if (item.highlighted) PulsarColors.PrimaryDark.copy(alpha = 0.13f) else Color.Transparent,
        shape = shape,
        border = if (item.highlighted) {
            androidx.compose.foundation.BorderStroke(1.4.dp, PulsarColors.PrimaryDark.copy(alpha = 0.35f))
        } else {
            null
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = rowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (item.highlighted) 48.dp else 32.dp)
                    .background(
                        color = if (item.highlighted) {
                            PulsarColors.PrimaryDark.copy(alpha = 0.22f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(if (item.highlighted) 16.dp else 0.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = PulsarColors.PrimaryDark,
                    modifier = Modifier.size(if (item.highlighted) highlightedIconSize else iconSize)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = if (item.highlighted) {
                        PulsarTypography.Typography.titleMedium
                    } else {
                        PulsarTypography.Typography.bodyLarge
                    },
                    color = Color.White,
                    fontSize = titleFontSize,
                    fontWeight = if (item.highlighted) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                item.subtitle?.let {
                    Text(
                        text = it,
                        style = PulsarTypography.CyberLabel,
                        fontSize = subtitleFontSize,
                        color = PulsarColors.TextSecondary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PulsarColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
