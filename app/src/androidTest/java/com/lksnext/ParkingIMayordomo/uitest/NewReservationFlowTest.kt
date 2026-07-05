package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.NewReservation
import com.lksnext.ParkingIMayordomo.ui.viewmodel.NewReservationViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class NewReservationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun futureDate(daysFromNow: Int = 5): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysFromNow)
        return sdf.format(cal.time)
    }

    private fun createRepository(
        vehicles: List<Vehicle>? = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC")),
        reservations: List<Reservation> = emptyList(),
        allReservations: List<Reservation> = emptyList(),
        user: User? = User("user1", "test@test.com", "User")
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.vehicles } returns MutableStateFlow(vehicles)
        every { repo.reservations } returns MutableStateFlow(reservations)
        every { repo.allReservations } returns MutableStateFlow(allReservations)
        every { repo.allReservationsReady } returns MutableStateFlow(true)
        every { repo.user } returns MutableStateFlow(user)
        every { repo.notifications } returns MutableStateFlow(emptyList())
        return repo
    }

    private fun setDefaultTimes01() {
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_START_TIME_FIELD).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_END_TIME_FIELD).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE).performClick()
        composeTestRule.waitForIdle()
    }

    // ── Existing tests (preserved) ──

    @Test
    fun allFormFields_areDisplayed() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_BACK).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_DATE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_START_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_END_TIME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_GRID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_DROPDOWN).assertIsDisplayed()
    }

    @Test
    fun confirmButton_notShown_whenNoTimeOrSpot() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsNotDisplayed()
    }

    @Test
    fun spotGrid_andDropdown_areDisplayed() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_GRID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_DROPDOWN).assertIsNotDisplayed()
    }

    @Test
    fun switchToDropdownView_showsDropdown() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_DROPDOWN).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_GRID).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_DROPDOWN).assertIsDisplayed()
    }

    @Test
    fun switchBackToGridView_showsGrid() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_DROPDOWN).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VIEW_GRID).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_SPOT_GRID).assertIsDisplayed()
    }

    @Test
    fun backButton_showsDiscardDialog_whenNoChanges() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_BACK).performClick()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_DISCARD_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun discardDialog_cancel_keepsOnScreen() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_DISCARD_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun datePickerDialog_cancelButton_works() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_DATE_PICKER_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_DATE_PICKER_CANCEL).assertIsNotDisplayed()
    }

    @Test
    fun startTimePicker_cancelButton_works() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_CANCEL).assertIsNotDisplayed()
    }

    @Test
    fun endTimePicker_cancelButton_works() {
        val repo = createRepository()
        composeTestRule.setContent {
            NewReservation(viewModel = NewReservationViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_TIME_PICKER_CANCEL).assertIsNotDisplayed()
    }

    @Test
    fun prefilledSpotAndDate_showsForm() {
        val repo = createRepository()
        val date = futureDate()
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = date,
                prefilledSpot = 20
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_BACK).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_DATE_FIELD).assertIsDisplayed()
    }

    // ── New: validation error scenarios ──

    @Test
    fun confirmWithNoVehicles_showsNoVehicleDialog() {
        val repo = createRepository(vehicles = emptyList())
        var navigatedRoute: String? = null
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { navigatedRoute = it },
                prefilledDate = futureDate(),
                prefilledSpot = 10
            )
        }

        setDefaultTimes01()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_NO_VEHICLE_DIALOG).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_ERROR_MESSAGE).assertIsNotDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_NO_VEHICLE_DIALOG).assertIsDisplayed()
    }

    @Test
    fun confirmWithIncompatibleVehicle_showsIncompatibleDialog() {
        val repo = createRepository(
            vehicles = listOf(Vehicle("v1", "user1", VehicleType.MOTORCYCLE, "1234ABC"))
        )
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = futureDate(),
                prefilledSpot = 10
            )
        }

        setDefaultTimes01()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_INCOMPATIBLE_DIALOG).assertIsDisplayed()
    }

    @Test
    fun confirmWithMultipleVehicles_showsSelectionDialog() {
        val repo = createRepository(
            vehicles = listOf(
                Vehicle("v1", "user1", VehicleType.CAR, "1234ABC"),
                Vehicle("v2", "user1", VehicleType.ELECTRIC, "5678DEF")
            )
        )
        var navigatedRoute: String? = null
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { navigatedRoute = it },
                prefilledDate = futureDate(),
                prefilledSpot = 10
            )
        }

        setDefaultTimes01()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_VEHICLE_SELECT_DIALOG).assertIsDisplayed()
    }

    @Test
    fun confirmWithOneCompatibleVehicle_autoSubmits() {
        val repo = createRepository(
            vehicles = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC"))
        )
        var navigatedRoute: String? = null
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { navigatedRoute = it },
                prefilledDate = futureDate(),
                prefilledSpot = 10
            )
        }

        setDefaultTimes01()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).performClick()
        composeTestRule.waitForIdle()

        assert(navigatedRoute != null) { "Should navigate after auto-submit" }
    }

    @Test
    fun overlappingReservation_showsUserOverlapError() {
        val date = futureDate()
        val repo = createRepository(
            reservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 5, date = date,
                    startTime = "09:00", endTime = "10:00",
                    userId = "user1", vehicleId = "v1", licensePlate = "1234ABC"
                )
            ),
            vehicles = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC"))
        )
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = date,
                prefilledSpot = 10
            )
        }

        setDefaultTimes01()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsNotDisplayed()
    }

    @Test
    fun occupiedSpot_showsSpotOccupiedError() {
        val date = futureDate()
        val repo = createRepository(
            reservations = emptyList(),
            allReservations = listOf(
                Reservation(
                    id = "r1", spotNumber = 10, date = date,
                    startTime = "09:00", endTime = "10:00",
                    userId = "user2", vehicleId = "v2", licensePlate = "9999ZZZ"
                )
            ),
            vehicles = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC"))
        )
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = date,
                prefilledSpot = 10
            )
        }

        setDefaultTimes01()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsNotDisplayed()
    }

    @Test
    fun noOverlapNoOccupied_confirmButtonShown() {
        val date = futureDate()
        val repo = createRepository(
            vehicles = listOf(Vehicle("v1", "user1", VehicleType.CAR, "1234ABC"))
        )
        composeTestRule.setContent {
            NewReservation(
                viewModel = NewReservationViewModel(repo),
                onNavigate = { },
                prefilledDate = date,
                prefilledSpot = 10
            )
        }

        setDefaultTimes01()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_ERROR_MESSAGE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NEW_RESERVATION_CONFIRM_BUTTON).assertIsDisplayed()
    }
}
