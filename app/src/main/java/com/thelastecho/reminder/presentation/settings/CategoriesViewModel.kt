package com.thelastecho.reminder.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.repository.ReminderRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CategoriesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeCategories()
    }

    private fun observeCategories() {
        repository.getAllCategories().onEach { categories ->
            _uiState.update { it.copy(categories = categories) }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: CategoriesIntent) {
        when (intent) {
            CategoriesIntent.LoadCategories -> {
                observeCategories()
            }
            is CategoriesIntent.DeleteCategory -> {
                viewModelScope.launch {
                    repository.deleteCategory(intent.categoryId)
                    _effect.send(CategoriesEffect.ShowSnackbar("Category deleted"))
                }
            }
            is CategoriesIntent.UpdateNewCategoryName -> {
                _uiState.update { it.copy(newCategoryName = intent.name) }
            }
            is CategoriesIntent.UpdateNewCategoryColor -> {
                _uiState.update { it.copy(newCategoryColor = intent.color) }
            }
            is CategoriesIntent.ShowAddDialog -> {
                _uiState.update { 
                    it.copy(
                        showAddDialog = intent.show,
                        newCategoryName = if (!intent.show) "" else it.newCategoryName
                    )
                }
            }
            CategoriesIntent.AddCategory -> {
                viewModelScope.launch {
                    val state = _uiState.value
                    if (state.newCategoryName.isNotBlank()) {
                        val category = Category(
                            name = state.newCategoryName,
                            colorArgb = state.newCategoryColor.toLong(),
                            iconName = "label"
                        )
                        repository.saveCategory(category)
                        _effect.send(CategoriesEffect.ShowSnackbar("Category added"))
                        _uiState.update { 
                            it.copy(
                                showAddDialog = false,
                                newCategoryName = ""
                            )
                        }
                    }
                }
            }
        }
    }
}
