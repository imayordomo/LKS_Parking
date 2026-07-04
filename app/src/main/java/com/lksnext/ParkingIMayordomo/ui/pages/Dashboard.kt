package com.lksnext.ParkingIMayordomo.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.utils.TestTags
import com.lksnext.ParkingIMayordomo.ui.viewmodel.DashboardViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.PARAM_SHOW_VEHICLE_ALERT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_ABOUT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_EDIT_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NEW_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val user by viewModel.user.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val userReservations by viewModel.userReservations.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var reservationToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    val navigateToNewReservation = remember(vehicles, onNavigate) {
        val list = vehicles
        {
            if (list != null && list.isEmpty()) {
                onNavigate("${ROUTE_PROFILE}?${PARAM_SHOW_VEHICLE_ALERT}=true")
            } else {
                onNavigate(ROUTE_NEW_RESERVATION)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_DASHBOARD,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                },
                user = user
            )
        }
    ) {
        val unreadCount = notifications.count { !it.read }
        Scaffold(
            modifier = modifier,
            topBar = {
                ParkingTopAppBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = { onNavigate(ROUTE_NOTIFICATIONS) },
                    unreadNotificationsCount = unreadCount
                )
            },
            bottomBar = {
                ParkingBottomBar(
                    selectedItem = 0,
                    onItemSelected = { index ->
                        val routes = listOf(ROUTE_DASHBOARD, ROUTE_HISTORY, ROUTE_PROFILE, ROUTE_VIEW_PARKING, ROUTE_ABOUT)
                        onNavigate(routes[index])
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = navigateToNewReservation,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag(TestTags.DASHBOARD_ADD_FAB)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add))
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.my_reservations),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = stringResource(R.string.welcome_user, user?.name ?: ""),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (vehicles != null && vehicles?.isEmpty() == true) {
                    item {
                        DashboardSpotOverview(onNavigate = onNavigate)
                    }
                }

                if (userReservations.isEmpty()) {
                    item {
                        DashboardNoReservationsState(navigateToNewReservation = navigateToNewReservation)
                    }
                } else {
                    val todayStr = ParkingUtils.formatDate(Date())
                    val hasTodayReservation = userReservations.any { it.date == todayStr }
                    val closestFutureReservationId = if (!hasTodayReservation) {
                        userReservations.firstOrNull()?.id
                    } else null

                    items(userReservations, key = { it.id }) { reservation ->
                        val vehicle = vehicles?.find { it.id == reservation.vehicleId }
                        ReservationItem(
                            reservation = reservation,
                            vehicle = vehicle,
                            onEdit = { onNavigate("${ROUTE_EDIT_RESERVATION}/${reservation.id}") },
                            onDelete = { reservationToDeleteId = reservation.id },
                            defaultExpanded = reservation.date == todayStr || reservation.id == closestFutureReservationId
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (reservationToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { reservationToDeleteId = null },
            modifier = Modifier.testTag(TestTags.DASHBOARD_DELETE_RESERVATION_DIALOG),
            title = { Text(stringResource(R.string.delete_reservation_title)) },
            text = { Text(stringResource(R.string.delete_reservation_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        reservationToDeleteId?.let { viewModel.deleteReservation(it) }
                        reservationToDeleteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag(TestTags.DASHBOARD_DELETE_RESERVATION_CONFIRM)
                ) {
                    Text(stringResource(R.string.delete_btn))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { reservationToDeleteId = null },
                    modifier = Modifier.testTag(TestTags.DASHBOARD_DELETE_RESERVATION_CANCEL)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun ReservationItem(
    reservation: Reservation,
    vehicle: Vehicle?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    defaultExpanded: Boolean = false
) {
    val todayStr = ParkingUtils.formatDate(Date())
    val isToday = reservation.date == todayStr
    
    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
    
    val tomorrowStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        ParkingUtils.formatDate(cal.time)
    }

    val isTomorrow = reservation.date == tomorrowStr

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
            .testTag(TestTags.DASHBOARD_RESERVATION_CARD),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.spot_display, 
                        stringResource(R.string.spot_short_prefix), 
                        reservation.spotNumber
                    ),
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Light
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isToday) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.today_tag),
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isTomorrow) {
                        Surface(
                            color = TomorrowOrange,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tomorrow_tag),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        val dayMonthFormatStr = stringResource(R.string.day_month_format)
                        val tagSdf = remember(dayMonthFormatStr) { SimpleDateFormat(dayMonthFormatStr, Locale.getDefault()) }
                        val date = remember(reservation.date) { ParkingUtils.parseDate(reservation.date) }
                        Surface(
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = date?.let { tagSdf.format(it).uppercase() } ?: "",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                DashboardReservationCard(
                    reservation = reservation,
                    vehicle = vehicle,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun DashboardReservationCard(
    reservation: Reservation,
    vehicle: Vehicle?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val fullDateFormatStr = stringResource(R.string.full_date_format)
    val displayDateSdf = remember(fullDateFormatStr) { SimpleDateFormat(fullDateFormatStr, Locale.getDefault()) }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            val date = remember(reservation.date) { ParkingUtils.parseDate(reservation.date) }
            Text(
                text = date?.let { displayDateSdf.format(it) } ?: reservation.date,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.time_range_format, reservation.startTime, reservation.endTime),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
        }

        if (reservation.licensePlate != null) {
            val vehicleType = vehicle?.type ?: run {
                val spotType = ParkingUtils.getSpotType(reservation.spotNumber)
                when (spotType) {
                    com.lksnext.ParkingIMayordomo.utils.SpotType.MOTORCYCLE -> com.lksnext.ParkingIMayordomo.data.model.VehicleType.MOTORCYCLE
                    com.lksnext.ParkingIMayordomo.utils.SpotType.DISABLED -> com.lksnext.ParkingIMayordomo.data.model.VehicleType.DISABLED
                    com.lksnext.ParkingIMayordomo.utils.SpotType.ELECTRIC -> com.lksnext.ParkingIMayordomo.data.model.VehicleType.ELECTRIC
                    else -> com.lksnext.ParkingIMayordomo.data.model.VehicleType.CAR
                }
            }
            val icon = ParkingUtils.getVehicleIcon(vehicleType)
            val iconColor = ParkingUtils.getVehicleColor(vehicleType)

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = reservation.licensePlate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onEdit, modifier = Modifier.testTag(TestTags.DASHBOARD_RESERVATION_EDIT)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.content_desc_edit),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag(TestTags.DASHBOARD_RESERVATION_DELETE)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_desc_delete),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DashboardSpotOverview(onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.DASHBOARD_ADD_VEHICLE_ALERT),
        colors = CardDefaults.cardColors(containerColor = LightOrangeBackground),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning, 
                contentDescription = stringResource(R.string.content_desc_warning), 
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.register_vehicle_alert),
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )
            TextButton(onClick = { onNavigate(ROUTE_PROFILE) }) {
                Text(
                    text = stringResource(R.string.add_button),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DashboardNoReservationsState(navigateToNewReservation: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .testTag(TestTags.DASHBOARD_CREATE_RESERVATION_EMPTY),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.spot_short_prefix),
                fontSize = 80.sp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.no_active_reservations),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = navigateToNewReservation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.create_reservation),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
