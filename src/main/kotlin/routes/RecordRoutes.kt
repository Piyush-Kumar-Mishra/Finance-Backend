package com.example.routes

import com.example.repository.UserRepository
import com.example.service.UserRecordService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.recordRoutes(
    userRepository: UserRepository,
    userRecordService: UserRecordService
) {
    authenticate("auth-jwt") {
        route("/records") {
            get {
                val user = call.requireCurrentUser(userRepository)
                call.respond(userRecordService.getAllRecords(user))
            }

            get("/{id}") {
                val user = call.requireCurrentUser(userRepository)
                val id = call.requireLongParameter("id", "Invalid record id")
                call.respond(userRecordService.getRecordById(user, id))
            }

            post {
                val user = call.requireCurrentUser(userRepository)
                val request = call.receive<com.example.dto.CreateRecordRequest>()
                val response = userRecordService.createRecord(user, request)
                call.respond(HttpStatusCode.Created, response)
            }

            put("/{id}") {
                val user = call.requireCurrentUser(userRepository)
                val id = call.requireLongParameter("id", "Invalid record id")
                val request = call.receive<com.example.dto.UpdateRecordRequest>()
                call.respond(userRecordService.updateRecord(user, id, request))
            }

            delete("/{id}") {
                val user = call.requireCurrentUser(userRepository)
                val id = call.requireLongParameter("id", "Invalid record id")
                call.respond(userRecordService.deleteRecord(user, id))
            }
        }
    }
}