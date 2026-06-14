package com.lksnext.ParkingIMayordomo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lksnext.ParkingIMayordomo.data.AuthManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraer el título y el cuerpo (de 'notification' o del mapa 'data')
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Aviso de Parking"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Tienes una nueva actualización."

        // Sincronizar con la base de datos local/UI de la app
        AuthManager.addExternalNotification(title, body)

        // Mostrar la notificación en el sistema
        mostrarNotificacion(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Nuevo token generado: $token")
    }

    private fun mostrarNotificacion(title: String, body: String) {
        val channelId = "parking_alerts_channel"
        val context: Context = this.applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Avisos de Turnos",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Canal para notificar el inicio y fin de turnos de parking"
            notificationManager.createNotificationChannel(channel)
        }

        // Configurar el Intent para abrir la app al pulsar
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        builder.setContentTitle(title)
        builder.setContentText(body)
        builder.setAutoCancel(true)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setContentIntent(pendingIntent)

        // Lanzar notificación con un ID único basado en el tiempo
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
