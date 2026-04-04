package com.example.security

import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class AuthRateLimiter(
    private val nowProvider: () -> Instant = { Instant.now() }
) {

    companion object {
        private val BLOCK_DURATION: Duration = Duration.ofMinutes(5)
    }

    private val attempts = ConcurrentHashMap<String, AuthRateLimitState>()

    fun checkSignup(clientId: String) {
        checkLimit(clientId, AuthRateLimitAction.SIGNUP)
    }

    fun checkLogin(clientId: String) {
        checkLimit(clientId, AuthRateLimitAction.LOGIN)
    }

    fun checkRefresh(clientId: String) {
        checkLimit(clientId, AuthRateLimitAction.REFRESH)
    }

    @Synchronized
    private fun checkLimit(clientId: String, action: AuthRateLimitAction) {
        val now = nowProvider()
        val key = "${action.name}:$clientId"
        val currentState = attempts[key]

        if (currentState != null && currentState.blockedUntil != null) {
            if (now.isBefore(currentState.blockedUntil)) {
                throw HttpException(
                    HttpStatusCode.TooManyRequests,
                    "Too many ${action.label} attempts. Try again after 5 minutes"
                )
            }

            attempts.remove(key)
        }

        val activeState = attempts[key]
        val state = if (activeState == null || !activeState.windowEndsAt.isAfter(now)) {
            AuthRateLimitState(
                requestCount = 0,
                windowEndsAt = now.plus(BLOCK_DURATION),
                blockedUntil = null
            )
        } else {
            activeState
        }

        val nextCount = state.requestCount + 1
        if (nextCount > action.maxAttempts) {
            attempts[key] = state.copy(
                requestCount = nextCount,
                blockedUntil = now.plus(BLOCK_DURATION)
            )
            throw HttpException(
                HttpStatusCode.TooManyRequests,
                "Too many ${action.label} attempts. Try again after 5 minutes"
            )
        }

        attempts[key] = state.copy(requestCount = nextCount)
    }
}

private data class AuthRateLimitState(
    val requestCount: Int,
    val windowEndsAt: Instant,
    val blockedUntil: Instant?
)

private enum class AuthRateLimitAction(
    val label: String,
    val maxAttempts: Int
) {
    SIGNUP("signup", 5),
    LOGIN("login", 5),
    REFRESH("refresh", 10)
}
