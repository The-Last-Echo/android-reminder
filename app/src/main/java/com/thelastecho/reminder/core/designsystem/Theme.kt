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
import com.thelastecho.reminder.core.preferences.resolveAccentSeedArgb

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

private fun ColorScheme.withAccentSeed(seedArgb: Int, dark: Boolean): ColorScheme {
    val generated = com.materialkolor.scheme.SchemeTonalSpot(
        com.materialkolor.hct.Hct.fromInt(seedArgb), dark, 0.0
    )
    val roles = com.materialkolor.dynamiccolor.MaterialDynamicColors(dark)
    fun color(role: com.materialkolor.dynamiccolor.DynamicColor) = Color(role.getArgb(generated))
    return copy(
        primary = color(roles.primary()), onPrimary = color(roles.onPrimary()),
        primaryContainer = color(roles.primaryContainer()), onPrimaryContainer = color(roles.onPrimaryContainer()),
        inversePrimary = color(roles.inversePrimary()),
        secondary = color(roles.secondary()), onSecondary = color(roles.onSecondary()),
        secondaryContainer = color(roles.secondaryContainer()), onSecondaryContainer = color(roles.onSecondaryContainer()),
        tertiary = color(roles.tertiary()), onTertiary = color(roles.onTertiary()),
        tertiaryContainer = color(roles.tertiaryContainer()), onTertiaryContainer = color(roles.onTertiaryContainer()),
        background = color(roles.background()), onBackground = color(roles.onBackground()),
        surface = color(roles.surface()), onSurface = color(roles.onSurface()),
        surfaceVariant = color(roles.surfaceVariant()), onSurfaceVariant = color(roles.onSurfaceVariant()),
        surfaceTint = color(roles.surfaceTint()),
        inverseSurface = color(roles.inverseSurface()), inverseOnSurface = color(roles.inverseOnSurface()),
        error = color(roles.error()), onError = color(roles.onError()),
        errorContainer = color(roles.errorContainer()), onErrorContainer = color(roles.onErrorContainer()),
        outline = color(roles.outline()), outlineVariant = color(roles.outlineVariant()),
        scrim = color(roles.scrim()),
        surfaceBright = color(roles.surfaceBright()), surfaceDim = color(roles.surfaceDim()),
        surfaceContainer = color(roles.surfaceContainer()), surfaceContainerHigh = color(roles.surfaceContainerHigh()),
        surfaceContainerHighest = color(roles.surfaceContainerHighest()), surfaceContainerLow = color(roles.surfaceContainerLow()),
        surfaceContainerLowest = color(roles.surfaceContainerLowest())
    )
}

@Composable
fun ReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoledMode: Boolean = false,
    dynamicColor: Boolean = true,
    accentColor: AccentColor = AccentColor.VIOLET,
    customAccentColor: Int? = null,
    useCustomAccent: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val manualSeed = if (useCustomAccent) customAccentColor else null
    val accentScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) baseScheme else baseScheme.withAccentSeed(manualSeed ?: resolveAccentSeedArgb(accentColor, customAccentColor, useCustomAccent), darkTheme)
    val colorScheme = if (darkTheme && isAmoledMode) accentScheme.copy(
        background = AmoledBackground,
        surface = AmoledSurface,
        surfaceContainer = AmoledSurfaceContainer,
        surfaceContainerLow = AmoledSurface,
        surfaceVariant = AmoledSurfaceVariant
    ) else accentScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
