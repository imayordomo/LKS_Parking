package com.lksnext.ParkingIMayordomo.ui.pages

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.AuthManager
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.viewmodel.HistoryViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.TestTags
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_ABOUT
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
fun History(
    viewModel: HistoryViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val reservations by viewModel.filteredReservations.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val startDateText by viewModel.startDateText.collectAsState()
    val endDateText by viewModel.endDateText.collectAsState()

    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }
    
    val now = remember { Date() }
    val todayStr = remember(now) { ParkingUtils.formatDate(now) }
    val currentTimeStr = remember(now) { ParkingUtils.formatTime(now) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val headers = listOf(
                        context.getString(R.string.csv_header_spot),
                        context.getString(R.string.csv_header_date),
                        context.getString(R.string.csv_header_start),
                        context.getString(R.string.csv_header_end),
                        context.getString(R.string.csv_header_license)
                    )
                    val csvContent = viewModel.generateCsvContent(headers)
                    outputStream.write(csvContent.toByteArray())
                }
            } catch (e: java.io.IOException) {
                Log.e("History", "Failed to write CSV", e)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_HISTORY,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        val notifications by AuthManager.notifications.collectAsState()
        val unreadCount = notifications.count { !it.read }
        Scaffold(
            topBar = {
                ParkingTopAppBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = { onNavigate(ROUTE_NOTIFICATIONS) },
                    unreadNotificationsCount = unreadCount
                )
            },
            bottomBar = {
                ParkingBottomBar(
                    selectedItem = 1,
                    onItemSelected = { index ->
                        val routes = listOf(ROUTE_DASHBOARD, ROUTE_HISTORY, ROUTE_PROFILE, ROUTE_VIEW_PARKING, ROUTE_ABOUT)
                        onNavigate(routes[index])
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.history_title),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.history_subtitle),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                createDocumentLauncher.launch(context.getString(R.string.csv_filename))
                            },
                            modifier = Modifier.padding(top = 8.dp).testTag(TestTags.HISTORY_DOWNLOAD_CSV),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.download_button),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FilterList, null, tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.filters),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { filtersExpanded = !filtersExpanded }, modifier = Modifier.testTag(TestTags.HISTORY_EXPAND_FILTERS)) {
                                    Icon(
                                        imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            AnimatedVisibility(visible = filtersExpanded) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    Text(
                                        text = stringResource(R.string.date_range),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val startInteractionSource = remember { MutableInteractionSource() }
                                        if (startInteractionSource.collectIsPressedAsState().value) showStartDatePicker = true

                                        OutlinedTextField(
                                            value = startDateText,
                                            onValueChange = { },
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.from_date)) },
                                            modifier = Modifier.weight(1f).testTag(TestTags.HISTORY_START_DATE_FIELD),
                                            interactionSource = startInteractionSource,
                                            enabled = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        val endInteractionSource = remember { MutableInteractionSource() }
                                        if (endInteractionSource.collectIsPressedAsState().value) showEndDatePicker = true

                                        OutlinedTextField(
                                            value = endDateText,
                                            onValueChange = { },
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.to_date)) },
                                            modifier = Modifier.weight(1f).testTag(TestTags.HISTORY_END_DATE_FIELD),
                                            interactionSource = endInteractionSource,
                                            enabled = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = stringResource(R.string.reservation_status),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatusFilterButton(
                                            label = stringResource(R.string.filter_all),
                                            selected = statusFilter == "all",
                                            onClick = { viewModel.setStatusFilter("all") },
                                            modifier = Modifier.weight(1f).testTag(TestTags.HISTORY_FILTER_ALL)
                                        )
                                        StatusFilterButton(
                                            label = stringResource(R.string.filter_past),
                                            selected = statusFilter == "past",
                                            onClick = { viewModel.setStatusFilter("past") },
                                            modifier = Modifier.weight(1f).testTag(TestTags.HISTORY_FILTER_PAST)
                                        )
                                        StatusFilterButton(
                                            label = stringResource(R.string.filter_future),
                                            selected = statusFilter == "future",
                                            onClick = { viewModel.setStatusFilter("future") },
                                            modifier = Modifier.weight(1f).testTag(TestTags.HISTORY_FILTER_FUTURE)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (statusFilter != "all" || startDateText.isNotEmpty() || endDateText.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { viewModel.clearFilters() }, modifier = Modifier.testTag(TestTags.HISTORY_CLEAR_FILTERS)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.clear_filters), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (reservations.isEmpty()) {
                    item {
                        EmptyHistory()
                    }
                } else {
                    itemsIndexed(reservations, key = { _, res -> res.id }) { index, reservation ->
                        val vehicle = vehicles?.find { it.id == reservation.vehicleId }
                        ReservationHistoryItem(
                            reservation = reservation, 
                            vehicle = vehicle,
                            todayStr = todayStr,
                            currentTimeStr = currentTimeStr
                        )
                        if (index < reservations.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let {
                        val selected = Calendar.getInstance().apply { timeInMillis = it }
                        viewModel.setStartDate(ParkingUtils.formatDate(selected.time))
                    }
                    showStartDatePicker = false
                }, modifier = Modifier.testTag(TestTags.HISTORY_DATE_PICKER_SAVE)) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }, modifier = Modifier.testTag(TestTags.HISTORY_DATE_PICKER_CANCEL)) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let {
                        val selected = Calendar.getInstance().apply { timeInMillis = it }
                        viewModel.setEndDate(ParkingUtils.formatDate(selected.time))
                    }
                    showEndDatePicker = false
                }, modifier = Modifier.testTag(TestTags.HISTORY_DATE_PICKER_SAVE)) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }, modifier = Modifier.testTag(TestTags.HISTORY_DATE_PICKER_CANCEL)) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

@Composable
fun StatusFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        elevation = null,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ReservationHistoryItem(
    reservation: Reservation, 
    vehicle: Vehicle?,
    todayStr: String, 
    currentTimeStr: String
) {
    val dateFormatStr = stringResource(R.string.date_display_format)
    val displaySdf = remember(dateFormatStr) { SimpleDateFormat(dateFormatStr, Locale.getDefault()) }
    val resDate = remember(reservation.date) { 
        ParkingUtils.parseDate(reservation.date) ?: Date()
    }
    
    val isPast = remember(reservation.date, reservation.endTime, todayStr, currentTimeStr) {
        reservation.date < todayStr || (reservation.date == todayStr && reservation.endTime < currentTimeStr)
    }
    val isActive = remember(reservation.date, reservation.startTime, reservation.endTime, todayStr, currentTimeStr) {
        reservation.date == todayStr && reservation.startTime <= currentTimeStr && reservation.endTime > currentTimeStr
    }

    val itemAlpha = when {
        isPast -> 0.5f
        isActive -> 1f
        else -> 1f
    }
    val badgeColor = when {
        isPast -> MaterialTheme.colorScheme.outlineVariant
        isActive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val badgeTextColor = when {
        isPast -> MaterialTheme.colorScheme.onSurfaceVariant
        isActive -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val statusLabel = when {
        isPast -> stringResource(R.string.status_past)
        isActive -> stringResource(R.string.status_active)
        else -> stringResource(R.string.status_next)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(end = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.spot_short_prefix),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = itemAlpha)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.spot_label_format, stringResource(R.string.spot_short_prefix), reservation.spotNumber),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = itemAlpha)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = displaySdf.format(resDate),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = badgeTextColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.time_range_format, reservation.startTime, reservation.endTime),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                if (vehicle != null || !reservation.licensePlate.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(16.dp))
                    val vehicleType = vehicle?.type ?: com.lksnext.ParkingIMayordomo.data.model.VehicleType.CAR
                    Icon(
                        imageVector = ParkingUtils.getVehicleIcon(vehicleType),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (reservation.licensePlate ?: vehicle?.licensePlate).orEmpty(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.History, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.no_history), color = MaterialTheme.colorScheme.secondary)
    }
}
