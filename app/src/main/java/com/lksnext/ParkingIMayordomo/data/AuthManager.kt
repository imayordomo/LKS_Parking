package com.lksnext.ParkingIMayordomo.data

import android.content.Context
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
import com.lksnext.ParkingIMayordomo.utils.ReservationReminderManager
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
    private var reminderManager: ReservationReminderManager? = null
    
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

    fun init(context: Context) {
        reminderManager = ReservationReminderManager(context.applicationContext)
    }

    private fun refreshAllData() {
        val userId = _user.value?.id ?: return
        
        clearListeners()

        activeListeners.add(
            db.collection("reservas")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    _reservations.value = snapshot?.documents?.mapNotNull { it.toObject<Reservation>() } ?: emptyList()
                }
        )

        activeListeners.add(
            db.collection("vehiculos")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    _vehicles.value = snapshot?.documents?.mapNotNull { it.toObject<Vehicle>() } ?: emptyList()
                }
        )

        activeListeners.add(
            db.collection("usuarios").document(userId).collection("notificaciones")
                .orderBy("time", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    _notifications.value = snapshot?.documents?.mapNotNull { it.toObject<Notification>() } ?: emptyList()
                }
        )

        activeListeners.add(
            db.collection("reportes")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AUTH_MANAGER", "Error listening to reports", error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { it.toObject<Report>() } ?: emptyList()
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
        if (!isEmailAuthorized(normalizedEmail)) throw Exception("error_corporate_only")
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
        } catch (e: Exception) { throw e }
    }

    suspend fun register(name: String, email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        try {
            val result = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed")
            val profileUpdates = userProfileChangeRequest { displayName = name.trim() }
            firebaseUser.updateProfile(profileUpdates).await()
            val userId = firebaseUser.uid
            val userProfile = User(id = userId, name = name.trim(), email = normalizedEmail, fcmToken = "")
            db.collection("usuarios").document(userId).set(userProfile).await()
            _user.value = userProfile
            refreshAllData()
            syncFcmToken()
            generateInitialNotifications(userId)
        } catch (e: Exception) { throw e }
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
        db.collection("usuarios").document(userId).update("fcmToken", token).await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val normalizedEmail = email.trim().lowercase()
        try {
            auth.sendPasswordResetEmail(normalizedEmail).await()
        } catch (e: Exception) { throw e }
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
        val newNotif = Notification(id = id, userId = userId, type = NotificationType.SUCCESS, titleRes = "notif_welcome_title", messageRes = "notif_welcome_msg", time = Date(), read = false)
        db.collection("usuarios").document(userId).collection("notificaciones").document(id).set(newNotif).await()
    }

    suspend fun addInternalNotification(type: NotificationType, titleRes: String, messageRes: String, messageArgs: List<Any> = emptyList()) {
        val userId = _user.value?.id ?: return
        val id = UUID.randomUUID().toString()
        val newNotif = Notification(id = id, userId = userId, type = type, titleRes = titleRes, messageRes = messageRes, messageArgs = messageArgs, time = Date(), read = false)
        db.collection("usuarios").document(userId).collection("notificaciones").document(id).set(newNotif).await()
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
        db.collection("usuarios").document(userId).collection("notificaciones").document(id).update("read", true).await()
    }

    suspend fun markAllAsRead() {
        val userId = _user.value?.id ?: return
        val batch = db.batch()
        val notifs = db.collection("usuarios").document(userId).collection("notificaciones").whereEqualTo("read", false).get().await()
        for (doc in notifs) { batch.update(doc.reference, "read", true) }
        batch.commit().await()
    }

    suspend fun deleteNotification(id: String) {
        val userId = _user.value?.id ?: return
        db.collection("usuarios").document(userId).collection("notificaciones").document(id).delete().await()
    }

    private fun getMillis(date: String, time: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.parse("$date $time")?.time ?: 0L
    }

    suspend fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String? = null) {
        val userId = _user.value?.id ?: return
        val userName = _user.value?.name

        if (ParkingUtils.isMidnightCrossing(startTime, endTime)) {
            val groupId = UUID.randomUUID().toString()
            val nextDate = ParkingUtils.addDays(date, 1)

            val ref1 = db.collection("reservas").document()
            val res1 = Reservation(id = ref1.id, spotNumber = spotNumber, date = date, startTime = startTime, endTime = "23:59", userId = userId, vehicleId = vehicleId, userName = userName, licensePlate = licensePlate, groupId = groupId)
            ref1.set(res1).await()
            reminderManager?.scheduleReminders(ref1.id, getMillis(date, startTime), getMillis(date, "23:59"))

            val ref2 = db.collection("reservas").document()
            val res2 = Reservation(id = ref2.id, spotNumber = spotNumber, date = nextDate, startTime = "00:00", endTime = endTime, userId = userId, vehicleId = vehicleId, userName = userName, licensePlate = licensePlate, groupId = groupId)
            ref2.set(res2).await()
            reminderManager?.scheduleReminders(ref2.id, getMillis(nextDate, "00:00"), getMillis(nextDate, endTime))
        } else {
            val ref = db.collection("reservas").document()
            val newRes = Reservation(id = ref.id, spotNumber = spotNumber, date = date, startTime = startTime, endTime = endTime, userId = userId, vehicleId = vehicleId, userName = userName, licensePlate = licensePlate)
            ref.set(newRes).await()
            reminderManager?.scheduleReminders(ref.id, getMillis(date, startTime), getMillis(date, endTime))
        }
        addInternalNotification(NotificationType.SUCCESS, "notif_confirm_title", "notif_confirm_msg", listOf(spotNumber, date, startTime))
    }

    suspend fun updateReservation(reservationId: String, spotNumber: Int? = null, date: String? = null, startTime: String? = null, endTime: String? = null, vehicleId: String? = null, licensePlate: String? = null) {
        val current = _reservations.value.find { it.id == reservationId } ?: return
        val finalDate = date ?: current.date
        val finalStart = startTime ?: current.startTime
        val finalEnd = endTime ?: current.endTime

        val updates = mutableMapOf<String, Any?>()
        spotNumber?.let { updates["spotNumber"] = it }
        date?.let { updates["date"] = it }
        startTime?.let { updates["startTime"] = it }
        endTime?.let { updates["endTime"] = it }
        vehicleId?.let { updates["vehicleId"] = it }
        licensePlate?.let { updates["licensePlate"] = it }

        db.collection("reservas").document(reservationId).update(updates.filterValues { it != null }).await()
        reminderManager?.updateReminders(reservationId, getMillis(finalDate, finalStart), getMillis(finalDate, finalEnd))

        addInternalNotification(NotificationType.INFO, "notif_modified_title", "notif_modified_msg", listOf(spotNumber ?: current.spotNumber, finalDate, finalStart))
    }

    suspend fun deleteReservation(reservationId: String) {
        val current = _reservations.value.find { it.id == reservationId } ?: return
        if (current.groupId.isNotEmpty()) {
            val siblings = _reservations.value.filter { it.groupId == current.groupId && it.id != reservationId }
            for (sibling in siblings) {
                db.collection("reservas").document(sibling.id).delete().await()
                reminderManager?.cancelReminders(sibling.id)
            }
        }
        db.collection("reservas").document(reservationId).delete().await()
        reminderManager?.cancelReminders(reservationId)

        addInternalNotification(NotificationType.WARNING, "notif_cancelled_title", "notif_cancelled_msg", listOf(current.spotNumber, current.date))
    }

    suspend fun addVehicle(type: VehicleType, licensePlate: String) {
        val userId = _user.value?.id ?: return
        val normalizedPlate = licensePlate.trim().uppercase()
        val id = UUID.randomUUID().toString()
        val newVehicle = Vehicle(id = id, userId = userId, type = type, licensePlate = normalizedPlate)
        db.collection("vehiculos").document(id).set(newVehicle).await()
    }

    suspend fun removeVehicle(vehicleId: String) {
        db.collection("vehiculos").document(vehicleId).delete().await()
    }

    suspend fun updateProfile(name: String, imageUri: String?) {
        val firebaseUser = auth.currentUser ?: return
        val profileUpdates = userProfileChangeRequest { displayName = name.trim() }
        firebaseUser.updateProfile(profileUpdates).await()
        val userId = firebaseUser.uid
        val updates = mutableMapOf<String, Any>("name" to name.trim())
        imageUri?.let { updates["profileImage"] = it }
        db.collection("usuarios").document(userId).update(updates).await()
        _user.value = _user.value?.copy(name = name.trim(), profileImage = imageUri ?: _user.value?.profileImage)
    }

    suspend fun addReport(spotNumber: Int?, title: String, description: String) {
        val userId = _user.value?.id ?: return
        val id = UUID.randomUUID().toString()
        val report = Report(id = id, userId = userId, spotNumber = spotNumber, title = title, description = description, timestamp = Timestamp.now(), status = "PENDING")
        db.collection("reportes").document(id).set(report).await()
        addInternalNotification(NotificationType.SUCCESS, "notif_report_sent_title", "notif_report_sent_msg")
    }
}
