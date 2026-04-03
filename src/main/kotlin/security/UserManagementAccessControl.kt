package com.example.security

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.model.User
import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode

class UserManagementAccessControl {

    fun allowManageUsers(user: User) {
        if (user.status != UserStatus.ACTIVE) {
            throw HttpException(HttpStatusCode.Forbidden, "User account is inactive")
        }

        if (user.role != UserRole.ADMIN) {
            throw HttpException(HttpStatusCode.Forbidden, "You are not allowed to manage users")
        }
    }
}
