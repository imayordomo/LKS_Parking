package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Report
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: ReportViewModel
    private val reportsFlow = MutableStateFlow<List<Report>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.reports } returns (reportsFlow as StateFlow<List<Report>>)
        viewModel = ReportViewModel(repository)
    }

    @Test
    fun `viewModel should expose repository reports flow`() {
        val testReports = listOf(Report(title = "Test"))
        reportsFlow.value = testReports

        assertEquals(testReports, viewModel.reports.value)
    }

    @Test
    fun `onReportTypeChange should update state`() {
        viewModel.onReportTypeChange("Damage")
        assertEquals("Damage", viewModel.reportType.value)
        assertNull(viewModel.errorResId.value)
    }

    @Test
    fun `onSpotNumberChange should filter non-digits and limit length`() {
        viewModel.onSpotNumberChange("12")
        assertEquals("12", viewModel.spotNumber.value)

        viewModel.onSpotNumberChange("123")
        assertEquals("12", viewModel.spotNumber.value)

        viewModel.onSpotNumberChange("1a")
        assertEquals("1", viewModel.spotNumber.value)
    }

    @Test
    fun `onDescriptionChange should update state`() {
        viewModel.onDescriptionChange("Fixed")
        assertEquals("Fixed", viewModel.description.value)
        assertNull(viewModel.errorResId.value)
    }

    @Test
    fun `isSpotNumberValid branches`() {
        viewModel.onSpotNumberChange("")
        assertTrue(viewModel.isSpotNumberValid())

        viewModel.onSpotNumberChange("25")
        assertTrue(viewModel.isSpotNumberValid())

        viewModel.onSpotNumberChange("0")
        assertFalse(viewModel.isSpotNumberValid())

        viewModel.onSpotNumberChange("51")
        assertFalse(viewModel.isSpotNumberValid())
    }

    @Test
    fun `sendReport validations`() {
        // Missing type
        viewModel.onDescriptionChange("Desc")
        viewModel.sendReport()
        assertEquals(R.string.error_required_fields, viewModel.errorResId.value)

        // Missing desc
        viewModel.onReportTypeChange("Type")
        viewModel.onDescriptionChange("")
        viewModel.sendReport()
        assertEquals(R.string.error_required_fields, viewModel.errorResId.value)

        // Invalid spot
        viewModel.onDescriptionChange("Desc")
        viewModel.onSpotNumberChange("99")
        viewModel.sendReport()
        assertEquals(R.string.error_invalid_spot_number, viewModel.errorResId.value)
    }

    @Test
    fun `sendReport success should call repository and reset state`() = runTest {
        viewModel.onReportTypeChange("Type")
        viewModel.onDescriptionChange("Desc")
        viewModel.onSpotNumberChange("10")

        viewModel.sendReport()

        coVerify { repository.addReport(10, "Type", "Desc") }
        assertTrue(viewModel.success.value)
        assertEquals("", viewModel.reportType.value)
        assertEquals("", viewModel.spotNumber.value)
        assertEquals("", viewModel.description.value)
        
        advanceTimeBy(3001)
        assertFalse(viewModel.success.value)
    }

    @Test
    fun `sendReport with empty spot success`() = runTest {
        viewModel.onReportTypeChange("Type")
        viewModel.onDescriptionChange("Desc")
        viewModel.onSpotNumberChange("")

        viewModel.sendReport()

        coVerify { repository.addReport(null, "Type", "Desc") }
    }

    @Test
    fun `sendReport failure should set error`() = runTest {
        viewModel.onReportTypeChange("Type")
        viewModel.onDescriptionChange("Desc")
        coEvery { repository.addReport(any(), any(), any()) } throws Exception()

        viewModel.sendReport()

        assertEquals(R.string.error_unknown, viewModel.errorResId.value)
        assertFalse(viewModel.success.value)
        assertFalse(viewModel.loading.value)
    }
}
