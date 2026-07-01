package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.Report
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ReportViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ReportScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.reports } returns MutableStateFlow(emptyList())
        return repo
    }

    @Test
    fun reportForm_isDisplayed() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_TYPE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REPORT_SPOT_NUMBER_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REPORT_DESCRIPTION_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REPORT_SEND_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REPORT_HISTORY_EXPAND).assertIsDisplayed()
    }

    @Test
    fun typeField_opensMenu() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_TYPE_FIELD).performClick()
        composeTestRule.onNodeWithTag(TestTags.REPORT_TYPE_MENU).assertIsDisplayed()
    }

    @Test
    fun spotNumberField_acceptsInput() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_SPOT_NUMBER_FIELD).performTextInput("5")
    }

    @Test
    fun descriptionField_acceptsInput() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_DESCRIPTION_FIELD).performTextInput("Test description")
    }

    @Test
    fun historyExpand_togglesHistorySection() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_HISTORY_EXPAND).performClick()
    }

    @Test
    fun sendButton_disabledWithEmptyFields() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_SEND_BUTTON).assertIsDisplayed()
    }

    @Test
    fun sendButton_enabledWithAllFields() {
        val vm = ReportViewModel(createRepository())
        vm.onReportTypeChange("Daño")
        vm.onDescriptionChange("Test description")
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_SEND_BUTTON).assertIsDisplayed()
    }

    @Test
    fun sendButton_click() {
        val repo = createRepository()
        val vm = ReportViewModel(repo)
        vm.onReportTypeChange("Daño")
        vm.onDescriptionChange("Test description")
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_SEND_BUTTON).performClick()
    }
}
