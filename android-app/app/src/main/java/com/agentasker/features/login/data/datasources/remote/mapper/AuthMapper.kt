package com.agentasker.features.login.data.datasources.remote.mapper

import com.agentasker.features.login.data.datasources.remote.model.AuthTokenDTO
import com.agentasker.features.login.data.datasources.remote.model.LoginResponseDTO
import com.agentasker.features.login.data.datasources.remote.model.UserDTO
import com.agentasker.features.login.domain.entities.AuthToken
import com.agentasker.features.login.domain.entities.User

fun UserDTO.toDomain(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        isEmailVerified = emailVerified ?: false
    )
}

fun AuthTokenDTO.toDomain(): AuthToken {
    return AuthToken(
        accessToken = accessToken,
        idToken = idToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn
    )
}

fun LoginResponseDTO.toDomainFromLogin(): User {
    return User(
        id = user.id.toString(),
        email = user.email,
        displayName = user.username,
        photoUrl = null,
        isEmailVerified = true
    )
}

