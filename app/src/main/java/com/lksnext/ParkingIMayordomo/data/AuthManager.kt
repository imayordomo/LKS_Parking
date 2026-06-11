package com.lksnext.ParkingIMayordomo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private const val CORPORATE_DOMAIN = "@lksnext.com"
    private const val TESTER_EMAIL = "imayordomo004@ikasle.ehu.eus"
    private const val DEFAULT_DATE = "2026-05-06"
    
    var user by mutableStateOf<User?>(null)
    var reservations by mutableStateOf<List<Reservation>>(emptyList())
    var vehicles by mutableStateOf<List<Vehicle>>(emptyList())
    var notifications by mutableStateOf<List<Notification>>(emptyList())

    // Patrón que permite el dominio corporativo o el email de tester
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]{1,256}(?:${Pattern.quote(CORPORATE_DOMAIN)}|${Pattern.quote(TESTER_EMAIL)})"
    )

    private val mockVehiclesByUser = mutableMapOf(
        "user$CORPORATE_DOMAIN" to mutableListOf(Vehicle("user-v1", VehicleType.CAR, "ABC-1234"), Vehicle("user-v2", VehicleType.ELECTRIC, "XYZ-5678")),
        "imayordomo$CORPORATE_DOMAIN" to mutableListOf(Vehicle("juan-v1", VehicleType.CAR, "1111-JPN"), Vehicle("juan-v2", VehicleType.CAR, "2222-JPN")),
        "maria.garcia$CORPORATE_DOMAIN" to mutableListOf(Vehicle("maria-v1", VehicleType.CAR, "3333-MGR"), Vehicle("maria-v2", VehicleType.ELECTRIC, "4444-MGR")),
        "aimar$CORPORATE_DOMAIN" to mutableListOf(
            Vehicle("aimar-v1", VehicleType.MOTORCYCLE, "1234-ABC"), 
            Vehicle("aimar-v2", VehicleType.ELECTRIC, "5678-XYZ"),
            Vehicle("aimar-v3", VehicleType.CAR, "9999-DEF")
        ),
        "iker$CORPORATE_DOMAIN" to mutableListOf(
            Vehicle("iker-v1", VehicleType.CAR, "7777-IKR"), 
            Vehicle("iker-v2", VehicleType.ELECTRIC, "8888-ELC"),
            Vehicle("iker-v3", VehicleType.DISABLED, "6666-DIS")
        )
    )

    private val mockNotificationsByUser = mutableMapOf<String, List<Notification>>()

    init {
        reservations = listOf(
            Reservation("m06-1", 12, DEFAULT_DATE, "08:00", "17:00", "user$CORPORATE_DOMAIN", "user-v1", "Usuario Demo", "ABC-1234"),
            Reservation("m06-2", 3, DEFAULT_DATE, "09:00", "14:00", "aimar$CORPORATE_DOMAIN", "aimar-v1", "Aimar", "1234-ABC"),
            Reservation("m06-3", 8, DEFAULT_DATE, "08:30", "16:30", "maria.garcia$CORPORATE_DOMAIN", "maria-v2", "María García", "4444-MGR")
        )

        auth.currentUser?.let { firebaseUser ->
            val userId = firebaseUser.uid
            val email = firebaseUser.email ?: ""
            user = User(
                id = userId,
                email = email,
                name = firebaseUser.displayName ?: email.substringBefore("@"),
                profileImage = firebaseUser.photoUrl?.toString()
            )
            
            val normalizedEmail = email.lowercase()
            vehicles = mockVehiclesByUser[normalizedEmail] ?: emptyList()
            if (!mockNotificationsByUser.containsKey(userId)) {
                generateInitialNotifications(userId)
            }
            notifications = mockNotificationsByUser[userId] ?: emptyList()
        }
    }

    fun isEmailAuthorized(email: String): Boolean {
        val normalized = email.trim().lowercase()
        return normalized == TESTER_EMAIL || normalized.endsWith(CORPORATE_DOMAIN)
    }

    suspend fun login(email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        
        if (!isEmailAuthorized(normalizedEmail)) {
            throw Exception("error_corporate_only")
        }

        try {
            val result = auth.signInWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = result.user ?: throw Exception("error_invalid_credentials")
            
            val userId = firebaseUser.uid
            user = User(
                id = userId,
                email = normalizedEmail,
                name = firebaseUser.displayName ?: normalizedEmail.substringBefore("@"),
                profileImage = firebaseUser.photoUrl?.toString()
            )

            vehicles = mockVehiclesByUser[normalizedEmail] ?: emptyList()
            if (!mockNotificationsByUser.containsKey(userId)) {
                generateInitialNotifications(userId)
            }
            notifications = mockNotificationsByUser[userId] ?: emptyList()
            
        } catch (_: Exception) {
            throw Exception("error_invalid_credentials")
        }
    }

    suspend fun register(name: String, email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            if (isEmailAuthorized(normalizedEmail)) {
                throw Exception("error_invalid_email_format")
            } else {
                throw Exception("error_corporate_only")
            }
        }
        if (password.length < 8) {
            throw Exception("error_password_too_short")
        }
        if (!password.any { it.isUpperCase() } || !password.any { it.isDigit() }) {
            throw Exception("error_password_complexity")
        }

        try {
            val result = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed")

            val profileUpdates = userProfileChangeRequest {
                displayName = name.trim()
            }
            firebaseUser.updateProfile(profileUpdates).await()

            val userId = firebaseUser.uid
            user = User(id = userId, email = normalizedEmail, name = name.trim())
            
            vehicles = emptyList()
            notifications = emptyList()
            mockNotificationsByUser[userId] = emptyList()
            mockVehiclesByUser[normalizedEmail] = mutableListOf()
            
            generateInitialNotifications(userId)
            
        } catch (e: Exception) {
            throw Exception(e.message ?: "Registration failed")
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (!isEmailAuthorized(normalizedEmail)) {
            throw Exception("error_corporate_only")
        }
        try {
            auth.sendPasswordResetEmail(normalizedEmail).await()
        } catch (e: Exception) {
            throw Exception(e.message ?: "error_unknown")
        }
    }

    fun logout() {
        auth.signOut()
        user = null
        vehicles = emptyList()
        notifications = emptyList()
    }

    private fun generateInitialNotifications(userId: String) {
        val now = System.currentTimeMillis()
        val initial = listOf(
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
        if (user?.id == userId) notifications = initial
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
        val userNotifs = (mockNotificationsByUser[userId] ?: emptyList()).toMutableList()
        userNotifs.add(0, newNotif)
        val updatedList = userNotifs.toList()
        mockNotificationsByUser[userId] = updatedList
        notifications = updatedList
    }

    fun markAsRead(id: String) {
        val userId = user?.id ?: return
        val userNotifs = mockNotificationsByUser[userId] ?: return
        val updatedList = userNotifs.map { if (it.id == id) it.copy(read = true) else it }
        mockNotificationsByUser[userId] = updatedList
        notifications = updatedList
    }

    fun markAllAsRead() {
        val userId = user?.id ?: return
        val userNotifs = mockNotificationsByUser[userId] ?: return
        val updatedList = userNotifs.map { it.copy(read = true) }
        mockNotificationsByUser[userId] = updatedList
        notifications = updatedList
    }

    fun deleteNotification(id: String) {
        val userId = user?.id ?: return
        val userNotifs = mockNotificationsByUser[userId] ?: return
        val updatedList = userNotifs.filter { it.id != id }
        mockNotificationsByUser[userId] = updatedList
        notifications = updatedList
    }

    private fun isToday(dateStr: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try { dateStr == sdf.format(Date()) } catch (_: Exception) { false }
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
        
        if (isToday(date) && isWithinMinutes(startTime)) {
            addInternalNotification(
                NotificationType.INFO,
                R.string.notif_start_soon_title,
                R.string.notif_start_soon_msg,
                listOf(spotNumber)
            )
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
        val userEmail = user?.email ?: return
        
        vehicles = vehicles + newVehicle
        val existing = (mockVehiclesByUser[userEmail] ?: mutableListOf()).toMutableList()
        existing.add(newVehicle)
        mockVehiclesByUser[userEmail] = existing
    }

    fun removeVehicle(id: String) {
        val userEmail = user?.email ?: return
        vehicles = vehicles.filter { it.id != id }
        val existing = (mockVehiclesByUser[userEmail] ?: mutableListOf()).toMutableList()
        existing.removeAll { it.id == id }
        mockVehiclesByUser[userEmail] = existing
    }

    fun updateProfile(name: String, profileImage: String? = null) {
        user = user?.copy(name = name, profileImage = profileImage)
    }
}
