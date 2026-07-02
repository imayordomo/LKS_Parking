package com.lksnext.ParkingIMayordomo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

class ReservationReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "ReservationReminderMgr"
        private const val THIRTY_MINUTES_MILLIS = 30 * 60 * 1000L
        private const val FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1000L
        
        // Offset to distinguish start and end notification IDs for the same reservation
        private const val END_NOTIFICATION_ID_OFFSET = 1000000 
    }

    /**
     * Programa las notificaciones para una reserva.
     * @param reservationId ID único de la reserva.
     * @param startTimeMillis Fecha de inicio en milisegundos.
     * @param endTimeMillis Fecha de finalización en milisegundos.
     */
    fun scheduleReminders(reservationId: String, startTimeMillis: Long, endTimeMillis: Long) {
        val startReminderTime = startTimeMillis - THIRTY_MINUTES_MILLIS
        val endReminderTime = endTimeMillis - FIFTEEN_MINUTES_MILLIS

        val currentTime = System.currentTimeMillis()

        // Programar notificación de inicio (30 min antes)
        if (startReminderTime > currentTime) {
            val intent = createIntent(
                reservationId,
                "Tu reserva comienza en 30 minutos.",
                "Prepárate para llegar a tu plaza de parking.",
                getStartNotificationId(reservationId)
            )
            scheduleAlarm(startReminderTime, intent, getStartNotificationId(reservationId))
        }

        // Programar notificación de fin (15 min antes)
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

    /**
     * Cancela las alarmas programadas para una reserva.
     */
    fun cancelReminders(reservationId: String) {
        cancelAlarm(getStartNotificationId(reservationId))
        cancelAlarm(getEndNotificationId(reservationId))
    }

    /**
     * Actualiza las notificaciones cancelando las previas y programando las nuevas.
     */
    fun updateReminders(reservationId: String, startTimeMillis: Long, endTimeMillis: Long) {
        cancelReminders(reservationId)
        scheduleReminders(reservationId, startTimeMillis, endTimeMillis)
    }

    private fun scheduleAlarm(triggerAtMillis: Long, intent: Intent, requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact or request permission
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    Log.w(TAG, "Exact alarms not permitted, scheduled using set()")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
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
        val intent = Intent(context, NotificationReceiver::class.java)
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
