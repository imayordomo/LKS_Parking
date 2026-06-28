package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: ProfileViewModel

    private val userFlow = MutableStateFlow<User?>(null)
    private val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    private val reservationsFlow = MutableStateFlow<List<Reservation>>(emptyList())

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        every { repository.user } returns (userFlow as StateFlow<User?>)
        every { repository.vehicles } returns (vehiclesFlow as StateFlow<List<Vehicle>>)
        every { repository.reservations } returns (reservationsFlow as StateFlow<List<Reservation>>)

        viewModel = ProfileViewModel(repository)
    }

    @Test
    fun `viewModel should expose repository flows`() {
        val testUser = User(id = "u1")
        val testVehicles = listOf(Vehicle(id = "v1"))
        val testReservations = listOf(Reservation(id = "r1"))

        userFlow.value = testUser
        vehiclesFlow.value = testVehicles
        reservationsFlow.value = testReservations

        assertEquals(testUser, viewModel.user.value)
        assertEquals(testVehicles, viewModel.vehicles.value)
        assertEquals(testReservations, viewModel.reservations.value)
    }

    @Test
    fun `updateProfile should call repository`() = runTest {
        val name = "New Name"
        val image = "new_image_url"
        viewModel.updateProfile(name, image)
        coVerify { repository.updateProfile(name, image) }
    }

    @Test
    fun `addVehicle success should call onSuccess`() = runTest {
        val type = VehicleType.CAR
        val plate = "1234ABC"
        val onSuccess = mockk<() -> Unit>(relaxed = true)

        viewModel.addVehicle(type, plate, onSuccess)

        coVerify { repository.addVehicle(type, plate) }
        verify { onSuccess() }
        assertNull(viewModel.errorResId.value)
    }

    @Test
    fun `addVehicle failure with plate exists should set specific error`() = runTest {
        val type = VehicleType.CAR
        val plate = "1234ABC"
        coEvery { repository.addVehicle(any(), any()) } throws Exception("error_license_plate_exists")

        viewModel.addVehicle(type, plate, {})

        assertEquals(R.string.error_license_plate_exists, viewModel.errorResId.value)
    }

    @Test
    fun `addVehicle failure with unknown error should set unknown error`() = runTest {
        val type = VehicleType.CAR
        val plate = "1234ABC"
        coEvery { repository.addVehicle(any(), any()) } throws Exception("some_other_error")

        viewModel.addVehicle(type, plate, {})

        assertEquals(R.string.error_unknown, viewModel.errorResId.value)
    }

    @Test
    fun `removeVehicle should call repository`() = runTest {
        val vehicleId = "v1"
        viewModel.removeVehicle(vehicleId)
        coVerify { repository.removeVehicle(vehicleId) }
    }

    @Test
    fun `logout should call repository`() = runTest {
        viewModel.logout()
        verify { repository.logout() }
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { repository.addVehicle(any(), any()) } throws Exception("error_license_plate_exists")
        viewModel.addVehicle(VehicleType.CAR, "123", {})
        assertNotNull(viewModel.errorResId.value)

        viewModel.clearError()
        assertNull(viewModel.errorResId.value)
    }
}
