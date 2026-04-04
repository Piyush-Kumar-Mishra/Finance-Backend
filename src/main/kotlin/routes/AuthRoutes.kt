package com.example.routes

import com.example.dto.LoginRequest
import com.example.dto.LogoutRequest
import com.example.dto.RefreshTokenRequest
import com.example.dto.SignupRequest
import com.example.dto.UpdateUserRoleRequest
import com.example.dto.UpdateUserStatusRequest
import com.example.repository.UserRepository
import com.example.security.AuthRateLimiter
import com.example.service.AuthService
import com.example.service.UserManagementService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(
    authService: AuthService,
    userManagementService: UserManagementService,
    userRepository: UserRepository,
    authRateLimiter: AuthRateLimiter
){
    route("/account"){
        post("/signup") {
            authRateLimiter.checkSignup(call.clientId())
            val request = call.receive<SignupRequest>()
            val response = authService.signup(request)
            call.respond(HttpStatusCode.Created, response)
        }
        post("/admin/signup") {
            val request = call.receive<SignupRequest>()
            val response = authService.createAdmin(request)
            call.respond(HttpStatusCode.Created, response)
        }
        post("/login"){
            authRateLimiter.checkLogin(call.clientId())
            val request = call.receive<LoginRequest>()
            call.respond(authService.login(request))
        }
        post("/refresh") {
            authRateLimiter.checkRefresh(call.clientId())
            val request = call.receive<RefreshTokenRequest>()
            call.respond(authService.refresh(request))
        }

        post("/logout") {
            val request = call.receive<LogoutRequest>()
            call.respond(authService.logout(request))
        }
    }

    authenticate("auth-jwt") {
        route("/users") {
            patch("/{id}/role") {
                val user = call.requireCurrentUser(userRepository)
                val userId = call.requireLongParameter("id", "Invalid user id")
                val request = call.receive<UpdateUserRoleRequest>()
                call.respond(userManagementService.updateUserRole(user, userId, request))
            }

            patch("/{id}/status") {
                val user = call.requireCurrentUser(userRepository)
                val userId = call.requireLongParameter("id", "Invalid user id")
                val request = call.receive<UpdateUserStatusRequest>()
                call.respond(userManagementService.updateUserStatus(user, userId, request))
            }
        }
    }
}

private fun ApplicationCall.clientId(): String {
    val forwardedFor = request.headers["X-Forwarded-For"]
        ?.substringBefore(",")
        ?.trim()

    return forwardedFor?.takeIf { it.isNotBlank() }
        ?: request.origin.remoteHost
}
