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
import com.jongheon.myreadle.domain.model.AvailableDate
import com.jongheon.myreadle.domain.model.IndexSnapshot
import com.jongheon.myreadle.domain.model.Level
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

sealed interface IndexResult {
    data object UpToDate : IndexResult
    data class Refreshed(val snapshot: IndexSnapshot) : IndexResult
    data class SchemaTooNew(val seen: Int, val supported: Int) : IndexResult
    data class Failed(val error: Throwable) : IndexResult
}

sealed interface DateLoadResult {
    data object AlreadyLoaded : DateLoadResult
    data class Loaded(val topicCount: Int) : DateLoadResult
    data class SchemaTooNew(val seen: Int, val supported: Int) : DateLoadResult
    data class Failed(val error: Throwable) : DateLoadResult
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

    private val _indexState = MutableStateFlow<IndexSnapshot?>(null)
    val indexState: StateFlow<IndexSnapshot?> = _indexState.asStateFlow()

    private val _loadingDates = MutableStateFlow<Set<String>>(emptySet())
    val loadingDates: StateFlow<Set<String>> = _loadingDates.asStateFlow()

    /** Prevents concurrent fetches for the same date. */
    private val dateLocks = mutableMapOf<String, Mutex>()
    private val locksMutex = Mutex()

    fun observeArticles(): Flow<List<Article>> =
        dao.observeAll().map { entities -> entities.map { it.toDomainArticle() } }

    fun observeArticlesForDate(date: String): Flow<List<Article>> =
        dao.observeByDate(date).map { entities -> entities.map { it.toDomainArticle() } }

    fun observeArticle(topicId: String): Flow<Article?> =
        dao.observeById(topicId).map { it?.toDomainArticle() }

    suspend fun getArticle(topicId: String): Article? = dao.byId(topicId)?.toDomainArticle()

    /** Re-fetches the index. Cheap (a few KB) — call on app start and pull-to-refresh. */
    suspend fun refreshIndex(force: Boolean = false): IndexResult {
        if (!force) {
            val lastUpdate = settings.lastIndexUpdate.first()
            if (clock() - lastUpdate < INDEX_TTL_MS && _indexState.value != null) {
                Log.d(TAG, "refreshIndex: skipped (fresh)")
                return IndexResult.UpToDate
            }
        }

        return runCatching {
            Log.d(TAG, "refreshIndex: fetching")
            val dto = api.fetchIndex()
            if (dto.schemaVersion > GitHubApi.SUPPORTED_SCHEMA_VERSION) {
                return@runCatching IndexResult.SchemaTooNew(
                    seen = dto.schemaVersion,
                    supported = GitHubApi.SUPPORTED_SCHEMA_VERSION,
                )
            }
            val snapshot = IndexSnapshot(
                updatedAt = dto.updatedAt,
                availableLevels = dto.availableLevels,
                categories = dto.categories,
                dates = dto.dates
                    .map { AvailableDate(it.date, it.topicCount, it.categories) }
                    .sortedByDescending { it.date },
            )
            _indexState.value = snapshot
            settings.setLastIndexUpdate(clock())
            Log.d(TAG, "refreshIndex: dates=${snapshot.dates.size}")
            IndexResult.Refreshed(snapshot)
        }.getOrElse {
            Log.e(TAG, "refreshIndex: FAILED", it)
            IndexResult.Failed(it)
        }
    }

    /**
     * Fetches a single day's articles into Room if not already present.
     * Safe to call repeatedly — concurrent calls for the same date are serialized.
     */
    suspend fun ensureDateLoaded(date: String, force: Boolean = false): DateLoadResult {
        val lock = locksMutex.withLock {
            dateLocks.getOrPut(date) { Mutex() }
        }
        return lock.withLock { loadDateLocked(date, force) }
    }

    private suspend fun loadDateLocked(date: String, force: Boolean): DateLoadResult {
        if (!force && dao.countForDate(date) > 0) {
            return DateLoadResult.AlreadyLoaded
        }

        _loadingDates.update { it + date }
        return try {
            Log.d(TAG, "loadDate: fetching $date")
            val daily = api.fetchDaily(date)
            if (daily.schemaVersion > GitHubApi.SUPPORTED_SCHEMA_VERSION) {
                return DateLoadResult.SchemaTooNew(
                    seen = daily.schemaVersion,
                    supported = GitHubApi.SUPPORTED_SCHEMA_VERSION,
                )
            }
            val entities = daily.topics.map { topic ->
                topic.toDomain(daily.date).toEntity(clock())
            }
            if (entities.isNotEmpty()) dao.upsertAll(entities)
            Log.d(TAG, "loadDate: $date -> ${entities.size} entities")
            DateLoadResult.Loaded(entities.size)
        } catch (e: Throwable) {
            Log.e(TAG, "loadDate: $date FAILED", e)
            DateLoadResult.Failed(e)
        } finally {
            _loadingDates.update { it - date }
        }
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

        /** How long the cached index is considered fresh. */
        const val INDEX_TTL_MS: Long = 5L * 60L * 1000L
    }
}
