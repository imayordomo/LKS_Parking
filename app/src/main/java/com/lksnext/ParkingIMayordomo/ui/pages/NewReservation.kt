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
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
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

    val displayDateSdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sdfDate = remember { SimpleDateFormat(ParkingUtils.DATE_FORMAT, Locale.getDefault()) }

    var selectedDateMillis by rememberSaveable(prefilledDate) { 
        mutableLongStateOf(Calendar.getInstance().apply {
            prefilledDate?.let { dateStr ->
                try {
                    sdfDate.parse(dateStr)?.let { time = it }
                } catch (_: Exception) {}
            }
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }
    val selectedDate = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    var startTimeMillis by rememberSaveable { mutableLongStateOf(-1L) }
    val startTime = remember(startTimeMillis) {
        if (startTimeMillis == -1L) null 
        else Calendar.getInstance().apply { timeInMillis = startTimeMillis }
    }

    var endTimeMillis by rememberSaveable { mutableLongStateOf(-1L) }
    val endTime = remember(endTimeMillis) {
        if (endTimeMillis == -1L) null 
        else Calendar.getInstance().apply { timeInMillis = endTimeMillis }
    }
    
    var selectedSpot by rememberSaveable(prefilledSpot) { mutableStateOf(prefilledSpot) }
    var viewMode by rememberSaveable { mutableStateOf("grid") } 
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
            startTimeMillis != -1L || endTimeMillis != -1L || (selectedSpot != null && selectedSpot != prefilledSpot)
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

    val availableSpotsCount = (1..50).count { spot ->
        !occupiedSpots.contains(spot) && (spotTypeFilter == null || ParkingUtils.getSpotType(spot) == spotTypeFilter)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val weekLater = Calendar.getInstance().apply {
                    timeInMillis = today.timeInMillis
                    add(Calendar.DAY_OF_YEAR, 7)
                }
                return utcTimeMillis >= today.timeInMillis && utcTimeMillis <= weekLater.timeInMillis
            }
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
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ParkingTopAppBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = { onNavigate(ROUTE_NOTIFICATIONS) }
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
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                        IconButton(onClick = { 
                            if (hasChanges) showDiscardDialog = true
                            else onNavigate(ROUTE_DASHBOARD)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back))
                        }
                        Text(stringResource(R.string.new_reservation_title), fontSize = 28.sp, fontWeight = FontWeight.Normal)
                    }

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
                                IconButton(onClick = { showPrefilledInfo = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, LightBorderGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.date_time_section), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            
                            val dateInteractionSource = remember { MutableInteractionSource() }
                            if (dateInteractionSource.collectIsPressedAsState().value) showDatePicker = true

                            Column {
                                OutlinedTextField(
                                    value = displayDateSdf.format(selectedDate.time),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.date_label)) },
                                    modifier = Modifier.fillMaxWidth(),
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
                                if (startInteractionSource.collectIsPressedAsState().value) showStartTimePicker = true
                                
                                OutlinedTextField(
                                    value = startTime?.let { sdfTime.format(it.time) } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.start_time_label)) },
                                    modifier = Modifier.weight(1f),
                                    interactionSource = startInteractionSource,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = LightBorderGray
                                    ),
                                    trailingIcon = { Icon(Icons.Default.AccessTime, null) }
                                )

                                val endInteractionSource = remember { MutableInteractionSource() }
                                if (endInteractionSource.collectIsPressedAsState().value) showEndTimePicker = true

                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = endTime?.let { sdfTime.format(it.time) } ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.end_time_label)) },
                                        modifier = Modifier.fillMaxWidth(),
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

                    Spacer(modifier = Modifier.height(24.dp))

                    if (selectedSpot != null || (startTime != null && endTime != null)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, LightBorderGray)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { spotsExpanded = !spotsExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.select_spot_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewMode = "dropdown" }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.List, 
                                                contentDescription = stringResource(R.string.content_desc_view_list), 
                                                tint = if(viewMode == "dropdown") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        IconButton(onClick = { viewMode = "grid" }) {
                                            Icon(
                                                imageVector = Icons.Default.ViewModule, 
                                                contentDescription = stringResource(R.string.content_desc_view_grid), 
                                                tint = if(viewMode == "grid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (spotsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = spotsExpanded) {
                                    Column {
                                        ResponsiveFilterChips(
                                            selectedType = spotTypeFilter,
                                            onTypeSelected = { spotTypeFilter = it; selectedSpot = null }
                                        )

                                        if (viewMode == "dropdown") {
                                            SpotDropdown(
                                                selectedSpot = selectedSpot,
                                                onSpotSelected = { selectedSpot = it },
                                                occupiedSpots = occupiedSpots,
                                                spotTypeFilter = spotTypeFilter
                                            )
                                        } else {
                                            SpotGrid(
                                                selectedSpot = selectedSpot,
                                                onSpotSelected = { spot ->
                                                    if (!occupiedSpots.contains(spot)) {
                                                        selectedSpot = spot
                                                    }
                                                },
                                                occupiedSpots = occupiedSpots,
                                                spotTypeFilter = spotTypeFilter
                                            )
                                        }

                                        Text(
                                            text = if (availableSpotsCount > 0) stringResource(R.string.spots_available_count, availableSpotsCount) else stringResource(R.string.no_spots_available),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    validationErrorResId?.let { AlertError(stringResource(it)) }

                    Spacer(modifier = Modifier.height(140.dp))
                }

                val canConfirm = selectedSpot != null && startTime != null && endTime != null && validationErrorResId == null
                
                if (canConfirm) {
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
                                    if (vehicles.isEmpty()) {
                                        showNoVehicleDialog = true
                                    } else {
                                        val compatibleVehicles = vehicles.filter { ParkingUtils.isVehicleAllowedInSpot(selectedSpot!!, it.type) }
                                        if (compatibleVehicles.isEmpty()) {
                                            showIncompatibleVehicleDialog = true
                                        } else if (compatibleVehicles.size == 1) {
                                            val v = compatibleVehicles.first()
                                            viewModel.addReservation(
                                                selectedSpot!!,
                                                selectedDate,
                                                startTime!!,
                                                endTime!!,
                                                v.id,
                                                v.licensePlate
                                            )
                                            onNavigate(ROUTE_DASHBOARD)
                                        } else {
                                            showVehicleDialog = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.confirm_reservation_btn, selectedSpot!!), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onNavigate(ROUTE_DASHBOARD)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.discard_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.keep_editing_btn))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                        set(Calendar.MINUTE, startTimePickerState.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    startTimeMillis = cal.timeInMillis
                    showStartTimePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = startTimePickerState) }
        )
    }

    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                        set(Calendar.MINUTE, endTimePickerState.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    endTimeMillis = cal.timeInMillis
                    showEndTimePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = endTimePickerState) }
        )
    }

    if (showNoVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showNoVehicleDialog = false },
            title = { Text(stringResource(R.string.vehicle_required_title)) },
            text = { Text(stringResource(R.string.vehicle_required_alert)) },
            confirmButton = {
                Button(onClick = { 
                    showNoVehicleDialog = false
                    onNavigate("${ROUTE_PROFILE}?${PARAM_SHOW_VEHICLE_ALERT}=true")
                }) {
                    Text(stringResource(R.string.go_to_profile))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNoVehicleDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showIncompatibleVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showIncompatibleVehicleDialog = false },
            title = { Text(stringResource(R.string.incompatible_vehicle_title)) },
            text = { Text(stringResource(R.string.incompatible_vehicle_msg, selectedSpot ?: 0)) },
            confirmButton = {
                Button(onClick = { 
                    showIncompatibleVehicleDialog = false
                    onNavigate("${ROUTE_PROFILE}?${PARAM_SHOW_VEHICLE_ALERT}=true")
                }) {
                    Text(stringResource(R.string.add_vehicle))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncompatibleVehicleDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showVehicleDialog) {
        VehicleSelectionDialog(
            vehicles = vehicles,
            selectedSpot = selectedSpot!!,
            onDismiss = { showVehicleDialog = false },
            onConfirm = { vehicle ->
                viewModel.addReservation(
                    selectedSpot!!,
                    selectedDate,
                    startTime!!,
                    endTime!!,
                    vehicle.id,
                    vehicle.licensePlate
                )
                showVehicleDialog = false
                onNavigate(ROUTE_DASHBOARD)
            }
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
    val availableSpots = (1..50).filter { !occupiedSpots.contains(it) && (spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
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
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendItem(stringResource(R.string.available_legend), SuccessGreen)
            LegendItem(stringResource(R.string.occupied_legend), LightBorderGray)
            LegendItem(stringResource(R.string.selection_legend), InfoBlue)
        }

        val spots = (1..50).filter { spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter }
        
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
                                isOccupied -> LightBorderGray
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
fun AlertError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        color = ErrorBackground,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
    }
}

@Composable
fun VehicleSelectionDialog(vehicles: List<com.lksnext.ParkingIMayordomo.data.model.Vehicle>, selectedSpot: Int, onDismiss: () -> Unit, onConfirm: (com.lksnext.ParkingIMayordomo.data.model.Vehicle) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_vehicle_dialog_title)) },
        text = {
            Column {
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
