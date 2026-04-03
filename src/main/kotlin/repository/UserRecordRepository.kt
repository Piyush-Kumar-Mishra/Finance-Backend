package com.example.repository

import com.example.db.schemas.RecordType
import com.example.model.UserRecord
import java.math.BigDecimal
import java.time.LocalDate

interface UserRecordRepository {
    fun findAll(): List<UserRecord>
    fun findById(id: Long): UserRecord?
    fun create(
        userId: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord
    fun update(
        id: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord?
    fun deleteById(id: Long): Boolean
}
