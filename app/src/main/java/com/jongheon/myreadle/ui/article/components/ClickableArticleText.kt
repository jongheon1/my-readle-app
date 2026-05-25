package com.jongheon.myreadle.ui.article.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.jongheon.myreadle.domain.model.Vocab
import com.jongheon.myreadle.ui.theme.VocabUnderlineDark
import com.jongheon.myreadle.ui.theme.VocabUnderlineLight

private const val TAG_VOCAB = "VOCAB"

@Composable
fun ClickableArticleText(
    body: String,
    vocabs: List<Vocab>,
    onVocabClick: (Vocab) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
) {
    val accent = if (isSystemInDarkTheme()) VocabUnderlineDark else VocabUnderlineLight
    val annotated = remember(body, vocabs, accent) {
        buildVocabAnnotatedString(body, vocabs, accent)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val baseStyle = LocalTextStyle.current.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = fontSize,
        lineHeight = lineHeight,
    )

    Text(
        text = annotated,
        modifier = modifier.pointerInput(annotated) {
            detectTapGestures { pos ->
                val result = layout ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(pos)
                annotated.getStringAnnotations(TAG_VOCAB, offset, offset)
                    .firstOrNull()?.let { ann ->
                        val key = ann.item.lowercase()
                        val vocab = vocabs.firstOrNull { it.word.lowercase() == key }
                        vocab?.let(onVocabClick)
                    }
            }
        },
        style = baseStyle,
        onTextLayout = { layout = it },
    )
}

internal fun buildVocabAnnotatedString(
    body: String,
    vocabs: List<Vocab>,
    underlineColor: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    if (vocabs.isEmpty()) return AnnotatedString(body)

    val matches = findVocabMatches(body, vocabs.map { it.word })
    if (matches.isEmpty()) return AnnotatedString(body)

    return buildAnnotatedString {
        var cursor = 0
        for (m in matches) {
            if (m.start > cursor) append(body.substring(cursor, m.start))
            val literal = body.substring(m.start, m.end)
            withStyle(
                SpanStyle(
                    color = underlineColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                )
            ) {
                pushStringAnnotation(tag = TAG_VOCAB, annotation = m.canonical)
                append(literal)
                pop()
            }
            cursor = m.end
        }
        if (cursor < body.length) append(body.substring(cursor))
    }
}

internal data class VocabMatch(val start: Int, val end: Int, val canonical: String)

internal fun findVocabMatches(body: String, words: List<String>): List<VocabMatch> {
    if (words.isEmpty() || body.isEmpty()) return emptyList()
    val sorted = words.distinct().sortedByDescending { it.length }
    val raw = mutableListOf<VocabMatch>()
    for (word in sorted) {
        val pattern = vocabPattern(word) ?: continue
        pattern.findAll(body).forEach { m ->
            raw += VocabMatch(start = m.range.first, end = m.range.last + 1, canonical = word)
        }
    }
    raw.sortWith(compareBy({ it.start }, { -(it.end - it.start) }))

    val out = mutableListOf<VocabMatch>()
    var lastEnd = -1
    for (m in raw) {
        if (m.start >= lastEnd) {
            out += m
            lastEnd = m.end
        }
    }
    return out
}

private fun vocabPattern(word: String): Regex? {
    val trimmed = word.trim()
    if (trimmed.isEmpty()) return null
    val parts = trimmed.split(Regex("\\s+")).map { Regex.escape(it) }
    val joined = parts.joinToString("\\s+")
    val needsBoundary = parts.first().firstOrNull()?.isLetterOrDigit() == true
    val pattern = if (needsBoundary) "(?<![A-Za-z0-9])$joined(?![A-Za-z0-9])" else joined
    return Regex(pattern, RegexOption.IGNORE_CASE)
}
