package com.example.dto


import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class UpdateUserRoleRequest(
    val role: String
)

@Serializable
data class UpdateUserStatusRequest(
    val status: String
)

@Serializable
data class UserRoleResponse(
    val id: Long,
    val email: String,
    val role: String,
    val status: String
)

@Serializable
data class ErrorResponse(
    val message: String
)
