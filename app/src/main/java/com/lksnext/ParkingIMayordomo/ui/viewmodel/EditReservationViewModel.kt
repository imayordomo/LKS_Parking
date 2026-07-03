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

data class ValidationParams(
    val selectedDate: Calendar,
    val startTime: Calendar,
    val endTime: Calendar,
    val allReservations: List<Reservation>,
    val currentReservationId: String,
    val currentSpotNumber: Int,
    val currentUserId: String?,
    val selectedVehicleId: String
)

class EditReservationViewModel(private val repository: ParkingRepository) : ViewModel() {

    val vehicles = repository.vehicles
    val user = repository.user
    val reservations = repository.reservations
    val allReservations = repository.allReservations
    val allReservationsReady = repository.allReservationsReady
    val notifications = repository.notifications

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

    private fun validateMidnightCrossing(
        params: ValidationParams,
        dateStr: String,
        startStr: String,
        endStr: String,
        startCal: Calendar,
        now: Calendar
    ): Int? {
        val totalMinutes = ParkingUtils.calculateDurationMinutes(startStr, endStr)
        if (totalMinutes > 9 * 60) return R.string.error_max_9_hours
        if (startCal.before(now)) return R.string.error_start_before_now

        val nextDateStr = ParkingUtils.addDays(dateStr, 1)
        val isSpotOccupiedByOthers = params.allReservations.any { r ->
            r.id != params.currentReservationId &&
            r.spotNumber == params.currentSpotNumber &&
            (ParkingUtils.isTimeOverlapping(dateStr, startStr, "23:59", r.date, r.startTime, r.endTime) ||
             ParkingUtils.isTimeOverlapping(nextDateStr, "00:00", endStr, r.date, r.startTime, r.endTime))
        }

        val hasUserOtherReservation = params.allReservations.any { r ->
            r.id != params.currentReservationId &&
            r.userId == params.currentUserId &&
            (ParkingUtils.isTimeOverlapping(dateStr, startStr, "23:59", r.date, r.startTime, r.endTime) ||
             ParkingUtils.isTimeOverlapping(nextDateStr, "00:00", endStr, r.date, r.startTime, r.endTime))
        }

        return when {
            isSpotOccupiedByOthers -> R.string.error_spot_occupied_simple
            hasUserOtherReservation -> R.string.error_user_overlap
            params.selectedVehicleId.isBlank() -> R.string.error_select_vehicle
            else -> null
        }
    }

    private fun validateNormal(
        params: ValidationParams,
        dateStr: String,
        startStr: String,
        endStr: String,
        startCal: Calendar,
        now: Calendar
    ): Int? {
        val diff = params.endTime.timeInMillis - params.startTime.timeInMillis
        val hours = diff / (1000 * 60 * 60.0)

        val isSpotOccupiedByOthers = params.allReservations.any { r ->
            r.id != params.currentReservationId &&
            r.spotNumber == params.currentSpotNumber &&
            ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
        }

        val hasUserOtherReservation = params.allReservations.any { r ->
            r.id != params.currentReservationId &&
            r.userId == params.currentUserId &&
            ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
        }

        return when {
            startCal.before(now) -> R.string.error_start_before_now
            hours <= 0 -> R.string.error_end_after_start
            hours > 9 -> R.string.error_max_9_hours
            isSpotOccupiedByOthers -> R.string.error_spot_occupied_simple
            hasUserOtherReservation -> R.string.error_user_overlap
            params.selectedVehicleId.isBlank() -> R.string.error_select_vehicle
            else -> null
        }
    }

    fun getValidationErrorResId(params: ValidationParams): Int? {
        val now = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startCal = Calendar.getInstance().apply {
            time = params.selectedDate.time
            set(Calendar.HOUR_OF_DAY, params.startTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, params.startTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dateStr = sdfDate.format(params.selectedDate.time)
        val startStr = sdfTime.format(params.startTime.time)
        val endStr = sdfTime.format(params.endTime.time)
        val crossesMidnight = ParkingUtils.isMidnightCrossing(startStr, endStr)

        return if (crossesMidnight) {
            validateMidnightCrossing(params, dateStr, startStr, endStr, startCal, now)
        } else {
            validateNormal(params, dateStr, startStr, endStr, startCal, now)
        }
    }
}
