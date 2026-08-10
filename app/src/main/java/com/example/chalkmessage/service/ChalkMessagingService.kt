package com.example.chalkmessage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.chalkmessage.R
import com.example.chalkmessage.data.local.AppDatabase
import com.example.chalkmessage.data.remote.FirebaseRepository
import com.example.chalkmessage.widget.WidgetUpdater
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChalkMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // FCM data payload contains the message info
        val messageId = remoteMessage.data["messageId"]
        val senderId = remoteMessage.data["senderId"]

        if (messageId != null) {
            // 1. Fetch full message from Firestore and save to Room
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(applicationContext)
                val firebaseRepo = FirebaseRepository()

                // In a real app, you'd fetch the specific message from Firestore here
                // For MVP, the Firestore listener in the repository will pick it up

                // 2. Update the widget
                WidgetUpdater.updateAllWidgets(applicationContext)

                // 3. Show notification
                showNotification("New chalk message!", "Someone sent you a drawing")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send this token to your server/Firestore so you can target this device
        // In MVP: store it in Firestore under the user's document
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "chalk_messages"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chalk Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
