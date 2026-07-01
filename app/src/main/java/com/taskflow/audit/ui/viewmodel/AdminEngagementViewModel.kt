package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.model.EngagementDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminEngagementState(
    val engagements: List<EngagementDocument> = emptyList(),
    val isLoading: Boolean = true,
    val successMessage: String? = null,
    val error: String? = null
)

class AdminEngagementViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminEngagementState())
    val state: StateFlow<AdminEngagementState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            AppRepositories.engagements.getActiveEngagementsFlow().collect { list ->
                _state.value = _state.value.copy(engagements = list, isLoading = false)
            }
        }
    }

    fun createEngagement(
        code: String,
        clientName: String,
        type: String,
        colorHex: String,
        budgetHours: Int
    ) {
        viewModelScope.launch {
            try {
                AppRepositories.engagements.createEngagement(code, clientName, type, colorHex, budgetHours)
                _state.value = _state.value.copy(successMessage = "Engagement created successfully")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to create engagement: ${e.message}")
            }
        }
    }

    fun updateEngagement(
        id: String,
        code: String,
        clientName: String,
        type: String,
        colorHex: String,
        budgetHours: Int
    ) {
        viewModelScope.launch {
            try {
                AppRepositories.engagements.updateEngagement(id, code, clientName, type, colorHex, budgetHours)
                _state.value = _state.value.copy(successMessage = "Engagement updated successfully")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to update engagement: ${e.message}")
            }
        }
    }

    fun archiveEngagement(id: String) {
        viewModelScope.launch {
            try {
                AppRepositories.engagements.archiveEngagement(id)
                _state.value = _state.value.copy(successMessage = "Engagement archived")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to archive engagement: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(successMessage = null, error = null)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            AdminEngagementViewModel() as T
    }
}
