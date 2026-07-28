package com.salmanlaghari.risingflix.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = CyanGlow,
    tertiary = GoldAccent,
    background = DeepBlueBg,
    surface = CardSurfaceDark,
    onPrimary = TrueBlack,
    onSecondary = TextMain,
    onTertiary = TrueBlack,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun RisingFlixTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val activity = view.context as? Activity
                activity?.let { act ->
                    val window = act.window
                    window.statusBarColor = DeepBlueBg.toArgb()
                    window.navigationBarColor = TrueBlack.toArgb()
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false
                }
            } catch (e: Exception) {
                // Silently handle if context is not an Activity
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
