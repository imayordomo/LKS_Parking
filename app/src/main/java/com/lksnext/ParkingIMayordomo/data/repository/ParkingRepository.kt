package com.lksnext.ParkingIMayordomo.data.repository

import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.flow.StateFlow

interface ParkingRepository {
    val user: StateFlow<User?>
    val reservations: StateFlow<List<Reservation>>
    val vehicles: StateFlow<List<Vehicle>>
    val notifications: StateFlow<List<Notification>>

    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    fun logout()
    
    fun markAsRead(id: String)
    fun markAllAsRead()
    fun deleteNotification(id: String)
    
    fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String? = null)
    fun updateReservation(id: String, spotNumber: Int? = null, date: String? = null, startTime: String? = null, endTime: String? = null, vehicleId: String? = null, licensePlate: String? = null)
    fun deleteReservation(id: String)
    
    fun addVehicle(type: VehicleType, licensePlate: String)
    fun removeVehicle(id: String)
    fun updateProfile(name: String, profileImage: String? = null)
}
