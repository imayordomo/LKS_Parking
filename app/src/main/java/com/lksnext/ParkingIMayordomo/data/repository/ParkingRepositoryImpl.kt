package com.lksnext.ParkingIMayordomo.data.repository

import com.lksnext.ParkingIMayordomo.data.AuthManager
import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class ParkingRepositoryImpl : ParkingRepository {
    private val _user = MutableStateFlow<User?>(AuthManager.user)
    override val user: StateFlow<User?> = _user.asStateFlow()

    private val _reservations = MutableStateFlow<List<Reservation>>(AuthManager.reservations)
    override val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(AuthManager.vehicles)
    override val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(AuthManager.notifications)
    override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    override suspend fun login(email: String, password: String) {
        AuthManager.login(email, password)
        syncState()
    }

    override suspend fun register(name: String, email: String, password: String) {
        AuthManager.register(name, email, password)
        syncState()
    }

    override suspend fun resetPassword(email: String) {
        AuthManager.sendPasswordResetEmail(email)
    }

    override fun logout() {
        AuthManager.logout()
        syncState()
    }

    override fun markAsRead(id: String) {
        AuthManager.markAsRead(id)
        syncState()
    }

    override fun markAllAsRead() {
        AuthManager.markAllAsRead()
        syncState()
    }

    override fun deleteNotification(id: String) {
        AuthManager.deleteNotification(id)
        syncState()
    }

    override fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String?) {
        AuthManager.addReservation(spotNumber, date, startTime, endTime, vehicleId, licensePlate)
        syncState()
    }

    override fun updateReservation(id: String, spotNumber: Int?, date: String?, startTime: String?, endTime: String?, vehicleId: String?, licensePlate: String?) {
        AuthManager.updateReservation(id, spotNumber, date, startTime, endTime, vehicleId, licensePlate)
        syncState()
    }

    override fun deleteReservation(id: String) {
        AuthManager.deleteReservation(id)
        syncState()
    }

    override fun addVehicle(type: VehicleType, licensePlate: String) {
        AuthManager.addVehicle(type, licensePlate)
        syncState()
    }

    override fun removeVehicle(id: String) {
        AuthManager.removeVehicle(id)
        syncState()
    }

    override fun updateProfile(name: String, profileImage: String?) {
        AuthManager.updateProfile(name, profileImage)
        syncState()
    }

    private fun syncState() {
        _user.value = AuthManager.user
        _reservations.value = AuthManager.reservations
        _vehicles.value = AuthManager.vehicles
        _notifications.value = AuthManager.notifications
    }
}
