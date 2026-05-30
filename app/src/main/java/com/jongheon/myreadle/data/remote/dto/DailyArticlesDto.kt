package com.jongheon.myreadle.data.remote.dto

import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.ArticleLevelContent
import com.jongheon.myreadle.domain.model.KeyPhrase
import com.jongheon.myreadle.domain.model.Level
import com.jongheon.myreadle.domain.model.Vocab
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyArticlesDto(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    val date: String,
    val topics: List<TopicDto> = emptyList(),
)

@Serializable
data class TopicDto(
    @SerialName("topic_id") val topicId: String,
    val category: String,
    @SerialName("title_ko") val titleKo: String,
    // source_url / image_url can legitimately be null when the topic was
    // generated rather than scraped from an article — keep them nullable.
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    val levels: Map<String, LevelContentDto> = emptyMap(),
)

@Serializable
data class LevelContentDto(
    val title: String,
    val body: String,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("estimated_read_seconds") val estimatedReadSeconds: Int = 0,
    val vocab: List<VocabDto> = emptyList(),
    @SerialName("key_phrases") val keyPhrases: List<KeyPhraseDto> = emptyList(),
)

@Serializable
data class VocabDto(
    val word: String,
    val pos: String = "",
    @SerialName("meaning_ko") val meaningKo: String,
)

@Serializable
data class KeyPhraseDto(
    val phrase: String,
    @SerialName("meaning_ko") val meaningKo: String,
)

fun TopicDto.toDomain(date: String): Article = Article(
    topicId = topicId,
    date = date,
    category = category,
    titleKo = titleKo,
    sourceUrl = sourceUrl.orEmpty(),
    sourceName = sourceName,
    imageUrl = imageUrl,
    levels = levels.mapNotNull { (key, content) ->
        Level.fromKey(key)?.let { it to content.toDomain() }
    }.toMap(),
)

fun LevelContentDto.toDomain(): ArticleLevelContent = ArticleLevelContent(
    title = title,
    body = body,
    wordCount = wordCount,
    estimatedReadSeconds = estimatedReadSeconds,
    vocab = vocab.map { Vocab(it.word, it.pos, it.meaningKo) },
    keyPhrases = keyPhrases.map { KeyPhrase(it.phrase, it.meaningKo) },
)
