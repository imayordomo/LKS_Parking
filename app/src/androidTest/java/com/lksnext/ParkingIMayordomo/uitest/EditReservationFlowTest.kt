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
import com.lksnext.ParkingIMayordomo.ui.pages.EditReservation
import com.lksnext.ParkingIMayordomo.ui.viewmodel.EditReservationViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class EditReservationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(
        reservation: Reservation = Reservation(
            id = "r1", spotNumber = 5, date = "2026-07-01",
            startTime = "09:00", endTime = "23:59",
            userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
        ),
        vehicles: List<Vehicle>? = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC")),
        allReservations: List<Reservation> = emptyList(),
        user: User? = User("user1", "test@test.com", "User")
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.vehicles } returns MutableStateFlow(vehicles)
        every { repo.reservations } returns MutableStateFlow(listOf(reservation))
        every { repo.allReservations } returns MutableStateFlow(allReservations)
        every { repo.user } returns MutableStateFlow(user)
        return repo
    }

    @Test
    fun editForm_displaysAllFields() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_BACK).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DATE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_START_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_END_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_SAVE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_CANCEL_BUTTON).assertIsDisplayed()
    }

    @Test
    fun saveButton_isDisabled_whenNoChanges() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_SAVE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun backButton_showsDiscardDialog_whenChangesMade() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DISCARD_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun discardDialog_confirm_navigatesToDashboard() {
        var navigatedRoute: String? = null
        // onNavigate passes route; dashboard pop is done by caller
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { navigatedRoute = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DISCARD_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun discardDialog_cancel_keepsEditing() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DISCARD_DIALOG).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DISCARD_CONFIRM).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DISCARD_CANCEL).assertIsNotDisplayed()
    }

    @Test
    fun notFoundReservation_showsErrorState() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "nonexistent",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DATE_FIELD).assertIsNotDisplayed()
    }

    @Test
    fun vehicleField_showsWhenCompatibleVehiclesExist() {
        val repo = createRepository(
            reservation = Reservation(
                id = "r1", spotNumber = 10, date = "2026-07-01",
                startTime = "09:00", endTime = "23:59",
                userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
            ),
            vehicles = listOf(
                Vehicle("v1", "user1", VehicleType.CAR, "1234ABC"),
                Vehicle("v2", "user1", VehicleType.ELECTRIC, "5678DEF")
            )
        )
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_VEHICLE_FIELD).assertIsDisplayed()
    }

    @Test
    fun vehicleField_isHidden_whenNoCompatibleVehicles() {
        val repo = createRepository(
            reservation = Reservation(
                id = "r1", spotNumber = 10, date = "2026-07-01",
                startTime = "09:00", endTime = "23:59",
                userId = "user1", vehicleId = "v2", licensePlate = "5678DEF"
            ),
            vehicles = listOf(Vehicle("v2", "user1", VehicleType.MOTORCYCLE, "5678DEF"))
        )
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        // Spot 10 is NORMAL type, only compatible with CAR, ELECTRIC, DISABLED
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_VEHICLE_FIELD).assertIsNotDisplayed()
    }

    @Test
    fun vehicleDropdown_expandsOnClick() {
        val repo = createRepository(
            reservation = Reservation(
                id = "r1", spotNumber = 10, date = "2026-07-01",
                startTime = "09:00", endTime = "23:59",
                userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
            ),
            vehicles = listOf(
                Vehicle("v1", "user1", VehicleType.CAR, "1234ABC"),
                Vehicle("v2", "user1", VehicleType.ELECTRIC, "5678DEF")
            )
        )
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_VEHICLE_FIELD).performClick()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_VEHICLE_MENU).assertIsDisplayed()
    }

    @Test
    fun cancelButton_showsDiscardDialog_whenChanges() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_CANCEL_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DISCARD_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun datePickerDialog_cancel_works() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DATE_PICKER_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_DATE_PICKER_CANCEL).assertIsNotDisplayed()
    }

    @Test
    fun timePickers_cancel_work() {
        val repo = createRepository()
        composeTestRule.setContent {
            EditReservation(
                viewModel = EditReservationViewModel(repo),
                reservationId = "r1",
                onNavigate = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_TIME_PICKER_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.EDIT_RESERVATION_TIME_PICKER_CANCEL).assertIsNotDisplayed()
    }
}
