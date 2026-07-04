package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.model.EngagementDocument
import com.taskflow.audit.data.model.StaffDocument
import com.taskflow.audit.data.model.TaskDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksState(
    val tasks: List<TaskDocument> = emptyList(),
    val staff: List<StaffDocument> = emptyList(),
    val engagements: List<EngagementDocument> = emptyList(),
    val isLoading: Boolean = true,
    val createSuccess: Boolean = false,
    val error: String? = null
) {
    fun staffByUid(uid: String): StaffDocument? = staff.find { it.uid == uid }
    fun engagementById(id: String): EngagementDocument? = engagements.find { it.id == id }
}

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
        viewModelScope.launch {
            AppRepositories.engagements.getActiveEngagementsFlow().collect { engs ->
                _state.value = _state.value.copy(engagements = engs)
            }
        }
        // Staff list powers assignee names on cards and the admin's assignee picker
        viewModelScope.launch {
            AppRepositories.staff.getAllStaffFlow().collect { staff ->
                _state.value = _state.value.copy(staff = staff)
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

    fun createTask(
        title: String,
        description: String,
        assigneeId: String,
        engagementId: String,
        priority: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            try {
                AppRepositories.tasks.createTask(
                    TaskDocument(
                        title = title.trim(),
                        description = description.trim(),
                        engagementId = engagementId,
                        assigneeId = assigneeId,
                        priority = priority,
                        status = "PENDING",
                        dueDate = dueDate,
                        createdBy = uid
                    )
                )
                _state.value = _state.value.copy(createSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Could not create task: ${e.message}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                AppRepositories.tasks.deleteTask(taskId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Delete failed: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(createSuccess = false, error = null)
    }

    class Factory(private val uid: String, private val isAdmin: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = TasksViewModel(uid, isAdmin) as T
    }
}
