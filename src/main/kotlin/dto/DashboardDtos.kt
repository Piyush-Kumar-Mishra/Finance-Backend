package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardSummaryResponse(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double
)

@Serializable
data class CategoryTotalResponse(
    val category: String,
    val total: Double
)

@Serializable
data class CategoryTotalsResponse(
    val categoryTotals: List<CategoryTotalResponse>
)

@Serializable
data class RecentActivityResponse(
    val records: List<RecordResponse>
)

@Serializable
data class TrendPointResponse(
    val period: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double
)

@Serializable
data class TrendResponse(
    val trends: List<TrendPointResponse>
)
