package com.lksnext.ParkingIMayordomo.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.subtleScrollbar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.ui.viewmodel.SpotOccupancyState
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ViewParkingViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.TestTags
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_ABOUT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NEW_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.utils.SpotType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewParking(
    viewModel: ViewParkingViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefilledDate: String? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val currentUser by viewModel.user.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    // Use Long to preserve state across rotation
    var selectedDateMillis by rememberSaveable { 
        mutableLongStateOf(
            prefilledDate?.let { dateStr ->
                try {
                    SimpleDateFormat(ParkingUtils.DATE_FORMAT, Locale.getDefault()).parse(dateStr)?.time
                } catch (_: Exception) { null }
            } ?: Calendar.getInstance().timeInMillis
        ) 
    }
    val selectedDate = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }

    var selectedSpot by rememberSaveable { mutableStateOf<Int?>(null) }
    var spotTypeFilter by rememberSaveable { mutableStateOf<SpotType?>(null) }
    var spotsExpanded by rememberSaveable { mutableStateOf(true) }
    var viewMode by rememberSaveable { mutableStateOf("grid") }
    
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val dateStr = sdfDate.format(selectedDate.time)
    
    val spotOccupancy by remember(selectedDateMillis) { 
        viewModel.getOccupiedSpots(selectedDate) 
    }.collectAsState(initial = emptyMap())
    
    val userSpots by remember(selectedDateMillis) { 
        viewModel.getUserSpots(selectedDate) 
    }.collectAsState(initial = emptyList())

    val currentReservations by remember(selectedDateMillis) {
        viewModel.getCurrentReservations(selectedDate)
    }.collectAsState(initial = emptyList())

    val isCurrentDay = remember(selectedDateMillis) {
        val today = Calendar.getInstance()
        val checkDate = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        today.get(Calendar.YEAR) == checkDate.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == checkDate.get(Calendar.DAY_OF_YEAR)
    }

    val isDateInReservationRange = remember(selectedDateMillis) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val maxDate = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        }
        val checkDate = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
        }
        !checkDate.before(today) && !checkDate.after(maxDate)
    }

    LaunchedEffect(selectedSpot) {
        if (selectedSpot != null) {
            spotsExpanded = false
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_VIEW_PARKING,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                },
                user = currentUser
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
                    selectedItem = 3,
                    onItemSelected = { index ->
                        val routes = listOf(ROUTE_DASHBOARD, ROUTE_HISTORY, ROUTE_PROFILE, ROUTE_VIEW_PARKING, ROUTE_ABOUT)
                        onNavigate(routes[index])
                    }
                )
            }
        ) { padding ->
            val scrollState = rememberScrollState()
            Box(modifier = modifier.padding(padding).fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                Text(text = stringResource(R.string.view_parking_title), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = stringResource(R.string.view_parking_subtitle), fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 24.dp))

                ViewParkingDateCard(selectedDate = selectedDate, displayDate = displayDate, onDateClicked = { showDatePicker = true })

                ViewParkingSpotSection(
                    spotsExpanded = spotsExpanded,
                    viewMode = viewMode,
                    spotTypeFilter = spotTypeFilter,
                    selectedSpot = selectedSpot,
                    spotOccupancy = spotOccupancy,
                    userSpots = userSpots,
                    isCurrentDay = isCurrentDay,
                    onToggleExpanded = { spotsExpanded = !spotsExpanded },
                    onViewModeChange = { viewMode = it },
                    onSpotTypeSelected = { spotTypeFilter = it; selectedSpot = null },
                    onSpotSelected = { selectedSpot = it }
                )

                if (selectedSpot != null) {
                    val spotNonNull = selectedSpot!!
                    val spotReservations = currentReservations.filter { it.spotNumber == spotNonNull }
                    val isUserSpot = userSpots.contains(spotNonNull)
                    val spotType = ParkingUtils.getSpotType(spotNonNull)
                    val occupancyState = spotOccupancy[spotNonNull] ?: SpotOccupancyState.FREE
                    ViewParkingSpotDetail(
                        selectedSpot = spotNonNull,
                        spotReservations = spotReservations,
                        isUserSpot = isUserSpot,
                        isCurrentDay = isCurrentDay,
                        spotType = spotType,
                        currentUser = currentUser,
                        isDateInReservationRange = isDateInReservationRange,
                        dateStr = dateStr,
                        occupancyState = occupancyState,
                        onNavigate = onNavigate
                    )
                }
            }
            subtleScrollbar(scrollState, Modifier.align(Alignment.CenterEnd))
        }
    }
    }

    ViewParkingDatePickerDialog(
        showDatePicker = showDatePicker,
        datePickerState = datePickerState,
        onDateSelected = { selectedDateMillis = it; selectedSpot = null },
        onDismiss = { showDatePicker = false }
    )
}

@Composable
private fun ViewParkingDateCard(
    selectedDate: Calendar,
    displayDate: SimpleDateFormat,
    onDateClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.date_label), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
            val dateInteractionSource = remember { MutableInteractionSource() }
            if (dateInteractionSource.collectIsPressedAsState().value) onDateClicked()
            OutlinedTextField(
                value = displayDate.format(selectedDate.time),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.date_label)) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.VIEW_PARKING_DATE_FIELD),
                interactionSource = dateInteractionSource,
                enabled = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewParkingSpotSection(
    spotsExpanded: Boolean,
    viewMode: String,
    spotTypeFilter: SpotType?,
    selectedSpot: Int?,
    spotOccupancy: Map<Int, SpotOccupancyState>,
    userSpots: List<Int>,
    isCurrentDay: Boolean,
    onToggleExpanded: () -> Unit,
    onViewModeChange: (String) -> Unit,
    onSpotTypeSelected: (SpotType?) -> Unit,
    onSpotSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.select_spot_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onViewModeChange("dropdown") }, modifier = Modifier.testTag(TestTags.VIEW_PARKING_VIEW_DROPDOWN)) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.content_desc_menu),
                            tint = if(viewMode == "dropdown") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = { onViewModeChange("grid") }, modifier = Modifier.testTag(TestTags.VIEW_PARKING_VIEW_GRID)) {
                        Icon(
                            Icons.Default.ViewModule,
                            contentDescription = stringResource(R.string.selection_legend),
                            tint = if(viewMode == "grid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onToggleExpanded, modifier = Modifier.testTag(TestTags.VIEW_PARKING_EXPAND_SPOTS)) {
                        Icon(
                            imageVector = if (spotsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = spotsExpanded) {
                Column {
                    ViewParkingFilterChips(selectedType = spotTypeFilter, onTypeSelected = onSpotTypeSelected)
                    if (viewMode == "dropdown") {
                        ViewSpotDropdown(selectedSpot = selectedSpot, onSpotSelected = onSpotSelected, spotOccupancy = spotOccupancy, userSpots = userSpots, spotTypeFilter = spotTypeFilter)
                    } else {
                        Column {
                            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ViewLegendItem(stringResource(R.string.available_legend), SuccessGreen)
                                ViewLegendItem(
                                    stringResource(R.string.partially_occupied_legend),
                                    PartiallyOccupiedGreen
                                )
                                ViewLegendItem(stringResource(R.string.occupied_legend), OccupiedGray)
                                ViewLegendItem(stringResource(R.string.user_spot_legend), UserSpotYellow)
                            }
                            ViewSpotGrid(selectedSpot = selectedSpot, onSpotSelected = onSpotSelected, spotOccupancy = spotOccupancy, userSpots = userSpots, spotTypeFilter = spotTypeFilter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewParkingSpotDetail(
    selectedSpot: Int,
    spotReservations: List<Reservation>,
    isUserSpot: Boolean,
    isCurrentDay: Boolean,
    spotType: SpotType,
    currentUser: com.lksnext.ParkingIMayordomo.data.model.User?,
    isDateInReservationRange: Boolean,
    dateStr: String,
    occupancyState: SpotOccupancyState,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when {
                            isUserSpot -> UserSpotYellow
                            occupancyState == SpotOccupancyState.FULLY_OCCUPIED -> OccupiedGray
                            occupancyState == SpotOccupancyState.PARTIALLY_OCCUPIED -> PartiallyOccupiedGreen
                            else -> SuccessGreen
                        },
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(ParkingUtils.getSpotIcon(spotType), null, tint = Color.White) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.spot_number_label, selectedSpot), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(ParkingUtils.getSpotLabelRes(spotType)), color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                    }
                }
                Badge(
                    containerColor = when {
                        isUserSpot -> UserSpotYellow.copy(alpha = 0.2f)
                        spotReservations.isNotEmpty() -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else -> SuccessGreen.copy(alpha = 0.2f)
                    },
                    contentColor = when {
                        isUserSpot -> UserSpotText
                        spotReservations.isNotEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> SuccessTextDark
                    }
                ) {
                    Text(text = when {
                        isUserSpot -> stringResource(R.string.status_user_reserve)
                        occupancyState == SpotOccupancyState.PARTIALLY_OCCUPIED -> stringResource(R.string.status_partially_occupied)
                        isCurrentDay && occupancyState == SpotOccupancyState.FREE -> stringResource(R.string.status_free_now)
                        spotReservations.isNotEmpty() -> stringResource(R.string.status_occupied)
                        else -> stringResource(R.string.status_available)
                    }, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                }
            }
            if (spotReservations.isNotEmpty()) {
                Text(stringResource(R.string.reservations_count_label, spotReservations.size), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurface)
                spotReservations.sortedBy { it.startTime }.forEach { reservation ->
                    val isMine = reservation.userId == currentUser?.id
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = if (isMine) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.license_plate_with_mine,
                                        reservation.licensePlate ?: stringResource(R.string.no_license_plate),
                                        if (isMine) stringResource(R.string.mine_suffix) else ""
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(stringResource(R.string.schedule_format, reservation.startTime, reservation.endTime), fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            if (spotReservations.isEmpty() || spotReservations.none { it.userId == currentUser?.id }) {
                if (isDateInReservationRange) {
                    Button(
                        onClick = { onNavigate("${ROUTE_NEW_RESERVATION}?spot=$selectedSpot&date=$dateStr") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag(TestTags.VIEW_PARKING_RESERVE_BUTTON),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text(stringResource(R.string.reserve_spot_btn)) }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            stringResource(R.string.consultation_mode_info),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewParkingDatePickerDialog(
    showDatePicker: Boolean,
    datePickerState: DatePickerState,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateSelected)
                        onDismiss()
                    },
                    modifier = Modifier.testTag(TestTags.VIEW_PARKING_DATE_PICKER_SAVE)
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(TestTags.VIEW_PARKING_DATE_PICKER_CANCEL)
                ) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewParkingFilterChips(selectedType: SpotType?, onTypeSelected: (SpotType?) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ViewParkingFilterChip(selected = selectedType == null, onClick = { onTypeSelected(null) }, label = stringResource(R.string.filter_all), icon = Icons.Default.LocalParking, modifier = Modifier.testTag(TestTags.VIEW_PARKING_FILTER_ALL))
        ViewParkingFilterChip(selected = selectedType == SpotType.NORMAL, onClick = { onTypeSelected(SpotType.NORMAL) }, label = stringResource(R.string.spot_type_normal), icon = Icons.Default.DirectionsCar, modifier = Modifier.testTag(TestTags.VIEW_PARKING_FILTER_NORMAL))
        ViewParkingFilterChip(selected = selectedType == SpotType.ELECTRIC, onClick = { onTypeSelected(SpotType.ELECTRIC) }, label = stringResource(R.string.spot_type_electric), icon = Icons.Default.ElectricCar, modifier = Modifier.testTag(TestTags.VIEW_PARKING_FILTER_ELECTRIC))
        ViewParkingFilterChip(selected = selectedType == SpotType.MOTORCYCLE, onClick = { onTypeSelected(SpotType.MOTORCYCLE) }, label = stringResource(R.string.spot_type_motorcycle), icon = Icons.Default.TwoWheeler, modifier = Modifier.testTag(TestTags.VIEW_PARKING_FILTER_MOTORCYCLE))
        ViewParkingFilterChip(selected = selectedType == SpotType.DISABLED, onClick = { onTypeSelected(SpotType.DISABLED) }, label = stringResource(R.string.spot_type_disabled), icon = Icons.AutoMirrored.Filled.Accessible, modifier = Modifier.testTag(TestTags.VIEW_PARKING_FILTER_DISABLED))
    }
}

@Composable
fun ViewParkingFilterChip(selected: Boolean, onClick: () -> Unit, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSpotDropdown(selectedSpot: Int?, onSpotSelected: (Int) -> Unit, spotOccupancy: Map<Int, SpotOccupancyState>, userSpots: List<Int>, spotTypeFilter: SpotType?) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val spots = (1..50).filter { spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag(TestTags.VIEW_PARKING_SPOT_DROPDOWN)) {
        OutlinedTextField(
            value = selectedSpot?.let { stringResource(R.string.spot_number_with_prefix, stringResource(R.string.available_legend), it) } ?: stringResource(R.string.select_spot_prompt), onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ), 
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            spots.forEach { spot ->
                val occupancy = spotOccupancy[spot] ?: SpotOccupancyState.FREE
                val isUserSpot = userSpots.contains(spot)
                val spotType = ParkingUtils.getSpotType(spot)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(ParkingUtils.getSpotIcon(spotType), null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.spot_number_with_prefix, stringResource(R.string.available_legend), spot), color = MaterialTheme.colorScheme.onSurface)
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                if (isUserSpot) Badge(containerColor = UserSpotYellow) { Text(stringResource(R.string.badge_mine), modifier = Modifier.padding(2.dp), color = Color.White) }
                                else if (occupancy == SpotOccupancyState.FULLY_OCCUPIED) Badge(containerColor = OccupiedGray) { Text(stringResource(R.string.status_occupied), modifier = Modifier.padding(2.dp), color = Color.White) }
                                else if (occupancy == SpotOccupancyState.PARTIALLY_OCCUPIED) Badge(containerColor = PartiallyOccupiedGreen) { Text(stringResource(R.string.status_partially_occupied), modifier = Modifier.padding(2.dp), color = Color.White) }
                                else Badge(containerColor = SuccessGreen) { Text(stringResource(R.string.status_available), modifier = Modifier.padding(2.dp), color = Color.White) }
                            }
                        }
                    },
                    onClick = { onSpotSelected(spot); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewSpotGrid(selectedSpot: Int?, onSpotSelected: (Int) -> Unit, spotOccupancy: Map<Int, SpotOccupancyState>, userSpots: List<Int>, spotTypeFilter: SpotType?) {
    val spots = (1..50).filter { spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter }
    FlowRow(modifier = Modifier.fillMaxWidth().testTag(TestTags.VIEW_PARKING_SPOT_GRID), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        spots.forEach { spot ->
            val occupancy = spotOccupancy[spot] ?: SpotOccupancyState.FREE
            val isUserSpot = userSpots.contains(spot)
            val isSelected = selectedSpot == spot
            val spotType = ParkingUtils.getSpotType(spot)
            Box(
                modifier = Modifier.size(60.dp)
                    .background(
                        when {
                            isUserSpot -> UserSpotYellow
                            occupancy == SpotOccupancyState.FULLY_OCCUPIED -> OccupiedGray
                            occupancy == SpotOccupancyState.PARTIALLY_OCCUPIED -> PartiallyOccupiedGreen
                            else -> SuccessGreen
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                    .clickable { onSpotSelected(spot) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(ParkingUtils.getSpotIcon(spotType), null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(spot.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ViewLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
    }
}
