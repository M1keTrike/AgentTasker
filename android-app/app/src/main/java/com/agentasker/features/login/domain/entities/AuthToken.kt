package com.agentasker.features.login.domain.entities

data class AuthToken(
    val accessToken: String,
    val idToken: String?,
    val refreshToken: String?,
    val expiresIn: Long
)

