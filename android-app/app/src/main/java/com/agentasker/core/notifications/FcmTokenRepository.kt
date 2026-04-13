package com.agentasker.core.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.login.data.datasources.local.SecureDataStoreTokenStorage
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val apiLazy: Lazy<AgentTaskerApi>,
    private val tokenStorage: SecureDataStoreTokenStorage
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val syncMutex = Mutex()

    init {
        scope.launch {
            tokenStorage.observeAuthToken()
                .distinctUntilChangedBy { it?.accessToken }
                .catch { e ->
                    Log.w(TAG, "observeAuthToken falló, observer se resetea: ${e.message}")
                }
                .collect { authToken ->
                    if (authToken?.accessToken != null && !isSynced()) {
                        Log.d(TAG, "Auth detectada, reintentando sync del token FCM")
                        syncWithBackend()
                    }
                }
        }
    }

    fun saveToken(token: String) {
        val previous = getToken()
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .apply()

        if (previous != token) {
            prefs.edit().putBoolean(KEY_SYNCED, false).apply()
        }

        syncWithBackend()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    private fun isSynced(): Boolean = prefs.getBoolean(KEY_SYNCED, false)

    fun syncWithBackend() {
        scope.launch {
            syncMutex.withLock {
                val token = getToken() ?: return@withLock
                if (isSynced()) return@withLock

                try {
                    val response = apiLazy.get().updateFcmToken(
                        UpdateFcmTokenRequest(fcmToken = token)
                    )
                    if (response.isSuccessful) {
                        prefs.edit().putBoolean(KEY_SYNCED, true).apply()
                        Log.d(TAG, "FCM token sincronizado con el backend")
                    } else {
                        Log.w(
                            TAG,
                            "Fallo al sincronizar token FCM: HTTP ${response.code()}"
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo enviar el token FCM al backend: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "FcmTokenRepository"
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_TOKEN = "fcm_token"
        private const val KEY_SYNCED = "fcm_token_synced"
    }
}

data class UpdateFcmTokenRequest(
    val fcmToken: String
)
