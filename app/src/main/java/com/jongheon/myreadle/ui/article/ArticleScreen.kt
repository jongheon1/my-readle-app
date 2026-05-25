package com.jongheon.myreadle.ui.article

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jongheon.myreadle.R
import com.jongheon.myreadle.domain.model.Article
import com.jongheon.myreadle.domain.model.ArticleLevelContent
import com.jongheon.myreadle.domain.model.KeyPhrase
import com.jongheon.myreadle.domain.model.Level
import com.jongheon.myreadle.domain.model.Vocab
import com.jongheon.myreadle.ui.article.components.ClickableArticleText
import com.jongheon.myreadle.ui.article.components.VocabBottomSheet
import com.jongheon.myreadle.ui.common.categoryLabel
import com.jongheon.myreadle.ui.common.formatMinutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    topicId: String,
    onBack: () -> Unit,
    viewModel: ArticleViewModel = viewModel(factory = ArticleViewModel.factory(topicId)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeVocab by remember { mutableStateOf<Vocab?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val article = state.article
                    val content = article?.contentFor(state.selectedLevel)
                    Text(
                        text = content?.title ?: article?.titleKo.orEmpty(),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when {
            state.loading && state.article == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.article == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_articles))
                }
            }
            else -> {
                val article = state.article!!
                val content = article.contentFor(state.selectedLevel)
                if (content == null) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Level ${state.selectedLevel.displayName} not available for this article.")
                    }
                } else {
                    ArticleBody(
                        article = article,
                        level = state.selectedLevel,
                        content = content,
                        fontScale = state.fontScale,
                        availableLevels = article.levels.keys.sortedBy { it.order },
                        onLevelChange = viewModel::onLevelSelected,
                        onVocabClick = { activeVocab = it },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }

    activeVocab?.let { vocab ->
        VocabBottomSheet(
            vocab = vocab,
            onDismiss = { activeVocab = null },
        )
    }
}

@Composable
private fun ArticleBody(
    article: Article,
    level: Level,
    content: ArticleLevelContent,
    fontScale: Float,
    availableLevels: List<Level>,
    onLevelChange: (Level) -> Unit,
    onVocabClick: (Vocab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseFontSize = (17.sp * fontScale)
    val baseLineHeight = (28.sp * fontScale)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryLabel(article.category),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("·", color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = level.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("·", color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${formatMinutes(content.estimatedReadSeconds)} min",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = (28.sp * fontScale),
                        lineHeight = (36.sp * fontScale),
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (article.titleKo.isNotBlank() && article.titleKo != content.title) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = article.titleKo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (availableLevels.size > 1) {
            item {
                LevelSwitcher(
                    available = availableLevels,
                    current = level,
                    onChange = onLevelChange,
                )
            }
        }

        item {
            ClickableArticleText(
                body = content.body,
                vocabs = content.vocab,
                onVocabClick = onVocabClick,
                fontSize = baseFontSize,
                lineHeight = baseLineHeight,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

        if (content.vocab.isNotEmpty()) {
            item {
                SectionHeader(text = stringResource(R.string.section_new_words))
            }
            items(content.vocab, key = { "vocab-${it.word}" }) { vocab ->
                VocabRow(vocab = vocab, onClick = { onVocabClick(vocab) })
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        if (content.keyPhrases.isNotEmpty()) {
            item {
                SectionHeader(text = stringResource(R.string.section_key_phrases))
            }
            items(content.keyPhrases, key = { "phrase-${it.phrase}" }) { phrase ->
                KeyPhraseRow(phrase = phrase)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.source_label, article.sourceName),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LevelSwitcher(
    available: List<Level>,
    current: Level,
    onChange: (Level) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        available.forEach { level ->
            AssistChip(
                onClick = { onChange(level) },
                label = { Text(level.displayName) },
                colors = if (level == current) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else AssistChipDefaults.assistChipColors(),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun VocabRow(vocab: Vocab, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vocab.word,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (vocab.pos.isNotBlank()) {
                        Text(
                            text = vocab.pos,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = vocab.meaningKo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KeyPhraseRow(phrase: KeyPhrase) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = phrase.phrase,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = phrase.meaningKo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
