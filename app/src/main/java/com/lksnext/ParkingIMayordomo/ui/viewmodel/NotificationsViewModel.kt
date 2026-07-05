package com.lksnext.ParkingIMayordomo.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repository: ParkingRepository
) : ViewModel() {
    val notifications = repository.notifications
    val user = repository.user

    private val _deleteConfirmState = MutableStateFlow<DeleteConfirmState?>(null)
    val deleteConfirmState: StateFlow<DeleteConfirmState?> = _deleteConfirmState.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun shouldSkipConfirm(): Boolean =
        prefs?.getBoolean(KEY_DONT_ASK_AGAIN, false) == true

    fun setDontAskAgain(value: Boolean) {
        prefs?.edit()?.putBoolean(KEY_DONT_ASK_AGAIN, value)?.apply()
    }

    fun requestDeleteNotification(id: String) {
        if (shouldSkipConfirm()) {
            deleteNotification(id)
        } else {
            _deleteConfirmState.value = DeleteConfirmState.Single(id)
        }
    }

    fun requestDeleteAll() {
        if (shouldSkipConfirm()) {
            deleteAllNotifications()
        } else {
            _deleteConfirmState.value = DeleteConfirmState.All
        }
    }

    fun confirmDelete(dontAskAgain: Boolean) {
        val state = _deleteConfirmState.value ?: return
        setDontAskAgain(dontAskAgain)
        when (state) {
            is DeleteConfirmState.Single -> deleteNotification(state.id)
            is DeleteConfirmState.All -> deleteAllNotifications()
        }
        _deleteConfirmState.value = null
    }

    fun dismissDeleteConfirm() {
        _deleteConfirmState.value = null
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    private fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    private fun deleteAllNotifications() {
        viewModelScope.launch {
            repository.deleteAllNotifications()
        }
    }

    companion object {
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_DONT_ASK_AGAIN = "dont_ask_delete_again"
    }
}

sealed interface DeleteConfirmState {
    data class Single(val id: String) : DeleteConfirmState
    data object All : DeleteConfirmState
}
