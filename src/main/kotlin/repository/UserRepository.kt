package com.example.repository

import com.example.model.User

interface UserRepository {

    fun findById(id: Long): User?
    fun findByEmail(email: String): User?
    fun create(email: String, passwordHash: String): User

}