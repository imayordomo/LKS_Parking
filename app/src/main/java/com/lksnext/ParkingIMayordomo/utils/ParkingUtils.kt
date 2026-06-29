package com.lksnext.ParkingIMayordomo.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class SpotType {
    NORMAL, ELECTRIC, MOTORCYCLE, DISABLED
}

object ParkingUtils {
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val TIME_FORMAT = "HH:mm"
    
    // Routes
    const val ROUTE_LANDING = "landing"
    const val ROUTE_LOGIN = "login"
    const val ROUTE_REGISTER = "register"
    const val ROUTE_FORGOT_PASSWORD = "forgot_password"
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_VIEW_PARKING = "view-parking"
    const val ROUTE_NEW_RESERVATION = "new-reservation"
    const val ROUTE_EDIT_RESERVATION = "edit-reservation"
    const val ROUTE_NOTIFICATIONS = "notifications"
    const val ROUTE_REPORT = "report"
    const val ROUTE_MY_REPORTS = "my-reports"
    const val ROUTE_HELP = "help"
    const val ROUTE_ABOUT = "about"
    
    // Params
    const val PARAM_SHOW_VEHICLE_ALERT = "showVehicleAlert"
    const val PARAM_RESERVATION_ID = "reservationId"

    fun getSdfDate() = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    fun getSdfTime() = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())

    fun formatDate(date: Date): String = getSdfDate().format(date)
    fun formatTime(date: Date): String = getSdfTime().format(date)
    fun parseDate(dateStr: String): Date? = try { getSdfDate().parse(dateStr) } catch (e: Exception) { null }
    fun parseTime(timeStr: String): Date? = try { getSdfTime().parse(timeStr) } catch (e: Exception) { null }

    fun getSpotType(spotNumber: Int): SpotType {
        return when (spotNumber) {
            in 1..5 -> SpotType.MOTORCYCLE
            in 6..7 -> SpotType.DISABLED
            in 8..9 -> SpotType.ELECTRIC
            else -> SpotType.NORMAL
        }
    }

    fun getSpotLabelRes(type: SpotType): Int {
        return when (type) {
            SpotType.NORMAL -> R.string.spot_type_normal
            SpotType.ELECTRIC -> R.string.spot_type_electric
            SpotType.MOTORCYCLE -> R.string.spot_type_motorcycle
            SpotType.DISABLED -> R.string.spot_type_disabled
        }
    }

    fun getVehicleTypeLabelRes(type: VehicleType): Int {
        return when (type) {
            VehicleType.CAR -> R.string.spot_type_normal
            VehicleType.ELECTRIC -> R.string.spot_type_electric
            VehicleType.MOTORCYCLE -> R.string.spot_type_motorcycle
            VehicleType.DISABLED -> R.string.spot_type_disabled
        }
    }
    
    fun getVehicleColor(type: VehicleType): Color {
        return when (type) {
            VehicleType.CAR -> MainOrange
            VehicleType.ELECTRIC -> ElectricColor
            VehicleType.DISABLED -> DisabledColor
            VehicleType.MOTORCYCLE -> MotorcycleColor
        }
    }
    
    fun getVehicleIcon(type: VehicleType): ImageVector {
        return when (type) {
            VehicleType.CAR -> Icons.Default.DirectionsCar
            VehicleType.ELECTRIC -> Icons.Default.ElectricCar
            VehicleType.MOTORCYCLE -> Icons.Default.TwoWheeler
            VehicleType.DISABLED -> Icons.AutoMirrored.Filled.Accessible
        }
    }

    fun getSpotIcon(type: SpotType): ImageVector {
        return when (type) {
            SpotType.MOTORCYCLE -> Icons.Default.TwoWheeler
            SpotType.ELECTRIC -> Icons.Default.ElectricCar
            SpotType.DISABLED -> Icons.AutoMirrored.Filled.Accessible
            SpotType.NORMAL -> Icons.Default.DirectionsCar
        }
    }

    fun getSpotColor(type: SpotType): Color {
        return when (type) {
            SpotType.MOTORCYCLE -> MotorcycleColor
            SpotType.ELECTRIC -> ElectricColor
            SpotType.DISABLED -> DisabledColor
            SpotType.NORMAL -> MainOrange
        }
    }

    fun isVehicleAllowedInSpot(spotNumber: Int, vehicleType: VehicleType): Boolean {
        val spotType = getSpotType(spotNumber)
        return when (spotType) {
            SpotType.MOTORCYCLE -> vehicleType == VehicleType.MOTORCYCLE
            SpotType.DISABLED -> vehicleType == VehicleType.DISABLED
            SpotType.ELECTRIC -> vehicleType == VehicleType.ELECTRIC
            SpotType.NORMAL -> vehicleType == VehicleType.CAR || vehicleType == VehicleType.ELECTRIC || vehicleType == VehicleType.DISABLED
        }
    }

    fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun minutesToTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    fun addDays(date: String, days: Int): String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val cal = Calendar.getInstance().apply { time = sdf.parse(date) ?: return date; add(Calendar.DAY_OF_YEAR, days) }
        return sdf.format(cal.time)
    }

    fun isMidnightCrossing(startTime: String, endTime: String): Boolean {
        return timeToMinutes(endTime) <= timeToMinutes(startTime)
    }

    fun calculateDurationMinutes(startTime: String, endTime: String): Int {
        val start = timeToMinutes(startTime)
        val end = timeToMinutes(endTime)
        return if (end > start) end - start else (24 * 60 - start) + end
    }

     //time format must come as "HH:mm"
    fun isTimeOverlapping(
        date1: String, start1: String, end1: String,
        date2: String, start2: String, end2: String
    ): Boolean {
        if (date1 != date2) return false
        return (start1 >= start2 && start1 < end2) ||
               (end1 > start2 && end1 <= end2) ||
               (start1 <= start2 && end1 >= end2)
    }
}
