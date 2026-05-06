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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ViewParkingViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
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
fun ViewParking(
    viewModel: ViewParkingViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val currentUser by viewModel.user.collectAsState()
    val reservations by viewModel.reservations.collectAsState()

    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedSpot by remember { mutableStateOf<Int?>(null) }
    var spotTypeFilter by remember { mutableStateOf<SpotType?>(null) }
    var spotsExpanded by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf("grid") }
    
    var showDatePicker by remember { mutableStateOf(false) }

    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val dateStr = sdfDate.format(selectedDate.time)
    
    val occupiedSpots by remember(selectedDate) { viewModel.getOccupiedSpots(selectedDate) }.collectAsState(initial = emptyList())
    val userSpots by remember(selectedDate) { viewModel.getUserSpots(selectedDate) }.collectAsState(initial = emptyList())

    val isDateInReservationRange = remember(selectedDate) {
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
            time = selectedDate.time
        }
        !checkDate.before(today) && !checkDate.after(maxDate)
    }

    LaunchedEffect(selectedSpot) {
        if (selectedSpot != null) {
            spotsExpanded = false
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.timeInMillis
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_VIEW_PARKING,
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
                    selectedItem = 3,
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
                Text(text = stringResource(R.string.view_parking_title), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = stringResource(R.string.view_parking_subtitle), fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.date_label), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
                        
                        val dateInteractionSource = remember { MutableInteractionSource() }
                        if (dateInteractionSource.collectIsPressedAsState().value) showDatePicker = true

                        OutlinedTextField(
                            value = displayDate.format(selectedDate.time),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.date_label)) },
                            modifier = Modifier.fillMaxWidth(),
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

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { spotsExpanded = !spotsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(R.string.select_spot_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewMode = "dropdown" }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List, 
                                        contentDescription = stringResource(R.string.content_desc_menu),
                                        tint = if(viewMode == "dropdown") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(onClick = { viewMode = "grid" }) {
                                    Icon(
                                        Icons.Default.ViewModule, 
                                        contentDescription = stringResource(R.string.selection_legend),
                                        tint = if(viewMode == "grid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                IconButton(onClick = { spotsExpanded = !spotsExpanded }) {
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
                                ViewParkingFilterChips(selectedType = spotTypeFilter, onTypeSelected = { spotTypeFilter = it; selectedSpot = null })
                                if (viewMode == "dropdown") {
                                    ViewSpotDropdown(selectedSpot = selectedSpot, onSpotSelected = { selectedSpot = it }, occupiedSpots = occupiedSpots, userSpots = userSpots, spotTypeFilter = spotTypeFilter)
                                } else {
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ViewLegendItem(stringResource(R.string.available_legend), SuccessGreen)
                                            ViewLegendItem(stringResource(R.string.occupied_legend), MaterialTheme.colorScheme.outlineVariant)
                                            ViewLegendItem(stringResource(R.string.user_spot_legend), UserSpotYellow)
                                        }
                                        ViewSpotGrid(selectedSpot = selectedSpot, onSpotSelected = { selectedSpot = it }, occupiedSpots = occupiedSpots, userSpots = userSpots, spotTypeFilter = spotTypeFilter)
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedSpot != null) {
                    val spotReservations = reservations.filter { it.date == dateStr && it.spotNumber == selectedSpot }
                    val isUserSpot = userSpots.contains(selectedSpot!!)
                    val spotType = ParkingUtils.getSpotType(selectedSpot!!)
                    
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
                                            spotReservations.isNotEmpty() -> MaterialTheme.colorScheme.outlineVariant
                                            else -> SuccessGreen
                                        },
                                        shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { 
                                            Icon(ParkingUtils.getSpotIcon(spotType), null, tint = Color.White) 
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(stringResource(R.string.spot_number_label, selectedSpot!!), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
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
                                        spotReservations.isNotEmpty() -> stringResource(R.string.status_occupied)
                                        else -> stringResource(R.string.status_available)
                                    }, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (spotReservations.isNotEmpty()) {
                                Text(stringResource(R.string.reservations_count_label, spotReservations.size), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurface)
                                spotReservations.forEach { reservation ->
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
                                                    color = MaterialTheme.colorScheme.onSurface
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
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Calendar.getInstance().apply { timeInMillis = it }
                        selectedSpot = null
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewParkingFilterChips(selectedType: SpotType?, onTypeSelected: (SpotType?) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ViewParkingFilterChip(selected = selectedType == null, onClick = { onTypeSelected(null) }, label = stringResource(R.string.filter_all), icon = Icons.Default.LocalParking)
        ViewParkingFilterChip(selected = selectedType == SpotType.NORMAL, onClick = { onTypeSelected(SpotType.NORMAL) }, label = stringResource(R.string.spot_type_normal), icon = Icons.Default.DirectionsCar)
        ViewParkingFilterChip(selected = selectedType == SpotType.ELECTRIC, onClick = { onTypeSelected(SpotType.ELECTRIC) }, label = stringResource(R.string.spot_type_electric), icon = Icons.Default.ElectricCar)
        ViewParkingFilterChip(selected = selectedType == SpotType.MOTORCYCLE, onClick = { onTypeSelected(SpotType.MOTORCYCLE) }, label = stringResource(R.string.spot_type_motorcycle), icon = Icons.Default.TwoWheeler)
        ViewParkingFilterChip(selected = selectedType == SpotType.DISABLED, onClick = { onTypeSelected(SpotType.DISABLED) }, label = stringResource(R.string.spot_type_disabled), icon = Icons.AutoMirrored.Filled.Accessible)
    }
}

@Composable
fun ViewParkingFilterChip(selected: Boolean, onClick: () -> Unit, label: String, icon: ImageVector) {
    Surface(
        modifier = Modifier.clickable { onClick() },
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
fun ViewSpotDropdown(selectedSpot: Int?, onSpotSelected: (Int) -> Unit, occupiedSpots: List<Int>, userSpots: List<Int>, spotTypeFilter: SpotType?) {
    var expanded by remember { mutableStateOf(false) }
    val spots = (1..50).filter { spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
                val isOccupied = occupiedSpots.contains(spot); val isUserSpot = userSpots.contains(spot)
                val spotType = ParkingUtils.getSpotType(spot)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(ParkingUtils.getSpotIcon(spotType), null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.spot_number_with_prefix, stringResource(R.string.available_legend), spot), color = MaterialTheme.colorScheme.onSurface)
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                if (isUserSpot) Badge(containerColor = UserSpotYellow) { Text(stringResource(R.string.badge_mine), modifier = Modifier.padding(2.dp), color = Color.White) }
                                else if (isOccupied) Badge(containerColor = MaterialTheme.colorScheme.outlineVariant) { Text(stringResource(R.string.status_occupied), modifier = Modifier.padding(2.dp)) }
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
fun ViewSpotGrid(selectedSpot: Int?, onSpotSelected: (Int) -> Unit, occupiedSpots: List<Int>, userSpots: List<Int>, spotTypeFilter: SpotType?) {
    val spots = (1..50).filter { spotTypeFilter == null || ParkingUtils.getSpotType(it) == spotTypeFilter }
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        spots.forEach { spot ->
            val isOccupied = occupiedSpots.contains(spot); val isUserSpot = userSpots.contains(spot); val isSelected = selectedSpot == spot
            val spotType = ParkingUtils.getSpotType(spot)
            Box(
                modifier = Modifier.size(60.dp)
                    .background(when { isUserSpot -> UserSpotYellow; isOccupied -> MaterialTheme.colorScheme.outlineVariant; else -> SuccessGreen }, RoundedCornerShape(8.dp))
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
