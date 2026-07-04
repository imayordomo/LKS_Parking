package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.data.model.Notification
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
        
        // occupied is Map<Int, SpotOccupancyState>
        assertEquals(2, occupied.size)
        assertEquals(listOf(5, 10), occupied.keys.sorted())
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
