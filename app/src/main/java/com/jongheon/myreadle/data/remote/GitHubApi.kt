package com.jongheon.myreadle.data.remote

import com.jongheon.myreadle.data.remote.dto.DailyArticlesDto
import com.jongheon.myreadle.data.remote.dto.IndexDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GitHubApi(
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            // raw.githubusercontent.com serves *.json files with
            // Content-Type: text/plain; charset=utf-8, so register the
            // kotlinx-json converter for both content types.
            json(json, ContentType.Application.Json)
            json(json, ContentType.Text.Plain)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        expectSuccess = true
    }

    suspend fun fetchIndex(): IndexDto = client.get("$baseUrl/index.json").body()

    suspend fun fetchDaily(date: String): DailyArticlesDto =
        client.get("$baseUrl/$date.json").body()

    companion object {
        const val DEFAULT_BASE_URL =
            "https://raw.githubusercontent.com/jongheon1/my-readle-content/main/articles"
        const val SUPPORTED_SCHEMA_VERSION = 1
    }
}
