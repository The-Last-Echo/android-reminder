package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.domain.model.Category

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isDeleting: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryColor: Int = 0xFF4CAF50.toInt(),
    val showAddDialog: Boolean = false
)

sealed interface CategoriesIntent {
    data object LoadCategories : CategoriesIntent
    data class DeleteCategory(val categoryId: Long) : CategoriesIntent
    data class UpdateNewCategoryName(val name: String) : CategoriesIntent
    data class UpdateNewCategoryColor(val color: Int) : CategoriesIntent
    data class ShowAddDialog(val show: Boolean) : CategoriesIntent
    data object AddCategory : CategoriesIntent
}

sealed interface CategoriesEffect {
    data class ShowSnackbar(val message: String) : CategoriesEffect
}
