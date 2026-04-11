package com.agentasker.core.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.agentasker.core.network.AgentTaskerApi
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el token FCM del dispositivo.
 *
 * Responsabilidades:
 *  1. Persistir el token localmente en SharedPreferences (por si la app se
 *     abre sin red y luego necesita reenviarlo).
 *  2. Registrar un flag `syncedWithBackend` para saber si el backend ya tiene
 *     el token actual.
 *  3. Enviar el token al backend mediante [AgentTaskerApi.updateFcmToken]
 *     cuando el usuario esté autenticado (el AuthInterceptor añade el Bearer).
 *
 * Nota: [AgentTaskerApi] se inyecta como [Lazy] porque este repositorio lo
 * usa también [AgentTaskerMessagingService], que puede construirse antes
 * que el resto del grafo Retrofit esté listo.
 */
@Singleton
class FcmTokenRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val apiLazy: Lazy<AgentTaskerApi>
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Persiste el token y dispara el envío al backend en segundo plano. */
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

    /** Devuelve el token local (puede ser null si FCM aún no lo entregó). */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** True si el backend ya recibió el token actual. */
    private fun isSynced(): Boolean = prefs.getBoolean(KEY_SYNCED, false)

    /**
     * Envía el token al backend si existe y aún no está sincronizado.
     * Se puede llamar manualmente tras un login exitoso para forzar el envío.
     */
    fun syncWithBackend() {
        val token = getToken() ?: return
        if (isSynced()) return

        scope.launch {
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
                // 401: el usuario no está autenticado todavía; es normal,
                // se reintentará tras el próximo login.
                Log.w(TAG, "No se pudo enviar el token FCM al backend: ${e.message}")
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

/** DTO del endpoint `POST /users/fcm-token`. */
data class UpdateFcmTokenRequest(
    val fcmToken: String
)
