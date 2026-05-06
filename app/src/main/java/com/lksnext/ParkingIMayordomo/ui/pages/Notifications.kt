package com.lksnext.ParkingIMayordomo.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Notification
import com.lksnext.ParkingIMayordomo.data.model.NotificationType
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.ui.viewmodel.NotificationsViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Notifications(
    viewModel: NotificationsViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.read }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_NOTIFICATIONS,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ParkingTopAppBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = { /* Already here */ }
                )
            },
            bottomBar = {
                ParkingBottomBar(
                    selectedItem = -1,
                    onItemSelected = { index ->
                        val routes = listOf(ROUTE_DASHBOARD, ROUTE_HISTORY, ROUTE_PROFILE, ROUTE_VIEW_PARKING)
                        onNavigate(routes[index])
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.notifications_title),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.notifications_subtitle),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (unreadCount > 0) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(
                            onClick = { viewModel.markAllAsRead() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.mark_all_read), fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (notifications.isEmpty()) {
                    EmptyNotifications()
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(notifications, key = { _, it -> it.id }) { index, notification ->
                                NotificationItem(
                                    notification = notification,
                                    onRead = { viewModel.markAsRead(notification.id) },
                                    onDelete = { viewModel.deleteNotification(notification.id) }
                                )
                                if (index < notifications.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = if (notification.read) Color.Transparent else when (notification.type) {
        NotificationType.WARNING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        NotificationType.SUCCESS -> SuccessGreen.copy(alpha = 0.1f)
        NotificationType.INFO -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    }

    val icon = when (notification.type) {
        NotificationType.WARNING -> Icons.Default.Warning
        NotificationType.SUCCESS -> Icons.Default.CheckCircle
        NotificationType.INFO -> Icons.Default.Info
    }

    val iconColor = when (notification.type) {
        NotificationType.WARNING -> MaterialTheme.colorScheme.error
        NotificationType.SUCCESS -> SuccessGreen
        NotificationType.INFO -> MaterialTheme.colorScheme.primary
    }

    val dateFormatStr = stringResource(R.string.notification_date_format)
    val sdf = remember(dateFormatStr) { SimpleDateFormat(dateFormatStr, Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val title = notification.titleResId?.let { stringResource(it) } ?: notification.title ?: ""
                    Text(
                        text = title,
                        fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!notification.read) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.new_label),
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val message = notification.messageResId?.let {
                    stringResource(it, *notification.messageArgs.toTypedArray())
                } ?: notification.message ?: ""
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sdf.format(notification.time),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (!notification.read) {
                    TextButton(
                        onClick = onRead,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.mark_read), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_delete), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyNotifications() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_notifications),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
