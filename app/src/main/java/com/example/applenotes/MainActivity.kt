package com.example.applenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.applenotes.theme.MyApplicationTheme
import com.example.applenotes.theme.ThemeMode
import com.example.applenotes.theme.ThemePreferences
import com.example.applenotes.ui.AppleNotesApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run BEFORE super.onCreate to back-port the Android-12+ splash
        // API to older releases. Theme is configured in res/values/themes.xml
        // (Theme.App.Starting) and switches to Theme.MyApplication once the
        // first frame of content is ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themePrefs = ThemePreferences(this)
        setContent {
            var themeMode by remember { mutableStateOf(themePrefs.get()) }
            // Re-read system dark flag when mode is SYSTEM so rotation / night
            // changes still flip the palette.
            @Suppress("UNUSED_VARIABLE")
            val systemDark = isSystemInDarkTheme()
            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppleNotesApp(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            themePrefs.set(mode)
                        },
                    )
                }
            }
        }
    }
}
