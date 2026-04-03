package com.example

import com.example.db.DatabaseFactory
import com.example.dto.ErrorResponse
import com.example.repository.UserRepository
import com.example.repository.UserRepositoryImpl
import com.example.repository.UserSessionRepository
import com.example.repository.UserSessionRepositoryImpl
import com.example.security.BCryptPasswordHasher
import com.example.security.JwtConfig
import com.example.service.AuthService
import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.jwt.JWTPrincipal
import com.example.routes.authRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import java.time.Instant

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    val userRepository: UserRepository = UserRepositoryImpl()
    val userSessionRepository: UserSessionRepository = UserSessionRepositoryImpl()
    val passwordHasher = BCryptPasswordHasher()
    val authService = AuthService(
        userRepository = userRepository,
        userSessionRepository = userSessionRepository,
        passwordHasher = passwordHasher
    )
    configureSerialization()
    configureStatusPages()
    configureSecurity(userSessionRepository)
    configureRouting()
    routing{
        authRoutes(authService)
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<HttpException> { call, exception ->
            call.respond(exception.statusCode, ErrorResponse(exception.message))
        }
    }
}

fun Application.configureSecurity(userSessionRepository: UserSessionRepository) {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JwtConfig.verifier)
            validate { credential ->
                val type = credential.payload.getClaim("type").asString()
                val userId = credential.payload.subject?.toLongOrNull()
                val sessionId = credential.payload.getClaim("sid").asLong()

                if (type != JwtConfig.ACCESS_TOKEN_TYPE || userId == null || sessionId == null) {
                    return@validate null
                }

                val session = userSessionRepository.findById(sessionId)
                    ?: return@validate null

                if (session.userId != userId || !session.expiresAt.isAfter(Instant.now())) {
                    return@validate null
                }

                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Access token is invalid or expired")
                )
            }
        }
    }
}
