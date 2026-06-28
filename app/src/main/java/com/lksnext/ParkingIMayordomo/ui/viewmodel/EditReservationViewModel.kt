package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditReservationViewModel(private val repository: ParkingRepository) : ViewModel() {

    val vehicles = repository.vehicles
    val user = repository.user
    val reservations = repository.reservations

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun getReservation(id: String): Flow<Reservation?> {
        return repository.reservations.map { list -> list.find { it.id == id } }
    }

    fun updateReservation(
        id: String,
        date: Calendar,
        startTime: Calendar,
        endTime: Calendar,
        vehicleId: String,
        licensePlate: String?
    ) {
        viewModelScope.launch {
            repository.updateReservation(
                id = id,
                date = sdfDate.format(date.time),
                startTime = sdfTime.format(startTime.time),
                endTime = sdfTime.format(endTime.time),
                vehicleId = vehicleId,
                licensePlate = licensePlate
            )
        }
    }

    fun deleteReservation(id: String) {
        viewModelScope.launch {
            repository.deleteReservation(id)
        }
    }

    fun getValidationErrorResId(
        selectedDate: Calendar,
        startTime: Calendar,
        endTime: Calendar,
        allReservations: List<Reservation>,
        currentReservationId: String,
        currentSpotNumber: Int,
        currentUserId: String?,
        selectedVehicleId: String
    ): Int? {
        val now = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startCal = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, startTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val diff = endTime.timeInMillis - startTime.timeInMillis
        val hours = diff / (1000 * 60 * 60.0)

        val dateStr = sdfDate.format(selectedDate.time)
        val startStr = sdfTime.format(startTime.time)
        val endStr = sdfTime.format(endTime.time)

        val isSpotOccupiedByOthers = allReservations.any { r ->
            r.id != currentReservationId &&
            r.spotNumber == currentSpotNumber &&
            ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
        }

        val hasUserOtherReservation = allReservations.any { r ->
            r.id != currentReservationId &&
            r.userId == currentUserId &&
            ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
        }

        return when {
            startCal.before(now) -> R.string.error_start_before_now
            hours <= 0 -> R.string.error_end_after_start
            hours > 9 -> R.string.error_max_9_hours
            isSpotOccupiedByOthers -> R.string.error_spot_occupied_simple
            hasUserOtherReservation -> R.string.error_user_overlap
            selectedVehicleId.isBlank() -> R.string.error_select_vehicle
            else -> null
        }
    }
}
