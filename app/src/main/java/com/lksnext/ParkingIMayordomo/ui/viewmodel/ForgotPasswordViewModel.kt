package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isCodeSent = MutableStateFlow(false)
    val isCodeSent: StateFlow<Boolean> = _isCodeSent.asStateFlow()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId.asStateFlow()

    fun onEmailChange(newEmail: String) { 
        _email.value = newEmail 
        _errorResId.value = null
    }
    
    fun onCodeChange(newCode: String) { 
        _code.value = newCode 
        _errorResId.value = null
    }

    fun sendCode() {
        if (_email.value.isBlank()) {
            _errorResId.value = R.string.error_enter_email
            return
        }
        if (!_email.value.endsWith("@lks.com")) {
            _errorResId.value = R.string.error_corporate_only
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            delay(1000)
            _isCodeSent.value = true
            _isLoading.value = false
        }
    }

    fun verifyCode(onSuccess: () -> Unit) {
        if (_code.value == "123456") {
            viewModelScope.launch {
                _isLoading.value = true
                delay(1000)
                _isVerified.value = true
                _isLoading.value = false
                onSuccess()
            }
        } else {
            _errorResId.value = R.string.error_invalid_code
        }
    }

    fun resetState() {
        _isCodeSent.value = false
        _code.value = ""
        _isVerified.value = false
        _errorResId.value = null
    }
}
