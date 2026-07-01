package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import kotlinx.coroutines.flow.*
import java.util.*

class HistoryViewModel(private val repository: ParkingRepository) : ViewModel() {

    private val _statusFilter = MutableStateFlow("all")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _startDateText = MutableStateFlow("")
    val startDateText: StateFlow<String> = _startDateText.asStateFlow()

    private val _endDateText = MutableStateFlow("")
    val endDateText: StateFlow<String> = _endDateText.asStateFlow()

    val vehicles = repository.vehicles

    val filteredReservations: StateFlow<List<Reservation>> = combine(
        repository.reservations,
        repository.user,
        _statusFilter,
        _startDateText,
        _endDateText
    ) { reservations, currentUser, status, start, end ->
        val now = Date()
        val todayStr = ParkingUtils.formatDate(now)
        val currentTimeStr = ParkingUtils.formatTime(now)

        reservations
            .filter { it.userId == currentUser?.id }
            .filter { res ->
                val isPast = res.date < todayStr || (res.date == todayStr && res.endTime < currentTimeStr)
                
                val matchesStatus = when (status) {
                    "past" -> isPast
                    "future" -> !isPast
                    else -> true
                }
                
                val matchesDate = (start.isEmpty() || res.date >= start) &&
                                (end.isEmpty() || res.date <= end)

                matchesStatus && matchesDate
            }
            .sortedWith(compareByDescending<Reservation> { it.date }.thenByDescending { it.startTime })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStatusFilter(status: String) { _statusFilter.value = status }
    fun setStartDate(date: String) { _startDateText.value = date }
    fun setEndDate(date: String) { _endDateText.value = date }
    
    fun clearFilters() {
        _statusFilter.value = "all"
        _startDateText.value = ""
        _endDateText.value = ""
    }

    fun generateCsvContent(headers: List<String>): String {
        val csvContent = StringBuilder()
        csvContent.append(headers.joinToString(",")).append("\n")
        filteredReservations.value.forEach { r ->
            val row = listOf(
                "#${r.spotNumber}",
                r.date,
                r.startTime,
                r.endTime,
                r.licensePlate.orEmpty()
            )
            csvContent.append(row.joinToString(",")).append("\n")
        }
        return csvContent.toString()
    }
}
