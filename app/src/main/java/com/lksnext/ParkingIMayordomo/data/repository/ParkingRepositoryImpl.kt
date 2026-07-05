package com.lksnext.ParkingIMayordomo.data.repository

import com.lksnext.ParkingIMayordomo.data.AuthManager
import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.flow.StateFlow

class ParkingRepositoryImpl : ParkingRepository {
    override val user: StateFlow<User?> = AuthManager.user
    override val reservations: StateFlow<List<Reservation>> = AuthManager.reservations
    override val allReservations: StateFlow<List<Reservation>>
        get() {
            AuthManager.startAllReservationsListener()
            return AuthManager.allReservations
        }
    override val allReservationsReady: StateFlow<Boolean>
        get() {
            AuthManager.startAllReservationsListener()
            return AuthManager.allReservationsReady
        }
    override val vehicles: StateFlow<List<Vehicle>?> = AuthManager.vehicles
    override val notifications: StateFlow<List<Notification>> = AuthManager.notifications
    override val reports: StateFlow<List<Report>> = AuthManager.reports

    override suspend fun login(email: String, password: String) {
        AuthManager.login(email, password)
    }

    override suspend fun register(name: String, email: String, password: String) {
        AuthManager.register(name, email, password)
    }

    override suspend fun resetPassword(email: String) {
        AuthManager.sendPasswordResetEmail(email)
    }

    override fun logout() {
        AuthManager.logout()
    }

    override suspend fun markAsRead(id: String) {
        AuthManager.markAsRead(id)
    }

    override suspend fun markAllAsRead() {
        AuthManager.markAllAsRead()
    }

    override suspend fun deleteNotification(id: String) {
        AuthManager.deleteNotification(id)
    }

    override suspend fun deleteAllNotifications() {
        AuthManager.deleteAllNotifications()
    }

    override suspend fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String?) {
        AuthManager.addReservation(spotNumber, date, startTime, endTime, vehicleId, licensePlate)
    }

    override suspend fun updateReservation(id: String, spotNumber: Int?, date: String?, startTime: String?, endTime: String?, vehicleId: String?, licensePlate: String?) {
        AuthManager.updateReservation(id, spotNumber, date, startTime, endTime, vehicleId, licensePlate)
    }

    override suspend fun deleteReservation(id: String) {
        AuthManager.deleteReservation(id)
    }

    override suspend fun addVehicle(type: VehicleType, licensePlate: String) {
        AuthManager.addVehicle(type, licensePlate)
    }

    override suspend fun removeVehicle(id: String) {
        AuthManager.removeVehicle(id)
    }

    override suspend fun updateProfile(name: String, profileImage: String?) {
        AuthManager.updateProfile(name, profileImage)
    }

    override suspend fun updateFcmToken(token: String) {
        AuthManager.updateFcmToken(token)
    }

    override suspend fun addReport(spotNumber: Int?, title: String, description: String) {
        AuthManager.addReport(spotNumber, title, description)
    }

    override suspend fun deleteAccount() {
        AuthManager.deleteAccount()
    }
}
