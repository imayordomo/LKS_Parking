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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NewReservationViewModel(private val repository: ParkingRepository) : ViewModel() {

    val vehicles = repository.vehicles
    val reservations = repository.reservations
    val user = repository.user
    val allReservationsReady = repository.allReservationsReady
    val notifications = repository.notifications

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
        viewModelScope.launch {
            repository.addReservation(
                spotNumber = spotNumber,
                date = sdfDate.format(date.time),
                startTime = sdfTime.format(startTime.time),
                endTime = sdfTime.format(endTime.time),
                vehicleId = vehicleId,
                licensePlate = licensePlate
            )
        }
    }

    fun getOccupiedSpots(selectedDate: Calendar, startTime: Calendar?, endTime: Calendar?): Flow<List<Int>> {
        if (startTime == null || endTime == null) return flowOf(emptyList())

        return repository.allReservations.map { reservations ->
            val dateStr = sdfDate.format(selectedDate.time)
            val startStr = sdfTime.format(startTime.time)
            val endStr = sdfTime.format(endTime.time)

            val crossesMidnight = ParkingUtils.isMidnightCrossing(startStr, endStr)

            if (crossesMidnight) {
                val nextDateStr = ParkingUtils.addDays(dateStr, 1)
                val day1Spots = reservations.filter { r ->
                    ParkingUtils.isTimeOverlapping(dateStr, startStr, "23:59", r.date, r.startTime, r.endTime)
                }.map { it.spotNumber }.toSet()
                val day2Spots = reservations.filter { r ->
                    ParkingUtils.isTimeOverlapping(nextDateStr, "00:00", endStr, r.date, r.startTime, r.endTime)
                }.map { it.spotNumber }.toSet()
                (day1Spots + day2Spots).toList()
            } else {
                reservations.filter { r ->
                    ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
                }.map { it.spotNumber }
            }
        }
    }
    
    fun hasExistingUserReservation(selectedDate: Calendar, startTime: Calendar?, endTime: Calendar?): Flow<Boolean> {
        if (startTime == null || endTime == null) return flowOf(false)
        
        return combine(repository.reservations, repository.user) { reservations, currentUser ->
            val dateStr = sdfDate.format(selectedDate.time)
            val startStr = sdfTime.format(startTime.time)
            val endStr = sdfTime.format(endTime.time)
            val currentUserId = currentUser?.id.orEmpty()

            val crossesMidnight = ParkingUtils.isMidnightCrossing(startStr, endStr)

            if (crossesMidnight) {
                val nextDateStr = ParkingUtils.addDays(dateStr, 1)
                reservations.any { r ->
                    r.userId == currentUserId && (
                        ParkingUtils.isTimeOverlapping(dateStr, startStr, "23:59", r.date, r.startTime, r.endTime) ||
                        ParkingUtils.isTimeOverlapping(nextDateStr, "00:00", endStr, r.date, r.startTime, r.endTime)
                    )
                }
            } else {
                reservations.any { r ->
                    r.userId == currentUserId && 
                    ParkingUtils.isTimeOverlapping(dateStr, startStr, endStr, r.date, r.startTime, r.endTime)
                }
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

        val startStr = sdfTime.format(startTime.time)
        val endStr = sdfTime.format(endTime.time)
        val crossesMidnight = ParkingUtils.isMidnightCrossing(startStr, endStr)

        val weekLater = Calendar.getInstance().apply {
            time = now.time
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        if (crossesMidnight) {
            val nextDate = Calendar.getInstance().apply {
                time = selectedDate.time
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }
            if (nextDate.after(weekLater)) return R.string.error_max_advance_exceeded

            val totalMinutes = ParkingUtils.calculateDurationMinutes(startStr, endStr)
            if (totalMinutes > 9 * 60) return R.string.error_max_9_hours
            if (startCal.before(now)) return R.string.error_start_before_now
        } else {
            val endCal = Calendar.getInstance().apply {
                time = selectedDate.time
                set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, endTime.get(Calendar.MINUTE))
            }
            if (endCal.after(weekLater)) return R.string.error_max_advance_exceeded

            val diff = endTime.timeInMillis - startTime.timeInMillis
            val hours = diff / (1000 * 60 * 60.0)
            if (startCal.before(now)) return R.string.error_start_before_now
            if (hours <= 0) return R.string.error_end_after_start
            if (hours > 9) return R.string.error_max_9_hours
        }

        return when {
            hasExistingUserReservation -> R.string.error_user_overlap
            selectedSpot != null && occupiedSpots.contains(selectedSpot) -> R.string.error_spot_occupied_simple
            else -> null
        }
    }
}
