package com.jongheon.myreadle.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val displayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

fun dateSectionLabel(date: String, today: LocalDate = LocalDate.now()): String {
    val parsed = runCatching { LocalDate.parse(date, isoFormatter) }.getOrNull() ?: return date
    return when (parsed) {
        today -> "Today · $date"
        today.minusDays(1) -> "Yesterday · $date"
        else -> "${parsed.format(displayFormatter)} · $date"
    }
}

fun formatMinutes(seconds: Int): Int = ((seconds + 59) / 60).coerceAtLeast(1)
