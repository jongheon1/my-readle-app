package com.jongheon.myreadle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jongheon.myreadle.core.ServiceLocator
import com.jongheon.myreadle.ui.nav.MyReadleNavHost
import com.jongheon.myreadle.ui.theme.MyReadleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = ServiceLocator.settingsDataStore
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = com.jongheon.myreadle.ui.theme.ThemeMode.System)
            MyReadleTheme(themeMode = themeMode) {
                MyReadleNavHost()
            }
        }
    }
}
