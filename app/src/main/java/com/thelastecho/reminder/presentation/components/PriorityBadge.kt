package com.thelastecho.reminder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.thelastecho.reminder.core.designsystem.PriorityHigh
import com.thelastecho.reminder.core.designsystem.PriorityLow
import com.thelastecho.reminder.core.designsystem.PriorityMedium
import com.thelastecho.reminder.core.designsystem.PriorityNone
import com.thelastecho.reminder.domain.model.Priority

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    if (priority == Priority.NONE) return

    val (badgeBg, badgeText) = when (priority) {
        Priority.HIGH -> PriorityHigh.copy(alpha = 0.15f) to PriorityHigh
        Priority.MEDIUM -> PriorityMedium.copy(alpha = 0.15f) to PriorityMedium
        Priority.LOW -> PriorityLow.copy(alpha = 0.15f) to PriorityLow
        Priority.NONE -> Color.Transparent to PriorityNone
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeBg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(priorityLabelResource(priority)),
            style = MaterialTheme.typography.labelSmall,
            color = badgeText
        )
    }
}
