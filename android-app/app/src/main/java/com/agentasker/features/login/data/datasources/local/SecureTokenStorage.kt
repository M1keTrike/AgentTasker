package com.agentasker.features.login.data.datasources.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.agentasker.features.login.domain.entities.AuthToken
import com.agentasker.features.login.domain.entities.User
import java.security.GeneralSecurityException

class SecureTokenStorage(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedPreferences()
    }

    /**
     * Crea EncryptedSharedPreferences con manejo de errores de corrupción.
     * Si los datos están corruptos, los elimina y crea nuevos preferences limpios.
     */
    private fun createEncryptedPreferences(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Error al crear EncryptedSharedPreferences (datos corruptos), limpiando...", e)
            // Los datos están corruptos, eliminar y crear nuevos
            deleteCorruptedPreferences()

            // Reintentar la creación
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al crear EncryptedSharedPreferences", e)
            // Si falla nuevamente, eliminar y reintentar una última vez
            deleteCorruptedPreferences()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * Elimina los archivos de SharedPreferences corruptos.
     */
    private fun deleteCorruptedPreferences() {
        try {
            // Eliminar el archivo de SharedPreferences
            val prefsDir = context.applicationContext.dataDir.resolve("shared_prefs")
            val prefsFile = prefsDir.resolve("$PREFS_NAME.xml")

            if (prefsFile.exists()) {
                val deleted = prefsFile.delete()
                Log.d(TAG, "Archivo de preferencias eliminado: $deleted (${prefsFile.absolutePath})")
            }

            // También eliminar el master key si existe
            val masterKeyFile = prefsDir.resolve("${PREFS_NAME}_master_key.xml")
            if (masterKeyFile.exists()) {
                val deleted = masterKeyFile.delete()
                Log.d(TAG, "Archivo de master key eliminado: $deleted (${masterKeyFile.absolutePath})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar archivos corruptos", e)
        }
    }

    fun saveAuthToken(token: AuthToken) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, token.accessToken)
            putString(KEY_ID_TOKEN, token.idToken)
            putString(KEY_REFRESH_TOKEN, token.refreshToken)
            putLong(KEY_EXPIRES_IN, token.expiresIn)
            putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    fun getAuthToken(): AuthToken? {
        val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val idToken = sharedPreferences.getString(KEY_ID_TOKEN, null)
        val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        val expiresIn = sharedPreferences.getLong(KEY_EXPIRES_IN, 0L)

        return AuthToken(
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn
        )
    }

    fun saveUser(user: User) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_DISPLAY_NAME, user.displayName)
            putString(KEY_USER_PHOTO_URL, user.photoUrl)
            putBoolean(KEY_USER_EMAIL_VERIFIED, user.isEmailVerified)
            apply()
        }
    }

    fun getUser(): User? {
        val id = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        val email = sharedPreferences.getString(KEY_USER_EMAIL, null) ?: return null
        val displayName = sharedPreferences.getString(KEY_USER_DISPLAY_NAME, null)
        val photoUrl = sharedPreferences.getString(KEY_USER_PHOTO_URL, null)
        val emailVerified = sharedPreferences.getBoolean(KEY_USER_EMAIL_VERIFIED, false)

        return User(
            id = id,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            isEmailVerified = emailVerified
        )
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun isTokenValid(): Boolean {
        val timestamp = sharedPreferences.getLong(KEY_TOKEN_TIMESTAMP, 0L)
        val expiresIn = sharedPreferences.getLong(KEY_EXPIRES_IN, 0L)
        val currentTime = System.currentTimeMillis()

        return (currentTime - timestamp) < (expiresIn * 1000)
    }

    companion object {
        private const val TAG = "SecureTokenStorage"
        private const val PREFS_NAME = "auth_secure_prefs"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_TOKEN_TIMESTAMP = "token_timestamp"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
        private const val KEY_USER_PHOTO_URL = "user_photo_url"
        private const val KEY_USER_EMAIL_VERIFIED = "user_email_verified"
    }
}

