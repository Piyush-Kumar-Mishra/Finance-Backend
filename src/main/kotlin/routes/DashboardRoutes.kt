package com.example.routes

import com.example.repository.UserRepository
import com.example.service.DashboardService
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.dashboardRoutes(
    userRepository: UserRepository,
    dashboardService: DashboardService
) {
    authenticate("auth-jwt") {
        route("/dashboard") {
            get("/summary") {
                val user = call.requireCurrentUser(userRepository)
                call.respond(dashboardService.getSummary(user))
            }

            get("/category-totals") {
                val user = call.requireCurrentUser(userRepository)
                call.respond(dashboardService.getCategoryTotals(user))
            }

            get("/recent-activity") {
                val user = call.requireCurrentUser(userRepository)
                call.respond(dashboardService.getRecentActivity(user))
            }

            get("/monthly-trends") {
                val user = call.requireCurrentUser(userRepository)
                call.respond(dashboardService.getMonthlyTrends(user))
            }

            get("/weekly-trends") {
                val user = call.requireCurrentUser(userRepository)
                call.respond(dashboardService.getWeeklyTrends(user))
            }
        }
    }
}
