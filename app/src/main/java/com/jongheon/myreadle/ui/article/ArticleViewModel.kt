package com.jongheon.myreadle.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jongheon.myreadle.core.ServiceLocator
import com.jongheon.myreadle.data.local.SettingsDataStore
import com.jongheon.myreadle.data.repository.ArticleRepository
import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.Level
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArticleUiState(
    val article: Article? = null,
    val selectedLevel: Level = Level.Default,
    val fontScale: Float = 1.0f,
    val loading: Boolean = true,
)

class ArticleViewModel(
    private val topicId: String,
    private val repository: ArticleRepository,
    private val settings: SettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<ArticleUiState> = combine(
        repository.observeArticle(topicId),
        settings.selectedLevel,
        settings.fontScale,
    ) { article, level, scale ->
        ArticleUiState(
            article = article,
            selectedLevel = level,
            fontScale = scale,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArticleUiState(),
    )

    fun onLevelSelected(level: Level) {
        viewModelScope.launch { settings.setSelectedLevel(level) }
    }

    companion object {
        fun factory(topicId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ArticleViewModel(
                    topicId = topicId,
                    repository = ServiceLocator.articleRepository,
                    settings = ServiceLocator.settingsDataStore,
                )
            }
        }
    }
}
