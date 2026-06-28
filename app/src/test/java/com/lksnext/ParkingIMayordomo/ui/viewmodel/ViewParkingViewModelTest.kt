package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class ViewParkingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: ViewParkingViewModel

    private val userFlow = MutableStateFlow<User?>(null)
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.user } returns (userFlow as StateFlow<User?>)
        every { repository.reservations } returns (reservationsFlow as StateFlow<List<Reservation>>)
        
        viewModel = ViewParkingViewModel(repository)
    }

    @Test
    fun `getOccupiedSpots should return distinct spot numbers for selected date`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 4, 10) // 2023-05-10
        
        reservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 5, date = "2023-05-10"),
            Reservation(id = "2", spotNumber = 10, date = "2023-05-10"),
            Reservation(id = "3", spotNumber = 5, date = "2023-05-10"),
            Reservation(id = "4", spotNumber = 15, date = "2023-05-11")
        )

        val occupied = viewModel.getOccupiedSpots(date).first()
        
        assertEquals(2, occupied.size)
        assertEquals(listOf(5, 10), occupied.sorted())
    }

    @Test
    fun `getUserSpots should return distinct spot numbers for current user and date`() = runTest {
        val date = Calendar.getInstance()
        date.set(2023, 4, 10)
        
        val userId = "user1"
        userFlow.value = User(id = userId)
        
        reservationsFlow.value = listOf(
            Reservation(id = "1", spotNumber = 5, date = "2023-05-10", userId = userId),
            Reservation(id = "2", spotNumber = 10, date = "2023-05-10", userId = "other"),
            Reservation(id = "3", spotNumber = 5, date = "2023-05-10", userId = userId)
        )

        val userSpots = viewModel.getUserSpots(date).first()
        
        assertEquals(1, userSpots.size)
        assertEquals(5, userSpots[0])
    }

    @Test
    fun `getUserSpots should return empty list if user is null`() = runTest {
        val date = Calendar.getInstance()
        userFlow.value = null
        reservationsFlow.value = listOf(Reservation(id = "1", spotNumber = 5, date = "2023-01-01", userId = "some_id"))

        val userSpots = viewModel.getUserSpots(date).first()
        assertTrue(userSpots.isEmpty())
    }
}
