package com.agentasker.features.login.domain.entities

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean = false
)

