package com.elementa.wallet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elementa.wallet.ui.designsystem.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// Notification Data Models
// ─────────────────────────────────────────────────────────────

data class WalletNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionRoute: String? = null
)

enum class NotificationType {
    TRANSACTION_RECEIVED,
    TRANSACTION_SENT,
    TRANSACTION_CONFIRMED,
    SECURITY_ALERT,
    PRICE_ALERT,
    SYSTEM_UPDATE,
    STAKING_REWARD
}

// ─────────────────────────────────────────────────────────────
// Notifications State
// ─────────────────────────────────────────────────────────────

data class NotificationsUiState(
    val notifications: List<WalletNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

// ─────────────────────────────────────────────────────────────
// Notifications ViewModel
// ─────────────────────────────────────────────────────────────

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    
    init {
        loadMockNotifications()
    }
    
    private fun loadMockNotifications() {
        // In production, this would fetch from a repository
        val mockNotifications = listOf(
            WalletNotification(
                id = "1",
                type = NotificationType.TRANSACTION_RECEIVED,
                title = "ETH Received",
                message = "You received 0.5 ETH from 0x71C...3921",
                timestamp = System.currentTimeMillis() - 300_000,
                isRead = false,
                actionRoute = "activity"
            ),
            WalletNotification(
                id = "2",
                type = NotificationType.TRANSACTION_CONFIRMED,
                title = "Transaction Confirmed",
                message = "Your swap of 1.2 ETH → 2,400 USDC has completed",
                timestamp = System.currentTimeMillis() - 3_600_000,
                isRead = false,
                actionRoute = "activity"
            ),
            WalletNotification(
                id = "3",
                type = NotificationType.SECURITY_ALERT,
                title = "Security Check",
                message = "Your wallet passed all security checks",
                timestamp = System.currentTimeMillis() - 86_400_000,
                isRead = true
            ),
            WalletNotification(
                id = "4",
                type = NotificationType.PRICE_ALERT,
                title = "Price Alert",
                message = "BTC crossed $100,000 - up 5.2% today",
                timestamp = System.currentTimeMillis() - 172_800_000,
                isRead = true
            ),
            WalletNotification(
                id = "5",
                type = NotificationType.STAKING_REWARD,
                title = "Staking Rewards",
                message = "You earned 0.02 ETH from staking",
                timestamp = System.currentTimeMillis() - 259_200_000,
                isRead = true
            )
        )
        
        _uiState.value = NotificationsUiState(
            notifications = mockNotifications,
            unreadCount = mockNotifications.count { !it.isRead }
        )
    }
    
    fun markAsRead(notificationId: String) {
        val updated = _uiState.value.notifications.map { notification ->
            if (notification.id == notificationId) {
                notification.copy(isRead = true)
            } else {
                notification
            }
        }
        _uiState.value = _uiState.value.copy(
            notifications = updated,
            unreadCount = updated.count { !it.isRead }
        )
    }
    
    fun markAllAsRead() {
        val updated = _uiState.value.notifications.map { it.copy(isRead = true) }
        _uiState.value = _uiState.value.copy(
            notifications = updated,
            unreadCount = 0
        )
    }
    
    fun deleteNotification(notificationId: String) {
        val updated = _uiState.value.notifications.filter { it.id != notificationId }
        _uiState.value = _uiState.value.copy(
            notifications = updated,
            unreadCount = updated.count { !it.isRead }
        )
    }
    
    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        // Reload notifications (in production, this would fetch from server)
        loadMockNotifications()
        _uiState.value = _uiState.value.copy(isLoading = false)
    }
}

// ─────────────────────────────────────────────────────────────
// Notifications Screen Composable
// ─────────────────────────────────────────────────────────────

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    PulsarBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NotificationsTopBar(
                    unreadCount = state.unreadCount,
                    onBack = onBack,
                    onMarkAllRead = { viewModel.markAllAsRead() },
                    onRefresh = { viewModel.refresh() }
                )
            }
        ) { padding ->
            if (state.notifications.isEmpty()) {
                EmptyNotificationsState(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(
                        items = state.notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { viewModel.markAsRead(notification.id) },
                            onDelete = { viewModel.deleteNotification(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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
                Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onRefresh() }
        ) {
            Text(
                "PULSAR",
                style = PulsarTypography.CyberLabel,
                color = PulsarColors.PrimaryDark,
                letterSpacing = 4.sp,
                fontSize = 14.sp
            )
            if (unreadCount > 0) {
                Text(
                    "$unreadCount unread",
                    style = PulsarTypography.Typography.labelSmall,
                    color = PulsarColors.PrimaryDark.copy(alpha = 0.7f)
                )
            }
        }
        
        if (unreadCount > 0) {
            TextButton(onClick = onMarkAllRead) {
                Text(
                    "Mark all read",
                    color = PulsarColors.PrimaryDark,
                    fontSize = 12.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.size(44.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationItem(
    notification: WalletNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            true
        }
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (notification.isRead) {
            PulsarColors.SurfaceDark
        } else {
            PulsarColors.PrimaryDark.copy(alpha = 0.08f)
        },
        label = "notificationBg"
    )
    
    val borderColor = if (notification.isRead) {
        Color.White.copy(alpha = 0.05f)
    } else {
        PulsarColors.PrimaryDark.copy(alpha = 0.3f)
    }
    
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PulsarColors.DangerRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = PulsarColors.DangerRed
                )
            }
        },
        content = {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                color = backgroundColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Notification type icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                getNotificationIconBgColor(notification.type).copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getNotificationIcon(notification.type),
                            contentDescription = null,
                            tint = getNotificationIconBgColor(notification.type),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                notification.title,
                                style = PulsarTypography.Typography.titleMedium,
                                color = Color.White,
                                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            if (!notification.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(PulsarColors.PrimaryDark, CircleShape)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            notification.message,
                            style = PulsarTypography.Typography.bodyMedium,
                            color = PulsarColors.TextSecondaryDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            formatTimestamp(notification.timestamp),
                            style = PulsarTypography.Typography.labelSmall,
                            color = PulsarColors.TextMutedDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun EmptyNotificationsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        PulsarColors.PrimaryDark.copy(alpha = 0.1f),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = PulsarColors.PrimaryDark,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Text(
                "All Caught Up!",
                style = PulsarTypography.Typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "No new notifications",
                style = PulsarTypography.Typography.bodyMedium,
                color = PulsarColors.TextSecondaryDark
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helper Functions
// ─────────────────────────────────────────────────────────────

private fun getNotificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.TRANSACTION_RECEIVED -> Icons.Default.CallReceived
        NotificationType.TRANSACTION_SENT -> Icons.Default.CallMade
        NotificationType.TRANSACTION_CONFIRMED -> Icons.Default.CheckCircle
        NotificationType.SECURITY_ALERT -> Icons.Default.Security
        NotificationType.PRICE_ALERT -> Icons.Default.TrendingUp
        NotificationType.SYSTEM_UPDATE -> Icons.Default.SystemUpdate
        NotificationType.STAKING_REWARD -> Icons.Default.Stars
    }
}

private fun getNotificationIconBgColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.TRANSACTION_RECEIVED -> PulsarColors.PrimaryDark
        NotificationType.TRANSACTION_SENT -> PulsarColors.WarningAmber
        NotificationType.TRANSACTION_CONFIRMED -> PulsarColors.SuccessGreen
        NotificationType.SECURITY_ALERT -> PulsarColors.DangerRed
        NotificationType.PRICE_ALERT -> PulsarColors.InfoBlue
        NotificationType.SYSTEM_UPDATE -> PulsarColors.TextSecondaryDark
        NotificationType.STAKING_REWARD -> PulsarColors.PrimaryDark
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> "${diff / 604_800_000}w ago"
    }
}
