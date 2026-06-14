package com.lksnext.ParkingIMayordomo.data.repository

import com.lksnext.ParkingIMayordomo.data.AuthManager
import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParkingRepositoryImpl : ParkingRepository {
    private val _user = MutableStateFlow(AuthManager.user)
    override val user: StateFlow<User?> = _user.asStateFlow()

    private val _reservations = MutableStateFlow(AuthManager.reservations)
    override val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _vehicles = MutableStateFlow(AuthManager.vehicles)
    override val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _notifications = MutableStateFlow(AuthManager.notifications)
    override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _reports = MutableStateFlow(AuthManager.reports)
    override val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Observe changes from AuthManager which now uses real-time listeners
        scope.launch {
            kotlin.concurrent.fixedRateTimer("sync", false, 0L, 1000) {
                scope.launch { syncState() }
            }
        }
    }

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

    override suspend fun markAsRead(id: String) {
        AuthManager.markAsRead(id)
        syncState()
    }

    override suspend fun markAllAsRead() {
        AuthManager.markAllAsRead()
        syncState()
    }

    override suspend fun deleteNotification(id: String) {
        AuthManager.deleteNotification(id)
        syncState()
    }

    override suspend fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String?) {
        AuthManager.addReservation(spotNumber, date, startTime, endTime, vehicleId, licensePlate)
        syncState()
    }

    override suspend fun updateReservation(id: String, spotNumber: Int?, date: String?, startTime: String?, endTime: String?, vehicleId: String?, licensePlate: String?) {
        AuthManager.updateReservation(id, spotNumber, date, startTime, endTime, vehicleId, licensePlate)
        syncState()
    }

    override suspend fun deleteReservation(id: String) {
        AuthManager.deleteReservation(id)
        syncState()
    }

    override suspend fun addVehicle(type: VehicleType, licensePlate: String) {
        AuthManager.addVehicle(type, licensePlate)
        syncState()
    }

    override suspend fun removeVehicle(id: String) {
        AuthManager.removeVehicle(id)
        syncState()
    }

    override suspend fun updateProfile(name: String, profileImage: String?) {
        AuthManager.updateProfile(name, profileImage)
        syncState()
    }

    override suspend fun updateFcmToken(token: String) {
        AuthManager.updateFcmToken(token)
        syncState()
    }

    override suspend fun addReport(spotNumber: Int?, title: String, description: String) {
        AuthManager.addReport(spotNumber, title, description)
        syncState()
    }

    private fun syncState() {
        _user.value = AuthManager.user
        _reservations.value = AuthManager.reservations
        _vehicles.value = AuthManager.vehicles
        _notifications.value = AuthManager.notifications
        _reports.value = AuthManager.reports
    }
}
