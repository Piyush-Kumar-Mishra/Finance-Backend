package com.example.repository

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.db.schemas.Users
import com.example.model.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepositoryImpl: UserRepository {

    override fun findByEmail(email: String): User? = transaction {
        Users.selectAll()
            .where { Users.email eq email }
            .map { User(it[Users.id], it[Users.email], it[Users.passwordHash],it[Users.status], it[Users.role]) }
            .singleOrNull()
    }

    override fun findById(id: Long): User? = transaction{
        Users.selectAll()
            .where{Users.id eq id}
            .map{ User(it[Users.id], it[Users.email], it[Users.passwordHash],it[Users.status], it[Users.role]) }
            .singleOrNull()
    }

    override fun create(email: String, passwordHash: String): User = transaction {
        val id = Users.insert {
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
        } get Users.id

        User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            status = UserStatus.ACTIVE,
            role = UserRole.VIEWER
        )
    }

}