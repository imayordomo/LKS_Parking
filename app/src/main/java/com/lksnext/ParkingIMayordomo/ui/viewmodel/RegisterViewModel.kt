package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: ParkingRepository) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId.asStateFlow()

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _errorResId.value = null
            try {
                repository.register(name, email, password)
                onSuccess()
            } catch (e: Exception) {
                _errorResId.value = when(e.message) {
                    "error_corporate_only" -> R.string.error_corporate_only
                    "error_passwords_dont_match" -> R.string.error_passwords_dont_match
                    "error_password_too_short" -> R.string.error_password_too_short
                    "error_password_complexity" -> R.string.error_password_complexity
                    "error_email_already_in_use" -> R.string.error_email_already_in_use
                    "error_invalid_email_format" -> R.string.error_invalid_email_format
                    else -> R.string.error_unknown
                }
            } finally {
                _loading.value = false
            }
        }
    }
}
