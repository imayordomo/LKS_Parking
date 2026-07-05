package com.lksnext.ParkingIMayordomo.data

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.perf.metrics.AddTrace
import com.lksnext.ParkingIMayordomo.data.model.*
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils
import com.lksnext.ParkingIMayordomo.utils.ReservationReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
@Suppress("StaticFieldLeak")
object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var reminderManager: ReservationReminderManager? = null
    
    private const val CORPORATE_DOMAIN = "@lksnext.com"
    private const val COLLECTION_USERS = "usuarios"
    private const val COLLECTION_RESERVATIONS = "reservas"
    private const val COLLECTION_NOTIFICATIONS = "notificaciones"
    private const val FIELD_FCM_TOKEN = "fcmToken"
    private const val FIELD_READ = "read"
    private const val FIELD_USER_ID = "userId"
    private const val COLLECTION_VEHICLES = "vehiculos"
    private const val COLLECTION_REPORTS = "reportes"
    
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _allReservations = MutableStateFlow<List<Reservation>>(emptyList())
    val allReservations: StateFlow<List<Reservation>> = _allReservations.asStateFlow()

    private val _allReservationsReady = MutableStateFlow(false)
    val allReservationsReady: StateFlow<Boolean> = _allReservationsReady.asStateFlow()

    private var allReservationsStarted = false

    private val _vehicles = MutableStateFlow<List<Vehicle>?>(null)
    val vehicles: StateFlow<List<Vehicle>?> = _vehicles.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()

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
            db.collection(COLLECTION_USERS).document(userId)
                .addSnapshotListener { snapshot, _ ->
                    val firestoreUser = snapshot?.toObject<User>()
                    if (firestoreUser != null) {
                        _user.value = _user.value?.copy(
                            name = firestoreUser.name,
                            profileImage = firestoreUser.profileImage
                        )
                    }
                }
        )

        activeListeners.add(
            db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .addSnapshotListener { snapshot, _ ->
                    _reservations.value = snapshot?.documents?.mapNotNull { it.toObject<Reservation>() } ?: emptyList()
                    rescheduleAllReminders()
                }
        )

        activeListeners.add(
            db.collection(COLLECTION_VEHICLES)
                .whereEqualTo(FIELD_USER_ID, userId)
                .addSnapshotListener { snapshot, _ ->
                    _vehicles.value = snapshot?.documents?.mapNotNull { it.toObject<Vehicle>() } ?: emptyList()
                }
        )

        activeListeners.add(
            db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS)
                .orderBy("time", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    _notifications.value = snapshot?.documents?.mapNotNull { it.toObject<Notification>() } ?: emptyList()
                }
        )

        activeListeners.add(
            db.collection(COLLECTION_REPORTS)
                .whereEqualTo(FIELD_USER_ID, userId)
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
            db.collection(COLLECTION_RESERVATIONS)
                .addSnapshotListener { snapshot, _ ->
                    _allReservations.value = snapshot?.documents?.mapNotNull { it.toObject<Reservation>() } ?: emptyList()
                    _allReservationsReady.value = true
                }
        )
    }

    @AddTrace(name = "login_trace")
    suspend fun login(email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        if (!isEmailAuthorized(normalizedEmail)) throw Exception("error_corporate_only")
        try {
            val result = auth.signInWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = result.user ?: throw Exception("error_invalid_credentials")
            val userId = firebaseUser.uid
            val userDoc = db.collection(COLLECTION_USERS).document(userId).get().await()
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

    @AddTrace(name = "register_trace")
    suspend fun register(name: String, email: String, password: String) {
        val normalizedEmail = email.trim().lowercase()
        if (!isEmailAuthorized(normalizedEmail)) throw Exception("error_corporate_only")
        try {
            val result = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed")
            val profileUpdates = userProfileChangeRequest { displayName = name.trim() }
            firebaseUser.updateProfile(profileUpdates).await()
            val userId = firebaseUser.uid
            val userProfile = User(id = userId, name = name.trim(), email = normalizedEmail, fcmToken = "")
            db.collection(COLLECTION_USERS).document(userId).set(userProfile).await()
            _user.value = userProfile
            refreshAllData()
            syncFcmToken()
            generateInitialNotifications(userId)
        } catch (e: Exception) { throw e }
    }

    @Suppress("DEPRECATION")
    fun syncFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = _user.value?.id ?: return@addOnCompleteListener
                db.collection(COLLECTION_USERS).document(userId).update(FIELD_FCM_TOKEN, token)
            }
        }
    }

    suspend fun updateFcmToken(token: String) {
        val userId = _user.value?.id ?: return
        db.collection(COLLECTION_USERS).document(userId).update(FIELD_FCM_TOKEN, token).await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val normalizedEmail = email.trim().lowercase()
        try {
            auth.sendPasswordResetEmail(normalizedEmail).await()
        } catch (e: Exception) { throw e }
    }

    fun logout() {
        clearListeners()
        allReservationsStarted = false
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
        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).document(id).set(newNotif).await()
    }

    suspend fun addInternalNotification(type: NotificationType, titleRes: String, messageRes: String, messageArgs: List<Any> = emptyList()) {
        val userId = _user.value?.id ?: return
        val id = UUID.randomUUID().toString()
        val newNotif = Notification(id = id, userId = userId, type = type, titleRes = titleRes, messageRes = messageRes, messageArgs = messageArgs, time = Date(), read = false)
        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).document(id).set(newNotif).await()
    }

    suspend fun addExternalNotification(title: String, message: String) {
        val userId = _user.value?.id ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
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
        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS)
            .document(id).set(newNotif).await()
    }

    suspend fun markAsRead(id: String) {
        val userId = _user.value?.id ?: return
        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).document(id).update(FIELD_READ, true).await()
    }

    suspend fun markAllAsRead() {
        val userId = _user.value?.id ?: return
        val batch = db.batch()
        val notifs = db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).whereEqualTo(FIELD_READ, false).get().await()
        for (doc in notifs) { batch.update(doc.reference, FIELD_READ, true) }
        batch.commit().await()
    }

    suspend fun deleteNotification(id: String) {
        val userId = _user.value?.id ?: return
        db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).document(id).delete().await()
    }

    suspend fun deleteAllNotifications() {
        val userId = _user.value?.id ?: return
        val batch = db.batch()
        val notifs = db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).get().await()
        for (doc in notifs) { batch.delete(doc.reference) }
        batch.commit().await()
    }

    private fun rescheduleAllReminders() {
        val now = System.currentTimeMillis()
        for (reservation in _reservations.value) {
            val startMillis = getMillis(reservation.date, reservation.startTime)
            if (startMillis > now) {
                reminderManager?.scheduleReminders(
                    reservation.id,
                    startMillis,
                    getMillis(reservation.date, reservation.endTime)
                )
            }
        }
    }

    private fun getMillis(date: String, time: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.parse("$date $time")?.time ?: 0L
    }

    @AddTrace(name = "add_reservation_trace")
    suspend fun addReservation(spotNumber: Int, date: String, startTime: String, endTime: String, vehicleId: String, licensePlate: String? = null) {
        val userId = _user.value?.id ?: return
        val userName = _user.value?.name

        if (ParkingUtils.isMidnightCrossing(startTime, endTime)) {
            val groupId = UUID.randomUUID().toString()
            val nextDate = ParkingUtils.addDays(date, 1)

            val ref1 = db.collection(COLLECTION_RESERVATIONS).document()
            val res1 = Reservation(id = ref1.id, spotNumber = spotNumber, date = date, startTime = startTime, endTime = "23:59", userId = userId, vehicleId = vehicleId, userName = userName, licensePlate = licensePlate, groupId = groupId)
            ref1.set(res1).await()
            reminderManager?.scheduleReminders(ref1.id, getMillis(date, startTime), getMillis(date, "23:59"))

            val ref2 = db.collection(COLLECTION_RESERVATIONS).document()
            val res2 = Reservation(id = ref2.id, spotNumber = spotNumber, date = nextDate, startTime = "00:00", endTime = endTime, userId = userId, vehicleId = vehicleId, userName = userName, licensePlate = licensePlate, groupId = groupId)
            ref2.set(res2).await()
            reminderManager?.scheduleReminders(ref2.id, getMillis(nextDate, "00:00"), getMillis(nextDate, endTime))
        } else {
            val ref = db.collection(COLLECTION_RESERVATIONS).document()
            val newRes = Reservation(id = ref.id, spotNumber = spotNumber, date = date, startTime = startTime, endTime = endTime, userId = userId, vehicleId = vehicleId, userName = userName, licensePlate = licensePlate)
            ref.set(newRes).await()
            reminderManager?.scheduleReminders(ref.id, getMillis(date, startTime), getMillis(date, endTime))
        }
        addInternalNotification(NotificationType.SUCCESS, "notif_confirm_title", "notif_confirm_msg", listOf(spotNumber, date, startTime))
    }

    @AddTrace(name = "update_reservation_trace")
    suspend fun updateReservation(reservationId: String, spotNumber: Int? = null, date: String? = null, startTime: String? = null, endTime: String? = null, vehicleId: String? = null, licensePlate: String? = null) {
        val current = _reservations.value.find { it.id == reservationId } ?: return
        val finalDate = date ?: current.date
        val finalStart = startTime ?: current.startTime
        val finalEnd = endTime ?: current.endTime
        val finalSpot = spotNumber ?: current.spotNumber
        val finalVehicle = vehicleId ?: current.vehicleId
        val finalPlate = licensePlate ?: current.licensePlate
        val userName = _user.value?.name

        // Delete existing reservation(s)
        if (current.groupId.isNotEmpty()) {
            val allInGroup = _reservations.value.filter { it.groupId == current.groupId }
            for (r in allInGroup) {
                db.collection(COLLECTION_RESERVATIONS).document(r.id).delete().await()
                reminderManager?.cancelReminders(r.id)
            }
        } else {
            db.collection(COLLECTION_RESERVATIONS).document(reservationId).delete().await()
            reminderManager?.cancelReminders(reservationId)
        }

        // Create new reservation(s) based on whether it crosses midnight
        if (ParkingUtils.isMidnightCrossing(finalStart, finalEnd)) {
            val groupId = UUID.randomUUID().toString()
            val nextDate = ParkingUtils.addDays(finalDate, 1)

            val ref1 = db.collection(COLLECTION_RESERVATIONS).document()
            val res1 = Reservation(id = ref1.id, spotNumber = finalSpot, date = finalDate, startTime = finalStart, endTime = "23:59", userId = current.userId, vehicleId = finalVehicle, userName = userName, licensePlate = finalPlate, groupId = groupId)
            ref1.set(res1).await()
            reminderManager?.scheduleReminders(ref1.id, getMillis(finalDate, finalStart), getMillis(finalDate, "23:59"))

            val ref2 = db.collection(COLLECTION_RESERVATIONS).document()
            val res2 = Reservation(id = ref2.id, spotNumber = finalSpot, date = nextDate, startTime = "00:00", endTime = finalEnd, userId = current.userId, vehicleId = finalVehicle, userName = userName, licensePlate = finalPlate, groupId = groupId)
            ref2.set(res2).await()
            reminderManager?.scheduleReminders(ref2.id, getMillis(nextDate, "00:00"), getMillis(nextDate, finalEnd))
        } else {
            val ref = db.collection(COLLECTION_RESERVATIONS).document()
            val res = Reservation(id = ref.id, spotNumber = finalSpot, date = finalDate, startTime = finalStart, endTime = finalEnd, userId = current.userId, vehicleId = finalVehicle, userName = userName, licensePlate = finalPlate)
            ref.set(res).await()
            reminderManager?.scheduleReminders(ref.id, getMillis(finalDate, finalStart), getMillis(finalDate, finalEnd))
        }

        addInternalNotification(NotificationType.INFO, "notif_modified_title", "notif_modified_msg", listOf(finalSpot, finalDate, finalStart))
    }

    @AddTrace(name = "delete_reservation_trace")
    suspend fun deleteReservation(reservationId: String) {
        val current = _reservations.value.find { it.id == reservationId } ?: return
        if (current.groupId.isNotEmpty()) {
            val siblings = _reservations.value.filter { it.groupId == current.groupId && it.id != reservationId }
            for (sibling in siblings) {
                db.collection(COLLECTION_RESERVATIONS).document(sibling.id).delete().await()
                reminderManager?.cancelReminders(sibling.id)
            }
        }
        db.collection(COLLECTION_RESERVATIONS).document(reservationId).delete().await()
        reminderManager?.cancelReminders(reservationId)

        addInternalNotification(NotificationType.WARNING, "notif_cancelled_title", "notif_cancelled_msg", listOf(current.spotNumber, current.date))
    }

    suspend fun addVehicle(type: VehicleType, licensePlate: String) {
        val userId = _user.value?.id ?: return
        val normalizedPlate = licensePlate.trim().uppercase()
        val existing = _vehicles.value?.any { it.licensePlate.equals(normalizedPlate, ignoreCase = true) }
        if (existing == true) throw Exception("error_license_plate_exists")
        val id = UUID.randomUUID().toString()
        val newVehicle = Vehicle(id = id, userId = userId, type = type, licensePlate = normalizedPlate)
        db.collection(COLLECTION_VEHICLES).document(id).set(newVehicle).await()
    }

    suspend fun removeVehicle(vehicleId: String) {
        db.collection(COLLECTION_VEHICLES).document(vehicleId).delete().await()
    }

    @AddTrace(name = "update_profile_trace")
    suspend fun updateProfile(name: String, profileImage: String?, updateImage: Boolean = false) {
        val firebaseUser = auth.currentUser ?: return
        val profileUpdates = userProfileChangeRequest { displayName = name.trim() }
        firebaseUser.updateProfile(profileUpdates).await()
        val userId = firebaseUser.uid
        val updates = mutableMapOf<String, Any>("name" to name.trim())
        if (updateImage) {
            if (profileImage == null) {
                updates["profileImage"] = com.google.firebase.firestore.FieldValue.delete()
            } else {
                updates["profileImage"] = profileImage
            }
        }
        db.collection(COLLECTION_USERS).document(userId).update(updates).await()
        _user.value = _user.value?.copy(
            name = name.trim(),
            profileImage = if (updateImage) profileImage else _user.value?.profileImage
        )
    }

    suspend fun addReport(spotNumber: Int?, title: String, description: String) {
        val userId = _user.value?.id ?: return
        val id = UUID.randomUUID().toString()
        val report = Report(id = id, userId = userId, spotNumber = spotNumber, title = title, description = description, timestamp = Timestamp.now(), status = ReportStatus.PENDING)
        db.collection(COLLECTION_REPORTS).document(id).set(report).await()
        addInternalNotification(NotificationType.SUCCESS, "notif_report_sent_title", "notif_report_sent_msg")
    }

    @AddTrace(name = "delete_account_trace")
    suspend fun deleteAccount() {
        val userId = _user.value?.id ?: return
        clearListeners()
        allReservationsStarted = false

        val vehiclesSnapshot = db.collection(COLLECTION_VEHICLES).whereEqualTo(FIELD_USER_ID, userId).get().await()
        for (doc in vehiclesSnapshot.documents) { doc.reference.delete().await() }

        val reservationsSnapshot = db.collection(COLLECTION_RESERVATIONS).whereEqualTo(FIELD_USER_ID, userId).get().await()
        for (doc in reservationsSnapshot.documents) { doc.reference.delete().await() }

        val reportsSnapshot = db.collection(COLLECTION_REPORTS).whereEqualTo(FIELD_USER_ID, userId).get().await()
        for (doc in reportsSnapshot.documents) { doc.reference.delete().await() }

        val notifSnapshot = db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS).get().await()
        for (doc in notifSnapshot.documents) { doc.reference.delete().await() }

        db.collection(COLLECTION_USERS).document(userId).delete().await()

        auth.currentUser?.delete()?.await()

        auth.signOut()
        _user.value = null
        _reservations.value = emptyList()
        _allReservations.value = emptyList()
        _vehicles.value = null
        _notifications.value = emptyList()
        _reports.value = emptyList()
    }
}
