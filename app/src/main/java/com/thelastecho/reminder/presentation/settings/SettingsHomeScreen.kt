package com.thelastecho.reminder.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.core.designsystem.styledSurfaceBorder
import com.thelastecho.reminder.core.designsystem.styledSurfaceColor
import com.thelastecho.reminder.presentation.components.CounterBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(
    trashViewModel: TrashViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (SettingsGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val trashState by trashViewModel.uiState.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsGroup.entries.forEach { group ->
                SettingsGroupCard(
                    group = group,
                    trashCount = if (group == SettingsGroup.DATA && !trashState.isLoading) trashState.deletedReminders.size else null,
                    onClick = { onNavigateToGroup(group) }
                )
            }
        }
    }
}

enum class SettingsGroup(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    GENERAL(R.string.settings_group_general_title, R.string.settings_group_general_description, Icons.Outlined.Info),
    APPEARANCE(R.string.settings_group_appearance_title, R.string.settings_group_appearance_description, Icons.Outlined.ColorLens),
    REMINDERS(R.string.settings_group_reminders_title, R.string.settings_group_reminders_description, Icons.Outlined.Category),
    NOTIFICATIONS(R.string.settings_group_notifications_title, R.string.settings_group_notifications_description, Icons.Outlined.Notifications),
    DATA(R.string.settings_group_data_title, R.string.settings_group_data_description, Icons.Outlined.Delete),
    PRIVACY(R.string.settings_group_privacy_title, R.string.settings_group_privacy_description, Icons.Outlined.Security),
    ABOUT(R.string.settings_group_about_title, R.string.settings_group_about_description, Icons.Outlined.Info)
}

@Composable
private fun SettingsGroupCard(
    group: SettingsGroup,
    trashCount: Int?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ReminderShapes.Card,
        color = styledSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = styledSurfaceBorder(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(group.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(group.titleRes), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(group.descriptionRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (group == SettingsGroup.DATA && trashCount != null) {
                CounterBadge(
                    count = trashCount,
                    isSelected = false,
                    parentContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
