package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import java.util.*

class DashboardReservationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(
        user: User? = null,
        vehicles: List<Vehicle>? = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC")),
        reservations: List<Reservation> = emptyList()
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.user } returns MutableStateFlow(user)
        every { repo.vehicles } returns MutableStateFlow(vehicles)
        every { repo.reservations } returns MutableStateFlow(reservations)
        every { repo.notifications } returns MutableStateFlow(emptyList())
        every { repo.reports } returns MutableStateFlow(emptyList())
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
        val repo = createRepository(
            user = User("user1", "test@test.com", "User"),
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = "2099-01-01",
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
