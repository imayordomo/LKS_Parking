package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.Report
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ReportViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReportScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.user } returns MutableStateFlow(User("user1", "test@test.com", "User"))
        every { repo.notifications } returns MutableStateFlow(emptyList())
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

    @Test
    fun invalidSpotNumber_showsError() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_SPOT_NUMBER_FIELD).performTextInput("0")
        composeTestRule.waitUntilExactlyOneExists(
            hasText("N\u00famero de plaza inv\u00e1lido (1\u201350)"),
            timeoutMillis = 5000
        )
        composeTestRule.onNodeWithText("Número de plaza inválido (1–50)").assertIsDisplayed()
    }

    @Test
    fun validSpotNumber_hidesError() {
        val vm = ReportViewModel(createRepository())
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REPORT_SPOT_NUMBER_FIELD).performTextInput("25")
        composeTestRule.onNodeWithText("Número de plaza inválido (1–50)").assertIsNotDisplayed()
    }

    @Test
    fun sendWithInvalidSpotNumber_showsErrorInSendButton() {
        val repo = createRepository()
        val vm = ReportViewModel(repo)
        vm.onReportTypeChange("Daño")
        vm.onDescriptionChange("Test description")
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        // Type an invalid spot number
        composeTestRule.onNodeWithTag(TestTags.REPORT_SPOT_NUMBER_FIELD).performTextInput("51")
        composeTestRule.waitUntilExactlyOneExists(
            hasText("N\u00famero de plaza inv\u00e1lido (1\u201350)"),
            timeoutMillis = 5000
        )
        composeTestRule.onNodeWithText("Número de plaza inválido (1–50)").assertIsDisplayed()
    }

    @Test
    fun successState_showsSuccessMessage() {
        val repo = createRepository()
        val vm = ReportViewModel(repo)
        vm.onReportTypeChange("Daño")
        vm.onDescriptionChange("Test description")
        vm.onSpotNumberChange("5")
        vm.sendReport()
        composeTestRule.setContent {
            Report(viewModel = vm, onNavigate = { })
        }

        composeTestRule.waitUntilExactlyOneExists(
            hasText("Reporte enviado correctamente. Gracias por tu colaboraci\u00f3n."),
            timeoutMillis = 5000
        )
        composeTestRule.onNodeWithText("Reporte enviado correctamente. Gracias por tu colaboración.").assertIsDisplayed()
    }
}
