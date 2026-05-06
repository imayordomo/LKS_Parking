package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: ParkingRepository) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            _errorResId.value = R.string.error_required_fields
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _errorResId.value = null
            try {
                repository.login(trimmedEmail, password)
                onSuccess()
            } catch (e: Exception) {
                _errorResId.value = when(e.message) {
                    "error_corporate_only" -> R.string.error_corporate_only
                    "error_invalid_credentials" -> R.string.error_invalid_credentials
                    "error_invalid_email_format" -> R.string.error_invalid_email_format
                    else -> R.string.error_unknown
                }
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorResId.value = null
    }
}
