package com.example.service

import com.example.db.schemas.RecordType
import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.dto.CreateRecordRequest
import com.example.dto.RecordFilters
import com.example.dto.UpdateRecordRequest
import com.example.model.User
import com.example.model.UserRecord
import com.example.repository.UserRecordRepository
import com.example.security.RecordAccessControl
import com.example.utils.HttpException
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class UserRecordServiceTest {

    private val recordAccessControl = RecordAccessControl()

    @Test
    fun `viewer cannot create record`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.VIEWER)

        try {
            userRecordService.createRecord(
                user,
                CreateRecordRequest("income", 1200.0, "salary", "2026-04-03", "monthly salary")
            )
            fail("Viewer should not be able to create record")
        } catch (e: HttpException) {
            assertEquals("You are not allowed to create records", e.message)
        }
    }

    @Test
    fun `editor can create and update record`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.EDITOR)

        val createdRecord = userRecordService.createRecord(
            user,
            CreateRecordRequest("income", 1200.0, "salary", "2026-04-03", "monthly salary")
        )

        val updatedRecord = userRecordService.updateRecord(
            user,
            createdRecord.id,
            UpdateRecordRequest("expense", 200.0, "food", "2026-04-04", "groceries")
        )

        assertEquals("INCOME", createdRecord.type)
        assertEquals("EXPENSE", updatedRecord.type)
        assertEquals(200.0, updatedRecord.amount)
    }

    @Test
    fun `admin can delete record`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.ADMIN)

        val createdRecord = userRecordService.createRecord(
            user,
            CreateRecordRequest("loan", 5000.0, "bank", "2026-04-05", "personal loan")
        )

        val message = userRecordService.deleteRecord(user, createdRecord.id)

        assertEquals("Record deleted successfully", message)
        assertEquals(0, userRecordRepository.count())
    }

    @Test
    fun `invalid record values return bad request`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.ADMIN)

        try {
            userRecordService.createRecord(
                user,
                CreateRecordRequest("wrong", 0.0, "salary", "2026/04/03", "monthly salary")
            )
            fail("Expected validation error")
        } catch (e: HttpException) {
            assertEquals("Invalid record type", e.message)
        }
    }

    @Test
    fun `view record returns not found when record is missing`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.VIEWER)

        try {
            userRecordService.getRecordById(user, 99)
            fail("Expected not found error")
        } catch (e: HttpException) {
            assertEquals("Record not found", e.message)
        }
    }

    @Test
    fun `get all records returns created records`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.ADMIN)

        userRecordService.createRecord(
            user,
            CreateRecordRequest("income", 1200.0, "salary", "2026-04-03", "monthly salary")
        )
        userRecordService.createRecord(
            user,
            CreateRecordRequest("expense", 300.0, "food", "2026-04-04", "groceries")
        )

        val response = userRecordService.getAllRecords(user)

        assertEquals(2, response.records.size)
        assertEquals("2026-04-04", response.records.first().date)
    }

    @Test
    fun `get all records filters by type category and date range`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.ADMIN)

        userRecordService.createRecord(
            user,
            CreateRecordRequest("income", 1200.0, "salary", "2026-04-03", "monthly salary")
        )
        userRecordService.createRecord(
            user,
            CreateRecordRequest("expense", 300.0, "food", "2026-04-04", "groceries")
        )
        userRecordService.createRecord(
            user,
            CreateRecordRequest("expense", 200.0, "food", "2026-04-06", "snacks")
        )

        val filteredByType = userRecordService.getAllRecords(user, RecordFilters(type = "expense"))
        val filteredByCategory = userRecordService.getAllRecords(user, RecordFilters(category = "food"))
        val filteredByDate = userRecordService.getAllRecords(user, RecordFilters(date = "2026-04-03"))
        val filteredByRange = userRecordService.getAllRecords(
            user,
            RecordFilters(fromDate = "2026-04-04", toDate = "2026-04-05")
        )

        assertEquals(2, filteredByType.records.size)
        assertEquals(2, filteredByCategory.records.size)
        assertEquals(1, filteredByDate.records.size)
        assertEquals(1, filteredByRange.records.size)
        assertEquals("2026-04-04", filteredByRange.records.first().date)
    }

    @Test
    fun `invalid date filters return bad request`() {
        val userRecordRepository = InMemoryUserRecordRepository()
        val userRecordService = UserRecordService(userRecordRepository, recordAccessControl)
        val user = buildUser(UserRole.ADMIN)

        try {
            userRecordService.getAllRecords(
                user,
                RecordFilters(date = "2026-04-03", fromDate = "2026-04-01")
            )
            fail("Expected date filter validation error")
        } catch (e: HttpException) {
            assertEquals("Use either date or date range filters", e.message)
        }

        try {
            userRecordService.getAllRecords(
                user,
                RecordFilters(fromDate = "2026-04-06", toDate = "2026-04-04")
            )
            fail("Expected date range validation error")
        } catch (e: HttpException) {
            assertEquals("From date cannot be after to date", e.message)
        }
    }

    private fun buildUser(role: UserRole): User {
        return User(
            id = 1,
            email = "test@example.com",
            passwordHash = "hash:pass123",
            status = UserStatus.ACTIVE,
            role = role
        )
    }
}

private class InMemoryUserRecordRepository : UserRecordRepository {
    private val records = linkedMapOf<Long, UserRecord>()
    private var nextId = 1L

    override fun findAll(
        type: RecordType?,
        category: String?,
        date: LocalDate?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): List<UserRecord> {
        return records.values
            .filter { type == null || it.type == type }
            .filter { category == null || it.category == category }
            .filter { date == null || it.date == date }
            .filter { fromDate == null || !it.date.isBefore(fromDate) }
            .filter { toDate == null || !it.date.isAfter(toDate) }
            .sortedWith(compareByDescending<UserRecord> { it.date }.thenByDescending { it.id })
    }

    override fun findById(id: Long): UserRecord? {
        return records[id]
    }

    override fun create(
        userId: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord {
        val record = UserRecord(
            id = nextId,
            userId = userId,
            type = type,
            amount = amount,
            category = category,
            date = date,
            description = description
        )
        records[record.id] = record
        nextId += 1
        return record
    }

    override fun update(
        id: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord? {
        val record = records[id] ?: return null
        val updatedRecord = record.copy(
            type = type,
            amount = amount,
            category = category,
            date = date,
            description = description
        )
        records[id] = updatedRecord
        return updatedRecord
    }

    override fun deleteById(id: Long): Boolean {
        return records.remove(id) != null
    }

    fun count(): Int {
        return records.size
    }
}
