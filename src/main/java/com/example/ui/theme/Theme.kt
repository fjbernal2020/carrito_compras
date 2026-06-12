package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EcoGreenPrimaryDark,
    secondary = EcoGreenSecondaryDark,
    tertiary = EcoGreenTertiaryDark,
    background = EcoGreenBackgroundDark,
    surface = EcoGreenSurfaceDark,
    onPrimary = Color(0xFF003912),        // Deep green for high contrast over light-green primary
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE1E3DF),     // Clean near-white for legible texts in dark theme
    onSurface = Color(0xFFE1E3DF),        // Clean near-white for texts on cards and dialogs
    surfaceVariant = Color(0xFF2C3E30),   // Slightly brighter card/input background in dark mode
    onSurfaceVariant = Color(0xFFC0C9BC)  // Legible gray-green for label/placeholder text
)

private val LightColorScheme = lightColorScheme(
    primary = EcoGreenPrimary,
    secondary = EcoGreenSecondary,
    tertiary = EcoGreenTertiary,
    background = EcoGreenBackground,
    surface = EcoGreenSurface,
    onPrimary = Color.White,              // White text over primary green
    onSecondary = Color.White,
    onBackground = Color(0xFF191C19),     // Deep forest-black for perfectly readable body/input texts
    onSurface = Color(0xFF191C19),        // Deep forest-black for texts inside white cards/dialogs
    surfaceVariant = Color(0xFFE1E5DC),   // Soft grey-green for input boxes backgrounds
    onSurfaceVariant = Color(0xFF434842)  // Medium green-grey for helper/placeholder texts
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
