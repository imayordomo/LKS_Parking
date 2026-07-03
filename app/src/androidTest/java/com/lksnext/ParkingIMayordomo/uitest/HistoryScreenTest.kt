package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.History
import com.lksnext.ParkingIMayordomo.ui.viewmodel.HistoryViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(
        reservations: List<Reservation> = emptyList(),
        user: User? = null
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.reservations } returns MutableStateFlow(reservations)
        every { repo.vehicles } returns MutableStateFlow(emptyList())
        every { repo.user } returns MutableStateFlow(user)
        every { repo.notifications } returns MutableStateFlow(emptyList())
        return repo
    }

    @Test
    fun downloadCsvButton_isDisplayed() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_DOWNLOAD_CSV).assertIsDisplayed()
    }

    @Test
    fun expandFilters_togglesFilterSection() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_START_DATE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_END_DATE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_ALL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_PAST).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_FUTURE).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_START_DATE_FIELD).assertIsNotDisplayed()
    }

    @Test
    fun filters_clickFilterAll() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_ALL).performClick()
    }

    @Test
    fun filters_clickFilterPast() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_PAST).performClick()
    }

    @Test
    fun filters_clickFilterFuture() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_FUTURE).performClick()
    }

    @Test
    fun startDateField_opensDatePicker() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_START_DATE_FIELD).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_DATE_PICKER_SAVE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_DATE_PICKER_CANCEL).assertIsDisplayed()
    }

    @Test
    fun endDateField_opensDatePicker() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_END_DATE_FIELD).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_DATE_PICKER_SAVE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_DATE_PICKER_CANCEL).assertIsDisplayed()
    }

    @Test
    fun datePickerCancel_closesDialog() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_START_DATE_FIELD).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_DATE_PICKER_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_DATE_PICKER_SAVE).assertIsNotDisplayed()
    }

    @Test
    fun clearFilters_showsWhenFilterActive() {
        val vm = HistoryViewModel(createRepository())
        vm.setStatusFilter("past")
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_CLEAR_FILTERS).assertIsDisplayed()
    }

    @Test
    fun clearFilters_hidesWhenNoFilters() {
        val vm = HistoryViewModel(createRepository())
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_CLEAR_FILTERS).assertIsNotDisplayed()
    }

    @Test
    fun clearFilters_removesFilters() {
        val vm = HistoryViewModel(createRepository())
        vm.setStatusFilter("past")
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_CLEAR_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_CLEAR_FILTERS).assertIsNotDisplayed()
    }

    // ── Filter logic verification (past / future / all) ──

    @Test
    fun filterAll_showsAllReservations() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val pastDate = ParkingUtils.formatDate(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 60)
        val futureDate = ParkingUtils.formatDate(cal.time)

        val pastReservation = Reservation("r1", 5, pastDate, "09:00", "11:00", "u1", "v1", licensePlate = "1234ABC")
        val futureReservation = Reservation("r2", 10, futureDate, "09:00", "11:00", "u1", "v1", licensePlate = "5678DEF")
        val vm = HistoryViewModel(createRepository(
            user = User("u1", "test@test.com", "Test User"),
            reservations = listOf(pastReservation, futureReservation)
        ))
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        // Default filter is "all" – both reservations displayed
        composeTestRule.onNodeWithText("P 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("P 10").assertIsDisplayed()
    }

    @Test
    fun filterPast_showsOnlyPastReservations() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val pastDate = ParkingUtils.formatDate(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 60)
        val futureDate = ParkingUtils.formatDate(cal.time)

        val pastReservation = Reservation("r1", 5, pastDate, "09:00", "11:00", "u1", "v1", licensePlate = "1234ABC")
        val futureReservation = Reservation("r2", 10, futureDate, "09:00", "11:00", "u1", "v1", licensePlate = "5678DEF")
        val vm = HistoryViewModel(createRepository(
            user = User("u1", "test@test.com", "Test User"),
            reservations = listOf(pastReservation, futureReservation)
        ))
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_PAST).performClick()

        composeTestRule.onNodeWithText("P 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("P 10").assertDoesNotExist()
    }

    @Test
    fun filterFuture_showsOnlyFutureReservations() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val pastDate = ParkingUtils.formatDate(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 60)
        val futureDate = ParkingUtils.formatDate(cal.time)

        val pastReservation = Reservation("r1", 5, pastDate, "09:00", "11:00", "u1", "v1", licensePlate = "1234ABC")
        val futureReservation = Reservation("r2", 10, futureDate, "09:00", "11:00", "u1", "v1", licensePlate = "5678DEF")
        val vm = HistoryViewModel(createRepository(
            user = User("u1", "test@test.com", "Test User"),
            reservations = listOf(pastReservation, futureReservation)
        ))
        composeTestRule.setContent {
            History(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.HISTORY_EXPAND_FILTERS).performClick()
        composeTestRule.onNodeWithTag(TestTags.HISTORY_FILTER_FUTURE).performClick()

        composeTestRule.onNodeWithText("P 10").assertIsDisplayed()
        composeTestRule.onNodeWithText("P 5").assertDoesNotExist()
    }
}
