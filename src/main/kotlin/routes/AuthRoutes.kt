package com.example.routes

import com.example.dto.LoginRequest
import com.example.dto.LogoutRequest
import com.example.dto.RefreshTokenRequest
import com.example.dto.SignupRequest
import com.example.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService){
    route("/account"){
        post("/signup") {
            val request = call.receive<SignupRequest>()
            val response = authService.signup(request)
            call.respond(HttpStatusCode.Created, response)
        }
        post("/login"){
            val request = call.receive<LoginRequest>()
            call.respond(authService.login(request))
        }
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            call.respond(authService.refresh(request))
        }

        post("/logout") {
            val request = call.receive<LogoutRequest>()
            call.respond(authService.logout(request))
        }
    }
}
