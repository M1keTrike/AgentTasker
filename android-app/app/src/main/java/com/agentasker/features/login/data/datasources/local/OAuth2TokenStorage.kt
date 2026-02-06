package com.agentasker.features.login.data.datasources.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.openid.appauth.AuthState
import org.json.JSONException

class OAuth2TokenStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "oauth2_tokens_secure"
        private const val KEY_AUTH_STATE = "auth_state"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthState(authState: AuthState) {
        encryptedPrefs.edit()
            .putString(KEY_AUTH_STATE, authState.jsonSerializeString())
            .apply()
    }

    fun loadAuthState(): AuthState? {
        val json = encryptedPrefs.getString(KEY_AUTH_STATE, null) ?: return null
        return try {
            AuthState.jsonDeserialize(json)
        } catch (e: JSONException) {
            null
        }
    }

    fun clearAuthState() {
        encryptedPrefs.edit().clear().apply()
    }

    fun isAuthenticated(): Boolean {
        val authState = loadAuthState() ?: return false
        return authState.isAuthorized
    }

    fun getAccessToken(): String? {
        val authState = loadAuthState() ?: return null
        return if (authState.isAuthorized) authState.accessToken else null
    }

    fun getIdToken(): String? {
        val authState = loadAuthState() ?: return null
        return authState.idToken
    }
}

