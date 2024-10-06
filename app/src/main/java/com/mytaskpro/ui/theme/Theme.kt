package com.mytaskpro.ui.theme
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Existing color schemes
private val DarkColorScheme = darkColorScheme(
    primary = VibrantBlue,
    secondary = VibrantPurple,
    tertiary = VibrantOrange,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = White,
    onSecondary = White,
    onTertiary = Black,
    onBackground = White,
    onSurface = White,
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantBlue,
    secondary = VibrantPurple,
    tertiary = VibrantOrange,
    background = Color(0xFFF5F5F5),
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = Black,
    onBackground = Black,
    onSurface = Black,
)

// Existing new color schemes
private val ClassicLightColors = lightColorScheme(
    primary = Color(0xFF5C9EAD),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF333333),
    onPrimary = Color.White,
)

private val WarmSepiaColors = lightColorScheme(
    primary = Color(0xFFD9534F),
    background = Color(0xFFF4ECD8),
    onBackground = Color(0xFF4B3F3A),
    onPrimary = Color.White,
)

private val DarkThemeColors = darkColorScheme(
    primary = Color(0xFF00BCD4),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    onPrimary = Color.White,
)

private val HighContrastColors = lightColorScheme(
    primary = Color(0xFFFFD700),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onPrimary = Color.Black,
)

// New color schemes
private val SoftBlueColorScheme = lightColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    background = Color(0xFFE3F2FD),
    surface = Color(0xFFBBDEFB),
    onBackground = Color(0xFF1A237E),
    onSurface = Color(0xFF283593)
)

private val PinkColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFEBEE),
    surface = Color(0xFFFCE4EC),
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF4E342E)
)

private val PaperLightColorScheme = lightColorScheme(
    primary = Color(0xFFDAB894),
    onPrimary = Color(0xFF4A4A4A),
    secondary = Color(0xFFC4C4C4),
    background = Color(0xFFFFF8E1),
    surface = Color(0xFFFFF8E1),
    onBackground = Color(0xFF4A4A4A),
    onSurface = Color(0xFF4A4A4A)
)

private val PaperDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBDAA7E),
    onPrimary = Color(0xFFE0E0E0),
    secondary = Color(0xFF757575),
    background = Color(0xFF4A4A4A),
    surface = Color(0xFF4A4A4A),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

enum class AppTheme {
    Default, ClassicLight, WarmSepia, Dark, HighContrast, SoftBlue, Pink, PaperLight, PaperDark
}

@Composable
fun MyTaskProTheme(
    appTheme: AppTheme = AppTheme.Default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.Default -> when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
        AppTheme.ClassicLight -> ClassicLightColors
        AppTheme.WarmSepia -> WarmSepiaColors
        AppTheme.Dark -> DarkThemeColors
        AppTheme.HighContrast -> HighContrastColors
        AppTheme.SoftBlue -> SoftBlueColorScheme
        AppTheme.Pink -> PinkColorScheme
        AppTheme.PaperLight -> PaperLightColorScheme
        AppTheme.PaperDark -> PaperDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)