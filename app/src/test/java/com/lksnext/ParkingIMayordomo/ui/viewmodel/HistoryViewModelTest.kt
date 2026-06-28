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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: HistoryViewModel

    private val userFlow = MutableStateFlow<User?>(null)
    private val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.user } returns (userFlow as StateFlow<User?>)
        every { repository.vehicles } returns (vehiclesFlow as StateFlow<List<Vehicle>>)
        every { repository.reservations } returns (reservationsFlow as StateFlow<List<Reservation>>)
        
        viewModel = HistoryViewModel(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setStatusFilter should update filter value`() {
        viewModel.setStatusFilter("past")
        assertEquals("past", viewModel.statusFilter.value)
    }

    @Test
    fun `setStartDate and setEndDate should update values`() {
        viewModel.setStartDate("2023-01-01")
        viewModel.setEndDate("2023-12-31")
        assertEquals("2023-01-01", viewModel.startDateText.value)
        assertEquals("2023-12-31", viewModel.endDateText.value)
    }

    @Test
    fun `clearFilters should reset all filters`() {
        viewModel.setStatusFilter("future")
        viewModel.setStartDate("2023-01-01")
        viewModel.setEndDate("2023-01-01")
        viewModel.clearFilters()
        
        assertEquals("all", viewModel.statusFilter.value)
        assertEquals("", viewModel.startDateText.value)
        assertEquals("", viewModel.endDateText.value)
    }

    @Test
    fun `viewModel should expose repository vehicles flow`() {
        val testVehicles = listOf(Vehicle(id = "v1", licensePlate = "ABC-123"))
        vehiclesFlow.value = testVehicles

        assertEquals(testVehicles, viewModel.vehicles.value)
    }

    @Test
    fun `filteredReservations logic branches`() = runTest {
        mockkObject(ParkingUtils)
        every { ParkingUtils.formatDate(any()) } returns "2023-05-10"
        every { ParkingUtils.formatTime(any()) } returns "12:00"

        val userId = "user1"
        userFlow.value = User(id = userId)
        
        val reservations = listOf(
            Reservation(id = "past", userId = userId, date = "2023-05-10", endTime = "11:59"),
            Reservation(id = "future", userId = userId, date = "2023-05-11", startTime = "10:00", endTime = "11:00"),
            Reservation(id = "other", userId = "user2", date = "2023-05-11")
        )
        reservationsFlow.value = reservations

        viewModel.filteredReservations.test {
            // StateFlow emits the current value immediately upon collection.
            // With UnconfinedTestDispatcher and the data already set, this should be the filtered list.
            var item = awaitItem()
            
            // In case combine hasn't finished and we get the initial emptyList() from stateIn
            if (item.isEmpty()) {
                item = awaitItem()
            }
            
            assertEquals(2, item.size)
            // Sorted descending by date
            assertEquals("future", item[0].id)
            assertEquals("past", item[1].id)

            // 1. Status: past
            viewModel.setStatusFilter("past")
            assertEquals("past", awaitItem()[0].id)

            // 2. Status: future
            viewModel.setStatusFilter("future")
            assertEquals("future", awaitItem()[0].id)

            // 3. Reset and test date filters
            viewModel.setStatusFilter("all")
            awaitItem()
            
            viewModel.setStartDate("2023-05-11")
            assertEquals("future", awaitItem()[0].id)

            viewModel.setStartDate("")
            awaitItem()
            
            viewModel.setEndDate("2023-05-10")
            assertEquals("past", awaitItem()[0].id)
            
            // Ensure no more items are expected
            expectNoEvents()
        }
    }
}
