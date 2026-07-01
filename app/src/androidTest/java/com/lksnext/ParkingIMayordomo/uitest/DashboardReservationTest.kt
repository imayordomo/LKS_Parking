package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.Dashboard
import com.lksnext.ParkingIMayordomo.ui.viewmodel.DashboardViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class DashboardReservationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(
        user: User? = User("user1", "test@test.com", "User"),
        vehicles: List<Vehicle>? = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC")),
        reservations: List<Reservation> = emptyList()
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.user } returns MutableStateFlow(user)
        every { repo.vehicles } returns MutableStateFlow(vehicles)
        every { repo.reservations } returns MutableStateFlow(reservations)
        return repo
    }

    @Test
    fun emptyState_showsNoReservations() {
        val repo = createRepository()
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_CREATE_RESERVATION_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_FAB).assertIsDisplayed()
    }

    @Test
    fun withReservation_cardIsDisplayed() {
        val futureDate = "2026-07-01"
        val repo = createRepository(
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = futureDate,
                    startTime = "09:00", endTime = "23:59",
                    userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
                )
            )
        )
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_RESERVATION_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_CREATE_RESERVATION_EMPTY).assertIsNotDisplayed()
    }

    @Test
    fun reservationCard_showsEditAndDeleteButtons() {
        val futureDate = "2026-07-01"
        val repo = createRepository(
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = futureDate,
                    startTime = "09:00", endTime = "23:59",
                    userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
                )
            )
        )
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_RESERVATION_EDIT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_RESERVATION_DELETE).assertIsDisplayed()
    }

    @Test
    fun deleteButton_showsDeleteDialog() {
        val futureDate = "2026-07-01"
        val repo = createRepository(
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = futureDate,
                    startTime = "09:00", endTime = "23:59",
                    userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
                )
            )
        )
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_RESERVATION_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_DELETE_RESERVATION_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_DELETE_RESERVATION_CONFIRM).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_DELETE_RESERVATION_CANCEL).assertIsDisplayed()
    }

    @Test
    fun deleteDialog_confirm_dismissesDialog() {
        val futureDate = "2026-07-01"
        val repo = createRepository(
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = futureDate,
                    startTime = "09:00", endTime = "23:59",
                    userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
                )
            )
        )
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_RESERVATION_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_DELETE_RESERVATION_CONFIRM).performClick()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_DELETE_RESERVATION_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun deleteDialog_cancel_dismissesDialog() {
        val futureDate = "2026-07-01"
        val repo = createRepository(
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = futureDate,
                    startTime = "09:00", endTime = "23:59",
                    userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
                )
            )
        )
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_RESERVATION_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_DELETE_RESERVATION_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_DELETE_RESERVATION_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun editButton_navigatesToEditReservation() {
        val futureDate = "2026-07-01"
        val repo = createRepository(
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = futureDate,
                    startTime = "09:00", endTime = "23:59",
                    userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
                )
            )
        )
        var navigatedRoute: String? = null
        composeTestRule.setContent {
            Dashboard(
                viewModel = DashboardViewModel(repo),
                onNavigate = { navigatedRoute = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_RESERVATION_EDIT).performClick()
        assert(navigatedRoute == "edit-reservation/r1") { "Expected edit-reservation/r1, got $navigatedRoute" }
    }

    @Test
    fun vehicleWarningBanner_shown_whenVehiclesEmpty() {
        val repo = createRepository(vehicles = emptyList())
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_VEHICLE_ALERT).assertIsDisplayed()
    }

    @Test
    fun vehicleWarningBanner_hidden_whenVehiclesPresent() {
        val repo = createRepository()
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_VEHICLE_ALERT).assertIsNotDisplayed()
    }

    @Test
    fun vehicleWarningBanner_hidden_whenVehiclesNotLoaded() {
        val repo = createRepository(vehicles = null)
        composeTestRule.setContent {
            Dashboard(viewModel = DashboardViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.DASHBOARD_ADD_VEHICLE_ALERT).assertIsNotDisplayed()
    }
}
