package com.elementa.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elementa.wallet.ui.components.PulsarBottomNav
import com.elementa.wallet.ui.designsystem.*
import com.elementa.wallet.viewmodel.LiveDataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    onDashboard: () -> Unit,
    onAssets: () -> Unit,
    onTransfers: () -> Unit,
    onSwap: () -> Unit,
    onActivity: () -> Unit,
    onHub: () -> Unit,
    onSettings: () -> Unit,
    viewModel: LiveDataViewModel = hiltViewModel()
) {
    var showFilter by remember { mutableStateOf(false) }
    var selectedChainFilter by remember { mutableStateOf("All") }

    // Mock data for Image 2 pixel perfect matching
    val allActivities = listOf(
        ActivityItem("Receive ETH", "Feb 24, 02:00", "completed", "+0.5 ETH", "tx1...", true, Icons.Default.SouthEast, Color(0xFF00FF88), "ETH"),
        ActivityItem("Send SOL", "Feb 23, 06:30", "completed", "-10 SOL", "tx2...", false, Icons.Default.NorthEast, Color(0xFFFF8844), "SOL"),
        ActivityItem("Swap USDC", "Feb 24, 03:15", "pending", "+100 USDC", "tx3...", true, Icons.Default.Sync, Color(0xFF00D3F2), "ETH")
    )

    val activities = if (selectedChainFilter == "All") allActivities else allActivities.filter { it.chain == selectedChainFilter }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040A20))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Activity",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        onClick = { showFilter = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1D293D),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FilterList, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Filter", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            bottomBar = {
                PulsarBottomNav(
                    onHome = onDashboard,
                    onAssets = onAssets,
                    onHub = onHub,
                    onActivity = {},
                    onSettings = onSettings,
                    currentRoute = "activity"
                )
            }
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(activities) { activity ->
                    ActivityCard(activity)
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        if (showFilter) {
            ModalBottomSheet(
                onDismissRequest = { showFilter = false },
                containerColor = Color(0xFF0D1421),
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp).padding(bottom = 40.dp)) {
                    Text("Filter by Chain", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    listOf("All", "ETH", "BTC", "SOL").forEach { chain ->
                        Surface(
                            onClick = { 
                                selectedChainFilter = chain
                                showFilter = false 
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedChainFilter == chain) Color(0xFF1D293D) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(chain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                if (selectedChainFilter == chain) Icon(Icons.Default.Check, null, tint = Color(0xFF00D3F2))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: ActivityItem) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0D1421).copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Surface(
                shape = CircleShape,
                color = activity.color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = activity.icon,
                        contentDescription = null,
                        tint = activity.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "${activity.date} • ${activity.status}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    activity.amount,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activity.isPositive) Color(0xFF00FF88) else Color.White
                )
                Text(
                    activity.txId,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private data class ActivityItem(
    val title: String,
    val date: String,
    val status: String,
    val amount: String,
    val txId: String,
    val isPositive: Boolean,
    val icon: ImageVector,
    val color: Color,
    val chain: String
)