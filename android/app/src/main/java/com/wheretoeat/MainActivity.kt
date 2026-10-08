package com.wheretoeat

import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalScreenScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Lets a piece (a restaurant's logo or name) glide between the list and its page. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.shared(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val screen = LocalScreenScope.current ?: return this
    return with(shared) {
        this@shared.sharedBounds(
            rememberSharedContentState(key),
            animatedVisibilityScope = screen,
            boundsTransform = { _, _ -> tween(durationMillis = 450) },
        )
    }
}

class MainActivity : ComponentActivity() {
    private val state by viewModels<AppState>()

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // The icon on its wine tile swells slightly and fades, revealing the list underneath.
        installSplashScreen().setOnExitAnimationListener { splash ->
            splash.iconView.animate().scaleX(1.35f).scaleY(1.35f).alpha(0f).setDuration(380)
                .setInterpolator(DecelerateInterpolator()).start()
            splash.view.animate().alpha(0f).setStartDelay(120).setDuration(320)
                .withEndAction { splash.remove() }.start()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhereToEatTheme(state.themeMode, state.palette) {
                RevealHost {
                    // Rebuilt on a theme change so every piece of text picks up the new colours (some kept the old
                    // ones until redrawn); the reveal animation covers the rebuild.
                    key(isDark(state.themeMode), state.palette) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        BackHandler(enabled = state.screen != Screen.Home) { state.back() }
                        SharedTransitionLayout {
                            AnimatedContent(
                                targetState = state.screen,
                                transitionSpec = {
                                    when {
                                        targetState == Screen.Detail ->
                                            (fadeIn(tween(300, delayMillis = 60)) + scaleIn(tween(350), initialScale = 0.96f)) togetherWith
                                                fadeOut(tween(200))
                                        initialState == Screen.Detail ->
                                            fadeIn(tween(300)) togetherWith
                                                (fadeOut(tween(200)) + scaleOut(tween(300), targetScale = 0.96f))
                                        targetState == Screen.Home ->
                                            (fadeIn() + slideInHorizontally { -it / 6 }) togetherWith
                                                (fadeOut() + slideOutHorizontally { it / 4 })
                                        else ->
                                            (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                                                (fadeOut() + slideOutHorizontally { -it / 6 })
                                    }
                                },
                                label = "screen",
                            ) { screen ->
                                CompositionLocalProvider(
                                    LocalSharedScope provides this@SharedTransitionLayout,
                                    LocalScreenScope provides this,
                                ) {
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
                }
            }
        }
    }
}
