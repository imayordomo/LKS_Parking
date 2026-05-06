package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.*

class DashboardViewModel(private val repository: ParkingRepository) : ViewModel() {

    val user = repository.user
    val vehicles = repository.vehicles

    val userReservations: StateFlow<List<Reservation>> = combine(
        repository.reservations,
        repository.user,
        flow {
            while (true) {
                emit(Unit)
                delay(60000) // Emit every minute to refresh the current time
            }
        }
    ) { reservations, currentUser, _ ->
        val now = Date()
        val todayStr = ParkingUtils.formatDate(now)
        val currentTimeStr = ParkingUtils.formatTime(now)
        
        reservations
            .filter { it.userId == currentUser?.id }
            .filter { 
                it.date > todayStr || (it.date == todayStr && it.endTime > currentTimeStr)
            }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteReservation(id: String) {
        repository.deleteReservation(id)
    }
}
