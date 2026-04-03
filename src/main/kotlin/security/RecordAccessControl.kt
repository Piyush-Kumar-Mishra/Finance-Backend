package com.example.security

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.model.User
import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode

enum class RecordAction{
    VIEW,
    CREATE,
    UPDATE,
    DELETE
}

class RecordAccessControl{
    private val permissionsByRole = mapOf(
        UserRole.VIEWER to setOf(
            RecordAction.VIEW
        ),
        UserRole.EDITOR to setOf(
            RecordAction.VIEW,
            RecordAction.CREATE,
            RecordAction.UPDATE
        ),
        UserRole.ADMIN to setOf(
            RecordAction.VIEW,
            RecordAction.CREATE,
            RecordAction.UPDATE,
            RecordAction.DELETE
        )
    )

    fun allowView(user: User){
        ensureAccess(user, RecordAction.VIEW)
    }

    fun allowCreate(user: User){
        ensureAccess(user, RecordAction.CREATE)
    }

    fun allowUpdate(user: User){
        ensureAccess(user, RecordAction.UPDATE)
    }

    fun allowDelete(user: User){
        ensureAccess(user, RecordAction.DELETE)
    }

    private fun ensureAccess(user: User, action: RecordAction) {
        if (user.status != UserStatus.ACTIVE) {
            throw HttpException(HttpStatusCode.Forbidden, "User account is inactive")
        }
        val allowedActions = permissionsByRole[user.role].orEmpty()
        if (action !in allowedActions) {
            throw HttpException(HttpStatusCode.Forbidden, buildMessage(action))
        }
    }

    private fun buildMessage(action: RecordAction): String {
        return when (action) {
            RecordAction.VIEW -> "You are not allowed to view records"
            RecordAction.CREATE -> "You are not allowed to create records"
            RecordAction.UPDATE -> "You are not allowed to update records"
            RecordAction.DELETE -> "You are not allowed to delete records"
        }
    }
}