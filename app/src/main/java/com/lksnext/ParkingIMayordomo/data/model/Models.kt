package com.lksnext.ParkingIMayordomo.data.model

import com.google.firebase.Timestamp
import java.util.Date

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val profileImage: String? = null,
    val fcmToken: String? = null
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
    val id: String = "",
    val userId: String = "",
    val type: VehicleType = VehicleType.CAR,
    val licensePlate: String = ""
)

data class Reservation(
    val id: String = "",
    val spotNumber: Int = 0,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val userId: String = "",
    val vehicleId: String = "",
    val userName: String? = null,
    val licensePlate: String? = null,
    val groupId: String = "",
    val alertaInicioEnviada: Boolean = false,
    val alertaFinEnviada: Boolean = false,
    val fechaAlertaInicio: Timestamp? = null,
    val fechaAlertaFin: Timestamp? = null
)

enum class NotificationType {
    INFO, WARNING, SUCCESS
}

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.INFO,
    val title: String? = null,
    val message: String? = null,
    val titleRes: String? = null,
    val messageRes: String? = null,
    val titleResId: Int? = null, // Legacy, avoid using for new notifications
    val messageResId: Int? = null, // Legacy, avoid using for new notifications
    val messageArgs: List<Any> = emptyList(),
    val time: Date = Date(),
    val read: Boolean = false
)

enum class ReportStatus {
    PENDING, IN_REVIEW, RESOLVED;

    companion object {
        fun fromString(value: String?): ReportStatus = when(value?.uppercase()) {
            "PENDING", "PENDIENTE" -> PENDING
            "IN_REVIEW", "EN_REVISION" -> IN_REVIEW
            "RESOLVED", "RESUELTO" -> RESOLVED
            else -> PENDING
        }
    }
}

data class Report(
    val id: String = "",
    val userId: String = "",
    val spotNumber: Int? = null,
    val title: String = "",
    val description: String = "",
    val timestamp: Timestamp? = null,
    val status: String = "PENDING"
)
