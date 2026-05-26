package com.vclass.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Custom colors for VClass
val GunadarmaBlue = Color(0xFF1565C0)
val GunadarmaDarkBlue = Color(0xFF0D47A1)
val GunadarmaLightBlue = Color(0xFF42A5F5)
val SuccessGreen = Color(0xFF4CAF50)
val WarningOrange = Color(0xFFFF9800)
val ErrorRed = Color(0xFFF44336)
val BackgroundGray = Color(0xFFF5F5F5)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF212121)
val TextGray = Color(0xFF757575)

// Dark theme colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkCard = Color(0xFF2C2C2C)
val DarkTextPrimary = Color(0xFFE0E0E0)
val DarkTextSecondary = Color(0xFF9E9E9E)
val DarkGunadarmaBlue = Color(0xFF64B5F6)
val DarkGunadarmaLightBlue = Color(0xFF90CAF9)
val DarkSuccessGreen = Color(0xFF81C784)
val DarkWarningOrange = Color(0xFFFFB74D)
val DarkErrorRed = Color(0xFFEF5350)

private val LightColorScheme = lightColorScheme(
    primary = GunadarmaBlue,
    onPrimary = Color.White,
    primaryContainer = GunadarmaLightBlue,
    secondary = PurpleGrey40,
    background = BackgroundGray,
    surface = CardWhite,
    onBackground = TextDark,
    onSurface = TextDark,
    error = ErrorRed,
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = TextGray
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGunadarmaBlue,
    onPrimary = Color(0xFF003258),
    primaryContainer = GunadarmaDarkBlue,
    onPrimaryContainer = DarkGunadarmaLightBlue,
    secondary = PurpleGrey80,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    error = DarkErrorRed,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary
)

@Composable
fun VClassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
