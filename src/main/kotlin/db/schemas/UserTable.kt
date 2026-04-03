package com.example.db.schemas

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val status = enumerationByName("status", 20, UserStatus::class).default(UserStatus.ACTIVE)
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.VIEWER)
    override val primaryKey = PrimaryKey(id)

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
