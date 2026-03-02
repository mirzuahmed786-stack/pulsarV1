package com.elementa.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.PulsarBackground
import com.elementa.wallet.ui.designsystem.PulsarColors
import com.elementa.wallet.ui.designsystem.PulsarTypography
import com.elementa.wallet.viewmodel.AddTokenViewModel

@Composable
fun AddTokenScreen(
    viewModel: AddTokenViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.toggleManual(true)
    }

    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AddTokenTopBar(onBack = onDone)
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                SecurityCheckCard()

                Spacer(modifier = Modifier.height(24.dp))

                Label(text = "TOKEN CONTRACT ADDRESS")
                FormField(
                    value = state.address,
                    onValueChange = viewModel::setAddress,
                    placeholder = "0x...",
                    trailing = {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = PulsarColors.PrimaryDark.copy(alpha = 0.16f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PASTE",
                                    color = PulsarColors.PrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    style = PulsarTypography.Typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = PulsarColors.PrimaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onTrailingClick = {
                        val text = clipboard.getText()?.text.orEmpty()
                        if (text.isNotBlank()) viewModel.setAddress(text)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Label(text = "TOKEN SYMBOL")
                FormField(
                    value = state.manualSymbol,
                    onValueChange = viewModel::setManualSymbol,
                    placeholder = "e.g. PUL"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Label(text = "TOKEN NAME")
                FormField(
                    value = state.manualName,
                    onValueChange = viewModel::setManualName,
                    placeholder = "e.g. Pulsar Token"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Label(text = "DECIMALS")
                FormField(
                    value = state.manualDecimals,
                    onValueChange = viewModel::setManualDecimals,
                    placeholder = "18"
                )

                if (!state.error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.error.orEmpty(),
                        color = PulsarColors.DangerRed,
                        style = PulsarTypography.Typography.bodySmall
                    )
                }

                HorizontalDivider(
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.12f),
                    modifier = Modifier.padding(top = 30.dp, bottom = 26.dp)
                )

                Button(
                    onClick = {
                        viewModel.toggleManual(true)
                        viewModel.importToken()
                    },
                    enabled = !state.isLoading && state.address.isNotBlank(),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulsarColors.PrimaryDark,
                        contentColor = PulsarColors.BackgroundDark,
                        disabledContainerColor = PulsarColors.PrimaryDark.copy(alpha = 0.45f),
                        disabledContentColor = PulsarColors.BackgroundDark.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(PulsarColors.BackgroundDark.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PulsarColors.PrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (state.isLoading) "Adding Token..." else "Add Token to Vault",
                        style = PulsarTypography.Typography.titleMedium,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "PULSAR VAULT SECURITY VERIFIED",
                    style = PulsarTypography.CyberLabel,
                    fontSize = 10.sp,
                    color = PulsarColors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 24.dp),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.7.sp
                )
            }
        }
    }
}

@Composable
private fun AddTokenTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = PulsarColors.PrimaryDark,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Add Custom Token",
            style = PulsarTypography.Typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SecurityCheckCard() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = PulsarColors.PrimaryDark.copy(alpha = 0.09f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PulsarColors.PrimaryDark.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = PulsarColors.PrimaryDark,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(30.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Security Check",
                    style = PulsarTypography.Typography.titleMedium,
                    color = PulsarColors.PrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Make sure you trust the token contract address.",
                    style = PulsarTypography.Typography.bodyMedium,
                    color = PulsarColors.TextSecondary
                )
                Text(
                    text = "Adding malicious tokens can put your vault",
                    style = PulsarTypography.Typography.bodyMedium,
                    color = PulsarColors.TextSecondary
                )
                Text(
                    text = "security at risk.",
                    style = PulsarTypography.Typography.bodyMedium,
                    color = PulsarColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Learn about contract safety",
                        style = PulsarTypography.Typography.bodyLarge,
                        color = PulsarColors.PrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = PulsarColors.PrimaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = PulsarTypography.CyberLabel,
        fontSize = 11.sp,
        color = PulsarColors.TextSecondary,
        letterSpacing = 1.8.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp, start = 6.dp)
    )
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailing: @Composable (() -> Unit)? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = PulsarTypography.Typography.bodyLarge,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        },
        trailingIcon = {
            if (trailing != null) {
                Box(
                    modifier = if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier
                ) {
                    trailing()
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PulsarColors.PrimaryDark.copy(alpha = 0.45f),
            unfocusedBorderColor = PulsarColors.PrimaryDark.copy(alpha = 0.35f),
            focusedContainerColor = Color(0xFF071A2C),
            unfocusedContainerColor = Color(0xFF071A2C),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = PulsarColors.PrimaryDark
        )
    )
}
