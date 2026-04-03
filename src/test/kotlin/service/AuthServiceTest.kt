package com.example.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.dto.LoginRequest
import com.example.dto.LogoutRequest
import com.example.dto.RefreshTokenRequest
import com.example.dto.SignupRequest
import com.example.model.User
import com.example.model.UserSession
import com.example.repository.UserRepository
import com.example.repository.UserSessionRepository
import com.example.security.JwtConfig
import com.example.security.PasswordHasher
import kotlin.math.max
import com.example.utils.HttpException
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class AuthServiceTest {

    private val passwordHasher = FakePasswordHasher()

    @Test
    fun `signup uses trimmed email and password`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        val response = authService.signup(
            SignupRequest(
                email = "  test@example.com  ",
                password = "  pass123  "
            )
        )

        val user = userRepository.findByEmail("test@example.com")

        assertNotNull(user)
        assertEquals("test@example.com", user.email)
        assertEquals(passwordHasher.hash("pass123"), user.passwordHash)
        assertTrue(response.accessToken.isNotBlank())
        assertTrue(response.refreshToken.isNotBlank())
        assertEquals(1, userSessionRepository.sessionCount())
    }

    @Test
    fun `signup returns conflict when email already exists`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        userRepository.saveUser(
            User(
                id = 1,
                email = "test@example.com",
                passwordHash = passwordHasher.hash("pass123"),
                status = UserStatus.ACTIVE,
                role = UserRole.VIEWER
            )
        )

        val exception = try {
            authService.signup(SignupRequest("test@example.com", "pass123"))
            null
        } catch (e: HttpException) {
            e
        }

        assertNotNull(exception)
        assertEquals("Email is already registered", exception.message)
    }

    @Test
    fun `login verifies trimmed password`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        userRepository.saveUser(
            User(
                id = 1,
                email = "test@example.com",
                passwordHash = passwordHasher.hash("pass123"),
                status = UserStatus.ACTIVE,
                role = UserRole.VIEWER
            )
        )

        val response = authService.login(
            LoginRequest(
                email = "  test@example.com  ",
                password = "  pass123  "
            )
        )

        assertTrue(response.accessToken.isNotBlank())
        assertTrue(response.refreshToken.isNotBlank())
        assertEquals(1, userSessionRepository.sessionCount())
    }

    @Test
    fun `login keeps only one refresh session for the user`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        userRepository.saveUser(
            User(
                id = 1,
                email = "test@example.com",
                passwordHash = passwordHasher.hash("pass123"),
                status = UserStatus.ACTIVE,
                role = UserRole.VIEWER
            )
        )

        val firstLogin = authService.login(LoginRequest("test@example.com", "pass123"))
        val secondLogin = authService.login(LoginRequest("test@example.com", "pass123"))

        assertEquals(1, userSessionRepository.sessionCount())
        assertTrue(secondLogin.refreshToken.isNotBlank())

        try {
            authService.refresh(RefreshTokenRequest(firstLogin.refreshToken))
            fail("Old refresh token should not work")
        } catch (e: HttpException) {
            assertEquals("Session not found, please login again", e.message)
        }
    }

    @Test
    fun `refresh keeps the stored refresh token in sync`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        val authResponse = authService.signup(SignupRequest("test@example.com", "pass123"))
        val refreshedResponse = authService.refresh(RefreshTokenRequest(authResponse.refreshToken))
        val session = userSessionRepository.findById(1)

        assertNotNull(session)
        assertTrue(passwordHasher.verify(refreshedResponse.refreshToken, session.refreshTokenHash))
        assertTrue(refreshedResponse.accessToken.isNotBlank())
    }

    @Test
    fun `expired refresh token removes session and asks user to login again`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        val authResponse = authService.signup(SignupRequest("test@example.com", "pass123"))
        val session = userSessionRepository.findById(1)

        assertNotNull(session)

        val expiredRefreshToken = JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .withIssuedAt(Date.from(Instant.now().minusSeconds(120)))
            .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
            .withSubject("1")
            .withClaim("sid", session.id)
            .withClaim("type", JwtConfig.REFRESH_TOKEN_TYPE)
            .sign(Algorithm.HMAC256(JwtConfig.secret))

        try {
            authService.refresh(RefreshTokenRequest(expiredRefreshToken))
            fail("Expired refresh token should not work")
        } catch (e: HttpException) {
            assertEquals("Session expired, please login again", e.message)
        }

        assertEquals(0, userSessionRepository.sessionCount())
    }

    @Test
    fun `logout removes the session`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        val authResponse = authService.signup(SignupRequest("test@example.com", "pass123"))
        val message = authService.logout(LogoutRequest(authResponse.refreshToken))

        assertEquals("Logged out successfully", message)
        assertEquals(0, userSessionRepository.sessionCount())
    }

    @Test
    fun `logout with expired refresh token clears session and asks user to login again`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        authService.signup(SignupRequest("test@example.com", "pass123"))
        val session = userSessionRepository.findById(1)

        assertNotNull(session)

        val expiredRefreshToken = JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .withIssuedAt(Date.from(Instant.now().minusSeconds(120)))
            .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
            .withSubject("1")
            .withClaim("sid", session.id)
            .withClaim("type", JwtConfig.REFRESH_TOKEN_TYPE)
            .sign(Algorithm.HMAC256(JwtConfig.secret))

        val message = authService.logout(LogoutRequest(expiredRefreshToken))

        assertEquals("Session expired, please login again", message)
        assertEquals(0, userSessionRepository.sessionCount())
    }

    @Test
    fun `create admin works when no admin exists`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        val response = authService.createAdmin(SignupRequest("admin@example.com", "pass123"))
        val user = userRepository.findByEmail("admin@example.com")

        assertNotNull(user)
        assertEquals(UserRole.ADMIN, user.role)
        assertTrue(response.accessToken.isNotBlank())
    }

    @Test
    fun `create admin fails when admin already exists`() {
        val userRepository = InMemoryUserRepository()
        val userSessionRepository = InMemoryUserSessionRepository()
        val authService = AuthService(userRepository, userSessionRepository, passwordHasher)

        userRepository.saveUser(
            User(
                id = 1,
                email = "admin@example.com",
                passwordHash = passwordHasher.hash("pass123"),
                status = UserStatus.ACTIVE,
                role = UserRole.ADMIN
            )
        )

        try {
            authService.createAdmin(SignupRequest("newadmin@example.com", "pass123"))
            fail("Expected admin creation to fail")
        } catch (e: HttpException) {
            assertEquals("Admin account already exists", e.message)
        }
    }
}

private class FakePasswordHasher : PasswordHasher {
    override fun hash(value: String): String = "hash:$value"

    override fun verify(value: String, hashedValue: String): Boolean {
        return hash(value) == hashedValue
    }
}

private class InMemoryUserRepository : UserRepository {
    private val users = linkedMapOf<Long, User>()
    private var nextId = 1L

    override fun findById(id: Long): User? {
        return users[id]
    }

    override fun findByEmail(email: String): User? {
        return users.values.singleOrNull { it.email == email }
    }

    override fun create(email: String, passwordHash: String): User {
        return create(email, passwordHash, UserRole.VIEWER)
    }

    override fun create(email: String, passwordHash: String, role: UserRole): User {
        val user = User(
            id = nextId,
            email = email,
            passwordHash = passwordHash,
            status = UserStatus.ACTIVE,
            role = role
        )
        users[user.id] = user
        nextId += 1
        return user
    }

    override fun updateRole(id: Long, role: UserRole): User? {
        val user = users[id] ?: return null
        val updatedUser = user.copy(role = role)
        users[id] = updatedUser
        return updatedUser
    }

    override fun updateStatus(id: Long, status: UserStatus): User? {
        val user = users[id] ?: return null
        val updatedUser = user.copy(status = status)
        users[id] = updatedUser
        return updatedUser
    }

    override fun adminExists(role: UserRole): Long {
        return users.values.count { it.role == role }.toLong()
    }

    fun saveUser(user: User) {
        users[user.id] = user
        nextId = max(nextId, user.id + 1)
    }
}

private class InMemoryUserSessionRepository : UserSessionRepository {
    private val sessions = linkedMapOf<Long, UserSession>()
    private var nextId = 1L

    override fun findById(id: Long): UserSession? {
        return sessions[id]
    }

    override fun create(userId: Long, refreshTokenHash: String, expiresAt: java.time.Instant): UserSession {
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

    override fun updateToken(
        sessionId: Long,
        refreshTokenHash: String,
        expiresAt: java.time.Instant
    ): UserSession? {
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

    override fun deleteExpiredSessions(now: java.time.Instant): Int {
        val expiredSessionIds = sessions.values
            .filter { !it.expiresAt.isAfter(now) }
            .map { it.id }

        expiredSessionIds.forEach { sessions.remove(it) }
        return expiredSessionIds.size
    }

    fun sessionCount(): Int {
        return sessions.size
    }
}
