package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.model.TaskDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksState(
    val tasks: List<TaskDocument> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class TasksViewModel(private val uid: String, private val isAdmin: Boolean) : ViewModel() {

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val flow = if (isAdmin) AppRepositories.tasks.getAllTasksFlow()
                       else AppRepositories.tasks.getTasksForStaffFlow(uid)
            flow.collect { tasks ->
                _state.value = _state.value.copy(tasks = tasks, isLoading = false)
            }
        }
    }

    fun updateStatus(taskId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                AppRepositories.tasks.updateStatus(taskId, newStatus)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Update failed: ${e.message}")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    class Factory(private val uid: String, private val isAdmin: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = TasksViewModel(uid, isAdmin) as T
    }
}
