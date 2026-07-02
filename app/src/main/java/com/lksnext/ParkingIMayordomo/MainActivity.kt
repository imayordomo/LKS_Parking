package com.lksnext.ParkingIMayordomo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.messaging.FirebaseMessaging
import com.lksnext.ParkingIMayordomo.data.AuthManager
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepositoryImpl
import com.lksnext.ParkingIMayordomo.ui.pages.*
import com.lksnext.ParkingIMayordomo.ui.theme.LKS_ParkingTheme
import com.lksnext.ParkingIMayordomo.ui.viewmodel.*
import com.lksnext.ParkingIMayordomo.utils.LocaleManager
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.PARAM_SHOW_VEHICLE_ALERT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_ABOUT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_EDIT_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_FORGOT_PASSWORD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HELP
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_LANDING
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_LOGIN
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NEW_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_REGISTER
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_REPORT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            obtenerFCMToken()
        } else {
            Toast.makeText(
                this,
                "No recibirás avisos de tus turnos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Capture system locale before wrapping (preserves it for auth pages)
        LocaleManager.captureSystemLocale(newBase)
        // Manually wrap the context with the saved locale before the Activity is created.
        // This is necessary for ComponentActivity to load the correct strings.xml.
        super.attachBaseContext(LocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize/Sync AppCompatDelegate with the saved preference for system-level strings
        LocaleManager.init(this)
        
        // Initialize AuthManager with context to enable local reminders
        AuthManager.init(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askNotificationPermission()

        setContent {
            LKS_ParkingTheme {
                AppNavigation()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                obtenerFCMToken()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            obtenerFCMToken()
        }
    }

    private fun obtenerFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_PARKING", "Error al obtener el token de Firebase", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM_PARKING", "TU TOKEN DE PRUEBA ES: $token")
            
            lifecycleScope.launch {
                AuthManager.updateFcmToken(token)
            }
        }
    }
}

@Composable
fun ProtectedRoute(
    user: User?,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    if (user == null) {
        LaunchedEffect(Unit) {
            navController.navigate(ROUTE_LANDING) {
                popUpTo(0) { inclusive = true }
            }
        }
    } else {
        content()
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val repository = remember { ParkingRepositoryImpl() }
    val factory = remember { ViewModelFactory(repository) }
    val currentUser by repository.user.collectAsState()
    
    val startRoute = remember {
        if (currentUser != null) ROUTE_DASHBOARD else ROUTE_LANDING
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(ROUTE_LANDING) {
            Landing(
                onLoginClick = { navController.navigate(ROUTE_LOGIN) }, 
                onRegisterClick = { navController.navigate(ROUTE_REGISTER) })
        }
        composable(ROUTE_LOGIN) {
            Login(viewModel = viewModel(factory = factory),
                onRegisterClick = { navController.navigate(ROUTE_REGISTER) },
                onForgotPasswordClick = { navController.navigate(ROUTE_FORGOT_PASSWORD) },
                onLoginSuccess = { navController.navigate(ROUTE_DASHBOARD) { popUpTo(ROUTE_LANDING) { inclusive = true } } })
        }
        composable(ROUTE_REGISTER) {
            Register(viewModel = viewModel(factory = factory),
                onBackToLogin = { navController.navigate(ROUTE_LOGIN) },
                onRegisterSuccess = { navController.navigate(ROUTE_DASHBOARD) { popUpTo(ROUTE_LANDING) { inclusive = true } } })
        }
        composable(ROUTE_FORGOT_PASSWORD) {
            ForgotPassword(viewModel = viewModel(factory = factory), onBackToLogin = { navController.popBackStack() })
        }
        
        composable(ROUTE_DASHBOARD) {
            ProtectedRoute(currentUser, navController) {
                Dashboard(viewModel = viewModel(factory = factory), onNavigate = { navController.navigate(it) })
            }
        }
        composable(ROUTE_HISTORY) {
            ProtectedRoute(currentUser, navController) {
                History(viewModel = viewModel(factory = factory), onNavigate = { navController.navigate(it) })
            }
        }
        composable("${ROUTE_PROFILE}?${PARAM_SHOW_VEHICLE_ALERT}={showVehicleAlert}",
            arguments = listOf(navArgument("showVehicleAlert") { type = NavType.BoolType; defaultValue = false })
        ) { backStackEntry ->
            ProtectedRoute(currentUser, navController) {
                Profile(viewModel = viewModel(factory = factory), 
                    onNavigate = { navController.navigate(it) }, 
                    showVehicleAlertInit = backStackEntry.arguments?.getBoolean("showVehicleAlert") ?: false)
            }
        }
        composable(ROUTE_VIEW_PARKING) {
            ProtectedRoute(currentUser, navController) {
                ViewParking(viewModel = viewModel(factory = factory), onNavigate = { navController.navigate(it) })
            }
        }
        composable(ROUTE_NOTIFICATIONS) {
            ProtectedRoute(currentUser, navController) {
                Notifications(viewModel = viewModel(factory = factory), onNavigate = { navController.navigate(it) })
            }
        }
        composable(ROUTE_REPORT) {
            ProtectedRoute(currentUser, navController) {
                Report(viewModel = viewModel(factory = factory), onNavigate = { navController.navigate(it) })
            }
        }
        composable(ROUTE_HELP) {
            ProtectedRoute(currentUser, navController) {
                Help(onNavigate = { navController.navigate(it) })
            }
        }
        composable(ROUTE_ABOUT) {
            ProtectedRoute(currentUser, navController) {
                About(onNavigate = { navController.navigate(it) })
            }
        }
        composable("${ROUTE_NEW_RESERVATION}?spot={spot}&date={date}",
            arguments = listOf(
                navArgument("spot") { type = NavType.IntType; defaultValue = -1 },
                navArgument("date") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            ProtectedRoute(currentUser, navController) {
                NewReservation(viewModel = viewModel(factory = factory), onNavigate = { navController.navigate(it) },
                    prefilledSpot = backStackEntry.arguments?.getInt("spot")?.takeIf { it != -1 },
                    prefilledDate = backStackEntry.arguments?.getString("date")?.takeIf { it.isNotEmpty() })
            }
        }
        composable("${ROUTE_EDIT_RESERVATION}/{reservationId}",
            arguments = listOf(navArgument("reservationId") { type = NavType.StringType })
        ) { backStackEntry ->
            ProtectedRoute(currentUser, navController) {
                EditReservation(viewModel = viewModel(factory = factory), 
                    reservationId = backStackEntry.arguments?.getString("reservationId") ?: "", 
                    onNavigate = { navController.navigate(it) })
            }
        }
    }
}
