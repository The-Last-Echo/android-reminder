package com.thelastecho.reminder.presentation.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.presentation.components.CategoryIcon
import com.thelastecho.reminder.presentation.components.SectionHeading
import com.thelastecho.reminder.presentation.components.CustomColorPickerDialog
import kotlinx.coroutines.flow.collectLatest

private val CategoryColors = listOf(
    0xFF6750A4, 0xFF386A20, 0xFF0061A4, 0xFF9C4146, 0xFF7D5260, 0xFF006B5E,
    0xFF805500, 0xFF526070, 0xFF984061, 0xFF65558F, 0xFF4F6354, 0xFF7A5900,
    0xFFB3261E, 0xFF006874, 0xFF7D5260, 0xFF4A5F7A, 0xFF8C4A00, 0xFF635B71,
    0xFF008577, 0xFF8E4585, 0xFF536D3A, 0xFF9A406D, 0xFF59636D, 0xFF7A5C00
).map { it.toInt() }

private val CategoryIcons = listOf(
    "label", "person", "work", "shopping_cart", "home", "school", "favorite", "flight",
    "cafe", "grocery", "health", "build", "pets", "fitness", "music", "book",
    "event", "car", "money", "restaurant", "cleaning", "gift", "nature", "computer",
    "phone", "game", "beach", "weekend", "lock", "star"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showCustomColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CategoriesEffect.ShowSnackbar -> snackbarHostState.showSnackbar(context.getString(effect.messageRes))
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(CategoriesIntent.ShowAddDialog(true)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_category))
            }
        }
    ) { innerPadding ->
        if (state.categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.categories_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.categories_empty_details),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { SectionHeading(stringResource(R.string.categories), Modifier.padding(start = 4.dp, bottom = 8.dp)) }
                items(state.categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        onOpen = { viewModel.onIntent(CategoriesIntent.EditCategory(category.id)) },
                        onEdit = { viewModel.onIntent(CategoriesIntent.EditCategory(category.id)) },
                        onDelete = { showDeleteDialog = category.id }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }

    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(CategoriesIntent.ShowAddDialog(false)) },
            title = { Text(stringResource(if (state.editingCategoryId == null) R.string.add_category else R.string.edit_category)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = ReminderShapes.Input,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CategoryColorIcon(state.newCategoryColor, state.newCategoryIcon, Modifier.size(48.dp))
                            Text(
                                text = state.newCategoryName.ifBlank { stringResource(R.string.category_name) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.newCategoryName,
                        onValueChange = { viewModel.onIntent(CategoriesIntent.UpdateNewCategoryName(it)) },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ReminderShapes.Input
                    )

                    Text(stringResource(R.string.color), style = MaterialTheme.typography.titleSmall)
                    Surface(
                        onClick = { showColorPicker = true },
                        shape = ReminderShapes.Input,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryColorIcon(state.newCategoryColor, "label", Modifier.size(30.dp))
                            Text(stringResource(R.string.color), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.forEach { iconName ->
                            val selected = state.newCategoryIcon == iconName
                            Surface(
                                onClick = { viewModel.onIntent(CategoriesIntent.UpdateNewCategoryIcon(iconName)) },
                                shape = ReminderShapes.Input,
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.size(48.dp),
                                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CategoryIcon(
                                        iconName,
                                        Modifier.size(22.dp),
                                        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onIntent(CategoriesIntent.AddCategory) },
                    enabled = state.newCategoryName.isNotBlank()
                ) { Text(stringResource(if (state.editingCategoryId == null) R.string.add else R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(CategoriesIntent.ShowAddDialog(false)) }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showColorPicker && state.showAddDialog) {
        CategoryColorPickerDialog(
            selectedColor = state.newCategoryColor,
            onSelect = { color ->
                viewModel.onIntent(CategoriesIntent.UpdateNewCategoryColor(color))
                showColorPicker = false
            },
            onCustomColorClick = {
                showColorPicker = false
                showCustomColorPicker = true
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showCustomColorPicker && state.showAddDialog) {
        CustomColorPickerDialog(
            initialColor = state.newCategoryColor,
            title = stringResource(R.string.custom_color_title),
            description = stringResource(R.string.custom_category_color_description),
            onDismiss = { showCustomColorPicker = false },
            onApply = { color ->
                viewModel.onIntent(CategoriesIntent.UpdateNewCategoryColor(color))
                showCustomColorPicker = false
            }
        )
    }

    showDeleteDialog?.let { categoryId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_category)) },
            text = { Text(stringResource(R.string.delete_category_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(CategoriesIntent.DeleteCategory(categoryId))
                    showDeleteDialog = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember(category.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CategoryColorIcon(category.colorArgb.toInt(), category.iconName, Modifier.size(44.dp))
        Text(
            text = category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_category)) },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = { menuExpanded = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_category)) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    onClick = { menuExpanded = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun CategoryColorIcon(color: Int, iconName: String, modifier: Modifier = Modifier) {
    val background = Color(color)
    Surface(shape = CircleShape, color = background, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            CategoryIcon(
                iconName,
                Modifier.size(22.dp),
                if (background.luminance() > 0.48f) Color.Black else Color.White
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryColorPickerDialog(
    selectedColor: Int,
    onSelect: (Int) -> Unit,
    onCustomColorClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    (CategoryColors + selectedColor).distinct().forEach { color ->
                        val selected = selectedColor == color
                        val colorValue = Color(color)
                        Surface(
                            modifier = Modifier.size(48.dp).selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(color) }
                            ),
                            shape = CircleShape,
                            color = colorValue,
                            border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
                        ) {
                            if (selected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (colorValue.luminance() > 0.48f) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Surface(
                    onClick = onCustomColorClick,
                    shape = ReminderShapes.Input,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.accent_custom), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}
