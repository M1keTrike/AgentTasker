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
 *  4. Observar [SecureDataStoreTokenStorage.observeAuthToken] y reintentar
 *     el envío automáticamente en cuanto aparezca un accessToken válido.
 *     Esto cubre el caso en que el token FCM se obtiene antes del login.
 *
 * Nota: [AgentTaskerApi] se inyecta como [Lazy] porque este repositorio lo
 * usa también [AgentTaskerMessagingService], que puede construirse antes
 * que el resto del grafo Retrofit esté listo.
 */
@Singleton
class FcmTokenRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val apiLazy: Lazy<AgentTaskerApi>,
    private val tokenStorage: SecureDataStoreTokenStorage
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Serializa todas las llamadas a syncWithBackend para evitar POSTs paralelos
    // cuando fetchInitialFcmToken y onNewToken se disparan simultáneamente.
    private val syncMutex = Mutex()

    init {
        // Reintenta el sync en cuanto el usuario se autentica o refresca su token.
        // `.catch {}` evita que una excepción transitoria del DataStore
        // (decriptación, I/O) mate el observer de forma silenciosa.
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
     *
     * Protegido por [syncMutex] para evitar llamadas concurrentes cuando
     * múltiples triggers (fetchInitialFcmToken, onNewToken, observer) se
     * disparan al mismo tiempo.
     */
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
                    // 401: el usuario no está autenticado todavía; es normal,
                    // se reintentará automáticamente desde el observer de authToken.
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

/** DTO del endpoint `POST /users/fcm-token`. */
data class UpdateFcmTokenRequest(
    val fcmToken: String
)
