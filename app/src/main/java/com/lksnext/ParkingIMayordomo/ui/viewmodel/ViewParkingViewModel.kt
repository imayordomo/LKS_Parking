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
    val user = repository.user

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getOccupiedSpots(selectedDate: Calendar): Flow<List<Int>> {
        return repository.reservations.map { reservations ->
            val dateStr = sdfDate.format(selectedDate.time)
            reservations.filter { it.date == dateStr }.map { it.spotNumber }.distinct()
        }
    }

    fun getUserSpots(selectedDate: Calendar): Flow<List<Int>> {
        return combine(repository.reservations, repository.user) { reservations, currentUser ->
            val dateStr = sdfDate.format(selectedDate.time)
            reservations.filter { it.date == dateStr && it.userId == currentUser?.id }
                .map { it.spotNumber }.distinct()
        }
    }
}
