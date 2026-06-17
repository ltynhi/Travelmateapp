package com.example.travelmate.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light scheme – Sky Blue Pastel (default) ──────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = SkyBlue40,
    onPrimary           = SoftWhite,
    primaryContainer    = SkyBlue90,
    onPrimaryContainer  = SkyBlue20,

    secondary           = Peach40,
    onSecondary         = SoftWhite,
    secondaryContainer  = Peach90,
    onSecondaryContainer = Color(0xFF3A1510),

    tertiary            = Mint40,
    onTertiary          = SoftWhite,
    tertiaryContainer   = Mint90,
    onTertiaryContainer = Color(0xFF00201A),

    background          = CloudWhite,
    onBackground        = DarkSlate,

    surface             = SoftWhite,
    onSurface           = DarkSlate,
    surfaceVariant      = SkyBlue95,
    onSurfaceVariant    = Color(0xFF4A6080),

    outline             = MidGray,
    outlineVariant      = SkyBlue90,

    error               = Color(0xFFE53935),
    onError             = SoftWhite,
    errorContainer      = Color(0xFFFFDAD6),
    onErrorContainer    = Color(0xFF410002),

    inverseSurface      = DarkSlate,
    inverseOnSurface    = CloudWhite,
    inversePrimary      = SkyBlue80,

    scrim               = Color(0xFF000000)
)

// ── Dark scheme ───────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = SkyBlue60,
    onPrimary           = SkyBlue10,
    primaryContainer    = SkyBlue20,
    onPrimaryContainer  = SkyBlue90,

    secondary           = Peach60,
    onSecondary         = Color(0xFF3A1510),
    secondaryContainer  = Color(0xFF5A2018),
    onSecondaryContainer = Peach90,

    tertiary            = Mint60,
    onTertiary          = Color(0xFF00201A),
    tertiaryContainer   = Color(0xFF00382E),
    onTertiaryContainer = Mint90,

    background          = DarkBg,
    onBackground        = SkyBlue95,

    surface             = DarkSurface,
    onSurface           = SkyBlue90,
    surfaceVariant      = DarkCard,
    onSurfaceVariant    = MidGray,

    outline             = DarkBorder,
    outlineVariant      = Color(0xFF1E3A55),

    error               = Color(0xFFFF6B6B),
    onError             = DarkBg,
    errorContainer      = Color(0xFF4A0010),
    onErrorContainer    = Color(0xFFFFDAD6),

    inverseSurface      = SkyBlue95,
    inverseOnSurface    = DarkSlate,
    inversePrimary      = SkyBlue40,

    scrim               = Color(0xFF000000)
)

@Composable
fun TravelMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
