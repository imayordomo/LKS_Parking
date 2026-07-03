package com.lksnext.ParkingIMayordomo.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting

class ReservationReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "ReservationReminderMgr"
        private const val THIRTY_MINUTES_MILLIS = 30 * 60 * 1000L
        private const val FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1000L
        
        private const val END_NOTIFICATION_ID_OFFSET = 1000000 
    }

    @VisibleForTesting
    internal var currentTimeProvider: () -> Long = { System.currentTimeMillis() }

    @VisibleForTesting
    internal var sdkVersionProvider: () -> Int = { Build.VERSION.SDK_INT }

    fun scheduleReminders(reservationId: String, startTimeMillis: Long, endTimeMillis: Long) {
        val startReminderTime = startTimeMillis - THIRTY_MINUTES_MILLIS
        val endReminderTime = endTimeMillis - FIFTEEN_MINUTES_MILLIS

        val currentTime = currentTimeProvider()

        if (startReminderTime > currentTime) {
            val intent = createIntent(
                reservationId,
                "Tu reserva comienza en 30 minutos.",
                "Prepárate para llegar a tu plaza de parking.",
                getStartNotificationId(reservationId)
            )
            scheduleAlarm(startReminderTime, intent, getStartNotificationId(reservationId))
        }

        if (endReminderTime > currentTime) {
            val intent = createIntent(
                reservationId,
                "Tu reserva finalizará en 15 minutos.",
                "Recuerda retirar tu vehículo a tiempo.",
                getEndNotificationId(reservationId)
            )
            scheduleAlarm(endReminderTime, intent, getEndNotificationId(reservationId))
        }
    }

    fun cancelReminders(reservationId: String) {
        cancelAlarm(getStartNotificationId(reservationId))
        cancelAlarm(getEndNotificationId(reservationId))
    }

    fun updateReminders(reservationId: String, startTimeMillis: Long, endTimeMillis: Long) {
        cancelReminders(reservationId)
        scheduleReminders(reservationId, startTimeMillis, endTimeMillis)
    }

    @SuppressLint("NewApi")
    private fun scheduleAlarm(triggerAtMillis: Long, intent: Intent, requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            val sdkInt = sdkVersionProvider()
            if (sdkInt >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    Log.w(TAG, "Exact alarms not permitted, scheduled using setAndAllowWhileIdle()")
                }
            } else if (sdkInt >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.lksnext.ParkingIMayordomo.ACTION_NOTIFY_$requestCode"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun createIntent(reservationId: String, title: String, message: String, notificationId: Int): Intent {
        return Intent(context, NotificationReceiver::class.java).apply {
            action = "com.lksnext.ParkingIMayordomo.ACTION_NOTIFY_$notificationId"
            putExtra("reservation_id", reservationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("notification_id", notificationId)
        }
    }

    private fun getStartNotificationId(reservationId: String): Int {
        return reservationId.hashCode()
    }

    private fun getEndNotificationId(reservationId: String): Int {
        return reservationId.hashCode() + END_NOTIFICATION_ID_OFFSET
    }
}
