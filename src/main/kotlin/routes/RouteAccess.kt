package com.example.routes

import com.example.model.User
import com.example.repository.UserRepository
import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

fun ApplicationCall.requireCurrentUser(userRepository: UserRepository): User {
    val principal = principal<JWTPrincipal>()
        ?: throw HttpException(HttpStatusCode.Unauthorized, "Unauthorized")

    val userId = principal.payload.subject?.toLongOrNull()
        ?: throw HttpException(HttpStatusCode.Unauthorized, "Unauthorized")

    return userRepository.findById(userId)
        ?: throw HttpException(HttpStatusCode.Unauthorized, "User not found")
}

fun ApplicationCall.requireLongParameter(name: String, message: String): Long {
    return parameters[name]?.toLongOrNull()
        ?: throw HttpException(HttpStatusCode.BadRequest, message)
}
