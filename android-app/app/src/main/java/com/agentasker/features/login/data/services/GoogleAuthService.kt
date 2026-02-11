package com.agentasker.features.login.data.services

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.agentasker.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Servicio especializado para manejar la autenticación con Google
 * usando Credential Manager y Firebase Auth.
 */
class GoogleAuthService(
    private val credentialManager: CredentialManager,
    private val firebaseAuth: FirebaseAuth
) {

    /**
     * Inicia el flujo de autenticación con Google.
     *
     * @param context Contexto de la aplicación
     * @return ID Token de Google si la autenticación es exitosa
     * @throws Exception si hay algún error en el proceso
     */
    suspend fun signInWithGoogle(context: Context): String {
        android.util.Log.d("GoogleAuthService", "Iniciando signInWithGoogle")

        // 1. Configurar la opción de ID de Google
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Mostrar todas las cuentas disponibles
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true) // Auto-seleccionar si hay una sola cuenta
            .build()

        android.util.Log.d("GoogleAuthService", "GoogleIdOption configurada")

        // 2. Crear la solicitud para Credential Manager
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        android.util.Log.d("GoogleAuthService", "GetCredentialRequest creada, solicitando credenciales...")

        // 3. Obtener credenciales del usuario
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        android.util.Log.d("GoogleAuthService", "Credenciales obtenidas: ${credential.type}")

        // 4. Verificar que sea una credencial de Google
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            android.util.Log.d("GoogleAuthService", "ID Token extraído, autenticando en Firebase...")

            // 5. Autenticar en Firebase con el ID Token
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(authCredential).await()

            android.util.Log.d("GoogleAuthService", "Autenticación en Firebase exitosa: ${authResult.user?.email}")

            return idToken
        } else {
            android.util.Log.e("GoogleAuthService", "Tipo de credencial inesperado: ${credential.type}")
            throw IllegalStateException("Tipo de credencial inesperado: ${credential.type}")
        }
    }

    /**
     * Cierra la sesión del usuario en Firebase y limpia las credenciales almacenadas.
     *
     * @param context Contexto de la aplicación
     */
    suspend fun signOut(context: Context) {
        firebaseAuth.signOut()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}

