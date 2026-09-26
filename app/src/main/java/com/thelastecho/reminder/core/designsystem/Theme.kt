package com.thelastecho.reminder.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.thelastecho.reminder.core.preferences.AccentColor

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight, onPrimary = OnPrimaryLight, primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight, secondary = SecondaryLight, onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight, onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight, onTertiary = OnTertiaryLight, tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight, background = BackgroundLight, onBackground = OnBackgroundLight,
    surface = SurfaceLight, onSurface = OnSurfaceLight, surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark, onPrimary = OnPrimaryDark, primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark, secondary = SecondaryDark, onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark, onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark, onTertiary = OnTertiaryDark, tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark, background = BackgroundDark, onBackground = OnBackgroundDark,
    surface = SurfaceDark, onSurface = OnSurfaceDark, surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private data class AccentTones(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
)

private fun AccentColor.tones() = when (this) {
    AccentColor.BLUE -> AccentTones(Color(0xFF0061A4), Color.White, Color(0xFFD1E4FF), Color(0xFF001D36), Color(0xFF9ECAFF), Color(0xFF003258), Color(0xFF00497D), Color(0xFFD1E4FF))
    AccentColor.VIOLET -> AccentTones(PrimaryLight, OnPrimaryLight, PrimaryContainerLight, OnPrimaryContainerLight, PrimaryDark, OnPrimaryDark, PrimaryContainerDark, OnPrimaryContainerDark)
    AccentColor.GREEN -> AccentTones(Color(0xFF386A20), Color.White, Color(0xFFB7F397), Color(0xFF082100), Color(0xFF9DD67E), Color(0xFF173800), Color(0xFF28500C), Color(0xFFB7F397))
    AccentColor.TEAL -> AccentTones(Color(0xFF006A60), Color.White, Color(0xFF9CF1E4), Color(0xFF00201C), Color(0xFF80D5C9), Color(0xFF003731), Color(0xFF005047), Color(0xFF9CF1E4))
    AccentColor.ORANGE -> AccentTones(Color(0xFF8A5000), Color.White, Color(0xFFFFDDB3), Color(0xFF2C1600), Color(0xFFFFB95F), Color(0xFF482900), Color(0xFF673D00), Color(0xFFFFDDB3))
    AccentColor.RED -> AccentTones(Color(0xFFBA1A1A), Color.White, Color(0xFFFFDAD6), Color(0xFF410002), Color(0xFFFFB4AB), Color(0xFF690005), Color(0xFF93000A), Color(0xFFFFDAD6))
    AccentColor.PINK -> AccentTones(Color(0xFF9A406D), Color.White, Color(0xFFFFD8E8), Color(0xFF3E0025), Color(0xFFFFB0D0), Color(0xFF5E113F), Color(0xFF7B2955), Color(0xFFFFD8E8))
}

private fun ColorScheme.withAccent(accent: AccentColor, dark: Boolean): ColorScheme {
    val tones = accent.tones()
    return if (dark) copy(
        primary = tones.darkPrimary, onPrimary = tones.darkOnPrimary,
        primaryContainer = tones.darkContainer, onPrimaryContainer = tones.darkOnContainer,
        secondary = tones.darkPrimary, onSecondary = tones.darkOnPrimary,
        secondaryContainer = tones.darkContainer, onSecondaryContainer = tones.darkOnContainer,
        tertiary = tones.darkPrimary, onTertiary = tones.darkOnPrimary,
        tertiaryContainer = tones.darkContainer, onTertiaryContainer = tones.darkOnContainer,
        inversePrimary = tones.lightPrimary, surfaceTint = tones.darkPrimary
    ) else copy(
        primary = tones.lightPrimary, onPrimary = tones.lightOnPrimary,
        primaryContainer = tones.lightContainer, onPrimaryContainer = tones.lightOnContainer,
        secondary = tones.lightPrimary, onSecondary = tones.lightOnPrimary,
        secondaryContainer = tones.lightContainer, onSecondaryContainer = tones.lightOnContainer,
        tertiary = tones.lightPrimary, onTertiary = tones.lightOnPrimary,
        tertiaryContainer = tones.lightContainer, onTertiaryContainer = tones.lightOnContainer,
        inversePrimary = tones.darkPrimary, surfaceTint = tones.lightPrimary
    )
}

@Composable
fun ReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoledMode: Boolean = false,
    dynamicColor: Boolean = true,
    accentColor: AccentColor = AccentColor.VIOLET,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val accentScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) baseScheme else baseScheme.withAccent(accentColor, darkTheme)
    val colorScheme = if (darkTheme && isAmoledMode) accentScheme.copy(
        background = AmoledBackground,
        surface = AmoledSurface,
        surfaceContainer = AmoledSurfaceContainer,
        surfaceContainerLow = AmoledSurface,
        surfaceVariant = AmoledSurfaceVariant
    ) else accentScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
