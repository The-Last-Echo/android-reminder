package com.thelastecho.reminder.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CounterBadge(
    count: Int,
    isSelected: Boolean,
    parentContentColor: Color,
    modifier: Modifier = Modifier
) {
    val badgeBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            parentContentColor.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "BadgeBackgroundColor"
    )

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = badgeBackgroundColor,
        contentColor = parentContentColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedCounterText(count = count)
        }
    }
}

@Composable
fun AnimatedCounterText(
    count: Int,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]

    AnimatedContent(
        targetState = count,
        transitionSpec = {
            (scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(150))) togetherWith
                (scaleOut(
                    targetScale = 1.3f,
                    animationSpec = tween(150)
                ) + fadeOut(animationSpec = tween(150)))
        },
        label = "CounterPopAnimation",
        modifier = modifier
    ) { targetCount ->
        Text(
            text = java.text.NumberFormat.getIntegerInstance(locale).format(targetCount),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}
