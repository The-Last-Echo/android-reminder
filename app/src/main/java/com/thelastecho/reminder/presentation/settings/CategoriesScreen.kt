package com.thelastecho.reminder.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.thelastecho.reminder.presentation.components.CategoryIcon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
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
        var selectedColor by remember { mutableStateOf(state.newCategoryColor) }
        
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(CategoriesIntent.ShowAddDialog(false)) },
            title = { Text(stringResource(if (state.editingCategoryId == null) com.thelastecho.reminder.R.string.add_category else com.thelastecho.reminder.R.string.edit_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.newCategoryName,
                        onValueChange = { viewModel.onIntent(CategoriesIntent.UpdateNewCategoryName(it)) },
                        label = { Text(stringResource(com.thelastecho.reminder.R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(stringResource(com.thelastecho.reminder.R.string.color), style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colors = listOf(0xFF4CAF50,0xFF2196F3,0xFFFF9800,0xFFF44336,0xFF9C27B0,0xFF607D8B,0xFF009688,0xFF3F51B5,0xFFE91E63,0xFF795548,0xFFCDDC39,0xFF00BCD4,0xFF673AB7,0xFF8BC34A,0xFFFFC107,0xFF546E7A).map { it.toInt() }
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable {
                                        selectedColor = color
                                        viewModel.onIntent(CategoriesIntent.UpdateNewCategoryColor(color))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                    Text(stringResource(com.thelastecho.reminder.R.string.category_icon), style = MaterialTheme.typography.labelMedium)
                    val icons = listOf("label", "person", "work", "shopping_cart", "home", "school", "favorite", "flight", "cafe", "grocery", "health", "build")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        icons.forEach { iconName ->
                            Box(Modifier.size(42.dp).clip(CircleShape).background(if (state.newCategoryIcon == iconName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).clickable { viewModel.onIntent(CategoriesIntent.UpdateNewCategoryIcon(iconName)) }, contentAlignment = Alignment.Center) {
                                CategoryIcon(iconName, Modifier.size(22.dp), if (state.newCategoryIcon == iconName) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
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
        shape = RoundedCornerShape(12.dp),
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
                    CategoryIcon(category.iconName, Modifier.size(20.dp), Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null) }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(com.thelastecho.reminder.R.string.delete_category))
            }
        }
    }
}
