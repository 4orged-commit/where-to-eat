package com.wheretoeat

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView

enum class Haptic {
    /** A light tick: filter chips, location, refresh. */
    Tick,
    /** A firmer confirm: favorites, theme changes. */
    Confirm,
}

/** Subtle vibration feedback (follows the phone's own touch-feedback setting). */
@Composable
fun rememberHaptics(): (Haptic) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { h ->
            view.performHapticFeedback(
                when (h) {
                    Haptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
                    Haptic.Confirm ->
                        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
                },
            )
        }
    }
}

/** A soft light sweep across a placeholder while something (a logo) loads. */
@Composable
fun Modifier.shimmer(): Modifier {
    val sweep by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sweep",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val light = MaterialTheme.colorScheme.surfaceContainerLowest
    return drawBehind {
        val w = size.width
        drawRect(Brush.linearGradient(listOf(base, light, base), start = Offset(w * sweep - w, 0f), end = Offset(w * sweep, size.height)))
    }
}
