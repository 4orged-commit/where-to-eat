@file:OptIn(ExperimentalTextApi::class)

package com.wheretoeat

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.hypot

// ------------------------------------------------------------------------------------------------ fonts

private fun fraunces(weight: Int) = Font(
    R.font.fraunces, FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", 48f), // the display cut: higher contrast, more refined
        FontVariation.Setting("SOFT", 50f), // slightly softened serifs
    ),
)

private fun manrope(weight: Int) =
    Font(R.font.manrope, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Fraunces: an elegant serif for names, headings and discounts. */
val Serif = FontFamily(fraunces(400), fraunces(500), fraunces(600), fraunces(700))

/** Manrope: a clean sans for everything else. */
val Sans = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700))

private val AppTypography = Typography().let { t ->
    fun TextStyle.serif(weight: Int, tracking: Float = -0.2f) =
        copy(fontFamily = Serif, fontWeight = FontWeight(weight), letterSpacing = tracking.sp)
    fun TextStyle.sans() = copy(fontFamily = Sans)
    t.copy(
        displayLarge = t.displayLarge.serif(600),
        displayMedium = t.displayMedium.serif(600),
        displaySmall = t.displaySmall.serif(600),
        headlineLarge = t.headlineLarge.serif(600),
        headlineMedium = t.headlineMedium.serif(600),
        headlineSmall = t.headlineSmall.serif(600),
        titleLarge = t.titleLarge.serif(600),
        titleMedium = t.titleMedium.serif(600, 0f),
        titleSmall = t.titleSmall.sans(),
        bodyLarge = t.bodyLarge.sans(),
        bodyMedium = t.bodyMedium.sans(),
        bodySmall = t.bodySmall.sans(),
        labelLarge = t.labelLarge.sans(),
        labelMedium = t.labelMedium.sans(),
        labelSmall = t.labelSmall.sans(),
    )
}

// ------------------------------------------------------------------------------------------------ colours

/** "Classic": warm ivory and charcoal, with wine as the accent and gold for highlights. */
private val ClassicLight = lightColorScheme(
    primary = Color(0xFF8C2F39), onPrimary = Color.White,
    primaryContainer = Color(0xFFF6DADB), onPrimaryContainer = Color(0xFF3B0A10),
    secondary = Color(0xFF6B5B4E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE4D8), onSecondaryContainer = Color(0xFF2A2017),
    tertiary = Color(0xFF9A6A12), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7E3BF), onTertiaryContainer = Color(0xFF3A2700),
    background = Color(0xFFFAF7F2), onBackground = Color(0xFF1F1B17),
    surface = Color(0xFFFAF7F2), onSurface = Color(0xFF1F1B17),
    surfaceVariant = Color(0xFFECE4DA), onSurfaceVariant = Color(0xFF5E5650),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F2EC),
    surfaceContainer = Color(0xFFF3EEE7), surfaceContainerHigh = Color(0xFFEFE9E1),
    surfaceContainerHighest = Color(0xFFE9E2D9),
    outline = Color(0xFF8F857C), outlineVariant = Color(0xFFD9D0C6),
)

private val ClassicDark = darkColorScheme(
    primary = Color(0xFFF0A8AE), onPrimary = Color(0xFF4A0F18),
    primaryContainer = Color(0xFF6A2129), onPrimaryContainer = Color(0xFFFFDADC),
    secondary = Color(0xFFD7C3B2), onSecondary = Color(0xFF3B2E23),
    secondaryContainer = Color(0xFF3B3128), onSecondaryContainer = Color(0xFFF3E3D3),
    tertiary = Color(0xFFE5C07B), onTertiary = Color(0xFF3D2B00),
    tertiaryContainer = Color(0xFF5A4310), onTertiaryContainer = Color(0xFFFFE6B3),
    background = Color(0xFF141211), onBackground = Color(0xFFEDE6DF),
    surface = Color(0xFF141211), onSurface = Color(0xFFEDE6DF),
    surfaceVariant = Color(0xFF2E2A27), onSurfaceVariant = Color(0xFFB5ABA2),
    surfaceContainerLowest = Color(0xFF0F0D0C), surfaceContainerLow = Color(0xFF191716),
    surfaceContainer = Color(0xFF1D1A18), surfaceContainerHigh = Color(0xFF242120),
    surfaceContainerHighest = Color(0xFF2F2B29),
    outline = Color(0xFF8A817A), outlineVariant = Color(0xFF3D3833),
)

enum class ThemeMode { System, Light, Dark }
enum class Palette { Classic, MaterialYou }

@Composable
fun isDark(mode: ThemeMode) = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun WhereToEatTheme(mode: ThemeMode, palette: Palette, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        palette == Palette.MaterialYou && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> ClassicDark
        else -> ClassicLight
    }
    // Status / navigation bar icons follow the app's own light or dark choice, not just the phone's.
    LaunchedEffect(dark) {
        val transparent = AndroidColor.TRANSPARENT
        (ctx as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

// ------------------------------------------------------------------------------------------------ circular reveal

/**
 * Call with the tap point (in window coordinates) and the change to make: the screen is photographed, the change is
 * applied, and the new look then grows out of the tap point in a circle over the old one.
 */
val LocalReveal = staticCompositionLocalOf<(Offset, () -> Unit) -> Unit> { { _, change -> change() } }

@Composable
fun RevealHost(content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val radius = remember { Animatable(0f) }
    val reveal: (Offset, () -> Unit) -> Unit = { at, change ->
        if (snapshot == null) scope.launch {
            snapshot = layer.toImageBitmap()
            center = at
            radius.snapTo(0f)
            change()
            val far = listOf(Offset.Zero, Offset(size.width.toFloat(), 0f), Offset(0f, size.height.toFloat()),
                Offset(size.width.toFloat(), size.height.toFloat())).maxOf { hypot(it.x - at.x, it.y - at.y) }
            radius.animateTo(far, tween(durationMillis = 700, easing = FastOutSlowInEasing))
            snapshot = null
        }
    }
    CompositionLocalProvider(LocalReveal provides reveal) {
        Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            ) { content() }
            snapshot?.let { old ->
                Canvas(Modifier.fillMaxSize()) {
                    val hole = Path().apply { addOval(Rect(center, radius.value)) }
                    clipPath(hole, ClipOp.Difference) { drawImage(old) }
                }
            }
        }
    }
}
