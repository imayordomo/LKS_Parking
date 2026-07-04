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

enum class SpotOccupancyState {
    FREE, PARTIALLY_OCCUPIED, FULLY_OCCUPIED
}

class ViewParkingViewModel(private val repository: ParkingRepository) : ViewModel() {
    val notifications = repository.notifications

    val reservations = repository.reservations
    val allReservations = repository.allReservations
    val user = repository.user

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val todayStr: String get() = sdfDate.format(Date())
    private val nowTimeStr: String get() = sdfTime.format(Date())

    fun getOccupiedSpots(selectedDate: Calendar): Flow<Map<Int, SpotOccupancyState>> {
        return repository.allReservations.map { reservations ->
            val dateStr = sdfDate.format(selectedDate.time)
            val isToday = dateStr == todayStr
            val prevDateStr = ParkingUtils.addDays(dateStr, -1)
            val spotStates = mutableMapOf<Int, SpotOccupancyState>()

            for (spot in 1..50) {
                val todayActiveReservations = reservations.filter { r ->
                    r.spotNumber == spot && r.date == dateStr &&
                    (isToday && isCurrentlyActive(r.startTime, r.endTime))
                }

                if (isToday) {
                    if (todayActiveReservations.isNotEmpty()) {
                        spotStates[spot] = SpotOccupancyState.FULLY_OCCUPIED
                    }
                } else {
                    val dayReservations = reservations.filter { r ->
                        r.spotNumber == spot && r.date == dateStr
                    }
                    val prevMidnightCrossing = reservations.filter { r ->
                        r.spotNumber == spot && r.date == prevDateStr &&
                        ParkingUtils.isMidnightCrossing(r.startTime, r.endTime)
                    }

                    if (dayReservations.isEmpty() && prevMidnightCrossing.isEmpty()) continue

                    if (isFullDayCovered(dayReservations, prevMidnightCrossing)) {
                        spotStates[spot] = SpotOccupancyState.FULLY_OCCUPIED
                    } else {
                        spotStates[spot] = SpotOccupancyState.PARTIALLY_OCCUPIED
                    }
                }
            }
            spotStates
        }
    }

    private fun isFullDayCovered(
        dayReservations: List<com.lksnext.ParkingIMayordomo.data.model.Reservation>,
        prevMidnightCrossing: List<com.lksnext.ParkingIMayordomo.data.model.Reservation>
    ): Boolean {
        val intervals = mutableListOf<Pair<Int, Int>>()
        val fullDay = 24 * 60

        for (r in dayReservations) {
            val start = ParkingUtils.timeToMinutes(r.startTime)
            val end = ParkingUtils.timeToMinutes(r.endTime)
            if (end > start) {
                intervals.add(Pair(start, end))
            } else {
                intervals.add(Pair(start, fullDay))
            }
        }

        for (r in prevMidnightCrossing) {
            val end = ParkingUtils.timeToMinutes(r.endTime)
            intervals.add(Pair(0, end))
        }

        if (intervals.isEmpty()) return false

        intervals.sortBy { it.first }
        var mergedStart = intervals[0].first
        var mergedEnd = intervals[0].second
        for (i in 1 until intervals.size) {
            val (s, e) = intervals[i]
            if (s <= mergedEnd) {
                if (e > mergedEnd) mergedEnd = e
            } else {
                return false
            }
        }

        return mergedStart <= 0 && mergedEnd >= fullDay
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
