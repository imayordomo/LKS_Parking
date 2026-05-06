package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel(private val repository: ParkingRepository) : ViewModel() {
    private val _reportType = MutableStateFlow("")
    val reportType: StateFlow<String> = _reportType.asStateFlow()

    private val _spotNumber = MutableStateFlow("")
    val spotNumber: StateFlow<String> = _spotNumber.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId.asStateFlow()

    fun onReportTypeChange(value: String) { 
        _reportType.value = value 
        _errorResId.value = null
    }
    
    fun onSpotNumberChange(value: String) {
        if (value.length <= 2) {
            _spotNumber.value = value.filter { it.isDigit() }
            _errorResId.value = null
        }
    }
    
    fun onDescriptionChange(value: String) { 
        _description.value = value 
        _errorResId.value = null
    }

    fun isSpotNumberValid(): Boolean {
        if (_spotNumber.value.isBlank()) return true
        val num = _spotNumber.value.toIntOrNull()
        return num != null && num in 1..50
    }

    fun sendReport() {
        if (_reportType.value.isBlank() || _description.value.isBlank()) {
            _errorResId.value = R.string.error_required_fields
            return
        }

        if (!isSpotNumberValid()) {
            _errorResId.value = R.string.error_invalid_spot_number
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _errorResId.value = null
            // Simular envío a repositorio (hasta que se implemente)
            delay(1500)
            _success.value = true
            _loading.value = false
            _reportType.value = ""
            _spotNumber.value = ""
            _description.value = ""
            delay(3000)
            _success.value = false
        }
    }
}
