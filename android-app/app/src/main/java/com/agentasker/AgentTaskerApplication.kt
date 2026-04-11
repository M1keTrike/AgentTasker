package com.agentasker

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.agentasker.core.hardware.NotificationHelper
import com.agentasker.core.notifications.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AgentTaskerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        fetchInitialFcmToken()
    }

    /**
     * Obtiene el token FCM actual del dispositivo y lo pasa al repositorio.
     * `onNewToken` del [com.agentasker.core.notifications.AgentTaskerMessagingService]
     * solo se dispara al rotar, por lo que en primer arranque hay que pedirlo.
     */
    private fun fetchInitialFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "Token FCM obtenido: $token")
                fcmTokenRepository.saveToken(token)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "No se pudo obtener el token FCM", e)
            }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        private const val TAG = "AgentTaskerApp"
    }
}
