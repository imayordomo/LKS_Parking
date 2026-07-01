package com.lksnext.ParkingIMayordomo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.AuthManager
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_ABOUT
import com.lksnext.ParkingIMayordomo.utils.TestTags
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HELP
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_REPORT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingTopAppBar(
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationsCount: Int = 0
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick, modifier = Modifier.testTag(TestTags.NAV_MENU_BUTTON)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.content_desc_menu),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationsClick, modifier = Modifier.testTag(TestTags.NAV_NOTIFICATIONS_BUTTON)) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationsCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(unreadNotificationsCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.content_desc_notifications),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun ParkingBottomBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Icons.Default.EventAvailable, stringResource(R.string.menu_dashboard), 0),
            Triple(Icons.Default.History, stringResource(R.string.menu_history), 1),
            Triple(Icons.Default.Person, stringResource(R.string.menu_profile), 2),
            Triple(Icons.Default.Search, stringResource(R.string.menu_parking), 3)
        )

        items.forEach { (icon, label, index) ->
            val tag = when (index) {
                0 -> TestTags.NAV_BOTTOM_DASHBOARD
                1 -> TestTags.NAV_BOTTOM_HISTORY
                2 -> TestTags.NAV_BOTTOM_PROFILE
                3 -> TestTags.NAV_BOTTOM_PARKING
                else -> ""
            }
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                modifier = Modifier.testTag(tag),
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(text = label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun ParkingDrawerContent(
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    val user by AuthManager.user.collectAsState()
    val userName = user?.name.orEmpty()
    val userEmail = user?.email.orEmpty()
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userName.firstOrNull()?.toString()?.uppercase().orEmpty(),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = userName.ifBlank { stringResource(R.string.default_user_name) },
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userEmail,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DrawerMenuItems(currentRoute = currentRoute, onItemClick = onItemClick)
    }
}

@Composable
private fun DrawerMenuItems(
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    val menuItems = listOf(
        Triple(Icons.Default.EventAvailable, stringResource(R.string.my_reservations), ROUTE_DASHBOARD),
        Triple(Icons.Default.History, stringResource(R.string.drawer_history), ROUTE_HISTORY),
        Triple(Icons.Default.Person, stringResource(R.string.drawer_profile), ROUTE_PROFILE),
        Triple(Icons.Default.Search, stringResource(R.string.drawer_parking), ROUTE_VIEW_PARKING),
        Triple(Icons.Default.Notifications, stringResource(R.string.menu_notifications), ROUTE_NOTIFICATIONS),
        Triple(Icons.Default.ReportProblem, stringResource(R.string.menu_report), ROUTE_REPORT),
        Triple(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.menu_help), ROUTE_HELP),
        Triple(Icons.Default.Info, stringResource(R.string.menu_about), ROUTE_ABOUT)
    )

    menuItems.forEach { (icon, label, route) ->
        val tag = when (route) {
            ROUTE_DASHBOARD -> TestTags.NAV_DRAWER_DASHBOARD
            ROUTE_HISTORY -> TestTags.NAV_DRAWER_HISTORY
            ROUTE_PROFILE -> TestTags.NAV_DRAWER_PROFILE
            ROUTE_VIEW_PARKING -> TestTags.NAV_DRAWER_PARKING
            ROUTE_NOTIFICATIONS -> TestTags.NAV_DRAWER_NOTIFICATIONS
            ROUTE_REPORT -> TestTags.NAV_DRAWER_REPORT
            ROUTE_HELP -> TestTags.NAV_DRAWER_HELP
            ROUTE_ABOUT -> TestTags.NAV_DRAWER_ABOUT
            else -> ""
        }
        NavigationDrawerItem(
            label = { Text(text = label, fontSize = 16.sp) },
            selected = currentRoute == route,
            onClick = { onItemClick(route) },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.padding(horizontal = 0.dp).testTag(tag)
        )
    }
}
