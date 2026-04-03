package com.example.repository

import com.example.model.UserSession
import java.time.Instant

interface UserSessionRepository {
    fun findById(id: Long): UserSession?
    fun create(userId: Long, refreshTokenHash: String, expiresAt: Instant): UserSession
    fun updateToken(sessionId: Long, refreshTokenHash: String, expiresAt: Instant): UserSession?
    fun deleteById(sessionId: Long): Boolean
    fun deleteByUserId(userId: Long): Int
    fun deleteExpiredSessions(now: Instant): Int
}
