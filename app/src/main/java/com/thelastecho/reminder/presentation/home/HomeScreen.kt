package com.thelastecho.reminder.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.usecase.ReminderFilter
import com.thelastecho.reminder.presentation.components.ReminderItem
import com.thelastecho.reminder.presentation.components.CategoryIcon
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToEditor -> onNavigateToEditor(effect.reminderId)
                is HomeEffect.NavigateToSettings -> onNavigateToSettings()
                is HomeEffect.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(com.thelastecho.reminder.R.string.reminder_deleted),
                        actionLabel = context.getString(com.thelastecho.reminder.R.string.undo)
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(HomeIntent.UndoDelete(effect.reminderId))
                    }
                }
            }
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onIntent(HomeIntent.UpdateSearch(it)) },
                            placeholder = { Text(stringResource(com.thelastecho.reminder.R.string.search_reminders)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onIntent(HomeIntent.UpdateSearch("")) }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(com.thelastecho.reminder.R.string.clear_search))
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(com.thelastecho.reminder.R.string.reminders),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.onIntent(HomeIntent.UpdateSearch(""))
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = stringResource(if (isSearchActive) com.thelastecho.reminder.R.string.close_search else com.thelastecho.reminder.R.string.search)
                        )
                    }
                    IconButton(onClick = { viewModel.onIntent(HomeIntent.OpenSettings) }) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(com.thelastecho.reminder.R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButtonPosition = if (state.addButtonOnLeft) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(HomeIntent.CreateNewReminder) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(com.thelastecho.reminder.R.string.create_reminder))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.onIntent(HomeIntent.Refresh) },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterSummaryCard(
                            title = stringResource(com.thelastecho.reminder.R.string.today),
                            count = state.todayCount,
                            icon = Icons.Outlined.Today,
                            isSelected = state.selectedFilter == ReminderFilter.TODAY,
                            onClick = { viewModel.onIntent(HomeIntent.SelectFilter(ReminderFilter.TODAY)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterSummaryCard(
                            title = stringResource(com.thelastecho.reminder.R.string.scheduled),
                            count = state.scheduledCount,
                            icon = Icons.Outlined.Event,
                            isSelected = state.selectedFilter == ReminderFilter.SCHEDULED,
                            onClick = { viewModel.onIntent(HomeIntent.SelectFilter(ReminderFilter.SCHEDULED)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterSummaryCard(
                            title = stringResource(com.thelastecho.reminder.R.string.all),
                            count = state.allCount,
                            icon = Icons.Outlined.Notifications,
                            isSelected = state.selectedFilter == ReminderFilter.ALL,
                            onClick = { viewModel.onIntent(HomeIntent.SelectFilter(ReminderFilter.ALL)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterSummaryCard(
                            title = stringResource(com.thelastecho.reminder.R.string.done),
                            count = state.completedCount,
                            icon = Icons.Outlined.CheckCircle,
                            isSelected = state.selectedFilter == ReminderFilter.COMPLETED,
                            onClick = { viewModel.onIntent(HomeIntent.SelectFilter(ReminderFilter.COMPLETED)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (state.categories.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedCategoryId == null,
                                onClick = { viewModel.onIntent(HomeIntent.SelectCategory(null)) },
                                label = { Text(stringResource(com.thelastecho.reminder.R.string.all_categories)) }
                            )
                            state.categories.forEach { category ->
                                FilterChip(
                                    selected = state.selectedCategoryId == category.id,
                                    onClick = { viewModel.onIntent(HomeIntent.SelectCategory(category.id)) },
                                    label = { Text(category.name) },
                                    leadingIcon = { CategoryIcon(category.iconName, Modifier.size(18.dp), Color(category.colorArgb)) }
                                )
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                } else if (state.reminders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp).padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (state.selectedFilter == ReminderFilter.COMPLETED) {
                                        stringResource(com.thelastecho.reminder.R.string.no_completed_reminders)
                                    } else {
                                        stringResource(com.thelastecho.reminder.R.string.no_reminders_here)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(com.thelastecho.reminder.R.string.tap_plus_button),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    items(items = state.reminders, key = { it.id }) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            modifier = Modifier.animateItem(),
                            onToggleComplete = {
                                viewModel.onIntent(HomeIntent.ToggleComplete(reminder.id, !reminder.isCompleted))
                            },
                            onClick = { viewModel.onIntent(HomeIntent.EditReminder(reminder.id)) },
                            onDelete = { viewModel.onIntent(HomeIntent.DeleteReminder(reminder.id)) },
                            onToggleSubTask = { subTask ->
                                viewModel.onIntent(HomeIntent.ToggleSubTask(reminder.id, subTask.id, !subTask.isCompleted))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSummaryCard(
    title: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = java.text.NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0]).format(count),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
	}
    }
