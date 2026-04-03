package com.example.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.exceptions.JWTVerificationException
import com.example.model.User
import io.github.cdimascio.dotenv.dotenv
import java.time.Duration
import java.time.Instant
import java.util.Date

data class TokenDetails(
    val token: String,
    val expiresAt: Instant
)

data class RefreshTokenClaims(
    val userId: Long,
    val sessionId: Long
)

object JwtConfig {
    private val env = dotenv()

    private fun requireEnv(key: String): String =
        env[key] ?: throw IllegalStateException("$key is missing from .env")

    val secret: String = requireEnv("JWT_SECRET")
    val issuer: String = requireEnv("JWT_ISSUER")
    val audience: String = requireEnv("JWT_AUDIENCE")

    val accessTokenTtl: Duration =
        Duration.ofMinutes(requireEnv("JWT_ACCESS_EXPIRATION").toLong())

    val refreshTokenTtl: Duration =
        Duration.ofDays(requireEnv("JWT_REFRESH_EXPIRATION").toLong())

    const val ACCESS_TOKEN_TYPE = "access"
    const val REFRESH_TOKEN_TYPE = "refresh"

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun createAccessToken(
        user: User,
        sessionId: Long,
        now: Instant = Instant.now()
    ): TokenDetails {
        val expiresAt = now.plus(accessTokenTtl)

        val token = createTokenBuilder(now, expiresAt)
            .withSubject(user.id.toString())
            .withClaim("sid", sessionId)
            .withClaim("type", ACCESS_TOKEN_TYPE)
            .sign(algorithm)

        return TokenDetails(token, expiresAt)
    }

    fun createRefreshToken(
        user: User,
        sessionId: Long,
        now: Instant = Instant.now()
    ): TokenDetails {
        val expiresAt = now.plus(refreshTokenTtl)

        val token = createTokenBuilder(now, expiresAt)
            .withSubject(user.id.toString())
            .withClaim("sid", sessionId)
            .withClaim("type", REFRESH_TOKEN_TYPE)
            .sign(algorithm)

        return TokenDetails(token, expiresAt)
    }

    fun verifyRefreshToken(token: String): RefreshTokenClaims? = try {
        readRefreshTokenClaims(verifier.verify(token))
    } catch (_: JWTVerificationException) {
        null
    }

    fun decodeRefreshToken(token: String): RefreshTokenClaims? = try {
        readRefreshTokenClaims(JWT.decode(token))
    } catch (_: JWTDecodeException) {
        null
    }

    fun isTokenExpired(token: String): Boolean = try {
        val expiresAt = JWT.decode(token).expiresAt?.toInstant() ?: return false
        !expiresAt.isAfter(Instant.now())
    } catch (_: JWTDecodeException) {
        false
    }

    private fun readRefreshTokenClaims(decodedJwt: DecodedJWT): RefreshTokenClaims? {
        val type = decodedJwt.getClaim("type").asString()
        val userId = decodedJwt.subject?.toLongOrNull()
        val sessionId = decodedJwt.getClaim("sid").asLong()

        if (type != REFRESH_TOKEN_TYPE || userId == null || sessionId == null) {
            return null
        }

        return RefreshTokenClaims(
            userId = userId,
            sessionId = sessionId
        )
    }

    private fun createTokenBuilder(now: Instant, expiresAt: Instant) = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withIssuedAt(Date.from(now))
        .withExpiresAt(Date.from(expiresAt))
}
