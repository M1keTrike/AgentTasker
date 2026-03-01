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


class GoogleAuthService(
    private val credentialManager: CredentialManager,
    private val firebaseAuth: FirebaseAuth
) {


    suspend fun signInWithGoogle(context: Context): String {
        android.util.Log.d("GoogleAuthService", "Iniciando signInWithGoogle")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .build()

        android.util.Log.d("GoogleAuthService", "GoogleIdOption configurada")

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        android.util.Log.d("GoogleAuthService", "GetCredentialRequest creada, solicitando credenciales...")

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        android.util.Log.d("GoogleAuthService", "Credenciales obtenidas: ${credential.type}")

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            android.util.Log.d("GoogleAuthService", "ID Token extraído, autenticando en Firebase...")

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(authCredential).await()

            android.util.Log.d("GoogleAuthService", "Autenticación en Firebase exitosa: ${authResult.user?.email}")

            return idToken
        } else {
            android.util.Log.e("GoogleAuthService", "Tipo de credencial inesperado: ${credential.type}")
            throw IllegalStateException("Tipo de credencial inesperado: ${credential.type}")
        }
    }


    suspend fun signOut(context: Context) {
        firebaseAuth.signOut()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}

