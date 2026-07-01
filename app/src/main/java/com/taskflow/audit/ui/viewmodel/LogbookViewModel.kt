package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.model.EngagementDocument
import com.taskflow.audit.data.model.LogEntryDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LogbookState(
    val entries: List<LogEntryDocument> = emptyList(),
    val engagements: List<EngagementDocument> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class LogbookViewModel(private val uid: String, private val isAdmin: Boolean) : ViewModel() {

    private val _state = MutableStateFlow(LogbookState())
    val state: StateFlow<LogbookState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val flow = if (isAdmin) AppRepositories.log.getAllLogEntriesFlow()
                       else AppRepositories.log.getStaffLogFlow(uid)
            flow.collect { entries ->
                _state.value = _state.value.copy(entries = entries, isLoading = false)
            }
        }
        viewModelScope.launch {
            AppRepositories.engagements.getActiveEngagementsFlow().collect { engs ->
                _state.value = _state.value.copy(engagements = engs)
            }
        }
    }

    fun addEntry(note: String, category: String, engagementId: String) {
        viewModelScope.launch {
            try {
                AppRepositories.log.addEntry(
                    LogEntryDocument(
                        staffId = uid,
                        engagementId = engagementId,
                        note = note,
                        category = category
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Save failed: ${e.message}")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    class Factory(private val uid: String, private val isAdmin: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = LogbookViewModel(uid, isAdmin) as T
    }
}
