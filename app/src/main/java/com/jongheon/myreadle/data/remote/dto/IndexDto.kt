package com.jongheon.myreadle.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IndexDto(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("available_levels") val availableLevels: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val dates: List<IndexDateEntry> = emptyList(),
)

@Serializable
data class IndexDateEntry(
    val date: String,
    @SerialName("topic_count") val topicCount: Int = 0,
    val categories: List<String> = emptyList(),
)
