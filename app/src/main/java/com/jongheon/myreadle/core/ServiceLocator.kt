package com.jongheon.myreadle.core

import android.content.Context
import com.jongheon.myreadle.data.local.MyReadleDatabase
import com.jongheon.myreadle.data.local.SettingsDataStore
import com.jongheon.myreadle.data.remote.GitHubApi
import com.jongheon.myreadle.data.repository.ArticleRepository

object ServiceLocator {
    private var appContext: Context? = null
    private val lock = Any()

    private var _db: MyReadleDatabase? = null
    private var _api: GitHubApi? = null
    private var _settings: SettingsDataStore? = null
    private var _repo: ArticleRepository? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context =
        appContext ?: error("ServiceLocator.init was not called from Application.onCreate")

    val database: MyReadleDatabase
        get() = synchronized(lock) {
            _db ?: MyReadleDatabase.build(requireContext()).also { _db = it }
        }

    val api: GitHubApi
        get() = synchronized(lock) { _api ?: GitHubApi().also { _api = it } }

    val settingsDataStore: SettingsDataStore
        get() = synchronized(lock) {
            _settings ?: SettingsDataStore(requireContext()).also { _settings = it }
        }

    val articleRepository: ArticleRepository
        get() = synchronized(lock) {
            _repo ?: ArticleRepository(
                api = api,
                dao = database.articleDao(),
                settings = settingsDataStore,
            ).also { _repo = it }
        }
}
