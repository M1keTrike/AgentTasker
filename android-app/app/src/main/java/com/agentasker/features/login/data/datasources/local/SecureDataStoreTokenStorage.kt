package com.agentasker.features.login.data.datasources.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentasker.features.login.domain.entities.AuthToken
import com.agentasker.features.login.domain.entities.User
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.util.Base64

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_datastore",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "auth_secure_prefs"
            )
        )
    }
)

class SecureDataStoreTokenStorage(private val context: Context) {

    private val aead: Aead by lazy { createAead() }

    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var cachedExpiresIn: Long = 0L
    @Volatile
    private var cachedTokenTimestamp: Long = 0L

    private val dataStore: DataStore<Preferences>
        get() = context.authDataStore

    private fun createAead(): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(Aead::class.java)
    }

    private fun encrypt(value: String): String {
        val ciphertext = aead.encrypt(
            value.toByteArray(StandardCharsets.UTF_8),
            ASSOCIATED_DATA
        )
        return Base64.getEncoder().encodeToString(ciphertext)
    }

    private fun decrypt(encrypted: String): String {
        val ciphertext = Base64.getDecoder().decode(encrypted)
        val plaintext = aead.decrypt(ciphertext, ASSOCIATED_DATA)
        return String(plaintext, StandardCharsets.UTF_8)
    }

    private fun encryptOrNull(value: String?): String? {
        return value?.let { encrypt(it) }
    }

    private fun decryptOrNull(value: String?): String? {
        return value?.let {
            try {
                decrypt(it)
            } catch (e: Exception) {
                it
            }
        }
    }

    suspend fun saveAuthToken(token: AuthToken) {
        val timestamp = System.currentTimeMillis()
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = encrypt(token.accessToken)
            prefs[KEY_ID_TOKEN] = encryptOrNull(token.idToken) ?: ""
            prefs[KEY_REFRESH_TOKEN] = encryptOrNull(token.refreshToken) ?: ""
            prefs[KEY_EXPIRES_IN] = token.expiresIn
            prefs[KEY_TOKEN_TIMESTAMP] = timestamp
        }
        cachedAccessToken = token.accessToken
        cachedExpiresIn = token.expiresIn
        cachedTokenTimestamp = timestamp
    }

    suspend fun getAuthToken(): AuthToken? {
        val prefs = dataStore.data.first()
        val encryptedAccessToken = prefs[KEY_ACCESS_TOKEN] ?: return null
        val accessToken = decryptOrNull(encryptedAccessToken) ?: return null

        val idToken = prefs[KEY_ID_TOKEN]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
        val refreshToken = prefs[KEY_REFRESH_TOKEN]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
        val expiresIn = prefs[KEY_EXPIRES_IN] ?: 0L

        return AuthToken(
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn
        )
    }

    fun observeAuthToken(): Flow<AuthToken?> {
        return dataStore.data.map { prefs ->
            val encryptedAccessToken = prefs[KEY_ACCESS_TOKEN] ?: return@map null
            val accessToken = decryptOrNull(encryptedAccessToken) ?: return@map null

            val idToken = prefs[KEY_ID_TOKEN]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
            val refreshToken = prefs[KEY_REFRESH_TOKEN]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
            val expiresIn = prefs[KEY_EXPIRES_IN] ?: 0L

            cachedAccessToken = accessToken
            cachedExpiresIn = expiresIn
            cachedTokenTimestamp = prefs[KEY_TOKEN_TIMESTAMP] ?: 0L

            AuthToken(
                accessToken = accessToken,
                idToken = idToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn
            )
        }
    }

    suspend fun saveUser(user: User) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = encrypt(user.id)
            prefs[KEY_USER_EMAIL] = encrypt(user.email)
            prefs[KEY_USER_DISPLAY_NAME] = encryptOrNull(user.displayName) ?: ""
            prefs[KEY_USER_PHOTO_URL] = encryptOrNull(user.photoUrl) ?: ""
            prefs[KEY_USER_EMAIL_VERIFIED] = user.isEmailVerified
        }
    }

    suspend fun getUser(): User? {
        val prefs = dataStore.data.first()
        val id = prefs[KEY_USER_ID]?.let { decryptOrNull(it) } ?: return null
        val email = prefs[KEY_USER_EMAIL]?.let { decryptOrNull(it) } ?: return null
        val displayName = prefs[KEY_USER_DISPLAY_NAME]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
        val photoUrl = prefs[KEY_USER_PHOTO_URL]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
        val emailVerified = prefs[KEY_USER_EMAIL_VERIFIED] ?: false

        return User(
            id = id,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            isEmailVerified = emailVerified
        )
    }

    fun observeUser(): Flow<User?> {
        return dataStore.data.map { prefs ->
            val id = prefs[KEY_USER_ID]?.let { decryptOrNull(it) } ?: return@map null
            val email = prefs[KEY_USER_EMAIL]?.let { decryptOrNull(it) } ?: return@map null
            val displayName = prefs[KEY_USER_DISPLAY_NAME]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
            val photoUrl = prefs[KEY_USER_PHOTO_URL]?.takeIf { it.isNotEmpty() }?.let { decryptOrNull(it) }
            val emailVerified = prefs[KEY_USER_EMAIL_VERIFIED] ?: false

            User(
                id = id,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                isEmailVerified = emailVerified
            )
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
        cachedAccessToken = null
        cachedExpiresIn = 0L
        cachedTokenTimestamp = 0L
    }

    suspend fun isTokenValid(): Boolean {
        val prefs = dataStore.data.first()
        val timestamp = prefs[KEY_TOKEN_TIMESTAMP] ?: return false
        val expiresIn = prefs[KEY_EXPIRES_IN] ?: return false
        return (System.currentTimeMillis() - timestamp) < (expiresIn * 1000)
    }

    fun getCachedAccessToken(): String? {
        val token = cachedAccessToken ?: return null
        val elapsed = System.currentTimeMillis() - cachedTokenTimestamp
        return if (elapsed < cachedExpiresIn * 1000) token else null
    }

    companion object {
        private const val KEYSET_NAME = "auth_datastore_keyset"
        private const val PREF_FILE_NAME = "auth_datastore_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://auth_datastore_master_key"
        private val ASSOCIATED_DATA = "agentasker_auth".toByteArray(StandardCharsets.UTF_8)

        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_ID_TOKEN = stringPreferencesKey("id_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES_IN = longPreferencesKey("expires_in")
        private val KEY_TOKEN_TIMESTAMP = longPreferencesKey("token_timestamp")

        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        private val KEY_USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
        private val KEY_USER_EMAIL_VERIFIED = booleanPreferencesKey("user_email_verified")
    }
}
