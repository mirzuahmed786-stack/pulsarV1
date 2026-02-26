package com.elementa.wallet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*

@Composable
fun SettingsScreen(
    viewModel: com.elementa.wallet.viewmodel.VaultViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val bioMetricsEnabled by viewModel.bioMetricsEnabled.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePin by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "SECURITY SETTINGS",
                        style = PulsarTypography.CyberLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PulsarColors.SurfaceDark.copy(alpha = 0.4f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Security Score Card
                SecurityScoreCard()

                            // Appearance Section
                            SettingsSection(title = "Appearance") {
                                // Dark Theme toggle
                                SecurityToggleItem(
                                    icon = Icons.Default.DarkMode,
                                    title = "Dark Theme",
                                    subtitle = "Use dark mode for the app",
                                    checked = ThemeManager.isDark,
                                    onCheckedChange = { /* Dark mode is enforced */ }
                                )
                            }

                // Access Section
                SettingsSection(title = "Access") {
                    SecurityToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometrics",
                        subtitle = "Quick unlock for your vault",
                        checked = bioMetricsEnabled,
                        onCheckedChange = { viewModel.setBiometricsEnabled(it) }
                    )
                    SecurityActionItem(
                        icon = Icons.Default.Timer,
                        title = "Auto-Lock",
                        subtitle = "Lock vault when inactive",
                        rightText = "Immediately"
                    )
                }

                // Advanced Protection
                SettingsSection(title = "Advanced Protection") {
                    SecurityActionItem(
                        icon = Icons.Default.Key,
                        title = "Recovery Phrase",
                        subtitle = "View your secret seed phrase"
                    )
                    SecurityActionItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Wallet",
                        subtitle = "Remove all data from this device",
                        isAlert = true,
                        onClick = { showDeleteDialog = true }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                PulsarComponents.PulsarOutlinedButton(
                    text = "Sign Out & Lock Vault",
                    onClick = { 
                        viewModel.lock()
                        onSignOut()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = PulsarColors.SurfaceDark,
            title = { Text("Delete Wallet?", color = Color.White) },
            text = {
                Column {
                    Text("This action will permanently remove this wallet from this device. Enter your PIN to confirm.", color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deletePin,
                        onValueChange = { deletePin = it },
                        label = { Text("Enter PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = deleteError != null,
                        supportingText = deleteError?.let { { Text(it, color = PulsarColors.DangerRed) } }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVault(deletePin) { success ->
                        if (success) {
                            showDeleteDialog = false
                            onSignOut()
                        } else {
                            deleteError = "Incorrect PIN"
                        }
                    }
                }) {
                    Text("DELETE", color = PulsarColors.DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SecurityScoreCard() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = PulsarColors.SurfaceDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.05f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = PulsarColors.PrimaryDark,
                        startAngle = -90f,
                        sweepAngle = 306f, // 85%
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text("85%", style = PulsarTypography.Typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Security Score", style = PulsarTypography.Typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Your vault is well protected. Add 2FA to reach 100%.", style = PulsarTypography.Typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                Surface(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    color = PulsarColors.PrimaryDark,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "Improve Score",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = PulsarTypography.Typography.labelSmall,
                        color = PulsarColors.BackgroundDark,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title.uppercase(),
            style = PulsarTypography.CyberLabel,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PulsarColors.SurfaceDark,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SecurityToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.05f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = PulsarTypography.Typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = PulsarTypography.Typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PulsarColors.PrimaryDark,
                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SecurityActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    rightText: String? = null,
    isAlert: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.05f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            if (isAlert) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(PulsarColors.DangerRed)
                        .border(2.dp, PulsarColors.SurfaceDark, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = PulsarTypography.Typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = PulsarTypography.Typography.bodySmall,
                color = if (isAlert) PulsarColors.DangerRed.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                fontWeight = if (isAlert) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (rightText != null) {
            Text(rightText, style = PulsarTypography.Typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
    }
}
