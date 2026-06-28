package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class EditReservationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: EditReservationViewModel
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.reservations } returns (reservationsFlow as StateFlow<List<Reservation>>)
        viewModel = EditReservationViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getReservation should return matching reservation from flow`() = runTest {
        val res = Reservation(id = "target", spotNumber = 1, date = "2023-01-01")
        reservationsFlow.value = listOf(res)
        assertEquals(res, viewModel.getReservation("target").first())
        assertNull(viewModel.getReservation("missing").first())
    }

    @Test
    fun `updateReservation should call repository with correct parameters`() = runTest {
        val testDate = Calendar.getInstance().apply { set(2023, 10, 15) }
        val testStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 14); set(Calendar.MINUTE, 0) }
        val testEnd = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 16); set(Calendar.MINUTE, 0) }
        
        viewModel.updateReservation("id123", testDate, testStart, testEnd, "v555", "ABC-123")
        
        coVerify { 
            repository.updateReservation(
                id = "id123", 
                date = "2023-11-15", 
                startTime = "14:00", 
                endTime = "16:00",
                vehicleId = "v555", 
                licensePlate = "ABC-123"
            ) 
        }
    }

    @Test
    fun `deleteReservation should call repository`() = runTest {
        viewModel.deleteReservation("del_id")
        coVerify { repository.deleteReservation("del_id") }
    }

    private fun createNormalizedFutureDate(daysAhead: Int, hour: Int): Calendar {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysAhead)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun `getValidationErrorResId - Branch start before now`() {
        val pastDate = createNormalizedFutureDate(-1, 10)
        val futureTime = createNormalizedFutureDate(1, 10)
        val error = viewModel.getValidationErrorResId(pastDate, futureTime, futureTime, emptyList(), "id", 1, "u", "v")
        assertEquals(R.string.error_start_before_now, error)
    }

    @Test
    fun `getValidationErrorResId - Branch hours non-positive`() {
        val future = createNormalizedFutureDate(1, 10)
        val error = viewModel.getValidationErrorResId(future, future, future, emptyList(), "id", 1, "u", "v")
        assertEquals(R.string.error_end_after_start, error)
    }

    @Test
    fun `getValidationErrorResId - Branch hours too long`() {
        val future = createNormalizedFutureDate(1, 10)
        val lateEnd = createNormalizedFutureDate(1, 21)
        val error = viewModel.getValidationErrorResId(future, future, lateEnd, emptyList(), "id", 1, "u", "v")
        assertEquals(R.string.error_max_9_hours, error)
    }

    @Test
    fun `getValidationErrorResId - Branch spot occupied`() {
        mockkObject(ParkingUtils)
        every { ParkingUtils.isTimeOverlapping(any(), any(), any(), any(), any(), any()) } returns true
        val future = createNormalizedFutureDate(1, 10)
        val end = createNormalizedFutureDate(1, 11)
        val list = listOf(Reservation(id = "other", spotNumber = 1))
        
        val error = viewModel.getValidationErrorResId(future, future, end, list, "current", 1, "u", "v")
        assertEquals(R.string.error_spot_occupied_simple, error)
    }

    @Test
    fun `getValidationErrorResId - Branch user overlap`() {
        mockkObject(ParkingUtils)
        // Ensure spot occupied is false (spot mismatched) so it reaches user check
        val list = listOf(Reservation(id = "other", userId = "u1", spotNumber = 5))
        every { ParkingUtils.isTimeOverlapping(any(), any(), any(), any(), any(), any()) } returns true
        
        val future = createNormalizedFutureDate(1, 10)
        val end = createNormalizedFutureDate(1, 11)
        
        val error = viewModel.getValidationErrorResId(future, future, end, list, "current", 1, "u1", "v")
        assertEquals(R.string.error_user_overlap, error)
    }

    @Test
    fun `getValidationErrorResId - Branch select vehicle`() {
        val future = createNormalizedFutureDate(1, 10)
        val end = createNormalizedFutureDate(1, 11) // Hours > 0
        val error = viewModel.getValidationErrorResId(future, future, end, emptyList(), "id", 1, "u", "")
        assertEquals(R.string.error_select_vehicle, error)
    }

    @Test
    fun `getValidationErrorResId - Success`() {
        val future = createNormalizedFutureDate(1, 10)
        val end = createNormalizedFutureDate(1, 11)
        val error = viewModel.getValidationErrorResId(future, future, end, emptyList(), "id", 1, "u", "v1")
        assertNull(error)
    }
}
