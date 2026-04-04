package com.example.service

import com.example.db.schemas.RecordType
import com.example.db.schemas.UserRole
import com.example.db.schemas.UserStatus
import com.example.model.User
import com.example.model.UserRecord
import com.example.repository.UserRecordRepository
import com.example.security.RecordAccessControl
import com.example.utils.HttpException
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DashboardServiceTest {

    private val recordAccessControl = RecordAccessControl()

    @Test
    fun `summary returns income expense and net balance`() {
        val dashboardService = DashboardService(buildRepository(), recordAccessControl)

        val response = dashboardService.getSummary(buildUser(UserRole.VIEWER))

        assertEquals(1800.0, response.totalIncome)
        assertEquals(500.0, response.totalExpense)
        assertEquals(1300.0, response.netBalance)
    }

    @Test
    fun `category totals groups records by category`() {
        val dashboardService = DashboardService(buildRepository(), recordAccessControl)

        val response = dashboardService.getCategoryTotals(buildUser(UserRole.ADMIN))

        assertEquals(3, response.categoryTotals.size)
        assertEquals("Salary", response.categoryTotals[0].category)
        assertEquals(1800.0, response.categoryTotals[0].total)
        assertEquals("Food", response.categoryTotals[1].category)
        assertEquals(300.0, response.categoryTotals[1].total)
    }

    @Test
    fun `recent activity returns last three records`() {
        val dashboardService = DashboardService(buildRepository(), recordAccessControl)

        val response = dashboardService.getRecentActivity(buildUser(UserRole.EDITOR))

        assertEquals(3, response.records.size)
        assertEquals("2026-04-04", response.records[0].date)
        assertEquals("2026-04-03", response.records[1].date)
        assertEquals("2026-04-02", response.records[2].date)
    }

    @Test
    fun `monthly trends groups records by month`() {
        val dashboardService = DashboardService(buildRepository(), recordAccessControl)

        val response = dashboardService.getMonthlyTrends(buildUser(UserRole.VIEWER))

        assertEquals(2, response.trends.size)
        assertEquals("2026-03", response.trends[0].period)
        assertEquals(800.0, response.trends[0].totalIncome)
        assertEquals(0.0, response.trends[0].totalExpense)
        assertEquals("2026-04", response.trends[1].period)
        assertEquals(1000.0, response.trends[1].totalIncome)
        assertEquals(500.0, response.trends[1].totalExpense)
        assertEquals(500.0, response.trends[1].netBalance)
    }

    @Test
    fun `weekly trends groups records by week`() {
        val dashboardService = DashboardService(buildRepository(), recordAccessControl)

        val response = dashboardService.getWeeklyTrends(buildUser(UserRole.ADMIN))

        assertEquals(2, response.trends.size)
        assertEquals("2026-W13", response.trends[0].period)
        assertEquals(800.0, response.trends[0].totalIncome)
        assertEquals(0.0, response.trends[0].totalExpense)
        assertEquals("2026-W14", response.trends[1].period)
        assertEquals(1000.0, response.trends[1].totalIncome)
        assertEquals(500.0, response.trends[1].totalExpense)
    }

    @Test
    fun `inactive user cannot access dashboard`() {
        val dashboardService = DashboardService(buildRepository(), recordAccessControl)
        val user = User(
            id = 1,
            email = "test@example.com",
            passwordHash = "hash:pass123",
            status = UserStatus.INACTIVE,
            role = UserRole.VIEWER
        )

        try {
            dashboardService.getSummary(user)
            fail("Inactive user should not access dashboard")
        } catch (e: HttpException) {
            assertEquals("User account is inactive", e.message)
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

    private fun buildRepository(): UserRecordRepository {
        return DashboardUserRecordRepository(
            listOf(
                UserRecord(
                    id = 1,
                    userId = 1,
                    type = RecordType.INCOME,
                    amount = BigDecimal.valueOf(800.0),
                    category = "Salary",
                    date = LocalDate.parse("2026-03-28"),
                    description = "march salary"
                ),
                UserRecord(
                    id = 2,
                    userId = 1,
                    type = RecordType.INCOME,
                    amount = BigDecimal.valueOf(1000.0),
                    category = "Salary",
                    date = LocalDate.parse("2026-04-04"),
                    description = "april salary"
                ),
                UserRecord(
                    id = 3,
                    userId = 1,
                    type = RecordType.EXPENSE,
                    amount = BigDecimal.valueOf(300.0),
                    category = "Food",
                    date = LocalDate.parse("2026-04-03"),
                    description = "groceries"
                ),
                UserRecord(
                    id = 4,
                    userId = 1,
                    type = RecordType.EXPENSE,
                    amount = BigDecimal.valueOf(200.0),
                    category = "Travel",
                    date = LocalDate.parse("2026-04-02"),
                    description = "cab"
                ),
                UserRecord(
                    id = 5,
                    userId = 1,
                    type = RecordType.LOAN,
                    amount = BigDecimal.valueOf(5000.0),
                    category = "Bank",
                    date = LocalDate.parse("2026-04-01"),
                    description = "loan credited"
                )
            )
        )
    }
}

private class DashboardUserRecordRepository(
    private val records: List<UserRecord>
) : UserRecordRepository {

    override fun findAll(
        type: RecordType?,
        category: String?,
        date: LocalDate?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): List<UserRecord> {
        return records
            .filter { type == null || it.type == type }
            .filter { category == null || it.category == category }
            .filter { date == null || it.date == date }
            .filter { fromDate == null || !it.date.isBefore(fromDate) }
            .filter { toDate == null || !it.date.isAfter(toDate) }
            .sortedWith(compareByDescending<UserRecord> { it.date }.thenByDescending { it.id })
    }

    override fun findById(id: Long): UserRecord? {
        return records.find { it.id == id }
    }

    override fun create(
        userId: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord {
        throw UnsupportedOperationException("Not needed in this test")
    }

    override fun update(
        id: Long,
        type: RecordType,
        amount: BigDecimal,
        category: String?,
        date: LocalDate,
        description: String?
    ): UserRecord? {
        throw UnsupportedOperationException("Not needed in this test")
    }

    override fun deleteById(id: Long): Boolean {
        throw UnsupportedOperationException("Not needed in this test")
    }
}
