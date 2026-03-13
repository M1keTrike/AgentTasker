package com.agentasker.features.login.domain.services

import android.content.Context

interface GoogleSignInProvider {
    suspend fun signInWithGoogle(context: Context): String
    suspend fun signOut(context: Context)
}
