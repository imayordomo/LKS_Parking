package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class NewReservationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: NewReservationViewModel
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())
    private val allReservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())
    private val userFlow = MutableStateFlow<User?>(null)

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.reservations } returns (reservationsFlow as StateFlow<List<Reservation>>)
        every { repository.allReservations } returns (allReservationsFlow as StateFlow<List<Reservation>>)
        every { repository.user } returns (userFlow as StateFlow<User?>)
        viewModel = NewReservationViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `addReservation should call repository with formatted dates`() = runTest {
        val testDate = Calendar.getInstance().apply { set(2023, 4, 10) }
        val testStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0) }
        val testEnd = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }
        
        viewModel.addReservation(1, testDate, testStart, testEnd, "v1", "1234ABC")

        coVerify {
            repository.addReservation(
                spotNumber = 1,
                date = "2023-05-10",
                startTime = "10:00",
                endTime = "12:00",
                vehicleId = "v1",
                licensePlate = "1234ABC"
            )
        }
    }

    @Test
    fun `getOccupiedSpots should return early if times are null`() = runTest {
        val occupied = viewModel.getOccupiedSpots(Calendar.getInstance(), null, null).first()
        assertTrue(occupied.isEmpty())
    }

    @Test
    fun `getOccupiedSpots should return list of spot numbers when overlapping`() = runTest {
        val testDate = Calendar.getInstance()
        val testStart = Calendar.getInstance()
        val testEnd = Calendar.getInstance()
        
        val res = Reservation(id = "1", date = "2023-05-10", startTime = "09:00", endTime = "11:00", spotNumber = 5)
        allReservationsFlow.value = listOf(res)
        
        mockkObject(ParkingUtils)
        // Ensure we test the filter logic inside the map
        every { ParkingUtils.isTimeOverlapping(any(), any(), any(), any(), any(), any()) } returns true

        val occupied = viewModel.getOccupiedSpots(testDate, testStart, testEnd).first()
        assertEquals(listOf(5), occupied)
    }

    @Test
    fun `hasExistingUserReservation should return early if times are null`() = runTest {
        val hasOverlap = viewModel.hasExistingUserReservation(Calendar.getInstance(), null, null).first()
        assertFalse(hasOverlap)
    }

    @Test
    fun `hasExistingUserReservation should return true if user has overlapping reservation`() = runTest {
        val testDate = Calendar.getInstance()
        val testStart = Calendar.getInstance()
        val testEnd = Calendar.getInstance()
        
        userFlow.value = User(id = "user1")
        val res = Reservation(id = "1", userId = "user1", date = "2023-05-10")
        reservationsFlow.value = listOf(res)
        
        mockkObject(ParkingUtils)
        every { ParkingUtils.isTimeOverlapping(any(), any(), any(), any(), any(), any()) } returns true

        val hasOverlap = viewModel.hasExistingUserReservation(testDate, testStart, testEnd).first()
        assertTrue(hasOverlap)
    }

    @Test
    fun `hasExistingUserReservation should handle null currentUser`() = runTest {
        userFlow.value = null
        val res = Reservation(id = "1", userId = "user1", date = "2023-05-10")
        reservationsFlow.value = listOf(res)
        
        val hasOverlap = viewModel.hasExistingUserReservation(Calendar.getInstance(), Calendar.getInstance(), Calendar.getInstance()).first()
        assertFalse(hasOverlap)
    }

    @Test
    fun `getValidationErrorResId should return null if times are null`() {
        assertNull(viewModel.getValidationErrorResId(Calendar.getInstance(), null, null, 1, false, emptyList()))
    }

    @Test
    fun `getValidationErrorResId should return error if start is before now`() {
        val pastDate = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -1) }
        val testEnd = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }

        assertEquals(R.string.error_start_before_now, viewModel.getValidationErrorResId(pastDate, pastDate, testEnd, 1, false, emptyList()))
    }

    @Test
    fun `getValidationErrorResId should return error if end is before start`() {
        val future = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }

        assertEquals(R.string.error_end_after_start, viewModel.getValidationErrorResId(future, future, future, 1, false, emptyList()))
    }

    @Test
    fun `getValidationErrorResId should return error if more than 9 hours`() {
        val now = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        val end = Calendar.getInstance().apply { time = now.time; add(Calendar.HOUR_OF_DAY, 10) }

        assertEquals(R.string.error_max_9_hours, viewModel.getValidationErrorResId(now, now, end, 1, false, emptyList()))
    }

    @Test
    fun `getValidationErrorResId should return error if user has overlap`() {
        val now = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        val end = Calendar.getInstance().apply { time = now.time; add(Calendar.HOUR_OF_DAY, 2) }

        assertEquals(R.string.error_user_overlap, viewModel.getValidationErrorResId(now, now, end, 1, true, emptyList()))
    }

    @Test
    fun `getValidationErrorResId should return error if spot is occupied`() {
        val now = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        val end = Calendar.getInstance().apply { time = now.time; add(Calendar.HOUR_OF_DAY, 2) }

        assertEquals(R.string.error_spot_occupied_simple, viewModel.getValidationErrorResId(now, now, end, 5, false, listOf(5)))
    }

    @Test
    fun `getValidationErrorResId should return null if everything is valid`() {
        val now = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        val end = Calendar.getInstance().apply { time = now.time; add(Calendar.HOUR_OF_DAY, 2) }

        assertNull(viewModel.getValidationErrorResId(now, now, end, 5, false, emptyList()))
    }

    @Test
    fun `getValidationErrorResId should handle midnight crossing too long`() {
        val future = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 22); set(Calendar.MINUTE, 0) }
        val end = Calendar.getInstance().apply { time = future.time; add(Calendar.HOUR_OF_DAY, 10) }

        assertEquals(R.string.error_max_9_hours, viewModel.getValidationErrorResId(future, future, end, 1, false, emptyList()))
    }

    @Test
    fun `getValidationErrorResId should handle midnight crossing start before now`() {
        val pastDate = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -2) }
        val end = Calendar.getInstance().apply { time = pastDate.time; add(Calendar.HOUR_OF_DAY, 4) }

        assertEquals(R.string.error_start_before_now, viewModel.getValidationErrorResId(pastDate, pastDate, end, 1, false, emptyList()))
    }

    @Test
    fun `hasExistingUserReservation should handle midnight crossing`() = runTest {
        val testDate = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 22) }
        val testStart = Calendar.getInstance().apply { time = testDate.time }
        val testEnd = Calendar.getInstance().apply { time = testDate.time; add(Calendar.HOUR_OF_DAY, 3) }

        userFlow.value = User(id = "user1")
        val res = Reservation(id = "1", userId = "user1", date = "2023-05-10")
        reservationsFlow.value = listOf(res)

        mockkObject(ParkingUtils)
        every { ParkingUtils.isMidnightCrossing(any(), any()) } returns true
        every { ParkingUtils.addDays(any(), 1) } returns "2023-05-11"
        every { ParkingUtils.isTimeOverlapping(any(), any(), any(), any(), any(), any()) } returns true

        val hasOverlap = viewModel.hasExistingUserReservation(testDate, testStart, testEnd).first()
        assertTrue(hasOverlap)
    }

    @Test
    fun `getOccupiedSpots should handle midnight crossing`() = runTest {
        val testDate = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 22) }
        val testStart = Calendar.getInstance().apply { time = testDate.time }
        val testEnd = Calendar.getInstance().apply { time = testDate.time; add(Calendar.HOUR_OF_DAY, 3) }

        allReservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 5, date = "2023-05-10")
        )

        mockkObject(ParkingUtils)
        every { ParkingUtils.isMidnightCrossing(any(), any()) } returns true
        every { ParkingUtils.addDays(any(), 1) } returns "2023-05-11"
        every { ParkingUtils.isTimeOverlapping(any(), any(), any(), any(), any(), any()) } returns true

        val occupied = viewModel.getOccupiedSpots(testDate, testStart, testEnd).first()
        assertEquals(listOf(5), occupied)
    }
}
