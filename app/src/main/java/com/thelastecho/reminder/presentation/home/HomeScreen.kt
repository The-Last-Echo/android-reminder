package com.thelastecho.reminder.presentation.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.core.designsystem.ReminderDimensions
import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.usecase.ReminderFilter
import com.thelastecho.reminder.presentation.components.ReminderItem
import com.thelastecho.reminder.presentation.components.CounterBadge
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
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchFieldBounds by remember { mutableStateOf<Rect?>(null) }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun closeSearch() {
        isSearchActive = false
        viewModel.onIntent(HomeIntent.UpdateSearch(""))
        keyboardController?.hide()
    }

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
        modifier = modifier.fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it }
            .pointerInput(isSearchActive, searchFieldBounds) {
                if (isSearchActive) {
                    awaitPointerEventScope {
                        while (isSearchActive) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down = event.changes.firstOrNull { it.changedToDown() } ?: continue
                            val positionInWindow = rootCoordinates?.localToWindow(down.position)
                            if (positionInWindow != null && searchFieldBounds?.contains(positionInWindow) != true) {
                                closeSearch()
                            }
                        }
                    }
                }
            },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onIntent(HomeIntent.UpdateSearch(it)) },
                            placeholder = { Text(stringResource(com.thelastecho.reminder.R.string.search_reminders)) },
                            modifier = Modifier.fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .onGloballyPositioned { searchFieldBounds = it.boundsInWindow() },
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
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ReminderDimensions.HomeContentMaxWidth)
                    .fillMaxSize()
            ) {
            HomeFilterTabs(
                selectedFilter = state.selectedFilter,
                todayCount = state.todayCount,
                overdueCount = state.overdueCount,
                scheduledCount = state.scheduledCount,
                allCount = state.allCount,
                completedCount = state.completedCount,
                onFilterSelected = { filter ->
                    viewModel.onIntent(HomeIntent.SelectFilter(filter))
                }
            )

            if (state.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.onIntent(HomeIntent.SelectCategory(null)) },
                        label = { Text(stringResource(R.string.all_categories)) }
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

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onIntent(HomeIntent.Refresh) },
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                        text = when {
                                            state.searchQuery.isNotBlank() -> stringResource(R.string.no_search_results)
                                            state.selectedFilter == ReminderFilter.COMPLETED -> stringResource(
                                                R.string.no_completed_reminders)
                                            else -> stringResource(R.string.no_reminders_here)
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (state.searchQuery.isBlank() && state.selectedFilter != ReminderFilter.COMPLETED) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.tap_plus_button),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(items = state.reminders, key = { it.id }) { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                category = reminder.categoryId?.let(categoriesById::get),
                                modifier = Modifier.animateItem(),
                                onToggleComplete = {
                                    viewModel.onIntent(HomeIntent.ToggleComplete(reminder.id, !reminder.isCompleted))
                                },
                                onClick = { viewModel.onIntent(HomeIntent.EditReminder(reminder.id)) },
                                onDelete = { viewModel.onIntent(HomeIntent.DeleteReminder(reminder.id)) },
                                onToggleFavorite = { viewModel.onIntent(HomeIntent.ToggleFavorite(reminder.id)) },
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
    }
}

private data class FilterTabItem(
    val filter: ReminderFilter,
    val labelRes: Int
)

@Composable
private fun HomeFilterTabs(
    selectedFilter: ReminderFilter,
    todayCount: Int,
    overdueCount: Int,
    scheduledCount: Int,
    allCount: Int,
    completedCount: Int,
    onFilterSelected: (ReminderFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = remember {
        listOf(
            FilterTabItem(ReminderFilter.TODAY, com.thelastecho.reminder.R.string.today),
            FilterTabItem(ReminderFilter.OVERDUE, R.string.overdue),
            FilterTabItem(ReminderFilter.SCHEDULED, com.thelastecho.reminder.R.string.scheduled),
            FilterTabItem(ReminderFilter.ALL, com.thelastecho.reminder.R.string.all),
            FilterTabItem(ReminderFilter.COMPLETED, com.thelastecho.reminder.R.string.done)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ReminderDimensions.Medium, vertical = ReminderDimensions.Small),
        shape = ReminderShapes.Filter,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { item ->
                val count = when (item.filter) {
                    ReminderFilter.TODAY -> todayCount
                    ReminderFilter.OVERDUE -> overdueCount
                    ReminderFilter.SCHEDULED -> scheduledCount
                    ReminderFilter.ALL -> allCount
                    ReminderFilter.COMPLETED -> completedCount
                }
                val isSelected = selectedFilter == item.filter

                FilterChipItem(
                    title = stringResource(item.labelRes),
                    count = count,
                    isSelected = isSelected,
                    onClick = { onFilterSelected(item.filter) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(durationMillis = 200),
        label = "ChipBackgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "ChipContentColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = ReminderShapes.Control,
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1
            )
            CounterBadge(
                count = count,
                isSelected = isSelected,
                parentContentColor = contentColor
            )
        }
    }
}
