package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository

class ProfileViewModel(private val repository: ParkingRepository) : ViewModel() {
    val user = repository.user
    val vehicles = repository.vehicles

    fun updateProfile(name: String, profileImage: String? = null) {
        repository.updateProfile(name, profileImage)
    }

    fun addVehicle(type: VehicleType, licensePlate: String) {
        repository.addVehicle(type, licensePlate)
    }

    fun removeVehicle(id: String) {
        repository.removeVehicle(id)
    }

    fun logout() {
        repository.logout()
    }
}
