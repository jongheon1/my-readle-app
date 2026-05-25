package com.jongheon.myreadle.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jongheon.myreadle.ui.article.ArticleScreen
import com.jongheon.myreadle.ui.home.HomeScreen
import com.jongheon.myreadle.ui.settings.SettingsScreen

@Composable
fun MyReadleNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onArticleClick = { topicId -> nav.navigate(Routes.article(topicId)) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.ARTICLE,
            arguments = listOf(navArgument(Routes.ARG_TOPIC_ID) { type = NavType.StringType }),
        ) { entry ->
            val topicId = entry.arguments?.getString(Routes.ARG_TOPIC_ID).orEmpty()
            ArticleScreen(
                topicId = topicId,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
