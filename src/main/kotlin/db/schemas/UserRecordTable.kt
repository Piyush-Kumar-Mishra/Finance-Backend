package com.example.db.schemas

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object UserRecords : Table("user_records") {
    val id = long("id").autoIncrement()
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val type = enumerationByName("type", 20, RecordType::class)
    val amount = decimal("amount", 10, 2)
    val category = varchar("category", 50).nullable()
    val date = date("date")
    val description = varchar("description", 100).nullable()
    override val primaryKey = PrimaryKey(id)
}

enum class RecordType {
    INCOME,
    EXPENSE,
    LOAN
}
