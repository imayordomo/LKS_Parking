package com.lksnext.ParkingIMayordomo.ui.pages

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ProfileViewModel
import com.lksnext.ParkingIMayordomo.utils.LocaleManager
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_ABOUT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_LOGIN
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Profile(
    viewModel: ProfileViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    showVehicleAlertInit: Boolean = false
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val user by viewModel.user.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val reservations by viewModel.reservations.collectAsState()
    val errorResId by viewModel.errorResId.collectAsState()

    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showAddVehicleDialog by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showVehicleAlert by rememberSaveable { mutableStateOf(showVehicleAlertInit) }
    
    var vehicleToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCannotDeleteVehicleDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(errorResId) {
        errorResId?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_PROFILE,
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
                    selectedItem = 2,
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(120.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = user?.name ?: "",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = user?.email ?: "",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Edit Profile Button
                            OutlinedButton(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6D00)),
                                border = BorderStroke(1.dp, Color(0xFFFF6D00)),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.SemiBold)
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Language Selector
                            LanguageSelector()

                            Spacer(modifier = Modifier.height(24.dp))

                            // Logout Button
                            TextButton(
                                onClick = { showLogoutDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.logout))
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.my_vehicles),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = { showAddVehicleDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add))
                        }
                    }
                }

                if (vehicles?.isEmpty() == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(48.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_vehicles),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    items(vehicles.orEmpty(), key = { it.id }) { vehicle ->
                        VehicleItem(
                            vehicle = vehicle,
                            onDelete = {
                                val hasReservations = reservations.any { it.vehicleId == vehicle.id }
                                if (hasReservations) {
                                    showCannotDeleteVehicleDialog = true
                                } else {
                                    vehicleToDeleteId = vehicle.id
                                }
                            }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showVehicleAlert) {
        AlertDialog(
            onDismissRequest = { showVehicleAlert = false },
            title = { Text(stringResource(R.string.vehicle_required_title)) },
            text = { Text(stringResource(R.string.vehicle_required_alert)) },
            confirmButton = {
                Button(onClick = { 
                    showVehicleAlert = false
                    showAddVehicleDialog = true
                }) {
                    Text(stringResource(R.string.add_vehicle))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVehicleAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = user?.name ?: "",
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName ->
                viewModel.updateProfile(newName)
                showEditProfileDialog = false
            }
        )
    }

    if (showAddVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onAdd = { type, plate ->
                viewModel.addVehicle(type, plate) {
                    showAddVehicleDialog = false
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        onNavigate(ROUTE_LOGIN)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        )
    }
    
    // Confirmation dialog for vehicle deletion
    if (vehicleToDeleteId != null) {
        val vToDelete = vehicles?.find { it.id == vehicleToDeleteId }
        AlertDialog(
            onDismissRequest = { vehicleToDeleteId = null },
            title = { Text(stringResource(R.string.delete_vehicle_title)) },
            text = { Text(stringResource(R.string.delete_vehicle_msg, vToDelete?.licensePlate ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        vehicleToDeleteId?.let { viewModel.removeVehicle(it) }
                        vehicleToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_btn), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDeleteId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        )
    }

    if (showCannotDeleteVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showCannotDeleteVehicleDialog = false },
            title = { Text(stringResource(R.string.delete_vehicle_error_title)) },
            text = { Text(stringResource(R.string.delete_vehicle_error_msg)) },
            confirmButton = {
                Button(onClick = { showCannotDeleteVehicleDialog = false }) {
                    Text(stringResource(R.string.save))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

data class LanguageOption(
    val code: String, 
    val nameRes: Int, 
    val flagEmoji: String? = null, 
    val flagDrawable: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector() {
    val context = LocalContext.current
    val currentLanguageCode by LocaleManager.localeFlow.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf(
        LanguageOption("es", R.string.language_spanish, flagEmoji = "🇪🇸"),
        LanguageOption("eu", R.string.language_basque, flagDrawable = R.drawable.basque_flag),
        LanguageOption("en", R.string.language_english, flagEmoji = "🇬🇧")
    )

    val currentLang = languages.find { it.code == currentLanguageCode } ?: languages[0]
    val localizedName = stringResource(currentLang.nameRes)

    Box(modifier = Modifier.width(220.dp), contentAlignment = Alignment.Center) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = localizedName,
                onValueChange = {},
                readOnly = true,
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.select_language))
                    }
                },
                leadingIcon = {
                    LanguageFlag(currentLang)
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6D00),
                    unfocusedBorderColor = Color(0xFFFF6D00),
                    focusedLabelColor = Color(0xFFFF6D00),
                    unfocusedLabelColor = Color(0xFFFF6D00),
                    focusedTrailingIconColor = Color(0xFF757575),
                    unfocusedTrailingIconColor = Color(0xFF757575)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start, fontSize = 16.sp),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                languages.forEach { lang ->
                    val langName = stringResource(lang.nameRes)
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LanguageFlag(lang)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(langName, fontWeight = if (lang.code == currentLanguageCode) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        onClick = {
                            expanded = false
                            LocaleManager.updateLocale(context, lang.code)
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageFlag(option: LanguageOption) {
    if (option.flagDrawable != null) {
        Image(
            painter = painterResource(id = option.flagDrawable),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Text(text = option.flagEmoji ?: "", fontSize = 18.sp)
    }
}

@Composable
fun VehicleItem(vehicle: Vehicle, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = ParkingUtils.getVehicleIcon(vehicle.type)
            val iconColor = ParkingUtils.getVehicleColor(vehicle.type)

            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = vehicle.licensePlate, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = stringResource(ParkingUtils.getVehicleTypeLabelRes(vehicle.type)),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.full_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(onDismiss: () -> Unit, onAdd: (VehicleType, String) -> Unit) {
    var type by rememberSaveable { mutableStateOf(VehicleType.CAR) }
    var plate by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_vehicle), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(ParkingUtils.getVehicleTypeLabelRes(type)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.vehicle_type)) },
                        leadingIcon = {
                            val icon = ParkingUtils.getVehicleIcon(type)
                            val iconColor = ParkingUtils.getVehicleColor(type)
                            Icon(icon, contentDescription = null, tint = iconColor)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        VehicleType.entries.forEach { vType ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = ParkingUtils.getVehicleIcon(vType)
                                        val iconColor = ParkingUtils.getVehicleColor(vType)
                                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(ParkingUtils.getVehicleTypeLabelRes(vType)))
                                    }
                                },
                                onClick = {
                                    type = vType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text(stringResource(R.string.license_plate)) },
                    placeholder = { Text(stringResource(R.string.license_plate_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(type, plate) },
                enabled = plate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                onDismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    )
}
