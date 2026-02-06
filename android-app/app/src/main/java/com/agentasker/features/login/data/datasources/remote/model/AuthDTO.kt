package com.agentasker.features.login.data.datasources.remote.model

import com.google.gson.annotations.SerializedName

data class AuthResponseDTO(
    @SerializedName("user")
    val user: UserDTO,
    @SerializedName("token")
    val token: AuthTokenDTO
)

data class UserDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("photoUrl")
    val photoUrl: String?,
    @SerializedName("emailVerified")
    val emailVerified: Boolean?
)

data class AuthTokenDTO(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("idToken")
    val idToken: String?,
    @SerializedName("refreshToken")
    val refreshToken: String?,
    @SerializedName("expiresIn")
    val expiresIn: Long
)

data class GoogleSignInRequestDTO(
    @SerializedName("idToken")
    val idToken: String
)

data class LoginRequestDTO(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class RegisterRequestDTO(
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class RegisterResponseDTO(
    @SerializedName("message")
    val message: String
)

data class LoginResponseDTO(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("user")
    val user: UserLoginDTO
)

data class UserLoginDTO(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String
)

data class ProfileResponseDTO(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String
)

