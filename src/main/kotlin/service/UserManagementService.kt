package com.example.service

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.dto.UpdateUserRoleRequest
import com.example.dto.UpdateUserStatusRequest
import com.example.dto.UserRoleResponse
import com.example.model.User
import com.example.repository.UserRepository
import com.example.security.UserManagementAccessControl
import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode

class UserManagementService(
    private val userRepository: UserRepository,
    private val userManagementAccessControl: UserManagementAccessControl
) {

    fun updateUserRole(currentUser: User, userId: Long, request: UpdateUserRoleRequest): UserRoleResponse {
        userManagementAccessControl.allowManageUsers(currentUser)
        ensureNotSelfUpdate(currentUser.id, userId)

        val targetUser = userRepository.findById(userId)
            ?: throw HttpException(HttpStatusCode.NotFound, "User not found")

        val role = parseRole(request.role)

        if (targetUser.role == UserRole.ADMIN && role != UserRole.ADMIN && userRepository.adminExists(UserRole.ADMIN) == 1L) {
            throw HttpException(HttpStatusCode.Forbidden, "At least one admin must remain in the system")
        }

        val updatedUser = userRepository.updateRole(userId, role)
            ?: throw HttpException(HttpStatusCode.NotFound, "User not found")

        return UserRoleResponse(
            id = updatedUser.id,
            email = updatedUser.email,
            role = updatedUser.role.name,
            status = updatedUser.status.name
        )
    }

    fun updateUserStatus(currentUser: User, userId: Long, request: UpdateUserStatusRequest): UserRoleResponse {
        userManagementAccessControl.allowManageUsers(currentUser)
        ensureNotSelfUpdate(currentUser.id, userId)

        userRepository.findById(userId)
            ?: throw HttpException(HttpStatusCode.NotFound, "User not found")

        val status = parseStatus(request.status)

        val updatedUser = userRepository.updateStatus(userId, status)
            ?: throw HttpException(HttpStatusCode.NotFound, "User not found")

        return UserRoleResponse(
            id = updatedUser.id,
            email = updatedUser.email,
            role = updatedUser.role.name,
            status = updatedUser.status.name
        )
    }

    private fun parseRole(role: String): UserRole {
        val normalizedRole = role.trim()
        if (normalizedRole.isBlank()) {
            throw HttpException(HttpStatusCode.BadRequest, "Role is required")
        }

        return try {
            UserRole.valueOf(normalizedRole.uppercase())
        } catch (_: IllegalArgumentException) {
            throw HttpException(HttpStatusCode.BadRequest, "Invalid user role")
        }
    }

    private fun parseStatus(status: String): UserStatus {
        val normalizedStatus = status.trim()
        if (normalizedStatus.isBlank()) {
            throw HttpException(HttpStatusCode.BadRequest, "Status is required")
        }

        return try {
            UserStatus.valueOf(normalizedStatus.uppercase())
        } catch (_: IllegalArgumentException) {
            throw HttpException(HttpStatusCode.BadRequest, "Invalid user status")
        }
    }

    private fun ensureNotSelfUpdate(currentUserId: Long, userId: Long) {
        if (currentUserId == userId) {
            throw HttpException(HttpStatusCode.Forbidden, "You cannot change your own role or status")
        }
    }
}
