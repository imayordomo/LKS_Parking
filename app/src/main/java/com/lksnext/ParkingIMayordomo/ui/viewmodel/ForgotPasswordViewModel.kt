package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val repository: ParkingRepository) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isEmailSent = MutableStateFlow(false)
    val isEmailSent: StateFlow<Boolean> = _isEmailSent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId.asStateFlow()

    fun onEmailChange(newEmail: String) { 
        _email.value = newEmail 
        _errorResId.value = null
    }

    fun sendResetEmail() {
        val trimmedEmail = _email.value.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            _errorResId.value = R.string.error_required_fields
            return
        }

        // Validación corporativa (@lksnext.com o tester)
        val isAuthorized = trimmedEmail.endsWith("@lksnext.com") || 
                          trimmedEmail == "imayordomo004@ikasle.ehu.eus"

        if (!isAuthorized) {
            _errorResId.value = R.string.error_corporate_only
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorResId.value = null
            try {
                repository.resetPassword(trimmedEmail)
                _isEmailSent.value = true
            } catch (e: Exception) {
                _errorResId.value = when(e.message) {
                    "error_corporate_only" -> R.string.error_corporate_only
                    else -> R.string.error_unknown
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetState() {
        _isEmailSent.value = false
        _errorResId.value = null
    }
}
