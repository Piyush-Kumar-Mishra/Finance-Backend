package com.example.repository

import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.db.schemas.Users
import com.example.model.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

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

    override fun create(email: String, passwordHash: String): User {
        return create(email, passwordHash, UserRole.VIEWER)
    }

    override fun create(email: String, passwordHash: String, role: UserRole): User = transaction {
        val id = Users.insert {
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role
        } get Users.id

        User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            status = UserStatus.ACTIVE,
            role = role
        )
    }

    override fun updateRole(id: Long, role: UserRole): User? = transaction {
        val updatedRows = Users.update({ Users.id eq id }) {
            it[Users.role] = role
        }

        if (updatedRows == 0) return@transaction null

        Users.selectAll()
            .where { Users.id eq id }
            .map { User(it[Users.id], it[Users.email], it[Users.passwordHash],it[Users.status], it[Users.role]) }
            .singleOrNull()
    }

    override fun updateStatus(id: Long, status: UserStatus): User? = transaction {
        val updatedRows = Users.update({ Users.id eq id }) {
            it[Users.status] = status
        }

        if (updatedRows == 0) return@transaction null

        Users.selectAll()
            .where { Users.id eq id }
            .map { User(it[Users.id], it[Users.email], it[Users.passwordHash],it[Users.status], it[Users.role]) }
            .singleOrNull()
    }

    override fun adminExists(role: UserRole): Long = transaction {
        Users.selectAll()
            .where { Users.role eq role }
            .count()
    }

}
