package com.jongheon.myreadle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.ArticleLevelContent
import com.jongheon.myreadle.domain.model.Level

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val topicId: String,
    val date: String,
    val category: String,
    val titleKo: String,
    val sourceUrl: String,
    val sourceName: String,
    val imageUrl: String?,
    /** JSON-encoded Map<String /* Level.name */, ArticleLevelContent>. */
    val levelsJson: String,
    val fetchedAt: Long,
)

fun ArticleEntity.toDomain(decode: (String) -> Map<Level, ArticleLevelContent>): Article = Article(
    topicId = topicId,
    date = date,
    category = category,
    titleKo = titleKo,
    sourceUrl = sourceUrl,
    sourceName = sourceName,
    imageUrl = imageUrl,
    levels = decode(levelsJson),
)
