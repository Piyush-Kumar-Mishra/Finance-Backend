package com.example.service

import com.example.db.schemas.RecordType
import com.example.dto.CreateRecordRequest
import com.example.dto.RecordListResponse
import com.example.dto.RecordResponse
import com.example.dto.UpdateRecordRequest
import com.example.model.User
import com.example.repository.UserRecordRepository
import com.example.security.RecordAccessControl
import com.example.utils.HttpException
import io.ktor.http.HttpStatusCode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

class UserRecordService(
    private val userRecordRepository: UserRecordRepository,
    private val recordAccessControl: RecordAccessControl
) {

    fun getAllRecords(user: User): RecordListResponse {
        recordAccessControl.allowView(user)
        val records = userRecordRepository.findAll().map { it.toResponse() }
        return RecordListResponse(records)
    }

    fun getRecordById(user: User, id: Long): RecordResponse {
        recordAccessControl.allowView(user)
        val record = userRecordRepository.findById(id)
            ?: throw HttpException(HttpStatusCode.NotFound, "Record not found")

        return record.toResponse()
    }

    fun createRecord(user: User, request: CreateRecordRequest): RecordResponse {
        recordAccessControl.allowCreate(user)

        val type = parseType(request.type)
        val amount = parseAmount(request.amount)
        val category = normalizeText(request.category)
        val date = parseDate(request.date)
        val description = normalizeText(request.description)

        validateLengths(category, description)

        val record = userRecordRepository.create(
            userId = user.id,
            type = type,
            amount = amount,
            category = category,
            date = date,
            description = description
        )

        return record.toResponse()
    }

    fun updateRecord(user: User, id: Long, request: UpdateRecordRequest): RecordResponse {
        recordAccessControl.allowUpdate(user)

        val type = parseType(request.type)
        val amount = parseAmount(request.amount)
        val category = normalizeText(request.category)
        val date = parseDate(request.date)
        val description = normalizeText(request.description)

        validateLengths(category, description)

        val updatedRecord = userRecordRepository.update(
            id = id,
            type = type,
            amount = amount,
            category = category,
            date = date,
            description = description
        ) ?: throw HttpException(HttpStatusCode.NotFound, "Record not found")

        return updatedRecord.toResponse()
    }

    fun deleteRecord(user: User, id: Long): String {
        recordAccessControl.allowDelete(user)

        val deleted = userRecordRepository.deleteById(id)
        if (!deleted) {
            throw HttpException(HttpStatusCode.NotFound, "Record not found")
        }

        return "Record deleted successfully"
    }

    private fun parseType(type: String): RecordType {
        val normalizedType = type.trim()

        if (normalizedType.isBlank()) {
            throw HttpException(HttpStatusCode.BadRequest, "Record type is required")
        }

        return try {
            RecordType.valueOf(normalizedType.uppercase())
        }
        catch (_: IllegalArgumentException) {
            throw HttpException(HttpStatusCode.BadRequest, "Invalid record type")
        }
    }

    private fun parseAmount(amount: Double): BigDecimal {
        if (amount <= 0.0) {
            throw HttpException(HttpStatusCode.BadRequest, "Amount must be greater than zero")
        }

        return BigDecimal.valueOf(amount)
    }

    private fun parseDate(date: String): LocalDate {
        val normalizedDate = date.trim()
        if (normalizedDate.isBlank()) {
            throw HttpException(HttpStatusCode.BadRequest, "Record date is required")
        }

        return try {
            LocalDate.parse(normalizedDate)
        } catch (_: DateTimeParseException) {
            throw HttpException(HttpStatusCode.BadRequest, "Invalid record date")
        }
    }

    private fun normalizeText(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun validateLengths(category: String?, description: String?) {
        if (category != null && category.length > 50) {
            throw HttpException(HttpStatusCode.BadRequest, "Category length exceeded")
        }

        if (description != null && description.length > 100) {
            throw HttpException(HttpStatusCode.BadRequest, "Description length exceeded")
        }
    }

    private fun com.example.model.UserRecord.toResponse(): RecordResponse {
        return RecordResponse(
            id = id,
            type = type.name,
            amount = amount.toDouble(),
            category = category,
            date = date.toString(),
            description = description
        )
    }
}
