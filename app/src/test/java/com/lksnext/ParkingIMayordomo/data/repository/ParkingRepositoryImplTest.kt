package com.lksnext.ParkingIMayordomo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.lksnext.ParkingIMayordomo.data.AuthManager
import com.lksnext.ParkingIMayordomo.data.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParkingRepositoryImplTest {

    private lateinit var repository: ParkingRepositoryImpl
    private val _userFlow = MutableStateFlow<User?>(null)
    private val _reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())
    private val _allReservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())
    private val _vehiclesFlow = MutableStateFlow<List<Vehicle>?>(null)
    private val _notificationsFlow = MutableStateFlow<List<Notification>>(emptyList())
    private val _reportsFlow = MutableStateFlow<List<Report>>(emptyList())

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseMessaging::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)

        mockkObject(AuthManager)
        
        // Ensure properties return the mock flows
        every { AuthManager.user } returns _userFlow.asStateFlow()
        every { AuthManager.reservations } returns _reservationsFlow.asStateFlow()
        every { AuthManager.allReservations } returns _allReservationsFlow.asStateFlow()
        every { AuthManager.vehicles } returns _vehiclesFlow.asStateFlow()
        every { AuthManager.notifications } returns _notificationsFlow.asStateFlow()
        every { AuthManager.reports } returns _reportsFlow.asStateFlow()
        every { AuthManager.allReservationsReady } returns MutableStateFlow(true).asStateFlow()
        
        every { AuthManager.startAllReservationsListener() } just Runs

        repository = ParkingRepositoryImpl()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `user flow should delegate to AuthManager`() {
        val user = User(id = "test")
        _userFlow.value = user
        assertEquals(user, repository.user.value)
    }

    @Test
    fun `reservations flow should delegate to AuthManager`() {
        val list = listOf(Reservation(id = "1"))
        _reservationsFlow.value = list
        assertEquals(list, repository.reservations.value)
    }

    @Test
    fun `allReservations should start listener and return flow`() {
        val list = listOf(Reservation(id = "all"))
        _allReservationsFlow.value = list
        
        val result = repository.allReservations
        
        verify { AuthManager.startAllReservationsListener() }
        assertEquals(list, result.value)
    }

    @Test
    fun `vehicles flow should delegate to AuthManager`() {
        val list = listOf(Vehicle(id = "v1"))
        _vehiclesFlow.value = list
        assertEquals(list, repository.vehicles.value)
    }

    @Test
    fun `notifications flow should delegate to AuthManager`() {
        val list = listOf(Notification(id = "n1"))
        _notificationsFlow.value = list
        assertEquals(list, repository.notifications.value)
    }

    @Test
    fun `reports flow should delegate to AuthManager`() {
        val list = listOf(Report(id = "rep1"))
        _reportsFlow.value = list
        assertEquals(list, repository.reports.value)
    }

    @Test
    fun `login should call AuthManager`() = runTest {
        coEvery { AuthManager.login(any(), any()) } returns Unit
        repository.login("email", "pass")
        coVerify { AuthManager.login("email", "pass") }
    }

    @Test
    fun `register should call AuthManager`() = runTest {
        coEvery { AuthManager.register(any(), any(), any()) } returns Unit
        repository.register("name", "email", "pass")
        coVerify { AuthManager.register("name", "email", "pass") }
    }

    @Test
    fun `resetPassword should call AuthManager`() = runTest {
        coEvery { AuthManager.sendPasswordResetEmail(any()) } returns Unit
        repository.resetPassword("email")
        coVerify { AuthManager.sendPasswordResetEmail("email") }
    }

    @Test
    fun `logout should call AuthManager`() {
        every { AuthManager.logout() } just Runs
        repository.logout()
        verify { AuthManager.logout() }
    }

    @Test
    fun `markAsRead should call AuthManager`() = runTest {
        coEvery { AuthManager.markAsRead(any()) } returns Unit
        repository.markAsRead("id")
        coVerify { AuthManager.markAsRead("id") }
    }

    @Test
    fun `markAllAsRead should call AuthManager`() = runTest {
        coEvery { AuthManager.markAllAsRead() } returns Unit
        repository.markAllAsRead()
        coVerify { AuthManager.markAllAsRead() }
    }

    @Test
    fun `deleteNotification should call AuthManager`() = runTest {
        coEvery { AuthManager.deleteNotification(any()) } returns Unit
        repository.deleteNotification("id")
        coVerify { AuthManager.deleteNotification("id") }
    }

    @Test
    fun `deleteAllNotifications should call AuthManager`() = runTest {
        coEvery { AuthManager.deleteAllNotifications() } returns Unit
        repository.deleteAllNotifications()
        coVerify { AuthManager.deleteAllNotifications() }
    }

    @Test
    fun `addReservation should call AuthManager`() = runTest {
        coEvery { AuthManager.addReservation(any(), any(), any(), any(), any(), any()) } returns Unit
        repository.addReservation(1, "date", "start", "end", "vid", "plate")
        coVerify { AuthManager.addReservation(1, "date", "start", "end", "vid", "plate") }
    }

    @Test
    fun `updateReservation should call AuthManager`() = runTest {
        coEvery { AuthManager.updateReservation(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        repository.updateReservation("id", 2, "d", "s", "e", "v", "p")
        coVerify { AuthManager.updateReservation("id", 2, "d", "s", "e", "v", "p") }
    }

    @Test
    fun `deleteReservation should call AuthManager`() = runTest {
        coEvery { AuthManager.deleteReservation(any()) } returns Unit
        repository.deleteReservation("id")
        coVerify { AuthManager.deleteReservation("id") }
    }

    @Test
    fun `addVehicle should call AuthManager`() = runTest {
        coEvery { AuthManager.addVehicle(any(), any()) } returns Unit
        repository.addVehicle(VehicleType.CAR, "ABC")
        coVerify { AuthManager.addVehicle(VehicleType.CAR, "ABC") }
    }

    @Test
    fun `removeVehicle should call AuthManager`() = runTest {
        coEvery { AuthManager.removeVehicle(any()) } returns Unit
        repository.removeVehicle("id")
        coVerify { AuthManager.removeVehicle("id") }
    }

    @Test
    fun `updateProfile should call AuthManager`() = runTest {
        coEvery { AuthManager.updateProfile(any(), any(), any()) } returns Unit
        repository.updateProfile("name", "img", true)
        coVerify { AuthManager.updateProfile("name", "img", true) }
    }

    @Test
    fun `updateFcmToken should call AuthManager`() = runTest {
        coEvery { AuthManager.updateFcmToken(any()) } returns Unit
        repository.updateFcmToken("token")
        coVerify { AuthManager.updateFcmToken("token") }
    }

    @Test
    fun `addReport should call AuthManager`() = runTest {
        coEvery { AuthManager.addReport(any(), any(), any()) } returns Unit
        repository.addReport(1, "title", "desc")
        coVerify { AuthManager.addReport(1, "title", "desc") }
    }

    @Test
    fun `deleteAccount should call AuthManager`() = runTest {
        coEvery { AuthManager.deleteAccount() } returns Unit
        repository.deleteAccount()
        coVerify { AuthManager.deleteAccount() }
    }
}
