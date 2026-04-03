package com.example

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.model.User
import com.example.model.UserSession
import com.example.repository.UserSessionRepository
import com.example.security.JwtConfig
import com.example.utils.HttpException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.response.respondText
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun `http exception returns the expected response`() = testApplication {
        application {
            configureSerialization()
            configureStatusPages()
            routing {
                get("/error") {
                    throw HttpException(HttpStatusCode.BadRequest, "Invalid input")
                }
            }
        }

        val response = client.get("/error")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid input"))
    }

    @Test
    fun `protected route requires active session`() = testApplication {
        val userSessionRepository = TestUserSessionRepository()
        val user = User(
            id = 1,
            email = "test@example.com",
            passwordHash = "hash:pass123",
            status = UserStatus.ACTIVE,
            role = UserRole.VIEWER
        )
        val session = userSessionRepository.create(
            userId = user.id,
            refreshTokenHash = "hash:refresh",
            expiresAt = Instant.now().plusSeconds(600)
        )
        val accessToken = JwtConfig.createAccessToken(user, session.id).token

        application {
            configureSerialization()
            configureStatusPages()
            configureSecurity(userSessionRepository)
            routing {
                authenticate("auth-jwt") {
                    get("/protected") {
                        call.respondText("ok")
                    }
                }
            }
        }

        val allowedResponse = client.get("/protected") {
            headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, allowedResponse.status)

        userSessionRepository.deleteById(session.id)

        val deniedResponse = client.get("/protected") {
            headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, deniedResponse.status)
        assertTrue(deniedResponse.bodyAsText().contains("Access token is invalid or expired"))
    }
}

private class TestUserSessionRepository : UserSessionRepository {
    private val sessions = linkedMapOf<Long, UserSession>()
    private var nextId = 1L

    override fun findById(id: Long): UserSession? {
        return sessions[id]
    }

    override fun create(userId: Long, refreshTokenHash: String, expiresAt: Instant): UserSession {
        val session = UserSession(
            id = nextId,
            userId = userId,
            refreshTokenHash = refreshTokenHash,
            expiresAt = expiresAt
        )
        sessions[session.id] = session
        nextId += 1
        return session
    }

    override fun updateToken(sessionId: Long, refreshTokenHash: String, expiresAt: Instant): UserSession? {
        val session = sessions[sessionId] ?: return null
        val updatedSession = session.copy(
            refreshTokenHash = refreshTokenHash,
            expiresAt = expiresAt
        )
        sessions[sessionId] = updatedSession
        return updatedSession
    }

    override fun deleteById(sessionId: Long): Boolean {
        return sessions.remove(sessionId) != null
    }

    override fun deleteByUserId(userId: Long): Int {
        val sessionIds = sessions.values
            .filter { it.userId == userId }
            .map { it.id }

        sessionIds.forEach { sessions.remove(it) }
        return sessionIds.size
    }

    override fun deleteExpiredSessions(now: Instant): Int {
        val sessionIds = sessions.values
            .filter { !it.expiresAt.isAfter(now) }
            .map { it.id }

        sessionIds.forEach { sessions.remove(it) }
        return sessionIds.size
    }
}
