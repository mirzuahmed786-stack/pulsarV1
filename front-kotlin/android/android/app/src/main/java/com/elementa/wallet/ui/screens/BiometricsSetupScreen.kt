package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.VaultViewModel

@Composable
// Offers a biometric enable prompt after PIN setup.
fun BiometricsSetupScreen(
    viewModel: VaultViewModel,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val isEnabled by viewModel.bioMetricsEnabled.collectAsState()

    PulsarBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.3f)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = PulsarColors.PrimaryDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Enable Biometrics",
                    style = PulsarTypography.Typography.headlineLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Use fingerprint or face unlock for faster access to your vault.",
                    style = PulsarTypography.Typography.bodyMedium,
                    color = PulsarColors.TextSecondaryLight,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PulsarColors.SurfaceDark,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Biometric Unlock",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Unlock without typing your PIN",
                                color = PulsarColors.TextSecondaryLight,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.setBiometricsEnabled(it) }
                        )
                    }
                }

                PulsarComponents.PulsarButton(
                    text = "Continue",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    backgroundColor = PulsarColors.PrimaryDark,
                    textColor = Color.Black,
                    glow = true
                )

                TextButton(onClick = onSkip) {
                    Text("Skip for now", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}
