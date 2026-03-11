package com.agentasker.features.classroom.data.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.agentasker.BuildConfig
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ClassroomAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        private const val REDIRECT_URI = "com.agentasker:/oauth2redirect"
        private val CLASSROOM_SCOPES = listOf(
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
            "https://www.googleapis.com/auth/classroom.student-submissions.me.readonly"
        )
    }

    fun createAuthIntent(): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(AUTH_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT)
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            BuildConfig.GOOGLE_WEB_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
            .setScopes(CLASSROOM_SCOPES)
            .setAdditionalParameters(
                mapOf(
                    "access_type" to "offline",
                    "prompt" to "consent"
                )
            )
            .build()

        val authService = AuthorizationService(context)
        return authService.getAuthorizationRequestIntent(authRequest)
    }

    fun handleAuthResponse(data: Intent): String {
        val response = AuthorizationResponse.fromIntent(data)
        val error = AuthorizationException.fromIntent(data)

        if (response != null && response.authorizationCode != null) {
            return response.authorizationCode!!
        }

        throw error ?: IllegalStateException("No authorization code received")
    }
}
