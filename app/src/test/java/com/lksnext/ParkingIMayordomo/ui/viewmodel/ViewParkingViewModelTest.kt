package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.data.model.Notification
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.*

class ViewParkingViewModelTest {

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: ViewParkingViewModel
    
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())
    private val allReservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())
    private val userFlow = MutableStateFlow<User?>(null)
    private val notificationsFlow = MutableStateFlow<List<Notification>>(emptyList())

    @Before
    fun setup() {
        repository = mockk()
        every { repository.reservations } returns reservationsFlow
        every { repository.allReservations } returns allReservationsFlow
        every { repository.user } returns userFlow
        every { repository.notifications } returns notificationsFlow
        
        viewModel = ViewParkingViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getOccupiedSpots should return distinct spot numbers for selected date`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 4, 10) // 2023-05-10
        
        allReservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 5, date = "2023-05-10", startTime = "08:00", endTime = "10:00"),
            Reservation(id = "2", spotNumber = 10, date = "2023-05-10", startTime = "09:00", endTime = "11:00"),
            Reservation(id = "3", spotNumber = 5, date = "2023-05-10", startTime = "12:00", endTime = "14:00"),
            Reservation(id = "4", spotNumber = 15, date = "2023-05-11", startTime = "08:00", endTime = "10:00")
        )

        val occupied = viewModel.getOccupiedSpots(date).first()
        
        // Non-today date uses partially/fully occupied logic
        // Since intervals [8-10] and [12-14] don't cover 00:00-23:59, they are PARTIALLY_OCCUPIED
        assertEquals(2, occupied.size)
        assertEquals(SpotOccupancyState.PARTIALLY_OCCUPIED, occupied[5])
        assertEquals(SpotOccupancyState.PARTIALLY_OCCUPIED, occupied[10])
    }

    @Test
    fun `getOccupiedSpots - Fully occupied branch`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 4, 10)
        
        // Cover entire day for spot 7
        allReservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 7, date = "2023-05-10", startTime = "00:00", endTime = "23:59")
        )

        val occupied = viewModel.getOccupiedSpots(date).first()
        assertEquals(SpotOccupancyState.FULLY_OCCUPIED, occupied[7])
    }
    
    @Test
    fun `getOccupiedSpots - Midnight crossing from previous day`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 4, 10) // 2023-05-10
        
        mockkObject(ParkingUtils)
        every { ParkingUtils.addDays("2023-05-10", -1) } returns "2023-05-09"
        every { ParkingUtils.isMidnightCrossing("22:00", "08:00") } returns true
        every { ParkingUtils.timeToMinutes("22:00") } returns 22 * 60
        every { ParkingUtils.timeToMinutes("08:00") } returns 8 * 60
        every { ParkingUtils.timeToMinutes("23:59") } returns 24 * 60 - 1
        
        // Spot 8: 00:00-08:00 from prev day, then 08:00-23:59 from current day
        allReservationsFlow.value = listOf(
            Reservation(id = "prev", spotNumber = 8, date = "2023-05-09", startTime = "22:00", endTime = "08:00"),
            Reservation(id = "curr", spotNumber = 8, date = "2023-05-10", startTime = "08:00", endTime = "23:59")
        )

        val occupied = viewModel.getOccupiedSpots(date).first()
        assertEquals(SpotOccupancyState.FULLY_OCCUPIED, occupied[8])
    }

    @Test
    fun `getUserSpots should return distinct spot numbers for current user and date`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 4, 10)
        
        val userId = "user1"
        userFlow.value = User(id = userId)
        
        reservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 5, date = "2023-05-10", userId = userId, startTime = "08:00", endTime = "10:00"),
            Reservation(id = "2", spotNumber = 10, date = "2023-05-10", userId = "other", startTime = "08:00", endTime = "10:00"),
            Reservation(id = "3", spotNumber = 5, date = "2023-05-10", userId = userId, startTime = "12:00", endTime = "14:00")
        )

        val userSpots = viewModel.getUserSpots(date).first()
        
        assertEquals(1, userSpots.size)
        assertEquals(5, userSpots[0])
    }

    @Test
    fun `getUserSpots should return empty list if user is null`() = runTest {
        val date = Calendar.getInstance()
        userFlow.value = null
        reservationsFlow.value = listOf(Reservation(id = "1", spotNumber = 5, date = "2023-01-01", userId = "some_id", startTime = "08:00", endTime = "10:00"))

        val userSpots = viewModel.getUserSpots(date).first()
        assertTrue(userSpots.isEmpty())
    }

    @Test
    fun `getCurrentReservations should return reservations for selected date`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 4, 10)

        allReservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 5, date = "2023-05-10", startTime = "09:00", endTime = "11:00"),
            Reservation(id = "2", spotNumber = 10, date = "2023-05-10", startTime = "14:00", endTime = "16:00"),
            Reservation(id = "3", spotNumber = 15, date = "2023-05-11", startTime = "09:00", endTime = "11:00")
        )

        val current = viewModel.getCurrentReservations(date).first()
        assertEquals(2, current.size)
    }

    @Test
    fun `getCurrentReservations should return empty for date with no reservations`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 5, 15)

        allReservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 5, date = "2023-05-10", startTime = "09:00", endTime = "11:00")
        )

        val current = viewModel.getCurrentReservations(date).first()
        assertTrue(current.isEmpty())
    }
}
