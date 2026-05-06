package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.SpotType
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class NewReservationViewModel(private val repository: ParkingRepository) : ViewModel() {

    val vehicles = repository.vehicles
    val reservations = repository.reservations
    val user = repository.user

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun addReservation(
        spotNumber: Int,
        date: Calendar,
        startTime: Calendar,
        endTime: Calendar,
        vehicleId: String,
        licensePlate: String
    ) {
        repository.addReservation(
            spotNumber = spotNumber,
            date = sdfDate.format(date.time),
            startTime = sdfTime.format(startTime.time),
            endTime = sdfTime.format(endTime.time),
            vehicleId = vehicleId,
            licensePlate = licensePlate
        )
    }

    fun getOccupiedSpots(selectedDate: Calendar, startTime: Calendar?, endTime: Calendar?): Flow<List<Int>> {
        if (startTime == null || endTime == null) return flowOf(emptyList())
        
        return repository.reservations.map { reservations ->
            val dateStr = sdfDate.format(selectedDate.time)
            val startStr = sdfTime.format(startTime.time)
            val endStr = sdfTime.format(endTime.time)
            
            reservations.filter { r ->
                ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
            }.map { it.spotNumber }
        }
    }
    
    fun hasExistingUserReservation(selectedDate: Calendar, startTime: Calendar?, endTime: Calendar?): Flow<Boolean> {
        if (startTime == null || endTime == null) return flowOf(false)
        
        return combine(repository.reservations, repository.user) { reservations, currentUser ->
            val dateStr = sdfDate.format(selectedDate.time)
            val startStr = sdfTime.format(startTime.time)
            val endStr = sdfTime.format(endTime.time)
            val currentUserId = currentUser?.id ?: ""
            
            reservations.any { r ->
                r.userId == currentUserId && 
                ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
            }
        }
    }

    fun getValidationErrorResId(
        selectedDate: Calendar,
        startTime: Calendar?,
        endTime: Calendar?,
        selectedSpot: Int?,
        hasExistingUserReservation: Boolean,
        occupiedSpots: List<Int>
    ): Int? {
        if (startTime == null || endTime == null) return null

        val now = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, startTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diff = endTime.timeInMillis - startTime.timeInMillis
        val hours = diff / (1000 * 60 * 60.0)

        return when {
            startCal.before(now) -> R.string.error_start_before_now
            hours <= 0 -> R.string.error_end_after_start
            hours > 9 -> R.string.error_max_9_hours
            hasExistingUserReservation -> R.string.error_user_overlap
            selectedSpot != null && occupiedSpots.contains(selectedSpot) -> R.string.error_spot_occupied_simple
            else -> null
        }
    }
}
