package com.example.model

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import java.time.Instant

data class User(
    val id: Long,
    val email: String,
    val passwordHash: String,
    val status: UserStatus,
    val role: UserRole
)

data class UserSession(
    val id: Long,
    val userId: Long,
    val refreshTokenHash: String,
    val expiresAt: Instant
)
