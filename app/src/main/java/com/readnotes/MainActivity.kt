package com.readnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.readnotes.theme.MyApplicationTheme
import com.readnotes.theme.NotesInk
import com.readnotes.theme.ThemeMode
import com.readnotes.theme.ThemePreferences
import com.readnotes.ui.ReadNotesApp

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
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Prefer dark nav glyphs on light theme. When the OEM can't do that
            // (MIUI often paints light glyphs anyway), we also draw our own
            // dark nav scrim below and force light glyphs via dark() style.
            DisposableEffect(darkTheme) {
                val transparent = Color.Transparent.toArgb()
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = SystemBarStyle.dark(transparent),
                )
                onDispose { }
            }
            MyApplicationTheme(themeMode = themeMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        ReadNotesApp(
                            themeMode = themeMode,
                            onThemeModeChange = { mode ->
                                themeMode = mode
                                themePrefs.set(mode)
                            },
                        )
                    }
                    // Edge-to-edge ignores window.navigationBarColor on modern
                    // Android / MIUI. Paint a real Compose scrim under the
                    // system nav buttons so light glyphs stay readable.
                    if (!darkTheme) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                                .background(NotesInk),
                        )
                    }
                }
            }
        }
    }
}
