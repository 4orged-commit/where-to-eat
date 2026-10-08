package com.wheretoeat

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val state by viewModels<AppState>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhereToEatTheme {
                BackHandler(enabled = state.screen != Screen.Home) { state.back() }
                AnimatedContent(
                    targetState = state.screen,
                    transitionSpec = {
                        if (targetState == Screen.Home) {
                            (fadeIn() + slideInHorizontally { -it / 6 }) togetherWith
                                (fadeOut() + slideOutHorizontally { it / 4 })
                        } else {
                            (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                                (fadeOut() + slideOutHorizontally { -it / 6 })
                        }
                    },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(state)
                        Screen.Detail -> DetailScreen(state)
                        Screen.Settings -> SettingsScreen(state)
                    }
                }
            }
        }
    }
}

@Composable
fun WhereToEatTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(ctx)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFF7FD6A4), background = Color(0xFF0F1512), surface = Color(0xFF0F1512))
        else -> lightColorScheme(primary = Color(0xFF1F6F4A))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
