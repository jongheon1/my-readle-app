package com.jongheon.myreadle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Vocab(
    val word: String,
    val pos: String,
    val meaningKo: String,
)

@Serializable
data class KeyPhrase(
    val phrase: String,
    val meaningKo: String,
)
