package com.example.db.schemas

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UserSessions : Table("user_sessions") {
    val id = long("id").autoIncrement()
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val refreshTokenHash = varchar("refresh_token_hash", 200).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    override val primaryKey = PrimaryKey(id)
}
