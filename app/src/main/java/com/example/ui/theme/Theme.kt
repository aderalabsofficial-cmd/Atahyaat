package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.data.DarkThemeMode

private val AtahyaatDarkColorScheme = darkColorScheme(
    primary = SleekDarkPrimary,
    onPrimary = SleekDarkOnPrimary,
    primaryContainer = SleekDarkPrimaryContainer,
    onPrimaryContainer = SleekDarkOnPrimaryContainer,
    secondary = SleekDarkSecondary,
    onSecondary = SleekDarkOnSecondary,
    secondaryContainer = SleekDarkSecondaryContainer,
    onSecondaryContainer = SleekDarkOnSecondaryContainer,
    background = SleekDarkBackground,
    surface = SleekDarkSurface,
    onBackground = SleekDarkOnSurface,
    onSurface = SleekDarkOnSurface,
    surfaceVariant = SleekDarkSurfaceVariant,
    onSurfaceVariant = SleekDarkOnSurfaceVariant
)

private val AtahyaatLightColorScheme = lightColorScheme(
    primary = SleekLightPrimary,
    onPrimary = SleekLightOnPrimary,
    primaryContainer = SleekLightPrimaryContainer,
    onPrimaryContainer = SleekLightOnPrimaryContainer,
    secondary = SleekLightSecondary,
    onSecondary = SleekLightOnSecondary,
    secondaryContainer = SleekLightSecondaryContainer,
    onSecondaryContainer = SleekLightOnSecondaryContainer,
    background = SleekLightBackground,
    surface = SleekLightSurface,
    onBackground = SleekLightOnSurface,
    onSurface = SleekLightOnSurface,
    surfaceVariant = SleekLightSurfaceVariant,
    onSurfaceVariant = SleekLightOnSurfaceVariant
)

@Composable
fun AtahyaatTheme(
    themeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        DarkThemeMode.SYSTEM -> isSystemInDarkTheme()
        DarkThemeMode.LIGHT -> false
        DarkThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) {
        AtahyaatDarkColorScheme
    } else {
        AtahyaatLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
