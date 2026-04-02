package com.example.db.schemas

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object UserSessions : LongIdTable("user_sessions") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val refreshTokenHash = varchar("refresh_token_hash", 200).uniqueIndex()
    val expiresAt = timestamp("expires_at")
}
