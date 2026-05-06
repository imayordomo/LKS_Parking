package com.lksnext.ParkingIMayordomo.data.model

import java.util.Date

data class User(
    val id: String, // El ID ahora es el email (redundancia a resolver)
    val email: String,
    val name: String,
    val profileImage: String? = null
)

enum class VehicleType {
    CAR, ELECTRIC, MOTORCYCLE, DISABLED;

    companion object {
        fun fromString(value: String): VehicleType = when(value.lowercase()) {
            "car" -> CAR
            "electric" -> ELECTRIC
            "motorcycle" -> MOTORCYCLE
            "disabled" -> DISABLED
            else -> CAR
        }
    }
}

data class Vehicle(
    val id: String,
    val type: VehicleType,
    val licensePlate: String
)

data class Reservation(
    val id: String,
    val spotNumber: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val userId: String,
    val vehicleId: String,
    val userName: String? = null,
    val licensePlate: String? = null
)

enum class NotificationType {
    INFO, WARNING, SUCCESS
}

data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String? = null,
    val message: String? = null,
    val titleResId: Int? = null,
    val messageResId: Int? = null,
    val messageArgs: List<Any> = emptyList(),
    val time: Date,
    val read: Boolean = false
)
