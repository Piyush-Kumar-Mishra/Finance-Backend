package com.example.service

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.dto.UpdateUserRoleRequest
import com.example.dto.UpdateUserStatusRequest
import com.example.model.User
import com.example.repository.UserRepository
import com.example.security.UserManagementAccessControl
import com.example.utils.HttpException
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class UserManagementServiceTest {

    private val userManagementAccessControl = UserManagementAccessControl()

    @Test
    fun `admin can update user role`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val admin = buildUser(1, "admin@example.com", UserRole.ADMIN)
        val viewer = buildUser(2, "viewer@example.com", UserRole.VIEWER)

        userRepository.saveUser(admin)
        userRepository.saveUser(viewer)

        val response = userManagementService.updateUserRole(
            currentUser = admin,
            userId = viewer.id,
            request = UpdateUserRoleRequest("editor")
        )

        assertEquals("EDITOR", response.role)
        assertEquals("viewer@example.com", response.email)
    }

    @Test
    fun `non admin cannot update user role`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val viewer = buildUser(1, "viewer@example.com", UserRole.VIEWER)
        val targetUser = buildUser(2, "target@example.com", UserRole.VIEWER)

        userRepository.saveUser(viewer)
        userRepository.saveUser(targetUser)

        try {
            userManagementService.updateUserRole(viewer, targetUser.id, UpdateUserRoleRequest("admin"))
            fail("Expected role update to fail")
        } catch (e: HttpException) {
            assertEquals("You are not allowed to manage users", e.message)
        }
    }

    @Test
    fun `invalid role returns bad request`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val admin = buildUser(1, "admin@example.com", UserRole.ADMIN)
        val targetUser = buildUser(2, "target@example.com", UserRole.VIEWER)

        userRepository.saveUser(admin)
        userRepository.saveUser(targetUser)

        try {
            userManagementService.updateUserRole(admin, targetUser.id, UpdateUserRoleRequest("wrong"))
            fail("Expected invalid role error")
        } catch (e: HttpException) {
            assertEquals("Invalid user role", e.message)
        }
    }

    @Test
    fun `admin can update user status`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val admin = buildUser(1, "admin@example.com", UserRole.ADMIN)
        val viewer = buildUser(2, "viewer@example.com", UserRole.VIEWER)

        userRepository.saveUser(admin)
        userRepository.saveUser(viewer)

        val response = userManagementService.updateUserStatus(
            currentUser = admin,
            userId = viewer.id,
            request = UpdateUserStatusRequest("inactive")
        )

        assertEquals("INACTIVE", response.status)
        assertEquals("viewer@example.com", response.email)
    }

    @Test
    fun `invalid status returns bad request`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val admin = buildUser(1, "admin@example.com", UserRole.ADMIN)
        val targetUser = buildUser(2, "target@example.com", UserRole.VIEWER)

        userRepository.saveUser(admin)
        userRepository.saveUser(targetUser)

        try {
            userManagementService.updateUserStatus(admin, targetUser.id, UpdateUserStatusRequest("wrong"))
            fail("Expected invalid status error")
        } catch (e: HttpException) {
            assertEquals("Invalid user status", e.message)
        }
    }

    @Test
    fun `admin cannot change own role`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val admin = buildUser(1, "admin@example.com", UserRole.ADMIN)

        userRepository.saveUser(admin)

        try {
            userManagementService.updateUserRole(admin, admin.id, UpdateUserRoleRequest("viewer"))
            fail("Expected self role update to fail")
        } catch (e: HttpException) {
            assertEquals("You cannot change your own role or status", e.message)
        }
    }

    @Test
    fun `admin cannot change own status`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val admin = buildUser(1, "admin@example.com", UserRole.ADMIN)

        userRepository.saveUser(admin)

        try {
            userManagementService.updateUserStatus(admin, admin.id, UpdateUserStatusRequest("inactive"))
            fail("Expected self status update to fail")
        } catch (e: HttpException) {
            assertEquals("You cannot change your own role or status", e.message)
        }
    }

    @Test
    fun `cannot remove the only admin role`() {
        val userRepository = InMemoryUserManagementRepository()
        val userManagementService = UserManagementService(userRepository, userManagementAccessControl)
        val admin = buildUser(1, "admin@example.com", UserRole.ADMIN)
        val targetAdmin = buildUser(2, "target-admin@example.com", UserRole.ADMIN)

        userRepository.saveUser(admin)
        userRepository.saveUser(targetAdmin)
        userRepository.updateRole(admin.id, UserRole.VIEWER)

        try {
            userManagementService.updateUserRole(admin, targetAdmin.id, UpdateUserRoleRequest("viewer"))
            fail("Expected admin role protection")
        } catch (e: HttpException) {
            assertEquals("At least one admin must remain in the system", e.message)
        }
    }

    private fun buildUser(id: Long, email: String, role: UserRole): User {
        return User(
            id = id,
            email = email,
            passwordHash = "hash:pass123",
            status = UserStatus.ACTIVE,
            role = role
        )
    }
}

private class InMemoryUserManagementRepository : UserRepository {
    private val users = linkedMapOf<Long, User>()
    private var nextId = 1L

    override fun findById(id: Long): User? {
        return users[id]
    }

    override fun findByEmail(email: String): User? {
        return users.values.singleOrNull { it.email == email }
    }

    override fun create(email: String, passwordHash: String): User {
        return create(email, passwordHash, UserRole.VIEWER)
    }

    override fun create(email: String, passwordHash: String, role: UserRole): User {
        val user = User(
            id = nextId,
            email = email,
            passwordHash = passwordHash,
            status = UserStatus.ACTIVE,
            role = role
        )
        users[user.id] = user
        nextId += 1
        return user
    }

    override fun updateRole(id: Long, role: UserRole): User? {
        val user = users[id] ?: return null
        val updatedUser = user.copy(role = role)
        users[id] = updatedUser
        return updatedUser
    }

    override fun updateStatus(id: Long, status: UserStatus): User? {
        val user = users[id] ?: return null
        val updatedUser = user.copy(status = status)
        users[id] = updatedUser
        return updatedUser
    }

    override fun adminExists(role: UserRole): Long {
        return users.values.count { it.role == role }.toLong()
    }

    fun saveUser(user: User) {
        users[user.id] = user
        nextId = max(nextId, user.id + 1)
    }
}
