package com.lksnext.ParkingIMayordomo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object AuthManager {
    var user by mutableStateOf<User?>(null)
    var reservations by mutableStateOf<List<Reservation>>(emptyList())
    var vehicles by mutableStateOf<List<Vehicle>>(emptyList())
    var notifications by mutableStateOf<List<Notification>>(emptyList())

    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9+._%+-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9-]{0,25}" +
                ")+"
    )

    private val mockUsers = mutableListOf(
        mutableMapOf("id" to "user@lks.com", "email" to "user@lks.com", "password" to "123456Aa", "name" to "Usuario Demo", "profileImage" to ""),
        mutableMapOf("id" to "juan.perez@lks.com", "email" to "juan.perez@lks.com", "password" to "123456Aa", "name" to "Juan Pérez", "profileImage" to ""),
        mutableMapOf("id" to "maria.garcia@lks.com", "email" to "maria.garcia@lks.com", "password" to "123456Aa", "name" to "María García", "profileImage" to ""),
        mutableMapOf("id" to "aimar@lks.com", "email" to "aimar@lks.com", "password" to "123456Aa", "name" to "Aimar", "profileImage" to ""),
        mutableMapOf("id" to "iker@lks.com", "email" to "iker@lks.com", "password" to "123456Aa", "name" to "Iker", "profileImage" to "")
    )

    private val allMockReservations = listOf(
        // === SEMANA DEL 06 AL 15 DE MAYO 2026 ===

        // Miércoles 06 de Mayo
        Reservation("m06-1", 12, "2026-05-06", "08:00", "17:00", "user@lks.com", "user-v1", "Usuario Demo", "ABC-1234"),
        Reservation("m06-2", 3, "2026-05-06", "09:00", "14:00", "aimar@lks.com", "aimar-v1", "Aimar", "1234-ABC"),
        Reservation("m06-3", 8, "2026-05-06", "08:30", "16:30", "maria.garcia@lks.com", "maria-v2", "María García", "4444-MGR"),
        Reservation("m06-4", 25, "2026-05-06", "07:00", "15:00", "juan.perez@lks.com", "juan-v1", "Juan Pérez", "1111-JPN"),
        Reservation("m06-5", 7, "2026-05-06", "10:00", "18:00", "iker@lks.com", "iker-v3", "Iker", "6666-DIS"),
        Reservation("m06-6", 1, "2026-05-06", "08:00", "18:00", "ext1@lks.com", "ext-1", "Paco", "1000-EXT"),
        Reservation("m06-7", 15, "2026-05-06", "09:00", "19:00", "ext2@lks.com", "ext-2", "Lucía", "2000-EXT"),
        Reservation("m06-8", 40, "2026-05-06", "08:00", "14:00", "ext3@lks.com", "ext-3", "Rafa", "3000-EXT"),
        Reservation("m06-9", 22, "2026-05-06", "08:30", "17:30", "ext4@lks.com", "ext-4", "Elena", "4000-EXT"),
        Reservation("m06-10", 35, "2026-05-06", "09:00", "18:00", "ext5@lks.com", "ext-5", "Mikel", "5000-EXT"),

        // Jueves 07 de Mayo
        Reservation("m07-1", 20, "2026-05-07", "08:00", "18:00", "aimar@lks.com", "aimar-v3", "Aimar", "9999-DEF"),
        Reservation("m07-2", 5, "2026-05-07", "09:30", "13:30", "user@lks.com", "user-v1", "Usuario Demo", "ABC-1234"),
        Reservation("m07-3", 18, "2026-05-07", "08:00", "16:00", "maria.garcia@lks.com", "maria-v1", "María García", "3333-MGR"),
        Reservation("m07-4", 24, "2026-05-07", "10:00", "19:00", "iker@lks.com", "iker-v1", "Iker", "7777-IKR"),
        Reservation("m07-5", 30, "2026-05-07", "07:30", "15:30", "juan.perez@lks.com", "juan-v2", "Juan Pérez", "2222-JPN"),
        Reservation("m07-6", 2, "2026-05-07", "08:00", "20:00", "ext6@lks.com", "ext-6", "Elena", "6000-EXT"),
        Reservation("m07-7", 11, "2026-05-07", "09:00", "18:00", "ext7@lks.com", "ext-7", "Mikel", "7000-EXT"),
        Reservation("m07-8", 42, "2026-05-07", "08:30", "14:30", "ext8@lks.com", "ext-8", "Ane", "8000-EXT"),

        // Viernes 08 de Mayo
        Reservation("m08-1", 4, "2026-05-08", "08:00", "15:00", "aimar@lks.com", "aimar-v2", "Aimar", "5678-XYZ"),
        Reservation("m08-2", 31, "2026-05-08", "09:00", "17:00", "iker@lks.com", "iker-v3", "Iker", "6666-DIS"),
        Reservation("m08-3", 9, "2026-05-08", "08:30", "18:30", "user@lks.com", "user-v2", "Usuario Demo", "XYZ-5678"),
        Reservation("m08-4", 45, "2026-05-08", "07:00", "14:00", "ext1@lks.com", "ext-1", "Paco", "1000-EXT"),
        Reservation("m08-5", 22, "2026-05-08", "10:00", "19:00", "juan.perez@lks.com", "juan-v1", "Juan Pérez", "1111-JPN"),
        Reservation("m08-6", 1, "2026-05-08", "08:00", "13:00", "ext2@lks.com", "ext-2", "Lucía", "2000-EXT"),
        Reservation("m08-7", 15, "2026-05-08", "14:00", "20:00", "ext3@lks.com", "ext-3", "Rafa", "3000-EXT"),

        // Sábado 09 de Mayo
        Reservation("m09-1", 12, "2026-05-09", "10:00", "22:00", "user@lks.com", "user-v1", "Usuario Demo", "ABC-1234"),
        Reservation("m09-2", 2, "2026-05-09", "09:00", "14:00", "ext4@lks.com", "ext-4", "Elena", "4000-EXT"),
        Reservation("m09-3", 44, "2026-05-09", "11:00", "16:00", "ext5@lks.com", "ext-5", "Mikel", "5000-EXT"),

        // Domingo 10 de Mayo
        Reservation("m10-1", 8, "2026-05-10", "10:00", "14:00", "maria.garcia@lks.com", "maria-v2", "María García", "4444-MGR"),
        Reservation("m10-2", 25, "2026-05-10", "16:00", "20:00", "juan.perez@lks.com", "juan-v2", "Juan Pérez", "2222-JPN"),
        Reservation("m10-3", 1, "2026-05-10", "08:00", "20:00", "ext6@lks.com", "ext-6", "Elena", "6000-EXT"),

        // Lunes 11 de Mayo
        Reservation("m11-1", 10, "2026-05-11", "08:00", "18:00", "user@lks.com", "user-v1", "Usuario Demo", "ABC-1234"),
        Reservation("m11-2", 33, "2026-05-11", "09:00", "16:00", "maria.garcia@lks.com", "maria-v1", "María García", "3333-MGR"),
        Reservation("m11-3", 2, "2026-05-11", "08:30", "14:30", "aimar@lks.com", "aimar-v1", "Aimar", "1234-ABC"),
        Reservation("m11-4", 16, "2026-05-11", "07:30", "15:30", "iker@lks.com", "iker-v2", "Iker", "8888-ELC"),
        Reservation("m11-5", 48, "2026-05-11", "10:00", "19:00", "juan.perez@lks.com", "juan-v1", "Juan Pérez", "1111-JPN"),
        Reservation("m11-6", 5, "2026-05-11", "08:00", "18:00", "ext7@lks.com", "ext-7", "Mikel", "7000-EXT"),
        Reservation("m11-7", 21, "2026-05-11", "09:00", "18:00", "ext8@lks.com", "ext-8", "Ane", "8000-EXT"),
        Reservation("m11-8", 41, "2026-05-11", "08:00", "12:00", "ext9@lks.com", "ext-9", "Julen", "9000-EXT"),
        Reservation("m11-9", 3, "2026-05-11", "08:00", "18:00", "ext1@lks.com", "ext-1", "Paco", "1000-EXT"),
        Reservation("m11-10", 13, "2026-05-11", "09:00", "17:00", "ext2@lks.com", "ext-2", "Lucía", "2000-EXT"),

        // Martes 12 de Mayo
        Reservation("m12-1", 14, "2026-05-12", "08:00", "17:00", "aimar@lks.com", "aimar-v3", "Aimar", "9999-DEF"),
        Reservation("m12-2", 23, "2026-05-12", "09:30", "18:30", "user@lks.com", "user-v2", "Usuario Demo", "XYZ-5678"),
        Reservation("m12-3", 1, "2026-05-12", "08:00", "16:00", "maria.garcia@lks.com", "maria-v1", "María García", "3333-MGR"),
        Reservation("m12-4", 7, "2026-05-12", "10:00", "19:00", "iker@lks.com", "iker-v3", "Iker", "6666-DIS"),
        Reservation("m12-5", 12, "2026-05-12", "07:30", "15:30", "ext3@lks.com", "ext-3", "Rafa", "3000-EXT"),
        Reservation("m12-6", 50, "2026-05-12", "08:00", "18:00", "ext4@lks.com", "ext-4", "Elena", "4000-EXT"),
        Reservation("m12-7", 32, "2026-05-12", "09:00", "18:00", "ext5@lks.com", "ext-5", "Mikel", "5000-EXT"),
        Reservation("m12-8", 22, "2026-05-12", "08:00", "14:00", "ext6@lks.com", "ext-6", "Ane", "6000-EXT"),

        // Miércoles 13 de Mayo
        Reservation("m13-1", 42, "2026-05-13", "08:00", "18:00", "maria.garcia@lks.com", "maria-v2", "María García", "4444-MGR"),
        Reservation("m13-2", 3, "2026-05-13", "09:00", "15:00", "juan.perez@lks.com", "juan-v2", "Juan Pérez", "2222-JPN"),
        Reservation("m13-3", 27, "2026-05-13", "08:30", "16:30", "aimar@lks.com", "aimar-v3", "Aimar", "9999-DEF"),
        Reservation("m13-4", 15, "2026-05-13", "07:00", "14:00", "user@lks.com", "user-v1", "Usuario Demo", "ABC-1234"),
        Reservation("m13-5", 11, "2026-05-13", "10:00", "19:00", "ext7@lks.com", "ext-7", "Elena", "7000-EXT"),
        Reservation("m13-6", 21, "2026-05-13", "08:00", "18:00", "ext8@lks.com", "ext-8", "Mikel", "8000-EXT"),
        Reservation("m13-7", 31, "2026-05-13", "09:00", "18:00", "ext9@lks.com", "ext-9", "Ane", "9000-EXT"),
        Reservation("m13-8", 41, "2026-05-13", "08:00", "12:00", "ext1@lks.com", "ext-1", "Julen", "1000-EXT"),

        // Jueves 14 de Mayo
        Reservation("m14-1", 5, "2026-05-14", "08:00", "18:00", "ext2@lks.com", "ext-2", "Elena", "2000-EXT"),
        Reservation("m14-2", 15, "2026-05-14", "09:00", "17:00", "ext3@lks.com", "ext-3", "Mikel", "3000-EXT"),
        Reservation("m14-3", 25, "2026-05-14", "08:30", "16:30", "ext4@lks.com", "ext-4", "Ane", "4000-EXT"),
        Reservation("m14-4", 35, "2026-05-14", "07:30", "15:30", "user@lks.com", "user-v1", "Usuario Demo", "ABC-1234"),
        Reservation("m14-5", 2, "2026-05-14", "10:00", "19:00", "juan.perez@lks.com", "juan-v2", "Juan Pérez", "2222-JPN"),
        Reservation("m14-6", 11, "2026-05-14", "08:00", "14:00", "aimar@lks.com", "aimar-v1", "Aimar", "1234-ABC"),
        Reservation("m14-7", 21, "2026-05-14", "09:00", "18:00", "iker@lks.com", "iker-v1", "Iker", "7777-IKR"),

        // Viernes 15 de Mayo (Ocupación Máxima)
        Reservation("m15-1", 8, "2026-05-15", "08:00", "18:00", "aimar@lks.com", "aimar-v2", "Aimar", "5678-XYZ"),
        Reservation("m15-2", 21, "2026-05-15", "09:30", "17:30", "iker@lks.com", "iker-v1", "Iker", "7777-IKR"),
        Reservation("m15-3", 10, "2026-05-15", "08:30", "15:30", "user@lks.com", "user-v2", "Usuario Demo", "XYZ-5678"),
        Reservation("m15-4", 43, "2026-05-15", "07:00", "14:00", "ext5@lks.com", "ext-5", "Lucía", "5000-EXT"),
        Reservation("m15-5", 33, "2026-05-15", "10:00", "19:00", "ext6@lks.com", "ext-6", "Rafa", "6000-EXT"),
        Reservation("m15-6", 13, "2026-05-15", "08:00", "18:00", "ext7@lks.com", "ext-7", "Elena", "7000-EXT"),
        Reservation("m15-7", 23, "2026-05-15", "09:00", "18:00", "ext8@lks.com", "ext-8", "Mikel", "8000-EXT"),
        Reservation("m15-8", 3, "2026-05-15", "08:00", "12:00", "ext9@lks.com", "ext-9", "Ane", "9000-EXT"),
        Reservation("m15-9", 9, "2026-05-15", "08:30", "16:30", "maria.garcia@lks.com", "maria-v2", "María García", "4444-MGR"),
        Reservation("m15-10", 1, "2026-05-15", "07:00", "15:00", "ext1@lks.com", "ext-1", "Paco", "1000-EXT"),
        Reservation("m15-11", 15, "2026-05-15", "08:00", "18:00", "ext2@lks.com", "ext-2", "Lucía", "2000-EXT"),
        Reservation("m15-12", 25, "2026-05-15", "09:00", "17:00", "ext3@lks.com", "ext-3", "Rafa", "3000-EXT"),
        Reservation("m15-13", 35, "2026-05-15", "08:30", "16:30", "ext4@lks.com", "ext-4", "Elena", "4000-EXT"),
        Reservation("m15-14", 45, "2026-05-15", "07:30", "15:30", "ext5@lks.com", "ext-5", "Mikel", "5000-EXT")
    )

    private val mockVehiclesByUser = mutableMapOf(
        "user@lks.com" to mutableListOf(Vehicle("user-v1", VehicleType.CAR, "ABC-1234"), Vehicle("user-v2", VehicleType.ELECTRIC, "XYZ-5678")),
        "juan.perez@lks.com" to mutableListOf(Vehicle("juan-v1", VehicleType.CAR, "1111-JPN"), Vehicle("juan-v2", VehicleType.CAR, "2222-JPN")),
        "maria.garcia@lks.com" to mutableListOf(Vehicle("maria-v1", VehicleType.CAR, "3333-MGR"), Vehicle("maria-v2", VehicleType.ELECTRIC, "4444-MGR")),
        "aimar@lks.com" to mutableListOf(
            Vehicle("aimar-v1", VehicleType.MOTORCYCLE, "1234-ABC"), 
            Vehicle("aimar-v2", VehicleType.ELECTRIC, "5678-XYZ"),
            Vehicle("aimar-v3", VehicleType.CAR, "9999-DEF")
        ),
        "iker@lks.com" to mutableListOf(
            Vehicle("iker-v1", VehicleType.CAR, "7777-IKR"), 
            Vehicle("iker-v2", VehicleType.ELECTRIC, "8888-ELC"),
            Vehicle("iker-v3", VehicleType.DISABLED, "6666-DIS")
        )
    )

    private val mockNotificationsByUser = mutableMapOf<String, MutableList<Notification>>()

    init {
        reservations = allMockReservations
    }

    suspend fun login(email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.endsWith("@lks.com")) {
            throw Exception("error_corporate_only")
        }
        delay(500)
        val found = mockUsers.find { it["email"] == normalizedEmail && it["password"] == password }
        if (found != null) {
            val userId = found["id"]!!
            user = User(userId, found["email"]!!, found["name"]!!, found["profileImage"])
            vehicles = mockVehiclesByUser[userId] ?: emptyList()
            
            if (!mockNotificationsByUser.containsKey(userId)) {
                generateInitialNotifications(userId)
            }
            notifications = mockNotificationsByUser[userId] ?: emptyList()
        } else {
            throw Exception("error_invalid_credentials")
        }
    }

    suspend fun register(name: String, email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        
        // Validaciones:
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw Exception("error_invalid_email_format")
        }

        if (!normalizedEmail.endsWith("@lks.com")) {
            throw Exception("error_corporate_only")
        }

        if (password.length < 8) {
            throw Exception("error_password_too_short")
        }
        if (!password.any { it.isUpperCase() } || !password.any { it.isDigit() }) {
            throw Exception("error_password_complexity")
        }

        delay(500)

        if (mockUsers.any { it["email"] == normalizedEmail }) {
            throw Exception("error_email_already_in_use")
        }

        val newUserMap = mutableMapOf(
            "id" to normalizedEmail,
            "email" to normalizedEmail,
            "password" to password,
            "name" to name.trim(),
            "profileImage" to ""
        )
        mockUsers.add(newUserMap)
        
        user = User(normalizedEmail, normalizedEmail, name.trim())
        vehicles = emptyList()
        notifications = emptyList()
        mockNotificationsByUser[normalizedEmail] = mutableListOf()
        mockVehiclesByUser[normalizedEmail] = mutableListOf()
    }

    fun logout() {
        user = null
        vehicles = emptyList()
        notifications = emptyList()
    }

    private fun generateInitialNotifications(userId: String) {
        val now = System.currentTimeMillis()
        val initial = mutableListOf(
            Notification(
                id = "init-$userId",
                type = NotificationType.SUCCESS,
                titleResId = R.string.notif_welcome_title,
                messageResId = R.string.notif_welcome_msg,
                time = Date(now - 100000),
                read = false
            )
        )
        mockNotificationsByUser[userId] = initial
    }

    private fun addInternalNotification(type: NotificationType, titleResId: Int, messageResId: Int, messageArgs: List<Any> = emptyList()) {
        val userId = user?.id ?: return
        val newNotif = Notification(
            id = "notif-${System.currentTimeMillis()}",
            type = type,
            titleResId = titleResId,
            messageResId = messageResId,
            messageArgs = messageArgs,
            time = Date(),
            read = false
        )
        val userNotifs = (mockNotificationsByUser[userId] ?: mutableListOf()).toMutableList()
        userNotifs.add(0, newNotif)
        mockNotificationsByUser[userId] = userNotifs
        notifications = userNotifs.toList()
    }

    fun markAsRead(id: String) {
        val userId = user?.id ?: return
        val userNotifs = mockNotificationsByUser[userId] ?: return
        val updated = userNotifs.map { if (it.id == id) it.copy(read = true) else it }.toMutableList()
        mockNotificationsByUser[userId] = updated
        notifications = updated
    }

    fun markAllAsRead() {
        val userId = user?.id ?: return
        val userNotifs = mockNotificationsByUser[userId] ?: return
        val updated = userNotifs.map { it.copy(read = true) }.toMutableList()
        mockNotificationsByUser[userId] = updated
        notifications = updated
    }

    fun deleteNotification(id: String) {
        val userId = user?.id ?: return
        val userNotifs = mockNotificationsByUser[userId] ?: return
        val updated = userNotifs.filter { it.id != id }.toMutableList()
        mockNotificationsByUser[userId] = updated
        notifications = updated
    }

    private fun isToday(dateStr: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateStr == sdf.format(Date())
    }

    private fun isWithinMinutes(timeStr: String, minutes: Int = 30): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                val parsed = sdf.parse(timeStr) ?: return false
                val timeCal = Calendar.getInstance().apply { time = parsed }
                set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val diff = target.timeInMillis - now.timeInMillis
            val diffMinutes = diff / (1000 * 60)
            diffMinutes in 0..minutes
        } catch (_: Exception) { false }
    }

    fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String? = null) {
        val newRes = Reservation(
            id = "res-${System.currentTimeMillis()}",
            spotNumber = spotNumber,
            date = date,
            startTime = startTime,
            endTime = endTime,
            userId = user?.id ?: "",
            userName = user?.name,
            vehicleId = vehicleId,
            licensePlate = licensePlate
        )
        reservations = reservations + newRes
        
        addInternalNotification(
            NotificationType.SUCCESS,
            R.string.notif_confirm_title,
            R.string.notif_confirm_msg,
            listOf(spotNumber, date, startTime)
        )
        
        if (isToday(date)) {
            if (isWithinMinutes(startTime)) {
                addInternalNotification(
                    NotificationType.INFO,
                    R.string.notif_start_soon_title,
                    R.string.notif_start_soon_msg,
                    listOf(spotNumber)
                )
            }
        }
    }

    fun updateReservation(id: String, spotNumber: Int? = null, date: String? = null, startTime: String? = null, endTime: String? = null, vehicleId: String? = null, licensePlate: String? = null) {
        var found = false
        var updatedSpot = 0
        var updatedDate = ""
        var updatedStartTime = ""
        
        reservations = reservations.map { res ->
            if (res.id == id) {
                found = true
                updatedSpot = spotNumber ?: res.spotNumber
                updatedDate = date ?: res.date
                updatedStartTime = startTime ?: res.startTime
                res.copy(
                    spotNumber = updatedSpot,
                    date = updatedDate,
                    startTime = updatedStartTime,
                    endTime = endTime ?: res.endTime,
                    vehicleId = vehicleId ?: res.vehicleId,
                    licensePlate = licensePlate ?: res.licensePlate
                )
            } else res
        }
        
        if (found) {
            addInternalNotification(
                NotificationType.INFO,
                R.string.notif_modified_title,
                R.string.notif_modified_msg,
                listOf(updatedSpot, updatedDate, updatedStartTime)
            )
        }
    }

    fun deleteReservation(id: String) {
        val res = reservations.find { it.id == id }
        if (res != null) {
            reservations = reservations.filter { it.id != id }
            addInternalNotification(
                NotificationType.WARNING,
                R.string.notif_cancelled_title,
                R.string.notif_cancelled_msg,
                listOf(res.spotNumber, res.date)
            )
        }
    }

    fun addVehicle(type: VehicleType, licensePlate: String) {
        val newVehicle = Vehicle(System.currentTimeMillis().toString(), type, licensePlate)
        val userId = user?.id ?: return
        
        vehicles = vehicles + newVehicle
        val existing = mockVehiclesByUser[userId] ?: mutableListOf()
        existing.add(newVehicle)
        mockVehiclesByUser[userId] = existing
    }

    fun removeVehicle(id: String) {
        val userId = user?.id ?: return
        vehicles = vehicles.filter { it.id != id }
        val existing = mockVehiclesByUser[userId] ?: mutableListOf()
        existing.removeAll { it.id == id }
        mockVehiclesByUser[userId] = existing
    }

    fun updateProfile(name: String, profileImage: String? = null) {
        val userId = user?.id ?: return
        user = user?.copy(name = name, profileImage = profileImage)
        
        mockUsers.find { it["id"] == userId }?.apply {
            put("name", name)
            if (profileImage != null) put("profileImage", profileImage)
        }
    }
}
