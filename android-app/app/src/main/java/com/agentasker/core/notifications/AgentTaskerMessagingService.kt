package com.agentasker.core.notifications

import android.util.Log
import com.agentasker.core.hardware.HapticFeedbackManager
import com.agentasker.core.hardware.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AgentTaskerMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository

    @Inject
    lateinit var hapticFeedbackManager: HapticFeedbackManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        fcmTokenRepository.saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Mensaje FCM recibido de: ${message.from}")

        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: "AgentTasker"
        val body = notification?.body ?: message.data["body"] ?: ""

        hapticFeedbackManager.notification()
        notificationHelper.showPushNotification(
            title = title,
            body = body,
            data = message.data
        )
    }

    companion object {
        private const val TAG = "FCMService"
    }
}
