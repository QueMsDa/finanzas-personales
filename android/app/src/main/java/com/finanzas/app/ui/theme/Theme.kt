package com.finanzas.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary        = YapePurple,
    onPrimary      = androidx.compose.ui.graphics.Color.White,
    primaryContainer = YapeLilac,
    surface        = SurfaceLight,
    error          = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary        = YapeLilac,
    primaryContainer = YapePurple
)

@Composable
fun FinanzasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
