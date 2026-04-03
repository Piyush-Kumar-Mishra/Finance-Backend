package com.example.model

import com.example.db.schemas.RecordType
import java.math.BigDecimal
import java.time.LocalDate

data class UserRecord(
    val id: Long,
    val userId: Long,
    val type: RecordType,
    val amount: BigDecimal,
    val category: String?,
    val date: LocalDate,
    val description: String?
)
