package com.jongheon.myreadle.data.repository

import android.util.Log
import com.jongheon.myreadle.data.local.SettingsDataStore
import com.jongheon.myreadle.data.local.dao.ArticleDao
import com.jongheon.myreadle.data.local.entity.ArticleEntity
import com.jongheon.myreadle.data.local.entity.toDomain
import com.jongheon.myreadle.data.remote.GitHubApi
import com.jongheon.myreadle.data.remote.dto.toDomain
import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.ArticleLevelContent
import com.jongheon.myreadle.domain.model.Level
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

sealed interface RefreshResult {
    data object UpToDate : RefreshResult
    data class Updated(val newDates: List<String>) : RefreshResult
    data class SchemaTooNew(val seen: Int, val supported: Int) : RefreshResult
    data class Failed(val error: Throwable) : RefreshResult
}

class ArticleRepository(
    private val api: GitHubApi,
    private val dao: ArticleDao,
    private val settings: SettingsDataStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val levelsSerializer = MapSerializer(String.serializer(), ArticleLevelContent.serializer())

    fun observeArticles(): Flow<List<Article>> =
        dao.observeAll().map { entities -> entities.map { it.toDomainArticle() } }

    fun observeArticle(topicId: String): Flow<Article?> =
        dao.observeById(topicId).map { it?.toDomainArticle() }

    suspend fun getArticle(topicId: String): Article? = dao.byId(topicId)?.toDomainArticle()

    suspend fun refreshIfStale(maxAgeMillis: Long = ONE_HOUR_MS): RefreshResult {
        val lastUpdate = settings.lastIndexUpdate.first()
        if (clock() - lastUpdate < maxAgeMillis) return RefreshResult.UpToDate
        return forceRefresh()
    }

    suspend fun forceRefresh(): RefreshResult = runCatching {
        Log.d(TAG, "forceRefresh: start")
        val index = api.fetchIndex()
        Log.d(TAG, "forceRefresh: index fetched, dates=${index.dates.size}")
        if (index.schemaVersion > GitHubApi.SUPPORTED_SCHEMA_VERSION) {
            return@runCatching RefreshResult.SchemaTooNew(
                seen = index.schemaVersion,
                supported = GitHubApi.SUPPORTED_SCHEMA_VERSION,
            )
        }

        val localDates = dao.allDates().toSet()
        val remoteDates = index.dates.map { it.date }
        val recent = remoteDates.sortedDescending().take(MAX_DAYS)
        val toFetch = recent.filter { it !in localDates }
        Log.d(TAG, "forceRefresh: toFetch=$toFetch (local=$localDates)")

        val fetched = mutableListOf<String>()
        for (date in toFetch) {
            Log.d(TAG, "forceRefresh: fetching $date")
            val daily = api.fetchDaily(date)
            if (daily.schemaVersion > GitHubApi.SUPPORTED_SCHEMA_VERSION) continue
            val entities = daily.topics.map { topic ->
                val article = topic.toDomain(daily.date)
                article.toEntity(clock())
            }
            Log.d(TAG, "forceRefresh: $date -> ${entities.size} entities")
            if (entities.isNotEmpty()) dao.upsertAll(entities)
            fetched += date
        }

        settings.setLastIndexUpdate(clock())
        Log.d(TAG, "forceRefresh: done, fetched=$fetched")

        if (fetched.isEmpty()) RefreshResult.UpToDate
        else RefreshResult.Updated(fetched)
    }.getOrElse {
        Log.e(TAG, "forceRefresh: FAILED", it)
        RefreshResult.Failed(it)
    }

    private fun Article.toEntity(now: Long): ArticleEntity {
        val encoded = json.encodeToString(
            levelsSerializer,
            levels.mapKeys { it.key.name },
        )
        return ArticleEntity(
            topicId = topicId,
            date = date,
            category = category,
            titleKo = titleKo,
            sourceUrl = sourceUrl,
            sourceName = sourceName,
            imageUrl = imageUrl,
            levelsJson = encoded,
            fetchedAt = now,
        )
    }

    private fun ArticleEntity.toDomainArticle(): Article = toDomain { encoded ->
        val raw = runCatching {
            json.decodeFromString(levelsSerializer, encoded)
        }.getOrElse { emptyMap() }
        raw.mapNotNull { (k, v) -> Level.fromKey(k)?.let { it to v } }.toMap()
    }

    companion object {
        private const val TAG = "MyReadle.Repo"
        const val ONE_HOUR_MS: Long = 60L * 60L * 1000L
        const val MAX_DAYS = 14
    }
}
