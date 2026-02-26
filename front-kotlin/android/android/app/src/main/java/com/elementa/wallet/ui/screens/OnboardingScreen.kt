package com.elementa.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    PulsarBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page)
            }

            // Pager Indicator
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) PulsarColors.PrimaryDark 
                                else PulsarColors.BorderSubtleDark
                            )
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.textButtonColors(contentColor = PulsarColors.TextSecondaryDark)
                ) {
                    Text("Skip", style = PulsarTypography.Typography.labelSmall)
                }

                PulsarComponents.PulsarButton(
                    text = if (pagerState.currentPage == 2) "Get Started" else "Next",
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.width(160.dp),
                    glow = pagerState.currentPage == 2
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: Int) {
    val data = listOf(
        OnboardingData("🛡️", "Unmatched Security", "Your assets are protected by enterprise-grade encryption and multi-signature security."),
        OnboardingData("⛓️", "Multi-Chain Access", "Manage Bitcoin, Ethereum, Solana, and more from a single, intuitive interface."),
        OnboardingData("⚡", "Instant Settlement", "Swap tokens and bridge assets across chains with blinding speed.")
    )[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = PulsarColors.PrimaryDark.copy(alpha = 0.1f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(data.icon, fontSize = 56.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            data.title,
            style = PulsarTypography.Typography.displayMedium,
            color = PulsarColors.TextPrimaryDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            data.description,
            style = PulsarTypography.Typography.bodyLarge,
            color = PulsarColors.TextSecondaryLight,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
    }
}

private data class OnboardingData(val icon: String, val title: String, val description: String)
