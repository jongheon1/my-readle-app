package com.jongheon.myreadle.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jongheon.myreadle.core.ServiceLocator
import com.jongheon.myreadle.data.local.SettingsDataStore
import com.jongheon.myreadle.data.repository.ArticleRepository
import com.jongheon.myreadle.data.repository.DateLoadResult
import com.jongheon.myreadle.data.repository.IndexResult
import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.AvailableDate
import com.jongheon.myreadle.domain.model.IndexSnapshot
import com.jongheon.myreadle.domain.model.Level
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val availableDates: List<AvailableDate> = emptyList(),
    val articlesByDate: Map<String, List<Article>> = emptyMap(),
    val loadingDates: Set<String> = emptySet(),
    val selectedLevel: Level = Level.Default,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val hasIndex: Boolean = false,
)

class HomeViewModel(
    private val repository: ArticleRepository,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val transientMessage = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        listOf(
            repository.indexState,
            repository.observeArticles().map { list -> list.groupBy(Article::date) },
            repository.loadingDates,
            settings.selectedLevel,
            refreshing,
            transientMessage,
        )
    ) { values ->
        val index = values[0] as IndexSnapshot?
        val byDate = values[1] as Map<String, List<Article>>
        val loading = values[2] as Set<String>
        val level = values[3] as Level
        val isRefreshing = values[4] as Boolean
        val msg = values[5] as String?
        HomeUiState(
            availableDates = index?.dates.orEmpty(),
            articlesByDate = byDate,
            loadingDates = loading,
            selectedLevel = level,
            isRefreshing = isRefreshing,
            message = msg,
            hasIndex = index != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        refreshIndex(force = false)
    }

    fun onLevelSelected(level: Level) {
        viewModelScope.launch { settings.setSelectedLevel(level) }
    }

    /** Pull-to-refresh handler. Forces a re-fetch of the index. */
    fun onPullToRefresh() {
        refreshIndex(force = true)
    }

    private fun refreshIndex(force: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "refreshIndex(force=$force)")
            refreshing.value = true
            val result = repository.refreshIndex(force = force)
            transientMessage.value = when (result) {
                is IndexResult.Failed -> "Couldn't reach GitHub. Showing cached articles."
                is IndexResult.SchemaTooNew ->
                    "Articles use schema v${result.seen}. App supports v${result.supported} — please update."
                else -> null
            }
            refreshing.value = false
        }
    }

    /** Called by HomeScreen when a date section becomes visible. */
    fun onDateVisible(date: String) {
        viewModelScope.launch {
            when (val result = repository.ensureDateLoaded(date)) {
                is DateLoadResult.Failed -> {
                    // Only surface a message once per failure burst — keep it light
                    if (transientMessage.value == null) {
                        transientMessage.value = "Couldn't load $date. Check connection."
                    }
                }
                is DateLoadResult.SchemaTooNew -> {
                    transientMessage.value =
                        "Schema v${result.seen} for $date is newer than supported (v${result.supported})."
                }
                else -> Unit
            }
        }
    }

    fun consumeMessage() {
        transientMessage.value = null
    }

    companion object {
        private const val TAG = "MyReadle.VM"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    repository = ServiceLocator.articleRepository,
                    settings = ServiceLocator.settingsDataStore,
                ) as T
            }
        }
    }
}
