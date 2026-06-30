package com.lksnext.ParkingIMayordomo.ui.viewmodel

import app.cash.turbine.test
import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import java.util.Date
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
import org.junit.Assert.assertTrue
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
    private val vehiclesFlow = MutableStateFlow<List<Vehicle>?>(emptyList())
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.user } returns (userFlow as StateFlow<User?>)
        every { repository.vehicles } returns (vehiclesFlow as StateFlow<List<Vehicle>?>)
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
    fun `generateCsvContent should return correct csv format including empty license plate`() = runTest {
        val userId = "user1"
        userFlow.value = User(id = userId)
        val reservations = listOf(
            Reservation(id = "1", userId = userId, spotNumber = 10, date = "2023-05-10", startTime = "10:00", endTime = "11:00", licensePlate = "1234ABC"),
            Reservation(id = "2", userId = userId, spotNumber = 11, date = "2023-05-11", startTime = "12:00", endTime = "13:00", licensePlate = null)
        )
        reservationsFlow.value = reservations
        
        viewModel.filteredReservations.test {
            var item = awaitItem()
            if (item.isEmpty()) item = awaitItem()
            
            val headers = listOf("Spot", "Date", "Start", "End", "Plate")
            val csv = viewModel.generateCsvContent(headers)
            
            val lines = csv.trim().split("\n")
            assertEquals(3, lines.size)
            assertEquals("Spot,Date,Start,End,Plate", lines[0])
            assertEquals("#11,2023-05-11,12:00,13:00,", lines[1])
            assertEquals("#10,2023-05-10,10:00,11:00,1234ABC", lines[2])
        }
    }

    @Test
    fun `filteredReservations should be empty when user is null`() = runTest {
        userFlow.value = null
        reservationsFlow.value = listOf(Reservation(id = "1", userId = "user1"))
        
        viewModel.filteredReservations.test {
            assertEquals(0, awaitItem().size)
        }
    }

    private fun applyFilter(
        reservations: List<Reservation>,
        currentUser: User?,
        status: String,
        start: String,
        end: String
    ): List<Reservation> {
        val now = Date()
        val todayStr = ParkingUtils.formatDate(now)
        val currentTimeStr = ParkingUtils.formatTime(now)

        return reservations
            .filter { it.userId == currentUser?.id }
            .filter { res ->
                val isPast = res.date < todayStr || (res.date == todayStr && res.endTime < currentTimeStr)

                val matchesStatus = when (status) {
                    "past" -> isPast
                    "future" -> !isPast
                    else -> true
                }

                val matchesDate = (start.isEmpty() || res.date >= start) &&
                        (end.isEmpty() || res.date <= end)

                matchesStatus && matchesDate
            }
            .sortedWith(compareByDescending<Reservation> { it.date }.thenByDescending { it.startTime })
    }

    @Test
    fun `filteredReservations logic branches`() {
        val userId = "user1"
        val todayStr = ParkingUtils.formatDate(Date())
        val yesterday = ParkingUtils.addDays(todayStr, -1)
        val tomorrow = ParkingUtils.addDays(todayStr, 1)

        val user = User(id = userId)
        val reservations = listOf(
            Reservation(id = "past_yesterday", userId = userId, date = yesterday, endTime = "23:59"),
            Reservation(id = "past_today_end_before", userId = userId, date = todayStr, endTime = "00:00"),
            Reservation(id = "active_today", userId = userId, date = todayStr, startTime = "00:00", endTime = "23:59"),
            Reservation(id = "future_tomorrow", userId = userId, date = tomorrow, startTime = "10:00", endTime = "11:00"),
            Reservation(id = "other", userId = "user2", date = tomorrow)
        )

        val all = applyFilter(reservations, user, "all", "", "")
        assertEquals(4, all.size)

        val past = applyFilter(reservations, user, "past", "", "")
        assertEquals(2, past.size)
        assertTrue(past.any { it.id == "past_yesterday" })
        assertTrue(past.any { it.id == "past_today_end_before" })

        val future = applyFilter(reservations, user, "future", "", "")
        assertEquals(2, future.size)
        assertTrue(future.any { it.id == "active_today" })
        assertTrue(future.any { it.id == "future_tomorrow" })

        val startTomorrow = applyFilter(reservations, user, "all", tomorrow, "")
        assertEquals(1, startTomorrow.size)

        val endTomorrow = applyFilter(reservations, user, "all", tomorrow, tomorrow)
        assertEquals(1, endTomorrow.size)

        val startDayAfter = applyFilter(reservations, user, "all", ParkingUtils.addDays(tomorrow, 1), "")
        assertEquals(0, startDayAfter.size)

        val noDateFilter = applyFilter(reservations, user, "all", "", "")
        assertEquals(4, noDateFilter.size)

        val endBeforeYesterday = applyFilter(reservations, user, "all", "", ParkingUtils.addDays(yesterday, -1))
        assertEquals(0, endBeforeYesterday.size)
    }

    @Test
    fun `filteredReservations stateflow updates when filters change`() = runTest {
        val userId = "user1"
        val todayStr = ParkingUtils.formatDate(Date())
        val yesterday = ParkingUtils.addDays(todayStr, -1)
        val tomorrow = ParkingUtils.addDays(todayStr, 1)

        userFlow.value = User(id = userId)
        reservationsFlow.value = listOf(
            Reservation(id = "past_yesterday", userId = userId, date = yesterday, endTime = "23:59"),
            Reservation(id = "past_today_end_before", userId = userId, date = todayStr, endTime = "00:00"),
            Reservation(id = "active_today", userId = userId, date = todayStr, startTime = "00:00", endTime = "23:59"),
            Reservation(id = "future_tomorrow", userId = userId, date = tomorrow, startTime = "10:00", endTime = "11:00"),
            Reservation(id = "other", userId = "user2", date = tomorrow)
        )

        viewModel.filteredReservations.test {
            var item = awaitItem()
            if (item.isEmpty()) item = awaitItem()
            assertEquals(4, item.size)

            viewModel.setStatusFilter("future")
            val futureItem = awaitItem()
            assertEquals(2, futureItem.size)

            expectNoEvents()
        }
    }
}