package com.dev_hss.customerapptocall

import android.app.PendingIntent
import android.net.Uri
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MessagingService : FirebaseMessagingService() {


    private lateinit var pendingIntent: PendingIntent
    private var soundUri: Uri? = null
    private val TAG = "MessagingService"
    private var notificationCount = 0

    private lateinit var brocast: LocalBroadcastManager
    var orderId: String = ""


    override fun onCreate() {
        super.onCreate()
        brocast = LocalBroadcastManager.getInstance(this)
        Log.d(TAG, "Service Create")


    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("fcm token:", token)
    }

    //generate the notification
    //attach the notification created with the custom layout
    //show the notification

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "onMessageReceived:notification?.body = ${message.notification?.body}")
        Log.d(TAG, "onMessageReceived.notification?.channelId = ${message.notification?.channelId}")
        Log.d(TAG, "onMessageReceived.notification.title = ${message.notification?.title}")

        // Check if message contains a notification payload.
        Log.d(TAG, "onMessageReceived.data: ${message.data}")


        if (message.data.isNotEmpty()) {
            // Handle data payload.
            //handleDataPayload(message.getData());
            Log.d(TAG, "onMessageReceived: ${message.data}")
        }

        // Check if the message contains a notification payload.
        if (message.notification != null) {
            // Handle notification payload.
            //handleNotificationPayload(message.getNotification());
            Log.d(TAG, "onMessageReceived: ${message.notification?.body}")

        }
//        if (message.notification != null) {
//            if (message.data.isNotEmpty() && message.data["type"] != null) {
//                generateAdminNotification(
//                    title = message.notification!!.title!!,
//                    message = message.notification!!.body!!,
//                    data = message.data
//                )
//            } else {
//                generateNotification(
//                    title = message.notification!!.title!!,
//                    message = message.notification!!.body!!,
//                    data = message.data
//                )
//            }
//        } else {
//            Log.d(TAG, "onMessageReceived: $message")
//        }
    }
}