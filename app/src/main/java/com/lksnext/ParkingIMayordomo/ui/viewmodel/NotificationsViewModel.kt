package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository

class NotificationsViewModel(private val repository: ParkingRepository) : ViewModel() {
    val notifications = repository.notifications

    fun markAsRead(id: String) {
        repository.markAsRead(id)
    }

    fun markAllAsRead() {
        repository.markAllAsRead()
    }

    fun deleteNotification(id: String) {
        repository.deleteNotification(id)
    }
}
