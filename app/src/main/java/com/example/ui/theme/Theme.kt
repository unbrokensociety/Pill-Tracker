package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.data.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = TelegramDarkPrimary,
    onPrimary = TelegramDarkOnPrimary,
    primaryContainer = TelegramDarkPrimaryContainer,
    onPrimaryContainer = TelegramDarkOnPrimaryContainer,
    secondary = TelegramDarkSecondary,
    onSecondary = TelegramDarkOnSecondary,
    secondaryContainer = TelegramDarkSecondaryContainer,
    onSecondaryContainer = TelegramDarkOnSecondaryContainer,
    tertiary = TelegramDarkTertiary,
    onTertiary = TelegramDarkOnTertiary,
    tertiaryContainer = TelegramDarkTertiaryContainer,
    onTertiaryContainer = TelegramDarkOnTertiaryContainer,
    background = TelegramDarkBackground,
    onBackground = TelegramDarkOnBackground,
    surface = TelegramDarkSurface,
    onSurface = TelegramDarkOnSurface,
    surfaceVariant = TelegramDarkSurfaceVariant,
    onSurfaceVariant = TelegramDarkOnSurfaceVariant,
    outline = TelegramDarkOutline,
    outlineVariant = TelegramDarkOutlineVariant,
    error = TelegramDarkError,
    onError = TelegramDarkOnError,
    errorContainer = TelegramDarkErrorContainer,
    onErrorContainer = TelegramDarkOnErrorContainer
)

private val BrandColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandOnTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    background = BrandBackground,
    onBackground = BrandOnBackground,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
    error = BrandError,
    onError = BrandOnError,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandOnErrorContainer
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.BRAND -> BrandColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

