package com.jongheon.myreadle.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jongheon.myreadle.R
import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.AvailableDate
import com.jongheon.myreadle.domain.model.Level
import com.jongheon.myreadle.ui.common.categoryIcon
import com.jongheon.myreadle.ui.common.categoryLabel
import com.jongheon.myreadle.ui.common.dateSectionLabel
import com.jongheon.myreadle.ui.common.formatMinutes
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onArticleClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        scope.launch {
            snackbar.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    // Fall back to dates derived from cached articles if the index hasn't
    // loaded yet, so an offline cold-start still shows something.
    val sections = remember(state.availableDates, state.articlesByDate) {
        if (state.availableDates.isNotEmpty()) state.availableDates
        else state.articlesByDate.keys.sortedDescending().map { date ->
            AvailableDate(
                date = date,
                topicCount = state.articlesByDate[date]?.size ?: 0,
                categories = emptyList(),
            )
        }
    }

    // Trigger lazy loading for any date currently on screen. Repository
    // dedupes by date and short-circuits if Room already has rows.
    LaunchedEffect(listState, sections) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { (it.key as? String)?.takeIf { k -> k.startsWith(KEY_HEADER) } }
                .map { it.removePrefix(KEY_HEADER) }
                .toSet()
        }
            .distinctUntilChanged()
            .collect { visibleDates ->
                visibleDates.forEach { date -> viewModel.onDateVisible(date) }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_home)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LevelTabs(
                selected = state.selectedLevel,
                onSelect = viewModel::onLevelSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::onPullToRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (sections.isEmpty()) {
                    EmptyState(showRefreshHint = !state.isRefreshing)
                } else {
                    ArticleList(
                        sections = sections,
                        articlesByDate = state.articlesByDate,
                        loadingDates = state.loadingDates,
                        level = state.selectedLevel,
                        listState = listState,
                        onArticleClick = onArticleClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelTabs(
    selected: Level,
    onSelect: (Level) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Level.entries.forEach { level ->
            FilterChip(
                selected = level == selected,
                onClick = { onSelect(level) },
                label = { Text(level.displayName) },
            )
        }
    }
}

@Composable
private fun ArticleList(
    sections: List<AvailableDate>,
    articlesByDate: Map<String, List<Article>>,
    loadingDates: Set<String>,
    level: Level,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onArticleClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sections.forEach { dateEntry ->
            item(key = "$KEY_HEADER${dateEntry.date}", contentType = "section-header") {
                Text(
                    text = dateSectionLabel(dateEntry.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }

            val articles = articlesByDate[dateEntry.date].orEmpty()
            when {
                articles.isNotEmpty() -> {
                    items(
                        items = articles,
                        key = { it.topicId },
                        contentType = { "article" },
                    ) { article ->
                        ArticleCard(
                            article = article,
                            level = level,
                            onClick = { onArticleClick(article.topicId) },
                        )
                    }
                }

                dateEntry.date in loadingDates || articles.isEmpty() -> {
                    val placeholderCount = dateEntry.topicCount.coerceAtLeast(1).coerceAtMost(4)
                    items(
                        count = placeholderCount,
                        key = { "skeleton-${dateEntry.date}-$it" },
                        contentType = { "skeleton" },
                    ) {
                        SkeletonCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(
    article: Article,
    level: Level,
    onClick: () -> Unit,
) {
    val content = article.contentFor(level)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon(article.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = categoryLabel(article.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = content?.title ?: article.titleKo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (article.titleKo.isNotBlank() && content?.title != article.titleKo) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = article.titleKo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (content != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${content.wordCount} words · ${formatMinutes(content.estimatedReadSeconds)} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SkeletonCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SkeletonLine(widthFraction = 0.35f)
            Spacer(Modifier.height(10.dp))
            SkeletonLine(widthFraction = 0.85f, heightDp = 18)
            Spacer(Modifier.height(8.dp))
            SkeletonLine(widthFraction = 0.55f, heightDp = 18)
            Spacer(Modifier.height(12.dp))
            SkeletonLine(widthFraction = 0.3f)
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, heightDp: Int = 12) {
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(onVariant.copy(alpha = 0.12f))
    )
}

@Composable
private fun EmptyState(showRefreshHint: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.empty_articles),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showRefreshHint) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pull down to retry.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

private const val KEY_HEADER = "header-"
