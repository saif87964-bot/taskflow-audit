package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.model.EngagementDocument
import com.taskflow.audit.data.model.TimeSessionDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimesheetState(
    val sessions: List<TimeSessionDocument> = emptyList(),
    val engagements: List<EngagementDocument> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalMinutes: Int get() = sessions.sumOf { it.durationMinutes }
    val hoursToday: Double get() = totalMinutes / 60.0

    fun engagementById(id: String) = engagements.find { it.id == id }

    val breakdownByEngagement: Map<String, Int> get() =
        sessions.groupBy { it.engagementId }
                .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
}

class TimesheetViewModel(private val uid: String) : ViewModel() {

    private val _state = MutableStateFlow(TimesheetState())
    val state: StateFlow<TimesheetState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            AppRepositories.timesheet.getTodaySessionsFlow(uid).collect { sessions ->
                _state.value = _state.value.copy(
                    sessions = sessions.filter { !it.isActive },
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            AppRepositories.engagements.getActiveEngagementsFlow().collect { engs ->
                _state.value = _state.value.copy(engagements = engs)
            }
        }
    }

    class Factory(private val uid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = TimesheetViewModel(uid) as T
    }
}
