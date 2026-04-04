package com.example.service

import com.example.db.schemas.RecordType
import com.example.dto.CategoryTotalResponse
import com.example.dto.CategoryTotalsResponse
import com.example.dto.DashboardSummaryResponse
import com.example.dto.RecentActivityResponse
import com.example.dto.RecordResponse
import com.example.dto.TrendPointResponse
import com.example.dto.TrendResponse
import com.example.model.User
import com.example.model.UserRecord
import com.example.repository.UserRecordRepository
import com.example.security.RecordAccessControl
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

class DashboardService(
    private val userRecordRepository: UserRecordRepository,
    private val recordAccessControl: RecordAccessControl
) {

    fun getSummary(user: User): DashboardSummaryResponse {
        recordAccessControl.allowView(user)
        val records = getDashboardRecords()
        val totalIncome = getTotalByType(records, RecordType.INCOME)
        val totalExpense = getTotalByType(records, RecordType.EXPENSE)

        return DashboardSummaryResponse(
            totalIncome = totalIncome.toDouble(),
            totalExpense = totalExpense.toDouble(),
            netBalance = totalIncome.subtract(totalExpense).toDouble()
        )
    }

    fun getCategoryTotals(user: User): CategoryTotalsResponse {
        recordAccessControl.allowView(user)
        val categoryTotals = getDashboardRecords()
            .groupBy { it.category ?: "Uncategorized" }
            .map { (category, records) ->
                CategoryTotalResponse(
                    category = category,
                    total = records.fold(BigDecimal.ZERO) { total, record -> total + record.amount }.toDouble()
                )
            }
            .sortedWith(compareByDescending<CategoryTotalResponse> { it.total }.thenBy { it.category })

        return CategoryTotalsResponse(categoryTotals)
    }

    fun getRecentActivity(user: User): RecentActivityResponse {
        recordAccessControl.allowView(user)
        val records = userRecordRepository.findAll()
            .take(3)
            .map { it.toResponse() }

        return RecentActivityResponse(records)
    }

    fun getMonthlyTrends(user: User): TrendResponse {
        recordAccessControl.allowView(user)
        val trends = getDashboardRecords()
            .groupBy { YearMonth.from(it.date) }
            .toSortedMap()
            .map { (month, records) ->
                buildTrendPoint(
                    period = month.toString(),
                    records = records
                )
            }

        return TrendResponse(trends)
    }

    fun getWeeklyTrends(user: User): TrendResponse {
        recordAccessControl.allowView(user)
        val trends = getDashboardRecords()
            .groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .toSortedMap()
            .map { (weekStart, records) ->
                buildTrendPoint(
                    period = buildWeekPeriod(weekStart),
                    records = records
                )
            }

        return TrendResponse(trends)
    }

    private fun buildTrendPoint(period: String, records: List<UserRecord>): TrendPointResponse {
        val totalIncome = getTotalByType(records, RecordType.INCOME)
        val totalExpense = getTotalByType(records, RecordType.EXPENSE)

        return TrendPointResponse(
            period = period,
            totalIncome = totalIncome.toDouble(),
            totalExpense = totalExpense.toDouble(),
            netBalance = totalIncome.subtract(totalExpense).toDouble()
        )
    }

    private fun getTotalByType(records: List<UserRecord>, type: RecordType): BigDecimal {
        return records
            .filter { it.type == type }
            .fold(BigDecimal.ZERO) { total, record -> total + record.amount }
    }

    private fun getDashboardRecords(): List<UserRecord> {
        return userRecordRepository.findAll()
            .filter { it.type == RecordType.INCOME || it.type == RecordType.EXPENSE }
    }

    private fun buildWeekPeriod(weekStart: LocalDate): String {
        val week = weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0')
        val year = weekStart.get(IsoFields.WEEK_BASED_YEAR)
        return "$year-W$week"
    }

    private fun UserRecord.toResponse(): RecordResponse {
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
