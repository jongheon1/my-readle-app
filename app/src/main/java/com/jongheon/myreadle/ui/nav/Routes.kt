package com.jongheon.myreadle.ui.nav

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"

    private const val ARTICLE_ROUTE_BASE = "article"
    const val ARG_TOPIC_ID = "topicId"
    const val ARTICLE = "$ARTICLE_ROUTE_BASE/{$ARG_TOPIC_ID}"

    fun article(topicId: String): String = "$ARTICLE_ROUTE_BASE/$topicId"
}
