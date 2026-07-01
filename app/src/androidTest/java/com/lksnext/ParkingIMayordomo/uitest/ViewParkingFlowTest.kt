package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.ViewParking
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ViewParkingViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ViewParkingFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(
        user: User? = User("user1", "test@test.com", "User"),
        reservations: List<Reservation> = emptyList(),
        allReservations: List<Reservation> = emptyList()
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.user } returns MutableStateFlow(user)
        every { repo.reservations } returns MutableStateFlow(reservations)
        every { repo.allReservations } returns MutableStateFlow(allReservations)
        return repo
    }

    @Test
    fun viewParking_displaysDateAndSpotSection() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_DATE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_VIEW_GRID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_VIEW_DROPDOWN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_SPOT_GRID).assertIsDisplayed()
    }

    @Test
    fun switchToDropdownView_inViewParking() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_VIEW_DROPDOWN).performClick()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_SPOT_DROPDOWN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_SPOT_GRID).assertIsNotDisplayed()
    }

    @Test
    fun switchBackToGridView_inViewParking() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_VIEW_DROPDOWN).performClick()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_VIEW_GRID).performClick()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_SPOT_GRID).assertIsDisplayed()
    }

    @Test
    fun filterChips_areDisplayed() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_FILTER_ALL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_FILTER_NORMAL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_FILTER_ELECTRIC).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_FILTER_MOTORCYCLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_FILTER_DISABLED).assertIsDisplayed()
    }

    @Test
    fun selectFilterChip_filtersSpots() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_FILTER_ELECTRIC).performClick()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_SPOT_GRID).assertIsDisplayed()
    }

    @Test
    fun datePickerDialog_showsOnDateClick() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_DATE_PICKER_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_DATE_PICKER_CANCEL).assertIsNotDisplayed()
    }

    @Test
    fun datePickerDialog_cancel_works() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_DATE_PICKER_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_DATE_PICKER_CANCEL).assertIsNotDisplayed()
    }

    @Test
    fun spotIsSelected_thenReserveButtonShows() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_RESERVE_BUTTON).assertIsNotDisplayed()
    }

    @Test
    fun expandSpots_toggleWorks() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_EXPAND_SPOTS).performClick()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_SPOT_GRID).assertIsNotDisplayed()
    }

    @Test
    fun selectSpotFromDropdown_showsReserveButton() {
        val repo = createRepository()
        composeTestRule.setContent {
            ViewParking(viewModel = ViewParkingViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_VIEW_DROPDOWN).performClick()
        composeTestRule.onNodeWithTag(TestTags.VIEW_PARKING_SPOT_DROPDOWN).assertIsDisplayed()
    }
}
