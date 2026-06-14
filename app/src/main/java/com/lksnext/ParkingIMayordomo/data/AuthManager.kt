package com.lksnext.ParkingIMayordomo.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.messaging.FirebaseMessaging
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private const val CORPORATE_DOMAIN = "@lksnext.com"
    
    var user by mutableStateOf<User?>(null)
    var reservations by mutableStateOf<List<Reservation>>(emptyList())
    var vehicles by mutableStateOf<List<Vehicle>>(emptyList())
    var notifications by mutableStateOf<List<Notification>>(emptyList())
    var reports by mutableStateOf<List<Report>>(emptyList())

    private val activeListeners = mutableListOf<ListenerRegistration>()

    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]{1,256}${Pattern.quote(CORPORATE_DOMAIN)}"
    )

    init {
        auth.currentUser?.let { firebaseUser ->
            val userId = firebaseUser.uid
            val email = firebaseUser.email ?: ""
            user = User(
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
        val userId = user?.id ?: return
        
        clearListeners()

        // Real-time Reservations
        activeListeners.add(
            db.collection("reservas")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    reservations = snapshot?.documents?.mapNotNull { it.toObject<Reservation>() } ?: emptyList()
                }
        )

        // Real-time Vehicles
        activeListeners.add(
            db.collection("vehiculos")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    vehicles = snapshot?.documents?.mapNotNull { it.toObject<Vehicle>() } ?: emptyList()
                }
        )

        // Real-time Notifications
        activeListeners.add(
            db.collection("usuarios").document(userId).collection("notificaciones")
                .orderBy("time", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    notifications = snapshot?.documents?.mapNotNull { it.toObject<Notification>() } ?: emptyList()
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
                    reports = list.sortedByDescending { it.timestamp?.seconds ?: Long.MAX_VALUE }
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

            user = User(
                id = userId,
                email = normalizedEmail,
                name = firestoreUser?.name ?: (firebaseUser.displayName ?: normalizedEmail.substringBefore("@")),
                profileImage = firestoreUser?.profileImage ?: firebaseUser.photoUrl?.toString()
            )

            refreshAllData()
            syncFcmToken()
            
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
            
            val userProfile = User(
                id = userId,
                name = name.trim(),
                email = normalizedEmail,
                fcmToken = "" 
            )
            db.collection("usuarios").document(userId).set(userProfile).await()

            user = userProfile
            
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
                val userId = user?.id ?: return@addOnCompleteListener
                db.collection("usuarios").document(userId).update("fcmToken", token)
            }
        }
    }

    suspend fun updateFcmToken(token: String) {
        val userId = user?.id ?: return
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
        user = null
        reservations = emptyList()
        vehicles = emptyList()
        notifications = emptyList()
        reports = emptyList()
    }

    private suspend fun generateInitialNotifications(userId: String) {
        val id = UUID.randomUUID().toString()
        val newNotif = Notification(
            id = id,
            userId = userId,
            type = NotificationType.SUCCESS,
            titleResId = R.string.notif_welcome_title,
            messageResId = R.string.notif_welcome_msg,
            time = Date(),
            read = false
        )
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).set(newNotif).await()
    }

    suspend fun addInternalNotification(type: NotificationType, titleResId: Int, messageResId: Int, messageArgs: List<Any> = emptyList()) {
        val userId = user?.id ?: return
        val id = UUID.randomUUID().toString()
        val newNotif = Notification(
            id = id,
            userId = userId,
            type = type,
            titleResId = titleResId,
            messageResId = messageResId,
            messageArgs = messageArgs,
            time = Date(),
            read = false
        )
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).set(newNotif).await()
    }

    suspend fun addExternalNotification(title: String, message: String) {
        val userId = user?.id ?: return
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
        val userId = user?.id ?: return
        db.collection("usuarios").document(userId).collection("notificaciones")
            .document(id).update("read", true).await()
    }

    suspend fun markAllAsRead() {
        val userId = user?.id ?: return
        val batch = db.batch()
        val notifs = db.collection("usuarios").document(userId).collection("notificaciones")
            .whereEqualTo("read", false).get().await()
        for (doc in notifs) {
            batch.update(doc.reference, "read", true)
        }
        batch.commit().await()
    }

    suspend fun deleteNotification(id: String) {
        val userId = user?.id ?: return
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
        val userId = user?.id ?: return
        
        val ref = db.collection("reservas").document()
        
        val newRes = Reservation(
            id = ref.id,
            spotNumber = spotNumber,
            date = date,
            startTime = startTime,
            endTime = endTime,
            userId = userId,
            vehicleId = vehicleId,
            userName = user?.name,
            licensePlate = licensePlate,
            alertaInicioEnviada = false,
            alertaFinEnviada = false,
            fechaAlertaInicio = calculateAlertTimestamp(date, startTime, -15),
            fechaAlertaFin = calculateAlertTimestamp(date, endTime, -15)
        )
        
        ref.set(newRes).await()
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

        val current = reservations.find { it.id == reservationId }
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
    }

    suspend fun deleteReservation(reservationId: String) {
        db.collection("reservas").document(reservationId).delete().await()
    }

    suspend fun addVehicle(type: VehicleType, licensePlate: String) {
        val userId = user?.id ?: return
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
        
        user = user?.copy(
            name = name.trim(),
            profileImage = imageUri ?: user?.profileImage
        )
    }

    suspend fun addReport(spotNumber: Int?, title: String, description: String) {
        val userId = user?.id ?: return
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
    }
}
