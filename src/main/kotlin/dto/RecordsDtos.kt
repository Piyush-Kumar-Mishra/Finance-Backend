package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecordRequest(
    val type: String,
    val amount: Double,
    val category: String?,
    val date: String,
    val description: String?
)

@Serializable
data class UpdateRecordRequest(
    val type: String,
    val amount: Double,
    val category: String?,
    val date: String,
    val description: String?
)

@Serializable
data class RecordResponse(
    val id: Long,
    val type: String,
    val amount: Double,
    val category: String?,
    val date: String,
    val description: String?
)

@Serializable
data class RecordListResponse(
    val records: List<RecordResponse>
)