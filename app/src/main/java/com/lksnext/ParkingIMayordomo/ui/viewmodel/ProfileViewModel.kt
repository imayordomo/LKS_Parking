package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: ParkingRepository) : ViewModel() {
    val user = repository.user
    val vehicles = repository.vehicles
    val reservations = repository.reservations
    val notifications = repository.notifications

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId.asStateFlow()

    private val _isSavingVehicle = MutableStateFlow(false)
    val isSavingVehicle: StateFlow<Boolean> = _isSavingVehicle.asStateFlow()

    fun clearError() {
        _errorResId.value = null
    }

    fun updateProfile(name: String, profileImage: String? = null) {
        viewModelScope.launch {
            repository.updateProfile(name, profileImage)
        }
    }

    fun addVehicle(type: VehicleType, licensePlate: String, onSuccess: () -> Unit) {
        if (_isSavingVehicle.value) return
        viewModelScope.launch {
            _isSavingVehicle.value = true
            _errorResId.value = null
            try {
                repository.addVehicle(type, licensePlate)
                onSuccess()
            } catch (e: Exception) {
                _errorResId.value = when(e.message) {
                    "error_license_plate_exists" -> R.string.error_license_plate_exists
                    else -> R.string.error_unknown
                }
            } finally {
                _isSavingVehicle.value = false
            }
        }
    }

    fun removeVehicle(id: String) {
        viewModelScope.launch {
            repository.removeVehicle(id)
        }
    }

    fun logout() {
        repository.logout()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                repository.deleteAccount()
            } catch (e: Exception) {
                _errorResId.value = R.string.error_delete_account
            }
        }
    }
}
