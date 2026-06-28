package com.lksnext.ParkingIMayordomo.ui.viewmodel

import app.cash.turbine.test
import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: DashboardViewModel

    private val userFlow = MutableStateFlow<User?>(null)
    private val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.user } returns (userFlow as StateFlow<User?>)
        every { repository.vehicles } returns (vehiclesFlow as StateFlow<List<Vehicle>>)
        every { repository.reservations } returns (reservationsFlow as StateFlow<List<Reservation>>)

        viewModel = DashboardViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `viewModel should expose repository user and vehicles flows`() {
        val testUser = User(id = "u1", email = "test@test.com")
        val testVehicles = listOf(Vehicle(id = "v1", licensePlate = "ABC"))

        userFlow.value = testUser
        vehiclesFlow.value = testVehicles

        assertEquals(testUser, viewModel.user.value)
        assertEquals(testVehicles, viewModel.vehicles.value)
    }

    @Test
    fun `deleteReservation should delegate to repository`() = runTest {
        viewModel.deleteReservation("res123")
        coVerify { repository.deleteReservation("res123") }
    }

    @Test
    fun `userReservations filtering branches`() = runTest {
        mockkObject(ParkingUtils)
        every { ParkingUtils.formatDate(any()) } returns "2023-01-01"
        every { ParkingUtils.formatTime(any()) } returns "12:00"

        val userId = "current_user"
        userFlow.value = User(id = userId)

        val reservations = listOf(
            // Branch: Other user (filtered out)
            Reservation(id = "other", userId = "someone_else", date = "2023-01-02"),
            // Branch: Past date (filtered out)
            Reservation(id = "past_day", userId = userId, date = "2022-12-31"),
            // Branch: Same day, past time (filtered out)
            Reservation(id = "past_today", userId = userId, date = "2023-01-01", endTime = "11:59"),
            // Branch: Same day, future time (kept)
            Reservation(id = "future_today", userId = userId, date = "2023-01-01", startTime = "13:00", endTime = "14:00"),
            // Branch: Future date (kept)
            Reservation(id = "future_day", userId = userId, date = "2023-01-02", startTime = "09:00", endTime = "10:00")
        )
        reservationsFlow.value = reservations

        viewModel.userReservations.test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertEquals("future_today", list[0].id)
            assertEquals("future_day", list[1].id)
        }
        
        // Branch: currentUser is null
        userFlow.value = null
        viewModel.userReservations.test {
            assertTrue(awaitItem().isEmpty())
        }
    }
}
