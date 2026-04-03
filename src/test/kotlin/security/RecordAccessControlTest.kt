package com.example.security

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.model.User
import com.example.utils.HttpException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class RecordAccessControlTest {

    private val recordAccessControl = RecordAccessControl()

    @Test
    fun `viewer can only view records`() {
        val user = buildUser(role = UserRole.VIEWER)

        recordAccessControl.allowView(user)

        expectForbidden("You are not allowed to create records") {
            recordAccessControl.allowCreate(user)
        }
        expectForbidden("You are not allowed to update records") {
            recordAccessControl.allowUpdate(user)
        }
        expectForbidden("You are not allowed to delete records") {
            recordAccessControl.allowDelete(user)
        }
    }

    @Test
    fun `editor can view create and update records`() {
        val user = buildUser(role = UserRole.EDITOR)

        recordAccessControl.allowView(user)
        recordAccessControl.allowCreate(user)
        recordAccessControl.allowUpdate(user)

        expectForbidden("You are not allowed to delete records") {
            recordAccessControl.allowDelete(user)
        }
    }

    @Test
    fun `admin can manage all record actions`() {
        val user = buildUser(role = UserRole.ADMIN)

        recordAccessControl.allowView(user)
        recordAccessControl.allowCreate(user)
        recordAccessControl.allowUpdate(user)
        recordAccessControl.allowDelete(user)
    }

    @Test
    fun `inactive user cannot access records`() {
        val user = buildUser(role = UserRole.ADMIN, status = UserStatus.INACTIVE)

        expectForbidden("User account is inactive") {
            recordAccessControl.allowView(user)
        }
    }

    private fun buildUser(role: UserRole, status: UserStatus = UserStatus.ACTIVE): User {
        return User(
            id = 1,
            email = "test@example.com",
            passwordHash = "hash:pass123",
            status = status,
            role = role
        )
    }

    private fun expectForbidden(message: String, block: () -> Unit) {
        try {
            block()
            fail("Expected forbidden exception")
        } catch (e: HttpException) {
            assertEquals(message, e.message)
        }
    }
}
