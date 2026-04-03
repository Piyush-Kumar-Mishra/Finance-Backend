package com.example.service

import com.example.db.schemas.UserStatus
import com.example.dto.AuthResponse
import com.example.dto.LoginRequest
import com.example.dto.LogoutRequest
import com.example.dto.RefreshTokenRequest
import com.example.dto.SignupRequest
import com.example.model.User
import com.example.repository.UserRepository
import com.example.repository.UserSessionRepository
import com.example.security.JwtConfig
import com.example.security.PasswordHasher
import com.example.security.RefreshTokenClaims
import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.time.Instant
import java.util.UUID

class AuthService(
    private val userRepository: UserRepository,
    private val userSessionRepository: UserSessionRepository,
    private val passwordHasher: PasswordHasher,
){
    companion object {
        private const val PASSWORD_MIN_LENGTH = 6
        private const val PASSWORD_MAX_LENGTH = 30
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val EXPIRED_SESSION_MESSAGE = "Session expired, please login again"
        private const val INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token"
    }

    fun signup(request: SignupRequest): AuthResponse{
        val email = request.email.trim()
        val password = request.password.trim()
        validateEmail(email)
        validatePassword(password)
        val passwordHash = passwordHasher.hash(password)

        if(userRepository.findByEmail(email) !=null){
            throw HttpException(HttpStatusCode.Conflict, "Email is already registered")
        }

        val user = try{
            userRepository.create(email, passwordHash)
        }
        catch (e: ExposedSQLException) {
                if (userRepository.findByEmail(email) != null) {
                    throw HttpException(HttpStatusCode.Conflict, "Email is already registered")
                }
                throw HttpException(HttpStatusCode.InternalServerError, "Failed to create user")
        }
        return issueAuthTokens(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim()
        val password = request.password.trim()
        validateEmail(email)
        validatePassword(password)

        val user = userRepository.findByEmail(email)
            ?: throw HttpException(HttpStatusCode.Unauthorized, "Invalid email or password")

        ensureUserIsActive(user)

        if (!passwordHasher.verify(password, user.passwordHash)) {
            throw HttpException(HttpStatusCode.Unauthorized, "Invalid email or password")
        }
        return issueAuthTokens(user)
    }

    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val rawRefreshToken = request.refreshToken.trim()
        if (rawRefreshToken.isBlank()) {
            throw HttpException(HttpStatusCode.BadRequest, "Refresh token is required")
        }

        val claims = getRefreshTokenClaims(rawRefreshToken)

        val session = userSessionRepository.findById(claims.sessionId)
            ?: throw HttpException(HttpStatusCode.Unauthorized, "Session not found, please login again")

        if (session.userId != claims.userId) {
            userSessionRepository.deleteById(session.id)
            throw HttpException(HttpStatusCode.Unauthorized, INVALID_REFRESH_TOKEN_MESSAGE)
        }

        if (!session.expiresAt.isAfter(Instant.now())) {
            userSessionRepository.deleteById(session.id)
            throw HttpException(HttpStatusCode.Unauthorized, EXPIRED_SESSION_MESSAGE)
        }

        if (!passwordHasher.verify(rawRefreshToken, session.refreshTokenHash)) {
            userSessionRepository.deleteById(session.id)
            throw HttpException(HttpStatusCode.Unauthorized, INVALID_REFRESH_TOKEN_MESSAGE)
        }

        val user = userRepository.findById(claims.userId)
            ?: throw HttpException(HttpStatusCode.Unauthorized, "User no longer exists")

        ensureUserIsActive(user)

        val now = Instant.now()
        val accessToken = JwtConfig.createAccessToken(user, session.id, now)
        val refreshToken = JwtConfig.createRefreshToken(user, session.id, now)

        userSessionRepository.updateToken(
            sessionId = session.id,
            refreshTokenHash = passwordHasher.hash(refreshToken.token),
            expiresAt = refreshToken.expiresAt
        )

        return AuthResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token
        )
    }

    fun logout(request: LogoutRequest): String {
        val rawRefreshToken = request.refreshToken.trim()
        if (rawRefreshToken.isBlank()) {
            throw HttpException(HttpStatusCode.BadRequest, "Refresh token is required")
        }
        val claims = JwtConfig.verifyRefreshToken(rawRefreshToken)
        if (claims == null) {
            val expiredClaims = getExpiredRefreshTokenClaims(rawRefreshToken)
            if (expiredClaims != null) {
                userSessionRepository.deleteById(expiredClaims.sessionId)
                return EXPIRED_SESSION_MESSAGE
            }
            throw HttpException(HttpStatusCode.Unauthorized, INVALID_REFRESH_TOKEN_MESSAGE)
        }

        val session = userSessionRepository.findById(claims.sessionId)
            ?: return "Session already logged out"

        if (session.userId != claims.userId || !passwordHasher.verify(rawRefreshToken, session.refreshTokenHash)) {
            userSessionRepository.deleteById(session.id)
            throw HttpException(HttpStatusCode.Unauthorized, INVALID_REFRESH_TOKEN_MESSAGE)
        }
        userSessionRepository.deleteById(session.id)
        return "Logged out successfully"
    }



    private fun validateEmail(email : String){
        if (email.isBlank()) {
            throw HttpException(HttpStatusCode.BadRequest, "Email is required")
        }
        if (!EMAIL_REGEX.matches(email)) {
            throw HttpException(HttpStatusCode.BadRequest, "Please provide a valid email address")
        }
    }

    private fun validatePassword(password: String){
        if(password.isBlank()){
            throw HttpException(HttpStatusCode.BadRequest,"Password is required")
        }
        if(password.length<PASSWORD_MIN_LENGTH){
            throw HttpException(HttpStatusCode.BadRequest,"Password must be at least 6 characters")
        }
        if(password.length>PASSWORD_MAX_LENGTH){
            throw HttpException(HttpStatusCode.BadRequest,"Password length exceeded")
        }
        if(!password.any{it.isDigit()}){
            throw HttpException(HttpStatusCode.BadRequest, "Password must contain at least one digit")
        }
    }
    private fun ensureUserIsActive(user: User) {
        if (user.status != UserStatus.ACTIVE) {
            throw HttpException(HttpStatusCode.Forbidden, "User account is inactive")
        }
    }

    private fun issueAuthTokens(user: User): AuthResponse {
        ensureUserIsActive(user)
        userSessionRepository.deleteExpiredSessions(Instant.now())
        userSessionRepository.deleteByUserId(user.id)

        val now = Instant.now()
        val placeholderHash = passwordHasher.hash(UUID.randomUUID().toString())
        val initialRefreshExpiry = now.plus(JwtConfig.refreshTokenTtl)

        val session = userSessionRepository.create(
            userId = user.id,
            refreshTokenHash = placeholderHash,
            expiresAt = initialRefreshExpiry
        )

        val accessToken = JwtConfig.createAccessToken(user, session.id, now)
        val refreshToken = JwtConfig.createRefreshToken(user, session.id, now)

        userSessionRepository.updateToken(
            sessionId = session.id,
            refreshTokenHash = passwordHasher.hash(refreshToken.token),
            expiresAt = refreshToken.expiresAt
        )
        return AuthResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token
        )
    }

    private fun getRefreshTokenClaims(rawRefreshToken: String): RefreshTokenClaims {
        val claims = JwtConfig.verifyRefreshToken(rawRefreshToken)
        if (claims != null) {
            return claims
        }

        val expiredClaims = getExpiredRefreshTokenClaims(rawRefreshToken)
        if (expiredClaims != null) {
            userSessionRepository.deleteById(expiredClaims.sessionId)
            throw HttpException(HttpStatusCode.Unauthorized, EXPIRED_SESSION_MESSAGE)
        }

        throw HttpException(HttpStatusCode.Unauthorized, INVALID_REFRESH_TOKEN_MESSAGE)
    }

    private fun getExpiredRefreshTokenClaims(rawRefreshToken: String): RefreshTokenClaims? {
        val claims = JwtConfig.decodeRefreshToken(rawRefreshToken) ?: return null
        if (!JwtConfig.isTokenExpired(rawRefreshToken)) {
            return null
        }
        return claims
    }
}
