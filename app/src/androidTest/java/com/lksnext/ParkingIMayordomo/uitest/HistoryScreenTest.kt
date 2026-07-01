package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.History
import com.lksnext.ParkingIMayordomo.ui.viewmodel.HistoryViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(
        reservations: List<Reservation> = emptyList()
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.reservations } returns MutableStateFlow(reservations)
        every { repo.vehicles } returns MutableStateFlow(emptyList())
        every { repo.user } returns MutableStateFlow(null)
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
}
