package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.domain.model.Category

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isDeleting: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryColor: Int = 0xFF4CAF50.toInt(),
    val newCategoryIcon: String = "label",
    val editingCategoryId: Long? = null,
    val showAddDialog: Boolean = false
)

sealed interface CategoriesIntent {
    data object LoadCategories : CategoriesIntent
    data class DeleteCategory(val categoryId: Long) : CategoriesIntent
    data class UpdateNewCategoryName(val name: String) : CategoriesIntent
    data class UpdateNewCategoryColor(val color: Int) : CategoriesIntent
    data class UpdateNewCategoryIcon(val icon: String) : CategoriesIntent
    data class EditCategory(val categoryId: Long) : CategoriesIntent
    data class ShowAddDialog(val show: Boolean) : CategoriesIntent
    data object AddCategory : CategoriesIntent
}

sealed interface CategoriesEffect {
    data class ShowSnackbar(val messageRes: Int) : CategoriesEffect
}
