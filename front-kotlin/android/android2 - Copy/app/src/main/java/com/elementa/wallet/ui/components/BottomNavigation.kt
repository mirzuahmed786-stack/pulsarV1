package com.elementa.wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.R
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarTypography

/**
 * Bottom navigation bar with Pulsar theme matching the provided design.
 */
@Composable
fun PulsarBottomNav(
    onHome: () -> Unit,
    onAssets: () -> Unit,
    onHub: () -> Unit,
    onActivity: () -> Unit,
    onSettings: () -> Unit,
    currentRoute: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        color = Color(0xFF030619), // Updated color matching request #030619
        tonalElevation = 8.dp
    ) {
        // Subtle top border
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                NavItemData("Home", R.drawable.home_nav, "home", onHome),
                NavItemData("Assets", R.drawable.asserts_nav, "assets", onAssets),
                NavItemData("Hub", R.drawable.hub_nav, "ecosystem", onHub),
                NavItemData("Activity", R.drawable.activity_nav, "activity", onActivity),
                NavItemData("Settings", R.drawable.setting_nav, "settings", onSettings)
            )

            navItems.forEach { item ->
                val active = currentRoute == item.route
                val activeColor = Color(0xFF00D3F2) // Updated active color
                val inactiveColor = Color(0xFF94A3B8)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                            .clickable { item.onClick() }
                    ) {
                        Image(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.label,
                            colorFilter = ColorFilter.tint(if (active) activeColor else inactiveColor),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            style = PulsarTypography.Typography.labelSmall,
                            color = if (active) activeColor else inactiveColor,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    }
}

private data class NavItemData(
    val label: String,
    val iconRes: Int,
    val route: String,
    val onClick: () -> Unit
)
