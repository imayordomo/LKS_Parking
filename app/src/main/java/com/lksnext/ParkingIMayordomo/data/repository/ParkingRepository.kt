package com.lksnext.ParkingIMayordomo.data.repository

import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.flow.StateFlow

interface ParkingRepository {
    val user: StateFlow<User?>
    val reservations: StateFlow<List<Reservation>>
    val allReservations: StateFlow<List<Reservation>>
    val allReservationsReady: StateFlow<Boolean>
    val vehicles: StateFlow<List<Vehicle>?>
    val notifications: StateFlow<List<Notification>>
    val reports: StateFlow<List<Report>>

    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    suspend fun resetPassword(email: String)
    fun logout()
    
    suspend fun markAsRead(id: String)
    suspend fun markAllAsRead()
    suspend fun deleteNotification(id: String)
    suspend fun deleteAllNotifications()
    
    suspend fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String? = null)
    suspend fun updateReservation(id: String, spotNumber: Int? = null, date: String? = null, startTime: String? = null, endTime: String? = null, vehicleId: String? = null, licensePlate: String? = null)
    suspend fun deleteReservation(id: String)
    
    suspend fun addVehicle(type: VehicleType, licensePlate: String)
    suspend fun removeVehicle(id: String)
    suspend fun updateProfile(name: String, profileImage: String? = null)
    suspend fun updateFcmToken(token: String)

    suspend fun addReport(spotNumber: Int?, title: String, description: String)
    suspend fun deleteAccount()
}
