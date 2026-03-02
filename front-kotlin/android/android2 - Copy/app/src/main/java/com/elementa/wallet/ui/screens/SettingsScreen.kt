package com.elementa.wallet.ui.screens

import android.widget.Toast

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.ui.components.PulsarBottomNav
import com.elementa.wallet.viewmodel.VaultViewModel

@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onHub: () -> Unit,
    onActivity: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bioMetricsEnabled by viewModel.bioMetricsEnabled.collectAsState()
    
    // Mock states for UI demonstration matching Image 1
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var darkModeEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val pulsarBlue = Color(0xFF00D3F2)
    val cardBg = Color(0xFF0D1421).copy(alpha = 0.7f)
    val iconBg = Color.White.copy(alpha = 0.05f)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040A20))) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                PulsarBottomNav(
                    onHome = onDashboard,
                    onAssets = onAssets,
                    onHub = onHub,
                    onActivity = onActivity,
                    onSettings = {},
                    currentRoute = "settings"
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Page Title
                Text(
                    "Settings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // --- SECURITY SECTION ---
                SettingsSectionHeader("SECURITY")
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsActionRow(
                            icon = Icons.Outlined.Lock,
                            title = "Change PIN",
                            onClick = { /* Navigate to change pin */ }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Outlined.Shield,
                            title = "Two-Factor Authentication",
                            checked = twoFactorEnabled,
                            onCheckedChange = { twoFactorEnabled = it },
                            activeColor = pulsarBlue
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Outlined.Smartphone,
                            title = "Biometrics",
                            checked = bioMetricsEnabled,
                            onCheckedChange = { 
                                viewModel.setBiometricsEnabled(it)
                                if (it) Toast.makeText(context, "Biometrics Enabled", Toast.LENGTH_SHORT).show()
                                else Toast.makeText(context, "Biometrics Disabled", Toast.LENGTH_SHORT).show()
                            },
                            activeColor = pulsarBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- GENERAL SECTION ---
                SettingsSectionHeader("GENERAL")
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsActionRow(
                            icon = Icons.Outlined.Public,
                            title = "Active Network",
                            subtitle = "Mainnet",
                            onClick = { /* Network selector */ }
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Outlined.DarkMode,
                            title = "Dark Mode",
                            checked = darkModeEnabled,
                            onCheckedChange = { darkModeEnabled = it },
                            activeColor = pulsarBlue
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            activeColor = pulsarBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // --- Lock Wallet Button ---
                Surface(
                    onClick = { 
                        viewModel.lock()
                        onSignOut()
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF4B4B).copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = null,
                                tint = Color(0xFFFF4B4B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Lock Wallet",
                                color = Color(0xFFFF4B4B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.4f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Color.White.copy(alpha = 0.05f),
        thickness = 1.dp
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.weight(1f))
        
        if (subtitle != null) {
            Text(subtitle, fontSize = 15.sp, color = Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.weight(1f))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
