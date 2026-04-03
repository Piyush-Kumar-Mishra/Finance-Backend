package com.example.repository

import com.example.db.schemas.RecordType
import com.example.db.schemas.UserRecords
import com.example.model.UserRecord
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.LocalDate

class UserRecordRepositoryImpl : UserRecordRepository {

    override fun findAll(): List<UserRecord> = transaction {
        UserRecords.selectAll()
            .orderBy(UserRecords.date to SortOrder.DESC, UserRecords.id to SortOrder.DESC)
            .map {
                UserRecord(
                    id = it[UserRecords.id],
                    userId = it[UserRecords.userId],
                    type = it[UserRecords.type],
                    amount = it[UserRecords.amount],
                    category = it[UserRecords.category],
                    date = it[UserRecords.date],
                    description = it[UserRecords.description]
                )
            }
    }

    override fun findById(id: Long): UserRecord? = transaction {
        UserRecords.selectAll()
            .where { UserRecords.id eq id }
            .map {
                UserRecord(
                    id = it[UserRecords.id],
                    userId = it[UserRecords.userId],
                    type = it[UserRecords.type],
                    amount = it[UserRecords.amount],
                    category = it[UserRecords.category],
                    date = it[UserRecords.date],
                    description = it[UserRecords.description]
                )
            }
            .singleOrNull()
    }

    override fun create(
        userId: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord = transaction {
        val id = UserRecords.insert {
            it[UserRecords.userId] = userId
            it[UserRecords.type] = type
            it[UserRecords.amount] = amount
            it[UserRecords.category] = category
            it[UserRecords.date] = date
            it[UserRecords.description] = description
        } get UserRecords.id

        UserRecord(
            id = id,
            userId = userId,
            type = type,
            amount = amount,
            category = category,
            date = date,
            description = description
        )
    }

    override fun update(
        id: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord? = transaction {
        val updatedRows = UserRecords.update({ UserRecords.id eq id }) {
            it[UserRecords.type] = type
            it[UserRecords.amount] = amount
            it[UserRecords.category] = category
            it[UserRecords.date] = date
            it[UserRecords.description] = description
        }

        if (updatedRows == 0) return@transaction null

        UserRecords.selectAll()
            .where { UserRecords.id eq id }
            .map {
                UserRecord(
                    id = it[UserRecords.id],
                    userId = it[UserRecords.userId],
                    type = it[UserRecords.type],
                    amount = it[UserRecords.amount],
                    category = it[UserRecords.category],
                    date = it[UserRecords.date],
                    description = it[UserRecords.description]
                )
            }
            .singleOrNull()
    }

    override fun deleteById(id: Long): Boolean = transaction {
        UserRecords.deleteWhere { UserRecords.id eq id } > 0
    }
}
