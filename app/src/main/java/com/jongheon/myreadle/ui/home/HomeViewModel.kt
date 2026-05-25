package com.jongheon.myreadle.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jongheon.myreadle.core.ServiceLocator
import com.jongheon.myreadle.data.local.SettingsDataStore
import com.jongheon.myreadle.data.repository.ArticleRepository
import com.jongheon.myreadle.data.repository.RefreshResult
import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.Level
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val articles: List<Article> = emptyList(),
    val selectedLevel: Level = Level.Default,
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

class HomeViewModel(
    private val repository: ArticleRepository,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val transientMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeArticles(),
        settings.selectedLevel,
        refreshing,
        transientMessage,
    ) { articles, level, refreshing, message ->
        HomeUiState(
            articles = articles,
            selectedLevel = level,
            isRefreshing = refreshing,
            message = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        refresh(force = false)
    }

    fun onLevelSelected(level: Level) {
        viewModelScope.launch { settings.setSelectedLevel(level) }
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            Log.d("MyReadle.VM", "refresh(force=$force) start")
            refreshing.value = true
            val result = if (force) repository.forceRefresh() else repository.refreshIfStale()
            Log.d("MyReadle.VM", "refresh result=$result")
            transientMessage.value = when (result) {
                is RefreshResult.Failed -> "Couldn't reach GitHub. Showing cached articles."
                is RefreshResult.SchemaTooNew ->
                    "Articles use schema v${result.seen}. App supports v${result.supported} — please update."
                else -> null
            }
            refreshing.value = false
        }
    }

    fun consumeMessage() {
        transientMessage.value = null
    }

    companion object {
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
