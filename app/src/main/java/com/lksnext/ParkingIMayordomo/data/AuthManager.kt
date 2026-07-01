package com.lksnext.ParkingIMayordomo.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.messaging.FirebaseMessaging
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.*
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@Suppress("StaticFieldLeak")
object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private const val CORPORATE_DOMAIN = "@lksnext.com"
    
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _allReservations = MutableStateFlow<List<Reservation>>(emptyList())
    val allReservations: StateFlow<List<Reservation>> = _allReservations.asStateFlow()

    private var allReservationsStarted = false

    private val _vehicles = MutableStateFlow<List<Vehicle>?>(null)
    val vehicles: StateFlow<List<Vehicle>?> = _vehicles.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()

    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]{1,256}${Pattern.quote(CORPORATE_DOMAIN)}"
    )

    init {
        auth.currentUser?.let { firebaseUser ->
            val userId = firebaseUser.uid
            val email = firebaseUser.email.orEmpty()
            _user.value = User(
                id = userId,
                email = email,
                name = firebaseUser.displayName ?: email.substringBefore("@"),
                profileImage = firebaseUser.photoUrl?.toString()
            )
            refreshAllData()
            syncFcmToken()
        }
    }

    private fun refreshAllData() {
        val userId = _user.value?.id ?: return
        
        clearListeners()

        // Real-time Reservations (user-specific)
        activeListeners.add(
            db.collection("reservas")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    _reservations.value = snapshot?.documents?.mapNotNull { it.toObject<Reservation>() } ?: emptyList()
                }
        )

        // Real-time Vehicles
        activeListeners.add(
            db.collection("vehiculos")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    _vehicles.value = snapshot?.documents?.mapNotNull { it.toObject<Vehicle>() } ?: emptyList()
                }
        )

        // Real-time Notifications
        activeListeners.add(
            db.collection("usuarios").document(userId).collection("notificaciones")
                .orderBy("time", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    _notifications.value = snapshot?.documents?.mapNotNull { it.toObject<Notification>() } ?: emptyList()
                }
        )

        // Real-time Reports - Sorting locally to avoid mandatory index error
        activeListeners.add(
            db.collection("reportes")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AUTH_MANAGER", "Error listening to reports", error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { it.toObject<Report>() } ?: emptyList()
                    // Use MAX_VALUE for null timestamps to keep new unsynced reports at top
                    _reports.value = list.sortedByDescending { it.timestamp?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    private fun clearListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }

    fun isEmailAuthorized(email: String): Boolean {
        val normalized = email.trim().lowercase()
        return normalized.endsWith(CORPORATE_DOMAIN)
    }

    fun startAllReservationsListener() {
        if (allReservationsStarted) return
        allReservationsStarted = true
        activeListeners.add(
            db.collection("reservas")
                .addSnapshotListener { snapshot, _ ->
                    _allReservations.value = snapshot?.documents?.mapNotNull { it.toObject<Reservation>() } ?: emptyList()
                }
        )
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
            val userDoc = db.collection("usuarios").document(userId).get().await()
            val firestoreUser = userDoc.toObject<User>()

            _user.value = User(
                id = userId,
                email = normalizedEmail,
                name = firestoreUser?.name ?: (firebaseUser.displayName ?: normalizedEmail.substringBefore("@")),
                profileImage = firestoreUser?.profileImage ?: firebaseUser.photoUrl?.toString()
            )

            refreshAllData()
            syncFcmToken()
            
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w("AuthManager", "Login failed", e)
            throw Exception("error_invalid_credentials")
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w("AuthManager", "Login failed", e)
            throw java.lang.IllegalStateException("error_user_disabled")
        } catch (e: FirebaseNetworkException) {
            Log.w("AuthManager", "Login failed", e)
            throw java.lang.IllegalStateException("error_network")
        } catch (e: FirebaseAuthException) {
            if (e.errorCode == "ERROR_TOO_MANY_ATTEMPTS_TRY_LATER") {
                throw java.lang.IllegalStateException("error_too_many_requests")
            }
            throw java.lang.IllegalStateException("error_unknown")
        } catch (_: Exception) {
            throw java.lang.IllegalStateException("error_unknown")
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
            
            val userProfile = User(
                id = userId,
                name = name.trim(),
                email = normalizedEmail,
                fcmToken = "" 
            )
            db.collection("usuarios").document(userId).set(userProfile).await()

            _user.value = userProfile
            
            refreshAllData()
            syncFcmToken()
            generateInitialNotifications(userId)
            
        } catch (e: FirebaseAuthUserCollisionException) {
            throw Exception("error_email_already_in_use")
        } catch (e: Exception) {
            throw Exception(e.message ?: "Registration failed")
        }
    }

    fun syncFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = _user.value?.id ?: return@addOnCompleteListener
                db.collection("usuarios").document(userId).update("fcmToken", token)
            }
        }
    }

    suspend fun updateFcmToken(token: String) {
        val userId = _user.value?.id ?: return
        db.collection("usuarios").document(userId)
            .update("fcmToken", token)
            .await()
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
        clearListeners()
        auth.signOut()
        _user.value = null
        _reservations.value = emptyList()
        _allReservations.value = emptyList()
        _vehicles.value = null
        _notifications.value = emptyList()
        _reports.value = emptyList()
    }

    private suspend fun generateInitialNotifications(userId: String) {
        val id = UUID.randomUUID().toString()
        val newNotif = Notification(
            id = id,
            userId = userId,
            type = NotificationType.SUCCESS,
            titleRes = "notif_welcome_title",
            messageRes = "notif_welcome_msg",
            time = Date(),
            read = false
        )
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).set(newNotif).await()
    }

    suspend fun addInternalNotification(type: NotificationType, titleRes: String, messageRes: String, messageArgs: List<Any> = emptyList()) {
        val userId = _user.value?.id ?: return
        val id = UUID.randomUUID().toString()
        val newNotif = Notification(
            id = id,
            userId = userId,
            type = type,
            titleRes = titleRes,
            messageRes = messageRes,
            messageArgs = messageArgs,
            time = Date(),
            read = false
        )
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).set(newNotif).await()
    }

    suspend fun addExternalNotification(title: String, message: String) {
        val userId = _user.value?.id ?: return
        val id = UUID.randomUUID().toString()
        val newNotif = Notification(
            id = id,
            userId = userId,
            type = NotificationType.INFO,
            title = title,
            message = message,
            time = Date(),
            read = false
        )
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).set(newNotif).await()
    }

    suspend fun markAsRead(id: String) {
        val userId = _user.value?.id ?: return
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).update("read", true).await()
    }

    suspend fun markAllAsRead() {
        val userId = _user.value?.id ?: return
        val batch = db.batch()
        val notifs = db.collection("usuarios").document(userId).collection("notificaciones")
            .whereEqualTo("read", false).get().await()
        for (doc in notifs) {
            batch.update(doc.reference, "read", true)
        }
        batch.commit().await()
    }

    suspend fun deleteNotification(id: String) {
        val userId = _user.value?.id ?: return
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).delete().await()
    }

    private fun calculateAlertTimestamp(date: String, time: String, minutesOffset: Int): Timestamp? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val calendar = Calendar.getInstance().apply {
                this.time = sdf.parse("$date $time") ?: return null
                add(Calendar.MINUTE, minutesOffset)
            }
            Timestamp(calendar.time)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String? = null) {
        val userId = _user.value?.id ?: return
        val userName = _user.value?.name

        if (ParkingUtils.isMidnightCrossing(startTime, endTime)) {
            val totalMinutes = ParkingUtils.calculateDurationMinutes(startTime, endTime)
            if (totalMinutes > 9 * 60) throw Exception("error_max_9_hours")

            val groupId = UUID.randomUUID().toString()
            val nextDate = ParkingUtils.addDays(date, 1)

            // Day 1: startTime to 23:59
            val ref1 = db.collection("reservas").document()
            val res1 = Reservation(
                id = ref1.id, spotNumber = spotNumber, date = date,
                startTime = startTime, endTime = "23:59",
                userId = userId, vehicleId = vehicleId, userName = userName,
                licensePlate = licensePlate, groupId = groupId,
                alertaInicioEnviada = false, alertaFinEnviada = false,
                fechaAlertaInicio = calculateAlertTimestamp(date, startTime, -15),
                fechaAlertaFin = calculateAlertTimestamp(date, "23:59", -15)
            )
            ref1.set(res1).await()

            // Day 2: 00:00 to endTime
            val ref2 = db.collection("reservas").document()
            val res2 = Reservation(
                id = ref2.id, spotNumber = spotNumber, date = nextDate,
                startTime = "00:00", endTime = endTime,
                userId = userId, vehicleId = vehicleId, userName = userName,
                licensePlate = licensePlate, groupId = groupId,
                alertaInicioEnviada = false, alertaFinEnviada = false,
                fechaAlertaInicio = calculateAlertTimestamp(nextDate, "00:00", -15),
                fechaAlertaFin = calculateAlertTimestamp(nextDate, endTime, -15)
            )
            ref2.set(res2).await()

            addInternalNotification(
                NotificationType.SUCCESS,
                "notif_confirm_title",
                "notif_confirm_msg",
                listOf(spotNumber, date, startTime)
            )
        } else {
            val ref = db.collection("reservas").document()

            val newRes = Reservation(
                id = ref.id, spotNumber = spotNumber, date = date,
                startTime = startTime, endTime = endTime,
                userId = userId, vehicleId = vehicleId, userName = userName,
                licensePlate = licensePlate,
                alertaInicioEnviada = false, alertaFinEnviada = false,
                fechaAlertaInicio = calculateAlertTimestamp(date, startTime, -15),
                fechaAlertaFin = calculateAlertTimestamp(date, endTime, -15)
            )

            ref.set(newRes).await()

            addInternalNotification(
                NotificationType.SUCCESS,
                "notif_confirm_title",
                "notif_confirm_msg",
                listOf(spotNumber, date, startTime)
            )
        }
    }

    suspend fun updateReservation(
        reservationId: String,
        spotNumber: Int? = null,
        date: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        vehicleId: String? = null,
        licensePlate: String? = null
    ) {
        val updates = mutableMapOf<String, Any?>()
        spotNumber?.let { updates["spotNumber"] = it }
        date?.let { updates["date"] = it }
        startTime?.let { updates["startTime"] = it }
        endTime?.let { updates["endTime"] = it }
        vehicleId?.let { updates["vehicleId"] = it }
        licensePlate?.let { updates["licensePlate"] = it }
        
        updates["alertaInicioEnviada"] = false
        updates["alertaFinEnviada"] = false

        val current = _reservations.value.find { it.id == reservationId }
        val finalDate = date ?: current?.date
        val finalStart = startTime ?: current?.startTime
        val finalEnd = endTime ?: current?.endTime
        
        if (finalDate != null && finalStart != null) {
            updates["fechaAlertaInicio"] = calculateAlertTimestamp(finalDate, finalStart, -15)
        }
        if (finalDate != null && finalEnd != null) {
            updates["fechaAlertaFin"] = calculateAlertTimestamp(finalDate, finalEnd, -15)
        }

        db.collection("reservas").document(reservationId).update(updates.filterValues { it != null }).await()

        // If this reservation belongs to a midnight-crossing group, update sibling
        val currentRes = current ?: _reservations.value.find { it.id == reservationId }
        val hasFieldUpdates = spotNumber != null || vehicleId != null || licensePlate != null
        if (currentRes?.groupId?.isNotEmpty() == true && hasFieldUpdates) {
            val siblings = _reservations.value.filter { it.groupId == currentRes.groupId && it.id != reservationId }
            val siblingDate = date?.let { ParkingUtils.addDays(it, 1) }
            for (sibling in siblings) {
                val sibUpdates = mutableMapOf<String, Any?>()
                spotNumber?.let { sibUpdates["spotNumber"] = it }
                vehicleId?.let { sibUpdates["vehicleId"] = it }
                licensePlate?.let { sibUpdates["licensePlate"] = it }
                sibUpdates["alertaInicioEnviada"] = false
                sibUpdates["alertaFinEnviada"] = false
                val sibFinalDate = siblingDate ?: currentRes.date
                val sibFinalStart = currentRes.startTime
                val sibFinalEnd = currentRes.endTime
                if (sibFinalDate != null && sibFinalStart != null) {
                    sibUpdates["fechaAlertaInicio"] = calculateAlertTimestamp(sibFinalDate, sibFinalStart, -15)
                }
                if (sibFinalDate != null && sibFinalEnd != null) {
                    sibUpdates["fechaAlertaFin"] = calculateAlertTimestamp(sibFinalDate, sibFinalEnd, -15)
                }
                if (sibUpdates.isNotEmpty()) {
                    db.collection("reservas").document(sibling.id).update(sibUpdates).await()
                }
            }
        }

        // Notification of modification
        addInternalNotification(
            NotificationType.INFO,
            "notif_modified_title",
            "notif_modified_msg",
            listOf((spotNumber ?: current?.spotNumber) ?: 0, finalDate.orEmpty(), finalStart.orEmpty())
        )
    }

    suspend fun deleteReservation(reservationId: String) {
        val current = _reservations.value.find { it.id == reservationId }
        if (current?.groupId?.isNotEmpty() == true) {
            val siblings = _reservations.value.filter { it.groupId == current.groupId && it.id != reservationId }
            for (sibling in siblings) {
                db.collection("reservas").document(sibling.id).delete().await()
            }
        }
        db.collection("reservas").document(reservationId).delete().await()

        // Notification of cancellation
        addInternalNotification(
            NotificationType.WARNING,
            "notif_cancelled_title",
            "notif_cancelled_msg",
            listOf(current?.spotNumber ?: 0, current?.date.orEmpty())
        )
    }

    suspend fun addVehicle(type: VehicleType, licensePlate: String) {
        val userId = _user.value?.id ?: return
        val normalizedPlate = licensePlate.trim().uppercase()
        
        // Check if license plate is unique
        val plateQuery = db.collection("vehiculos")
            .whereEqualTo("licensePlate", normalizedPlate)
            .get().await()
            
        if (!plateQuery.isEmpty) {
            throw Exception("error_license_plate_exists")
        }

        val id = UUID.randomUUID().toString()
        val newVehicle = Vehicle(
            id = id,
            userId = userId,
            type = type,
            licensePlate = normalizedPlate
        )
        db.collection("vehiculos").document(id).set(newVehicle).await()
    }

    suspend fun removeVehicle(vehicleId: String) {
        db.collection("vehiculos").document(vehicleId).delete().await()
    }

    suspend fun updateProfile(name: String, imageUri: String?) {
        val firebaseUser = auth.currentUser ?: return
        
        val profileUpdates = userProfileChangeRequest {
            displayName = name.trim()
        }
        firebaseUser.updateProfile(profileUpdates).await()

        val userId = firebaseUser.uid
        val updates = mutableMapOf<String, Any>("name" to name.trim())
        imageUri?.let { updates["profileImage"] = it }

        db.collection("usuarios").document(userId).update(updates).await()
        
        _user.value = _user.value?.copy(
            name = name.trim(),
            profileImage = imageUri ?: _user.value?.profileImage
        )
    }

    suspend fun addReport(spotNumber: Int?, title: String, description: String) {
        val userId = _user.value?.id ?: return
        val id = UUID.randomUUID().toString()
        val report = Report(
            id = id,
            userId = userId,
            spotNumber = spotNumber,
            title = title,
            description = description,
            timestamp = Timestamp.now(),
            status = "PENDING"
        )
        db.collection("reportes").document(id).set(report).await()

        // Notification of report sent
        addInternalNotification(
            NotificationType.SUCCESS,
            "notif_report_sent_title",
            "notif_report_sent_msg"
        )
    }
}
