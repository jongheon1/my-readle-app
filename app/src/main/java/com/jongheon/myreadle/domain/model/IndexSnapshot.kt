package com.jongheon.myreadle.domain.model

/** Snapshot of the remote index.json. Dates are sorted DESC (newest first). */
data class IndexSnapshot(
    val updatedAt: String,
    val availableLevels: List<String>,
    val categories: List<String>,
    val dates: List<AvailableDate>,
)

data class AvailableDate(
    val date: String,
    val topicCount: Int,
    val categories: List<String>,
)
