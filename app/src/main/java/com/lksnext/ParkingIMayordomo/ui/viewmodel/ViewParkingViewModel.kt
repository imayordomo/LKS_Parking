package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.SpotType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class ViewParkingViewModel(private val repository: ParkingRepository) : ViewModel() {

    val reservations = repository.reservations
    val allReservations = repository.allReservations
    val user = repository.user

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val todayStr: String get() = sdfDate.format(Date())
    private val nowTimeStr: String get() = sdfTime.format(Date())

    fun getOccupiedSpots(selectedDate: Calendar): Flow<List<Int>> {
        return repository.allReservations.map { reservations ->
            val dateStr = sdfDate.format(selectedDate.time)
            val isToday = dateStr == todayStr
            reservations.filter { r ->
                r.date == dateStr && (!isToday || isCurrentlyActive(r.startTime, r.endTime))
            }.map { it.spotNumber }.distinct()
        }
    }

    fun getUserSpots(selectedDate: Calendar): Flow<List<Int>> {
        return combine(repository.reservations, repository.user) { reservations, currentUser ->
            val dateStr = sdfDate.format(selectedDate.time)
            val isToday = dateStr == todayStr
            reservations.filter { r ->
                r.date == dateStr && r.userId == currentUser?.id &&
                (!isToday || isCurrentlyActive(r.startTime, r.endTime))
            }.map { it.spotNumber }.distinct()
        }
    }

    fun getCurrentReservations(selectedDate: Calendar): Flow<List<com.lksnext.ParkingIMayordomo.data.model.Reservation>> {
        return repository.allReservations.map { reservations ->
            val dateStr = sdfDate.format(selectedDate.time)
            reservations.filter { r -> r.date == dateStr }
        }
    }

    private fun isCurrentlyActive(startTime: String, endTime: String): Boolean {
        return nowTimeStr >= startTime && nowTimeStr < endTime
    }
}
