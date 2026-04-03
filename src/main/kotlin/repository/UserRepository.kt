package com.example.repository

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.model.User

interface UserRepository {

    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun create(email: String, passwordHash: String): User
    fun create(email: String, passwordHash: String, role: UserRole): User
    fun updateRole(id: Long, role: UserRole): User?
    fun updateStatus(id: Long, status: UserStatus): User?
    fun adminExists(role: UserRole): Long

}
