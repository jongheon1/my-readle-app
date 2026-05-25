package com.jongheon.myreadle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ArticleLevelContent(
    val title: String,
    val body: String,
    val wordCount: Int,
    val estimatedReadSeconds: Int,
    val vocab: List<Vocab>,
    val keyPhrases: List<KeyPhrase>,
)

data class Article(
    val topicId: String,
    val date: String,
    val category: String,
    val titleKo: String,
    val sourceUrl: String,
    val sourceName: String,
    val imageUrl: String?,
    val levels: Map<Level, ArticleLevelContent>,
) {
    fun contentFor(level: Level): ArticleLevelContent? =
        levels[level] ?: levels.entries.minByOrNull { kotlin.math.abs(it.key.order - level.order) }?.value
}
