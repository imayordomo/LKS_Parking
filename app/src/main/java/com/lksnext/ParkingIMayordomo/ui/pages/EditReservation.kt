package com.lksnext.ParkingIMayordomo.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.ui.viewmodel.EditReservationViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_EDIT_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReservation(
    viewModel: EditReservationViewModel,
    reservationId: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val reservation by viewModel.getReservation(reservationId).collectAsState(initial = null)

    if (reservation == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.reservation_not_found), color = MaterialTheme.colorScheme.onBackground)
                Button(onClick = { onNavigate(ROUTE_DASHBOARD) }) {
                    Text(stringResource(R.string.back_to_dashboard))
                }
            }
        }
        return
    }

    val displayDateFormat = stringResource(R.string.date_display_format)
    val displayDate = remember(displayDateFormat) { SimpleDateFormat(displayDateFormat, Locale.getDefault()) }

    var selectedDateMillis by rememberSaveable(reservation) {
        mutableLongStateOf(Calendar.getInstance().apply { 
            time = ParkingUtils.parseDate(reservation!!.date) ?: Date() 
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }
    
    var startTimeMillis by rememberSaveable(reservation) {
        mutableLongStateOf(Calendar.getInstance().apply { 
            val timeDate = ParkingUtils.parseTime(reservation!!.startTime) ?: Date()
            val cal = Calendar.getInstance().apply { time = timeDate }
            set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }
    
    var endTimeMillis by rememberSaveable(reservation) {
        mutableLongStateOf(Calendar.getInstance().apply { 
            val timeDate = ParkingUtils.parseTime(reservation!!.endTime) ?: Date()
            val cal = Calendar.getInstance().apply { time = timeDate }
            set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }

    val selectedDate = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }
    val startTime = remember(startTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = startTimeMillis }
    }
    val endTime = remember(endTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = endTimeMillis }
    }
    
    var selectedVehicleId by rememberSaveable(reservation) { mutableStateOf(reservation!!.vehicleId) }
    
    val vehicles by viewModel.vehicles.collectAsState()
    val currentUser by viewModel.user.collectAsState()
    val allReservations by viewModel.reservations.collectAsState()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val hasChanges by remember(selectedDateMillis, startTimeMillis, endTimeMillis, selectedVehicleId, reservation) {
        derivedStateOf {
            reservation?.let { res ->
                val currentDateStr = ParkingUtils.formatDate(selectedDate.time)
                val currentStartTimeStr = ParkingUtils.formatTime(startTime.time)
                val currentEndTimeStr = ParkingUtils.formatTime(endTime.time)
                
                currentDateStr != res.date || 
                currentStartTimeStr != res.startTime || 
                currentEndTimeStr != res.endTime || 
                selectedVehicleId != res.vehicleId
            } ?: false
        }
    }

    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    val compatibleVehicles = remember(reservation?.spotNumber, vehicles) {
        reservation?.let { res ->
            vehicles.filter { ParkingUtils.isVehicleAllowedInSpot(res.spotNumber, it.type) }
        } ?: emptyList()
    }
    
    var vehicleExpanded by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }

    val validationErrorResId by remember(selectedDateMillis, startTimeMillis, endTimeMillis, allReservations, selectedVehicleId) {
        derivedStateOf {
            viewModel.getValidationErrorResId(
                selectedDate = selectedDate,
                startTime = startTime,
                endTime = endTime,
                allReservations = allReservations,
                currentReservationId = reservation!!.id,
                currentSpotNumber = reservation!!.spotNumber,
                currentUserId = currentUser?.id,
                selectedVehicleId = selectedVehicleId
            )
        }
    }

    // Material 3 Picker States
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
        initialHour = startTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = startTime.get(Calendar.MINUTE),
        is24Hour = true
    )
    val endTimePickerState = rememberTimePickerState(
        initialHour = endTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = endTime.get(Calendar.MINUTE),
        is24Hour = true
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_EDIT_RESERVATION,
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
            Column(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            if (hasChanges) showDiscardDialog = true
                            else onNavigate(ROUTE_DASHBOARD)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                stringResource(R.string.content_desc_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.edit_reservation_title, reservation!!.spotNumber),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            text = stringResource(R.string.edit_instruction),
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val dateInteractionSource = remember { MutableInteractionSource() }
                        if (dateInteractionSource.collectIsPressedAsState().value) showDatePicker = true

                        OutlinedTextField(
                            value = displayDate.format(selectedDate.time),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.date_label_required)) },
                            modifier = Modifier.fillMaxWidth(),
                            interactionSource = dateInteractionSource,
                            enabled = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            supportingText = { Text(stringResource(R.string.max_advance_hint)) }
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val startInteractionSource = remember { MutableInteractionSource() }
                            if (startInteractionSource.collectIsPressedAsState().value) showStartTimePicker = true

                            OutlinedTextField(
                                value = ParkingUtils.formatTime(startTime.time),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.start_time_required)) },
                                modifier = Modifier.weight(1f),
                                interactionSource = startInteractionSource,
                                enabled = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            
                            val endInteractionSource = remember { MutableInteractionSource() }
                            if (endInteractionSource.collectIsPressedAsState().value) showEndTimePicker = true

                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = ParkingUtils.formatTime(endTime.time),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.end_time_required)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    interactionSource = endInteractionSource,
                                    enabled = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.max_duration_hint), 
                                    fontSize = 12.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }

                        if (compatibleVehicles.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = vehicleExpanded,
                                onExpandedChange = { vehicleExpanded = !vehicleExpanded }
                            ) {
                                val selectedVehicle = compatibleVehicles.find { it.id == selectedVehicleId }
                                OutlinedTextField(
                                    value = selectedVehicle?.let { 
                                        stringResource(
                                            R.string.vehicle_display_format, 
                                            it.licensePlate, 
                                            stringResource(ParkingUtils.getVehicleTypeLabelRes(it.type))
                                        ) 
                                    } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.vehicle_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    leadingIcon = {
                                        val icon = if (selectedVehicle != null) ParkingUtils.getVehicleIcon(selectedVehicle.type) else Icons.Default.DirectionsCar
                                        Icon(icon, null)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = vehicleExpanded, 
                                    onDismissRequest = { vehicleExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    compatibleVehicles.forEach { vehicle ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val icon = ParkingUtils.getVehicleIcon(vehicle.type)
                                                    Icon(icon, null, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        stringResource(
                                                            R.string.vehicle_display_format, 
                                                            vehicle.licensePlate, 
                                                            stringResource(ParkingUtils.getVehicleTypeLabelRes(vehicle.type))
                                                        )
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedVehicleId = vehicle.id
                                                vehicleExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                                leadingIconColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        validationErrorResId?.let { resId ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text(
                                    text = stringResource(resId), 
                                    color = MaterialTheme.colorScheme.onErrorContainer, 
                                    modifier = Modifier.padding(12.dp), 
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = { 
                                    if (hasChanges) showDiscardDialog = true
                                    else onNavigate(ROUTE_DASHBOARD)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        val vehicle = vehicles.find { it.id == selectedVehicleId }
                                        viewModel.updateReservation(
                                            id = reservation!!.id,
                                            date = selectedDate,
                                            startTime = startTime,
                                            endTime = endTime,
                                            vehicleId = selectedVehicleId,
                                            licensePlate = vehicle?.licensePlate
                                        )
                                        onNavigate(ROUTE_DASHBOARD)
                                    }
                                },
                                enabled = validationErrorResId == null && selectedVehicleId.isNotBlank() && !loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.save_changes))
                                }
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
}
