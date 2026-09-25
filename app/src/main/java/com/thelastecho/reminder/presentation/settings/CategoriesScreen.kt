package com.thelastecho.reminder.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.presentation.components.CategoryIcon
import com.thelastecho.reminder.presentation.components.SectionHeading
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CategoriesEffect.ShowSnackbar -> snackbarHostState.showSnackbar(context.getString(effect.messageRes))
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.thelastecho.reminder.R.string.manage_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.thelastecho.reminder.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(CategoriesIntent.ShowAddDialog(true)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(com.thelastecho.reminder.R.string.create_category))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.categories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(320.dp).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(com.thelastecho.reminder.R.string.categories_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(com.thelastecho.reminder.R.string.categories_empty_details),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    SectionHeading(stringResource(com.thelastecho.reminder.R.string.categories))
                }
            }
            items(
                items = state.categories,
                key = { it.id }
            ) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { viewModel.onIntent(CategoriesIntent.EditCategory(category.id)) },
                    onDelete = { showDeleteDialog = category.id }
                )
            }
        }
    }

    // Add Category Dialog
    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(CategoriesIntent.ShowAddDialog(false)) },
            title = { Text(stringResource(if (state.editingCategoryId == null) com.thelastecho.reminder.R.string.add_category else com.thelastecho.reminder.R.string.edit_category)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = ReminderShapes.Card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(state.newCategoryColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIcon(
                                    state.newCategoryIcon,
                                    Modifier.size(22.dp),
                                    if (Color(state.newCategoryColor).luminance() > 0.45f) Color.Black else Color.White
                                )
                            }
                            Text(
                                state.newCategoryName.ifBlank {
                                    context.getString(com.thelastecho.reminder.R.string.category_name)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.newCategoryName,
                        onValueChange = { viewModel.onIntent(CategoriesIntent.UpdateNewCategoryName(it)) },
                        label = { Text(stringResource(com.thelastecho.reminder.R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(stringResource(com.thelastecho.reminder.R.string.color), style = MaterialTheme.typography.labelLarge)
                    val colors = listOf(
                        0xFF6750A4, 0xFF386A20, 0xFF0061A4, 0xFF9C4146,
                        0xFF7D5260, 0xFF006B5E, 0xFF805500, 0xFF526070,
                        0xFF984061, 0xFF65558F, 0xFF4F6354, 0xFF7A5900
                    ).map { it.toInt() }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .selectable(
                                        selected = state.newCategoryColor == color,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.onIntent(CategoriesIntent.UpdateNewCategoryColor(color)) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.newCategoryColor == color) {
                                    Box(
                                        Modifier.size(14.dp).clip(CircleShape)
                                            .background(if (Color(color).luminance() > 0.45f) Color.Black else Color.White)
                                    )
                                }
                            }
                        }
                    }

                    Text(stringResource(com.thelastecho.reminder.R.string.category_icon), style = MaterialTheme.typography.labelLarge)
                    val icons = listOf("label", "person", "work", "shopping_cart", "home", "school", "favorite", "flight", "cafe", "grocery", "health", "build")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.forEach { iconName ->
                            val selected = state.newCategoryIcon == iconName
                            Box(
                                Modifier.size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.onIntent(CategoriesIntent.UpdateNewCategoryIcon(iconName)) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIcon(
                                    iconName,
                                    Modifier.size(22.dp),
                                    if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onIntent(CategoriesIntent.AddCategory) },
                    enabled = state.newCategoryName.isNotBlank()
                ) {
                    Text(stringResource(if (state.editingCategoryId == null) com.thelastecho.reminder.R.string.add else com.thelastecho.reminder.R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(CategoriesIntent.ShowAddDialog(false)) }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.cancel))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { categoryId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(com.thelastecho.reminder.R.string.delete_category)) },
            text = { Text(stringResource(com.thelastecho.reminder.R.string.delete_category_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(CategoriesIntent.DeleteCategory(categoryId))
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(com.thelastecho.reminder.R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: com.thelastecho.reminder.domain.model.Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = ReminderShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(category.colorArgb)),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(category.iconName, Modifier.size(20.dp), if (Color(category.colorArgb).luminance() > 0.45f) Color.Black else Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(com.thelastecho.reminder.R.string.edit_category)) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(com.thelastecho.reminder.R.string.delete_category))
            }
        }
    }
}
