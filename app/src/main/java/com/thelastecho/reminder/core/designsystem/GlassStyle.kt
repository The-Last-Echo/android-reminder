package com.thelastecho.reminder.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.core.preferences.ThemeStyle

/** The selected visual language, provided by [ReminderTheme]. */
val LocalThemeStyle = staticCompositionLocalOf { ThemeStyle.MATERIAL }

/** Applies the Glass surface treatment without changing the opacity of its content. */
@Composable
fun styledSurfaceColor(materialColor: Color): Color =
    if (LocalThemeStyle.current == ThemeStyle.MATERIAL) {
        materialColor
    } else {
        val scheme = MaterialTheme.colorScheme
        // A restrained accent wash gives Glass its own tone; translucency is applied to the fill only.
        lerp(materialColor, scheme.primary, 0.055f).copy(alpha = materialColor.alpha * 0.93f)
    }

/** Preserves each component's Material border and adds a lightly accent-tinted edge in Glass mode. */
@Composable
fun styledSurfaceBorder(materialBorder: BorderStroke? = null): BorderStroke? =
    if (LocalThemeStyle.current == ThemeStyle.MATERIAL) {
        materialBorder
    } else {
        val scheme = MaterialTheme.colorScheme
        BorderStroke(1.dp, lerp(scheme.outlineVariant, scheme.primary, 0.08f).copy(alpha = 0.48f))
    }

@Composable
fun styledSurfaceEdgeColor(): Color? =
    if (LocalThemeStyle.current == ThemeStyle.MATERIAL) {
        null
    } else {
        val scheme = MaterialTheme.colorScheme
        lerp(scheme.outlineVariant, scheme.primary, 0.08f).copy(alpha = 0.48f)
    }

@Composable
fun isGlassStyle(): Boolean = LocalThemeStyle.current == ThemeStyle.GLASS
