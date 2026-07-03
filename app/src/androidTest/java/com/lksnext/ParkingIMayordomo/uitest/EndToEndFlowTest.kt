package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lksnext.ParkingIMayordomo.data.model.Notification
import com.lksnext.ParkingIMayordomo.data.model.Report
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.About
import com.lksnext.ParkingIMayordomo.ui.pages.Dashboard
import com.lksnext.ParkingIMayordomo.ui.pages.EditReservation
import com.lksnext.ParkingIMayordomo.ui.pages.Help
import com.lksnext.ParkingIMayordomo.ui.pages.History
import com.lksnext.ParkingIMayordomo.ui.pages.NewReservation
import com.lksnext.ParkingIMayordomo.ui.pages.Notifications
import com.lksnext.ParkingIMayordomo.ui.pages.Profile
import com.lksnext.ParkingIMayordomo.ui.pages.Register
import com.lksnext.ParkingIMayordomo.ui.pages.Report
import com.lksnext.ParkingIMayordomo.ui.pages.ViewParking
import com.lksnext.ParkingIMayordomo.ui.viewmodel.DashboardViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.EditReservationViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.HistoryViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.NewReservationViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.NotificationsViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ProfileViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.RegisterViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ReportViewModel
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ViewParkingViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_ABOUT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_EDIT_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HELP
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_LOGIN
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NEW_RESERVATION
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_REGISTER
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_REPORT
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.TestTags
import java.util.Calendar
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EndToEndFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockRepository(
        user: User? = null,
        vehicles: List<Vehicle>? = emptyList(),
        reservations: List<Reservation> = emptyList(),
        allReservations: List<Reservation> = emptyList(),
        notifications: List<Notification> = emptyList(),
        reports: List<Report> = emptyList()
    ): ParkingRepository {
        val userFlow = MutableStateFlow(user)
        val vehiclesFlow = MutableStateFlow(vehicles)
        val reservationsFlow = MutableStateFlow(reservations)
        val allReservationsFlow = MutableStateFlow(allReservations)
        val notificationsFlow = MutableStateFlow(notifications)
        val reportsFlow = MutableStateFlow(reports)

        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.user } returns userFlow
        every { repo.vehicles } returns vehiclesFlow
        every { repo.reservations } returns reservationsFlow
        every { repo.allReservations } returns allReservationsFlow
        every { repo.allReservationsReady } returns MutableStateFlow(true)
        every { repo.notifications } returns notificationsFlow
        every { repo.reports } returns reportsFlow

        coEvery { repo.register(any(), any(), any()) } answers {
            userFlow.value = User("test-id", secondArg(), firstArg())
        }

        coEvery { repo.addVehicle(any(), any()) } answers {
            val type: VehicleType = firstArg()
            val plate: String = secondArg()
            val current = (vehiclesFlow.value ?: emptyList()).toMutableList()
            current.add(Vehicle(UUID.randomUUID().toString(), "test-id", type, plate))
            vehiclesFlow.value = current
        }

        coEvery { repo.addReservation(any(), any(), any(), any(), any(), any()) } answers {
            val spotNumber: Int = firstArg()
            val date: String = secondArg()
            val startTime: String = thirdArg()
            val endTime: String = arg(3)
            val vehicleId: String = arg(4)
            val licensePlate: String = arg(5)
            val reservation = Reservation(
                id = UUID.randomUUID().toString(),
                spotNumber = spotNumber,
                date = date,
                startTime = startTime,
                endTime = endTime,
                userId = "test-id",
                vehicleId = vehicleId,
                licensePlate = licensePlate
            )
            val current = (reservationsFlow.value ?: emptyList()).toMutableList()
            current.add(reservation)
            reservationsFlow.value = current
            allReservationsFlow.value = current
        }

        coEvery { repo.updateReservation(any(), any(), any(), any(), any(), any()) } answers {
            val id: String = firstArg()
            val current = (reservationsFlow.value ?: emptyList()).toMutableList()
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) {
                current[idx] = current[idx].copy(
                    date = secondArg(),
                    startTime = thirdArg(),
                    endTime = arg(3),
                    vehicleId = arg(4),
                    licensePlate = arg(5)
                )
                reservationsFlow.value = current
                allReservationsFlow.value = current
            }
        }

        coEvery { repo.deleteAccount() } answers {
            userFlow.value = null
        }

        every { repo.logout() } answers {
            userFlow.value = null
        }

        coEvery { repo.addReport(any(), any(), any()) } answers { }

        return repo
    }

    @Test
    fun registerToDeleteAccount_flow() {
        val repo = createMockRepository()
        var currentRoute by mutableStateOf(ROUTE_REGISTER)

        val registerVm = RegisterViewModel(repo)
        val dashboardVm = DashboardViewModel(repo)
        val profileVm = ProfileViewModel(repo)

        composeTestRule.setContent {
            when (currentRoute) {
                ROUTE_REGISTER -> Register(
                    viewModel = registerVm,
                    onBackToLogin = { currentRoute = ROUTE_LOGIN },
                    onRegisterSuccess = { currentRoute = ROUTE_DASHBOARD }
                )
                ROUTE_DASHBOARD -> Dashboard(
                    viewModel = dashboardVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_PROFILE -> Profile(
                    viewModel = profileVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_LOGIN -> Unit
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("Test User")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("test@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_FAB).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_PROFILE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_CONFIRM).performClick()
        composeTestRule.waitForIdle()

        assertEquals(ROUTE_LOGIN, currentRoute)
    }

    @Test
    fun registerToLogout_flow() {
        val repo = createMockRepository()
        var currentRoute by mutableStateOf(ROUTE_REGISTER)

        val registerVm = RegisterViewModel(repo)
        val dashboardVm = DashboardViewModel(repo)
        val profileVm = ProfileViewModel(repo)

        composeTestRule.setContent {
            when (currentRoute) {
                ROUTE_REGISTER -> Register(
                    viewModel = registerVm,
                    onBackToLogin = { currentRoute = ROUTE_LOGIN },
                    onRegisterSuccess = { currentRoute = ROUTE_DASHBOARD }
                )
                ROUTE_DASHBOARD -> Dashboard(
                    viewModel = dashboardVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_PROFILE -> Profile(
                    viewModel = profileVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_LOGIN -> Unit
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("Test User")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("test@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_FAB).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_PROFILE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_CONFIRM).performClick()
        composeTestRule.waitForIdle()

        assertEquals(ROUTE_LOGIN, currentRoute)
    }

    @Test
    fun registerAddVehicleAndCreateReservation_flow() {
        val repo = createMockRepository()
        var currentRoute by mutableStateOf(ROUTE_REGISTER)

        val registerVm = RegisterViewModel(repo)
        val dashboardVm = DashboardViewModel(repo)
        val profileVm = ProfileViewModel(repo)
        val newReservationVm = NewReservationViewModel(repo)

        composeTestRule.setContent {
            when (currentRoute) {
                ROUTE_REGISTER -> Register(
                    viewModel = registerVm,
                    onBackToLogin = { currentRoute = ROUTE_LOGIN },
                    onRegisterSuccess = { currentRoute = ROUTE_DASHBOARD }
                )
                ROUTE_DASHBOARD -> Dashboard(
                    viewModel = dashboardVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_PROFILE -> Profile(
                    viewModel = profileVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_NEW_RESERVATION -> NewReservation(
                    viewModel = newReservationVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_LOGIN -> Unit
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("Test User")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("test@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_FAB).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_PROFILE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Vehicle type defaults to CAR ("Normal"), so skip
        // the dropdown selection to avoid matching both the field and menu item
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_PLATE_FIELD).performTextInput("1234ABC")
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_ADD_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_DASHBOARD).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_FAB).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_DATE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_START_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_END_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_GRID).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_DROPDOWN).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_DROPDOWN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_GRID).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_GRID).assertIsDisplayed()
    }

    @Test
    fun editExistingReservation_flow() {
        val existingReservation = Reservation(
            id = "r1",
            spotNumber = 5,
            date = "2026-07-10",
            startTime = "09:00",
            endTime = "11:00",
            userId = "test-id",
            vehicleId = "v1",
            licensePlate = "1234ABC"
        )
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            vehicles = listOf(Vehicle("v1", "test-id", VehicleType.CAR, "1234ABC")),
            reservations = listOf(existingReservation),
            allReservations = listOf(existingReservation)
        )

        var navigatedRoute: String? = null

        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { navigatedRoute = it }
            )
        }

        composeTestRule.waitForIdle()

        // The reservation flow may emit asynchronously. Wait for the back button to appear (which means reservation != null).
        composeTestRule.waitUntil(timeoutMillis = 10000, condition = {
            try {
                composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_BACK).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        })

        // Wait for the date field to be displayed
        composeTestRule.waitUntil(timeoutMillis = 5000, condition = {
            try {
                composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DATE_FIELD).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        })
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_START_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_END_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_SAVE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_CANCEL_BUTTON).assertIsDisplayed()

        // Since hasChanges is false (no edits made), clicking cancel navigates directly without discard dialog
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_CANCEL_BUTTON).performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_DASHBOARD, navigatedRoute)
    }

    @Test
    fun sendReport_flow() {
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User")
        )
        var currentRoute by mutableStateOf(ROUTE_DASHBOARD)

        val dashboardVm = DashboardViewModel(repo)
        val reportVm = ReportViewModel(repo)

        composeTestRule.setContent {
            when (currentRoute) {
                ROUTE_DASHBOARD -> Dashboard(
                    viewModel = dashboardVm,
                    onNavigate = { route -> currentRoute = route }
                )
                ROUTE_REPORT -> Report(
                    viewModel = reportVm,
                    onNavigate = { route -> currentRoute = route }
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_FAB).assertIsDisplayed()

        currentRoute = ROUTE_REPORT
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.REPORT_TYPE_FIELD).performClick()
        composeTestRule.onNodeWithText("Da\u00f1o en la plaza").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.REPORT_SPOT_NUMBER_FIELD).performTextInput("5")
        composeTestRule.onNodeWithTag(TestTags.REPORT_DESCRIPTION_FIELD).performTextInput("Test report description")

        composeTestRule.onNodeWithTag(TestTags.REPORT_SEND_BUTTON).performClick()
        composeTestRule.waitForIdle()

        assertTrue("Report should show success state", reportVm.success.value)
    }

    @Test
    fun goToEveryPage_flow() {
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            vehicles = listOf(Vehicle("v1", "test-id", VehicleType.CAR, "1234ABC")),
            reservations = listOf(Reservation("r1", 5, "2026-07-10", "09:00", "11:00", "test-id", "v1", "1234ABC"))
        )
        var currentRoute by mutableStateOf(ROUTE_REGISTER)

        val registerVm = RegisterViewModel(repo)
        val dashboardVm = DashboardViewModel(repo)
        val historyVm = HistoryViewModel(repo)
        val profileVm = ProfileViewModel(repo)
        val viewParkingVm = ViewParkingViewModel(repo)
        val notificationsVm = NotificationsViewModel(repo)
        val reportVm = ReportViewModel(repo)

        composeTestRule.setContent {
            when (currentRoute) {
                ROUTE_REGISTER -> Register(viewModel = registerVm,
                    onBackToLogin = { currentRoute = ROUTE_LOGIN },
                    onRegisterSuccess = { currentRoute = ROUTE_DASHBOARD })
                ROUTE_DASHBOARD -> Dashboard(viewModel = dashboardVm, onNavigate = { currentRoute = it })
                ROUTE_HISTORY -> History(viewModel = historyVm, onNavigate = { currentRoute = it })
                ROUTE_PROFILE -> Profile(viewModel = profileVm, onNavigate = { currentRoute = it })
                ROUTE_VIEW_PARKING -> ViewParking(viewModel = viewParkingVm, onNavigate = { currentRoute = it })
                ROUTE_NOTIFICATIONS -> Notifications(viewModel = notificationsVm, onNavigate = { currentRoute = it })
                ROUTE_REPORT -> Report(viewModel = reportVm, onNavigate = { currentRoute = it })
                ROUTE_HELP -> Help(onNavigate = { currentRoute = it })
                ROUTE_ABOUT -> About(onNavigate = { currentRoute = it })
            }
        }

        composeTestRule.waitForIdle()

        // Register to get to Dashboard
        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("Test User")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("test@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("Test1234!")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Dashboard
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_FAB).assertIsDisplayed()

        // History
        currentRoute = ROUTE_HISTORY
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_DOWNLOAD_CSV).assertIsDisplayed()

        // Profile
        currentRoute = ROUTE_PROFILE
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_PROFILE_BUTTON).assertIsDisplayed()

        // View Parking
        currentRoute = ROUTE_VIEW_PARKING
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_DATE_FIELD).assertIsDisplayed()

        // Notifications
        currentRoute = ROUTE_NOTIFICATIONS
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).assertIsNotDisplayed()

        // Report
        currentRoute = ROUTE_REPORT
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.REPORT_TYPE_FIELD).assertIsDisplayed()

        // Help
        currentRoute = ROUTE_HELP
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag(TestTags.HELP_FAQ_ACCORDION)[0].assertIsDisplayed()

        // About
        currentRoute = ROUTE_ABOUT
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NAV_MENU_BUTTON).assertIsDisplayed()
    }

    @Test
    fun reservateFromGrid_flow() {
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            vehicles = listOf(Vehicle("v1", "test-id", VehicleType.CAR, "1234ABC"))
        )

        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = ParkingUtils.formatDate(Calendar.getInstance().time),
                prefilledSpot = null
            )
        }

        composeTestRule.waitForIdle()

        // Grid is shown by default
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_GRID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_DROPDOWN).assertIsNotDisplayed()

        // Select spot 5 from grid by clicking the spot text (pointer events propagate to parent Box)
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun reservateFromDdl_flow() {
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            vehicles = listOf(Vehicle("v1", "test-id", VehicleType.CAR, "1234ABC"))
        )

        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = ParkingUtils.formatDate(Calendar.getInstance().time),
                prefilledSpot = null
            )
        }

        composeTestRule.waitForIdle()

        // Switch to dropdown view
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_DROPDOWN).assertIsDisplayed()

        // Open the dropdown by clicking the "Libre" label (menuAnchor on the text field)
        composeTestRule.onNodeWithText("Libre").performClick()
        composeTestRule.waitForIdle()

        // Select spot 5 from the dropdown menu items (spots 1-5 are MOTORCYCLE type)
        composeTestRule.onNodeWithText("Libre #5 (Moto)").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun reservateFromViewParking_flow() {
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            reservations = listOf(Reservation("r1", 5, "2026-07-10", "09:00", "11:00", "test-id", "v1", "1234ABC"))
        )

        var navigatedRoute: String? = null

        composeTestRule.setContent {
            ViewParking(
                viewModel = ViewParkingViewModel(repo),
                onNavigate = { navigatedRoute = it }
            )
        }

        composeTestRule.waitForIdle()

        // Switch to dropdown view for easier spot selection
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_VIEW_DROPDOWN).performClick()
        composeTestRule.waitForIdle()

        // Open the dropdown by clicking the prompt text (menuAnchor on the text field)
        composeTestRule.onNodeWithText("Seleccionar plaza").performClick()
        composeTestRule.waitForIdle()

        // Select spot 5 from the dropdown menu items ("Libre #5" in Spanish local)
        composeTestRule.onNodeWithText("Libre #5").performClick()
        composeTestRule.waitForIdle()

        // Reserve button should appear
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_RESERVE_BUTTON).assertIsDisplayed()

        // Click Reserve
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_RESERVE_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Verify navigation includes new-reservation with spot param
        assertTrue(navigatedRoute?.startsWith(ROUTE_NEW_RESERVATION) == true)
    }

    @Test
    fun errorsDoingReservation_flow() {
        val existingReservation = Reservation(
            id = "r1", spotNumber = 5, date = ParkingUtils.formatDate(Calendar.getInstance().time),
            startTime = "09:00", endTime = "11:00",
            userId = "test-id", vehicleId = "v1", licensePlate = "1234ABC"
        )
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            vehicles = listOf(Vehicle("v1", "test-id", VehicleType.CAR, "1234ABC")),
            reservations = listOf(existingReservation),
            allReservations = listOf(existingReservation)
        )

        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = existingReservation.date,
                prefilledSpot = 5
            )
        }

        composeTestRule.waitForIdle()

        // Set start and end times that overlap with the existing reservation
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_START_TIME_FIELD).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_END_TIME_FIELD).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE).performClick()
        composeTestRule.waitForIdle()

        // With overlapping time, the confirm button should be hidden due to validation error
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsNotDisplayed()
    }

    @Test
    fun errorsEditingReservation_flow() {
        val existingReservation = Reservation(
            id = "r1", spotNumber = 5, date = "2026-07-10",
            startTime = "09:00", endTime = "11:00",
            userId = "test-id", vehicleId = "v1", licensePlate = "1234ABC"
        )
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            vehicles = listOf(Vehicle("v1", "test-id", VehicleType.CAR, "1234ABC")),
            reservations = listOf(existingReservation),
            allReservations = listOf(existingReservation)
        )

        var navigatedRoute: String? = null

        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { navigatedRoute = it }
            )
        }

        composeTestRule.waitForIdle()

        // Wait for the EditReservation form to load
        composeTestRule.waitUntil(timeoutMillis = 10000, condition = {
            try {
                composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_BACK).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        })

        // Wait for date field
        composeTestRule.waitUntil(timeoutMillis = 5000, condition = {
            try {
                composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DATE_FIELD).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        })

        // The save button is shown (it's disabled if no changes, but visible)
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_SAVE_BUTTON).assertIsDisplayed()

        // Since hasChanges is false (no edits made), clicking cancel navigates directly without discard dialog
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_CANCEL_BUTTON).performClick()
        composeTestRule.waitForIdle()
        assertEquals(ROUTE_DASHBOARD, navigatedRoute)
    }

    @Test
    fun viewParkingLegend_showsOccupiedForToday() {
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            reservations = listOf(Reservation("r1", 5, "2026-07-10", "09:00", "11:00", "test-id", "v1", "1234ABC"))
        )

        composeTestRule.setContent {
            ViewParking(
                viewModel = ViewParkingViewModel(repo),
                onNavigate = { }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Libre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ocupada").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu plaza").assertIsDisplayed()
    }

    @Test
    fun viewParkingLegend_showsPartiallyOccupiedForFutureDate() {
        val repo = createMockRepository(
            user = User("test-id", "test@lksnext.com", "Test User"),
            reservations = listOf(Reservation("r1", 5, "2026-07-10", "09:00", "11:00", "test-id", "v1", "1234ABC"))
        )

        composeTestRule.setContent {
            ViewParking(
                viewModel = ViewParkingViewModel(repo),
                onNavigate = { },
                prefilledDate = "2026-07-04"
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Libre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Parcialmente ocupada").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tu plaza").assertIsDisplayed()
    }
}
