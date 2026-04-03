package com.example.repository

import com.example.db.schemas.UserSessions
import com.example.model.UserSession
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class UserSessionRepositoryImpl : UserSessionRepository {

    override fun findById(id: Long): UserSession? = transaction {
        UserSessions.selectAll()
            .where{ UserSessions.id eq id }
            .map {
                UserSession(
                    id = it[UserSessions.id],
                    userId = it[UserSessions.userId],
                    refreshTokenHash = it[UserSessions.refreshTokenHash],
                    expiresAt = it[UserSessions.expiresAt]
                )
            }
            .singleOrNull()
    }

    override fun create(
        userId: Long,
        refreshTokenHash: String,
        expiresAt: Instant
    ): UserSession = transaction {
        val sessionId = UserSessions.insert {
            it[UserSessions.userId] = userId
            it[UserSessions.refreshTokenHash] = refreshTokenHash
            it[UserSessions.expiresAt] = expiresAt
        } get UserSessions.id

        UserSession(
            id = sessionId,
            userId = userId,
            refreshTokenHash = refreshTokenHash,
            expiresAt = expiresAt
        )
    }

    override fun updateToken(
        sessionId: Long,
        refreshTokenHash: String,
        expiresAt: Instant
    ): UserSession? = transaction {

        val updatedRows = UserSessions.update({ UserSessions.id eq sessionId }) {
            it[UserSessions.refreshTokenHash] = refreshTokenHash
            it[UserSessions.expiresAt] = expiresAt
        }

        if (updatedRows == 0) return@transaction null

        UserSessions.selectAll()
            .where{UserSessions.id eq sessionId}
            .map {
                UserSession(
                    id = it[UserSessions.id],
                    userId = it[UserSessions.userId],
                    refreshTokenHash = it[UserSessions.refreshTokenHash],
                    expiresAt = it[UserSessions.expiresAt]
                )
            }
            .singleOrNull()
    }

    override fun deleteById(sessionId: Long): Boolean = transaction {
        UserSessions.deleteWhere { UserSessions.id eq sessionId } > 0
    }

    override fun deleteByUserId(userId: Long): Int = transaction {
        UserSessions.deleteWhere { UserSessions.userId eq userId }
    }

    override fun deleteExpiredSessions(now: Instant): Int = transaction {
        UserSessions.deleteWhere { UserSessions.expiresAt lessEq now }
    }
}
