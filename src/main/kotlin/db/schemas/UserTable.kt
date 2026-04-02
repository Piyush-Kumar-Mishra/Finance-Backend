package com.example.db.schemas

import org.jetbrains.exposed.dao.id.LongIdTable

object Users : LongIdTable("users") {
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val status = enumerationByName("status", 20, UserStatus::class).default(UserStatus.ACTIVE)
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.VIEWER)
}

enum class UserStatus{
    ACTIVE,
    INACTIVE
}

enum class UserRole{
    ADMIN,
    VIEWER,
    EDITOR
}
