package com.lksnext.ParkingIMayordomo.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.utils.TestTags
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.components.subtleScrollbar
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.ui.viewmodel.NewReservationViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.PARAM_SHOW_VEHICLE_ALERT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NEW_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import com.lksnext.ParkingIMayordomo.utils.SpotType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val GRID_VIEW = "grid"
private const val DROPDOWN_VIEW = "dropdown"
private const val NO_TIME_SELECTED = -1L
private const val TOTAL_SPOTS = 50

private data class DateTimeDisplay(
    val dateText: String,
    val startTimeText: String?,
    val endTimeText: String?
)

private data class SpotSelectionState(
    val selectedSpot: Int?,
    val spotsExpanded: Boolean,
    val viewMode: String,
    val spotTypeFilter: SpotType?,
    val occupiedSpots: List<Int>,
    val availableSpotsCount: Int,
    val validationErrorResId: Int?
)

private data class ReservationConfirmData(
    val canConfirm: Boolean,
    val selectedSpot: Int?,
    val selectedDate: Calendar,
    val startTime: Calendar?,
    val endTime: Calendar?
)

private fun getFilteredSpots(occupiedSpots: List<Int>, spotTypeFilter: SpotType?): List<Int> =
    (1..TOTAL_SPOTS).filter { !occupiedSpots.contains(it) && (spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter) }

private fun getSpotsByType(spotTypeFilter: SpotType?): List<Int> =
    (1..TOTAL_SPOTS).filter { spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter }

private fun parsePrefilledDate(prefilledDate: String?, sdfDate: SimpleDateFormat): Long {
    val cal = Calendar.getInstance()
    if (prefilledDate != null) {
        try {
            sdfDate.parse(prefilledDate)?.let { cal.time = it }
        } catch (_: Exception) { android.util.Log.w("NewReservation", "Date parse failed, using default") }
    }
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun isDateWithinRange(utcTimeMillis: Long): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val weekLater = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_YEAR, 7)
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
    }
    val candidateLocal = Calendar.getInstance().apply { timeInMillis = utcTimeMillis }
    val candidateDayStart = Calendar.getInstance().apply {
        timeInMillis = candidateLocal.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return candidateDayStart.timeInMillis >= today.timeInMillis && candidateDayStart.timeInMillis <= weekLater.timeInMillis
}

@Composable
private fun NewReservationHeader(
    hasChanges: Boolean,
    onNavigate: (String) -> Unit,
    onShowDiscard: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
        IconButton(onClick = {
            if (hasChanges) onShowDiscard()
            else onNavigate(ROUTE_DASHBOARD)
        }, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_BACK)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back))
        }
        Text(stringResource(R.string.new_reservation_title), fontSize = 28.sp, fontWeight = FontWeight.Normal)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewReservation(
    viewModel: NewReservationViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefilledDate: String? = null,
    prefilledSpot: Int? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val vehicles by viewModel.vehicles.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val displayDateSdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sdfDate = remember { SimpleDateFormat(ParkingUtils.DATE_FORMAT, Locale.getDefault()) }

    var selectedDateMillis by rememberSaveable(prefilledDate) { 
        mutableLongStateOf(parsePrefilledDate(prefilledDate, sdfDate))
    }
    val selectedDate = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    var startTimeMillis by rememberSaveable { mutableLongStateOf(NO_TIME_SELECTED) }
    val startTime = remember(startTimeMillis) {
        if (startTimeMillis == NO_TIME_SELECTED) null 
        else Calendar.getInstance().apply { timeInMillis = startTimeMillis }
    }

    var endTimeMillis by rememberSaveable { mutableLongStateOf(NO_TIME_SELECTED) }
    val endTime = remember(endTimeMillis) {
        if (endTimeMillis == NO_TIME_SELECTED) null 
        else Calendar.getInstance().apply { timeInMillis = endTimeMillis }
    }
    
    var selectedSpot by rememberSaveable(prefilledSpot) { mutableStateOf(prefilledSpot) }
    var viewMode by rememberSaveable { mutableStateOf(GRID_VIEW) } 
    var spotTypeFilter by rememberSaveable { mutableStateOf<SpotType?>(null) }
    var spotsExpanded by rememberSaveable { mutableStateOf(true) }
    
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(false) }
    
    var showVehicleDialog by rememberSaveable { mutableStateOf(false) }
    var showNoVehicleDialog by rememberSaveable { mutableStateOf(false) }
    var showIncompatibleVehicleDialog by rememberSaveable { mutableStateOf(false) }
    var showPrefilledInfo by rememberSaveable(prefilledDate, prefilledSpot) { 
        mutableStateOf(prefilledDate != null || prefilledSpot != null) 
    }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val hasChanges by remember(startTimeMillis, endTimeMillis, selectedSpot) {
        derivedStateOf {
            startTimeMillis != NO_TIME_SELECTED || endTimeMillis != NO_TIME_SELECTED || (selectedSpot != null && selectedSpot != prefilledSpot)
        }
    }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }
    
    val occupiedSpots by remember(selectedDateMillis, startTimeMillis, endTimeMillis) {
        viewModel.getOccupiedSpots(selectedDate, startTime, endTime) 
    }.collectAsState(initial = emptyList())
    
    val hasExistingUserReservation by remember(selectedDateMillis, startTimeMillis, endTimeMillis) { 
        viewModel.hasExistingUserReservation(selectedDate, startTime, endTime) 
    }.collectAsState(initial = false)

    val allReservationsReady by viewModel.allReservationsReady.collectAsState()

    val validationErrorResId by remember(selectedDateMillis, startTimeMillis, endTimeMillis, selectedSpot, hasExistingUserReservation, occupiedSpots) {
        derivedStateOf {
            viewModel.getValidationErrorResId(
                selectedDate,
                startTime,
                endTime,
                selectedSpot,
                hasExistingUserReservation,
                occupiedSpots
            )
        }
    }

    LaunchedEffect(validationErrorResId) {
        if (validationErrorResId != null) {
            spotsExpanded = false
        } else {
            spotsExpanded = true
        }
    }

    val filteredSpots = getFilteredSpots(occupiedSpots, spotTypeFilter)
    val availableSpotsCount = filteredSpots.size

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = isDateWithinRange(utcTimeMillis)
        }
    )

    val startTimePickerState = rememberTimePickerState(
        initialHour = startTime?.get(Calendar.HOUR_OF_DAY) ?: 9,
        initialMinute = startTime?.get(Calendar.MINUTE) ?: 0,
        is24Hour = true
    )
    val endTimePickerState = rememberTimePickerState(
        initialHour = endTime?.get(Calendar.HOUR_OF_DAY) ?: 18,
        initialMinute = endTime?.get(Calendar.MINUTE) ?: 0,
        is24Hour = true
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_NEW_RESERVATION,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                },
                user = viewModel.user.collectAsState().value
            )
        }
    ) {
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
                    selectedItem = 0,
                    onItemSelected = { index ->
                        val routes = listOf(ROUTE_DASHBOARD, ROUTE_HISTORY, ROUTE_PROFILE, ROUTE_VIEW_PARKING)
                        onNavigate(routes[index])
                    }
                )
            }
        ) { padding ->
            val scrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    NewReservationHeader(
                        hasChanges = hasChanges,
                        onNavigate = onNavigate,
                        onShowDiscard = { showDiscardDialog = true }
                    )

                    PrefilledInfoBanner(
                        showPrefilledInfo = showPrefilledInfo,
                        prefilledSpot = prefilledSpot,
                        displayDateSdf = displayDateSdf,
                        selectedDate = selectedDate,
                        onDismiss = { showPrefilledInfo = false }
                    )

                    val dateTimeDisplay = remember(selectedDate, startTime, endTime) {
                        DateTimeDisplay(
                            dateText = displayDateSdf.format(selectedDate.time),
                            startTimeText = startTime?.let { sdfTime.format(it.time) },
                            endTimeText = endTime?.let { sdfTime.format(it.time) }
                        )
                    }
                    DateTimeCard(
                        display = dateTimeDisplay,
                        onDateClick = { showDatePicker = true },
                        onStartTimeClick = { showStartTimePicker = true },
                        onEndTimeClick = { showEndTimePicker = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val spotState = remember(selectedSpot, spotsExpanded, viewMode, spotTypeFilter, occupiedSpots, availableSpotsCount, validationErrorResId) {
                        SpotSelectionState(
                            selectedSpot = selectedSpot,
                            spotsExpanded = spotsExpanded,
                            viewMode = viewMode,
                            spotTypeFilter = spotTypeFilter,
                            occupiedSpots = occupiedSpots,
                            availableSpotsCount = availableSpotsCount,
                            validationErrorResId = validationErrorResId
                        )
                    }
                    SpotSelectionCard(
                        state = spotState,
                        onSpotSelected = { selectedSpot = it },
                        onSpotTypeFilterChange = { spotTypeFilter = it; selectedSpot = null },
                        onToggleExpanded = { spotsExpanded = !spotsExpanded },
                        onViewModeChange = { viewMode = it }
                    )

                    Spacer(modifier = Modifier.height(140.dp))
                }

                val confirmData = remember(selectedSpot, selectedDate, startTime, endTime, validationErrorResId, allReservationsReady) {
                    ReservationConfirmData(
                        canConfirm = selectedSpot != null && startTime != null && endTime != null && validationErrorResId == null && allReservationsReady,
                        selectedSpot = selectedSpot,
                        selectedDate = selectedDate,
                        startTime = startTime,
                        endTime = endTime
                    )
                }
                ConfirmButtonSection(
                    data = confirmData,
                    vehicles = vehicles,
                    viewModel = viewModel,
                    onNavigate = onNavigate,
                    onShowNoVehicleDialog = { showNoVehicleDialog = true },
                    onShowIncompatibleVehicleDialog = { showIncompatibleVehicleDialog = true },
                    onShowVehicleDialog = { showVehicleDialog = true }
                )
                subtleScrollbar(scrollState, Modifier.align(Alignment.CenterEnd))
            }
        }
    }

    DiscardDialog(
        showDiscardDialog = showDiscardDialog,
        onDismiss = { showDiscardDialog = false },
        onDiscard = { showDiscardDialog = false; onNavigate(ROUTE_DASHBOARD) }
    )
    DatePickerSection(
        showDatePicker = showDatePicker,
        datePickerState = datePickerState,
        onDateSelected = { selectedDateMillis = it },
        onDismiss = { showDatePicker = false }
    )
    StartTimePickerSection(
        showStartTimePicker = showStartTimePicker,
        startTimePickerState = startTimePickerState,
        onStartTimeSelected = { startTimeMillis = it },
        onDismiss = { showStartTimePicker = false }
    )
    EndTimePickerSection(
        showEndTimePicker = showEndTimePicker,
        endTimePickerState = endTimePickerState,
        onEndTimeSelected = { endTimeMillis = it },
        onDismiss = { showEndTimePicker = false }
    )
    NoVehicleDialog(
        showNoVehicleDialog = showNoVehicleDialog,
        onDismiss = { showNoVehicleDialog = false },
        onGoToProfile = {
            showNoVehicleDialog = false
            onNavigate("${ROUTE_PROFILE}?${PARAM_SHOW_VEHICLE_ALERT}=true")
        }
    )
    IncompatibleVehicleDialog(
        showIncompatibleVehicleDialog = showIncompatibleVehicleDialog,
        selectedSpot = selectedSpot,
        onDismiss = { showIncompatibleVehicleDialog = false },
        onAddVehicle = {
            showIncompatibleVehicleDialog = false
            onNavigate("${ROUTE_PROFILE}?${PARAM_SHOW_VEHICLE_ALERT}=true")
        }
    )
    if (showVehicleDialog) {
        val spot = selectedSpot
        val sTime = startTime
        val eTime = endTime
        if (spot != null && sTime != null && eTime != null) {
            VehicleSelectionDialog(
                vehicles = vehicles.orEmpty(),
                selectedSpot = spot,
                onDismiss = { showVehicleDialog = false },
                onConfirm = { vehicle ->
                    viewModel.addReservation(spot, selectedDate, sTime, eTime, vehicle.id, vehicle.licensePlate)
                    showVehicleDialog = false
                    onNavigate(ROUTE_DASHBOARD)
                }
            )
        }
    }
}

@Composable
private fun PrefilledInfoBanner(
    showPrefilledInfo: Boolean,
    prefilledSpot: Int?,
    displayDateSdf: SimpleDateFormat,
    selectedDate: Calendar,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = showPrefilledInfo) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = InfoBackground,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prefilled_info_msg),
                        fontSize = 12.sp,
                        color = InfoText
                    )
                    val prefilledInfoText = stringResource(
                        R.string.prefilled_summary, 
                        prefilledSpot ?: 0, 
                        displayDateSdf.format(selectedDate.time)
                    )
                    Text(
                        text = prefilledInfoText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = InfoText
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DateTimeCard(
    display: DateTimeDisplay,
    onDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, LightBorderGray)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.date_time_section), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            
            val dateInteractionSource = remember { MutableInteractionSource() }
            if (dateInteractionSource.collectIsPressedAsState().value) onDateClick()

            Column {
                OutlinedTextField(
                    value = display.dateText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date_label)) },
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.NEW_RESERVATION_DATE_FIELD),
                    interactionSource = dateInteractionSource,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = LightBorderGray,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
                )
                Text(
                    text = stringResource(R.string.max_advance_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val startInteractionSource = remember { MutableInteractionSource() }
                if (startInteractionSource.collectIsPressedAsState().value) onStartTimeClick()
                
                OutlinedTextField(
                    value = display.startTimeText ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.start_time_label)) },
                    modifier = Modifier.weight(1f).testTag(TestTags.NEW_RESERVATION_START_TIME_FIELD),
                    interactionSource = startInteractionSource,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = LightBorderGray
                    ),
                    trailingIcon = { Icon(Icons.Default.AccessTime, null) }
                )

                val endInteractionSource = remember { MutableInteractionSource() }
                if (endInteractionSource.collectIsPressedAsState().value) onEndTimeClick()

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = display.endTimeText ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.end_time_label)) },
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.NEW_RESERVATION_END_TIME_FIELD),
                        interactionSource = endInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = LightBorderGray
                        ),
                        trailingIcon = { Icon(Icons.Default.AccessTime, null) }
                    )
                    Text(
                        text = stringResource(R.string.max_duration_hint),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeToggle(viewMode: String, onViewModeChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onViewModeChange(DROPDOWN_VIEW) }, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_VIEW_DROPDOWN)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(R.string.content_desc_view_list),
                tint = if(viewMode == DROPDOWN_VIEW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
        IconButton(onClick = { onViewModeChange(GRID_VIEW) }, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_VIEW_GRID)) {
            Icon(
                imageVector = Icons.Default.ViewModule,
                contentDescription = stringResource(R.string.content_desc_view_grid),
                tint = if(viewMode == GRID_VIEW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun SpotAvailabilityText(availableSpotsCount: Int) {
    Text(
        text = if (availableSpotsCount > 0) LocalContext.current.resources.getQuantityString(R.plurals.spots_available_count, availableSpotsCount, availableSpotsCount) else stringResource(R.string.no_spots_available),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SpotViewContent(
    state: SpotSelectionState,
    onSpotSelected: (Int) -> Unit,
    onSpotTypeFilterChange: (SpotType?) -> Unit
) {
    Column {
        ResponsiveFilterChips(
            selectedType = state.spotTypeFilter,
            onTypeSelected = onSpotTypeFilterChange
        )
        if (state.viewMode == DROPDOWN_VIEW) {
            SpotDropdown(
                selectedSpot = state.selectedSpot,
                onSpotSelected = onSpotSelected,
                occupiedSpots = state.occupiedSpots,
                spotTypeFilter = state.spotTypeFilter
            )
        } else {
            SpotGrid(
                selectedSpot = state.selectedSpot,
                onSpotSelected = { spot ->
                    if (!state.occupiedSpots.contains(spot)) {
                        onSpotSelected(spot)
                    }
                },
                occupiedSpots = state.occupiedSpots,
                spotTypeFilter = state.spotTypeFilter
            )
        }
        SpotAvailabilityText(state.availableSpotsCount)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotSelectionCard(
    state: SpotSelectionState,
    onSpotSelected: (Int) -> Unit,
    onSpotTypeFilterChange: (SpotType?) -> Unit,
    onToggleExpanded: () -> Unit,
    onViewModeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, LightBorderGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.select_spot_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ViewModeToggle(state.viewMode, onViewModeChange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (state.spotsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = state.spotsExpanded) {
                SpotViewContent(state, onSpotSelected, onSpotTypeFilterChange)
            }
        }
    }

    state.validationErrorResId?.let {
        AlertError(
            message = stringResource(it),
            modifier = Modifier.testTag(TestTags.NEW_RESERVATION_ERROR_MESSAGE)
        )
    }
}

@Composable
private fun BoxScope.ConfirmButtonSection(
    data: ReservationConfirmData,
    vehicles: List<Vehicle>?,
    viewModel: NewReservationViewModel,
    onNavigate: (String) -> Unit,
    onShowNoVehicleDialog: () -> Unit,
    onShowIncompatibleVehicleDialog: () -> Unit,
    onShowVehicleDialog: () -> Unit
) {
    if (data.canConfirm) {
        val spot = data.selectedSpot ?: return
        val sTime = data.startTime ?: return
        val eTime = data.endTime ?: return
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp 
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (vehicles?.isEmpty() == true) {
                            onShowNoVehicleDialog()
                        } else {
                            val compatibleVehicles = (vehicles ?: emptyList()).filter { ParkingUtils.isVehicleAllowedInSpot(spot, it.type) }
                            if (compatibleVehicles.isEmpty()) {
                                onShowIncompatibleVehicleDialog()
                            } else if (compatibleVehicles.size == 1) {
                                val v = compatibleVehicles.first()
                                viewModel.addReservation(
                                    spot,
                                    data.selectedDate,
                                    sTime,
                                    eTime,
                                    v.id,
                                    v.licensePlate
                                )
                                onNavigate(ROUTE_DASHBOARD)
                            } else {
                                onShowVehicleDialog()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.confirm_reservation_btn, spot), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun DiscardDialog(
    showDiscardDialog: Boolean,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit
) {
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag(TestTags.NEW_RESERVATION_DISCARD_DIALOG),
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_msg)) },
            confirmButton = {
                Button(
                    onClick = onDiscard,
                    modifier = Modifier.testTag(TestTags.NEW_RESERVATION_DISCARD_CONFIRM),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.discard_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_DISCARD_CANCEL)) {
                    Text(stringResource(R.string.keep_editing_btn))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSection(
    showDatePicker: Boolean,
    datePickerState: DatePickerState,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_DATE_PICKER_SAVE)) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_DATE_PICKER_CANCEL)) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartTimePickerSection(
    showStartTimePicker: Boolean,
    startTimePickerState: TimePickerState,
    onStartTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                        set(Calendar.MINUTE, startTimePickerState.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    onStartTimeSelected(cal.timeInMillis)
                    onDismiss()
                }, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE)) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_TIME_PICKER_CANCEL)) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = startTimePickerState) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndTimePickerSection(
    showEndTimePicker: Boolean,
    endTimePickerState: TimePickerState,
    onEndTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                        set(Calendar.MINUTE, endTimePickerState.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    onEndTimeSelected(cal.timeInMillis)
                    onDismiss()
                }, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE)) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_TIME_PICKER_CANCEL)) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = endTimePickerState) }
        )
    }
}

@Composable
private fun NoVehicleDialog(
    showNoVehicleDialog: Boolean,
    onDismiss: () -> Unit,
    onGoToProfile: () -> Unit
) {
    if (showNoVehicleDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag(TestTags.NEW_RESERVATION_NO_VEHICLE_DIALOG),
            title = { Text(stringResource(R.string.vehicle_required_title)) },
            text = { Text(stringResource(R.string.vehicle_required_alert)) },
            confirmButton = {
                Button(onClick = onGoToProfile, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_NO_VEHICLE_PROFILE)) {
                    Text(stringResource(R.string.go_to_profile))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_NO_VEHICLE_CANCEL)) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun IncompatibleVehicleDialog(
    showIncompatibleVehicleDialog: Boolean,
    selectedSpot: Int?,
    onDismiss: () -> Unit,
    onAddVehicle: () -> Unit
) {
    if (showIncompatibleVehicleDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag(TestTags.NEW_RESERVATION_INCOMPATIBLE_DIALOG),
            title = { Text(stringResource(R.string.incompatible_vehicle_title)) },
            text = { Text(stringResource(R.string.incompatible_vehicle_msg, selectedSpot ?: 0)) },
            confirmButton = {
                Button(onClick = onAddVehicle, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_INCOMPATIBLE_ADD)) {
                    Text(stringResource(R.string.add_vehicle))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_INCOMPATIBLE_CANCEL)) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResponsiveFilterChips(selectedType: SpotType?, onTypeSelected: (SpotType?) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ParkingFilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = stringResource(R.string.filter_all),
            icon = Icons.Default.LocalParking
        )
        ParkingFilterChip(
            selected = selectedType == SpotType.NORMAL,
            onClick = { onTypeSelected(SpotType.NORMAL) },
            label = stringResource(R.string.spot_type_normal),
            icon = Icons.Default.DirectionsCar
        )
        ParkingFilterChip(
            selected = selectedType == SpotType.ELECTRIC,
            onClick = { onTypeSelected(SpotType.ELECTRIC) },
            label = stringResource(R.string.spot_type_electric),
            icon = Icons.Default.ElectricCar
        )
        ParkingFilterChip(
            selected = selectedType == SpotType.MOTORCYCLE,
            onClick = { onTypeSelected(SpotType.MOTORCYCLE) },
            label = stringResource(R.string.spot_type_motorcycle),
            icon = Icons.Default.TwoWheeler
        )
        ParkingFilterChip(
            selected = selectedType == SpotType.DISABLED,
            onClick = { onTypeSelected(SpotType.DISABLED) },
            label = stringResource(R.string.spot_type_disabled),
            icon = Icons.AutoMirrored.Filled.Accessible
        )
    }
}

@Composable
fun ParkingFilterChip(selected: Boolean, onClick: () -> Unit, label: String, icon: ImageVector) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else LightBorderGray),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp), 
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label, 
                fontSize = 11.sp, 
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, 
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDropdown(selectedSpot: Int?, onSpotSelected: (Int) -> Unit, occupiedSpots: List<Int>, spotTypeFilter: SpotType?) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val availableSpots = getFilteredSpots(occupiedSpots, spotTypeFilter)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_SPOT_DROPDOWN)) {
        OutlinedTextField(
            value = selectedSpot?.let { stringResource(R.string.spot_number_with_prefix, stringResource(R.string.available_legend), it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.available_legend)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = LightBorderGray
            )
        )
        ExposedDropdownMenu(
            expanded = expanded, 
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            if (availableSpots.isEmpty()) {
                DropdownMenuItem(text = { Text(stringResource(R.string.no_spots_available)) }, onClick = {})
            } else {
                availableSpots.forEach { spot ->
                    val spotType = ParkingUtils.getSpotType(spot)
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(ParkingUtils.getSpotIcon(spotType), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.spot_with_type_format, stringResource(R.string.available_legend), spot, stringResource(ParkingUtils.getSpotLabelRes(spotType))))
                            }
                        },
                        onClick = { onSpotSelected(spot); expanded = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotGrid(selectedSpot: Int?, onSpotSelected: (Int) -> Unit, occupiedSpots: List<Int>, spotTypeFilter: SpotType?) {
    Column(modifier = Modifier.testTag(TestTags.NEW_RESERVATION_SPOT_GRID)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendItem(stringResource(R.string.available_legend), SuccessGreen)
            LegendItem(stringResource(R.string.occupied_legend), OccupiedGray)
            LegendItem(stringResource(R.string.selection_legend), InfoBlue)
        }

        val spots = getSpotsByType(spotTypeFilter)
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            spots.forEach { spot ->
                val isOccupied = occupiedSpots.contains(spot)
                val isSelected = selectedSpot == spot
                val spotType = ParkingUtils.getSpotType(spot)
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            when {
                                isSelected -> InfoBlue
                                isOccupied -> OccupiedGray
                                else -> SuccessGreen
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = !isOccupied) { onSpotSelected(spot) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(ParkingUtils.getSpotIcon(spotType), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        Text(spot.toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun AlertError(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        color = ErrorBackground,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
    }
}

@Composable
fun VehicleSelectionDialog(vehicles: List<Vehicle>, selectedSpot: Int, onDismiss: () -> Unit, onConfirm: (Vehicle) -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag(TestTags.NEW_RESERVATION_VEHICLE_SELECT_DIALOG),
            title = { Text(stringResource(R.string.select_vehicle_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    vehicles.forEach { vehicle ->
                        val isCompatible = ParkingUtils.isVehicleAllowedInSpot(selectedSpot, vehicle.type)
                        ListItem(
                            headlineContent = { Text(vehicle.licensePlate, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(stringResource(ParkingUtils.getVehicleTypeLabelRes(vehicle.type))) },
                            leadingContent = { 
                                val icon = ParkingUtils.getVehicleIcon(vehicle.type)
                                Icon(icon, null, tint = if (isCompatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary) 
                            },
                            modifier = Modifier.clickable(enabled = isCompatible) { onConfirm(vehicle) },
                            trailingContent = { if (!isCompatible) Text(stringResource(R.string.vehicle_not_compatible_short), color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag(TestTags.NEW_RESERVATION_VEHICLE_SELECT_CANCEL)) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
