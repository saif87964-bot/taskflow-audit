package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.model.EngagementDocument
import com.taskflow.audit.data.model.StaffDocument
import com.taskflow.audit.data.model.TimeSessionDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class StaffDetailState(
    val staff: StaffDocument? = null,
    val todaySessions: List<TimeSessionDocument> = emptyList(),
    val engagements: List<EngagementDocument> = emptyList(),
    val isLoading: Boolean = true,
    val pinResetSuccess: Boolean = false,
    val error: String? = null
) {
    val hoursToday: Double get() = todaySessions.sumOf { it.durationMinutes } / 60.0
    val isCheckedIn: Boolean get() = todaySessions.any { it.isActive }
    val currentEngagementId: String? get() = todaySessions.firstOrNull { it.isActive }?.engagementId
}

class AdminStaffDetailViewModel(private val staffUid: String) : ViewModel() {

    private val _state = MutableStateFlow(StaffDetailState())
    val state: StateFlow<StaffDetailState> = _state.asStateFlow()

    init {
        // One-time fetch for the staff document
        viewModelScope.launch {
            try {
                val staffDoc = AppRepositories.staff.getStaffByUid(staffUid)
                _state.value = _state.value.copy(staff = staffDoc)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to load staff: ${e.message}")
            }
        }

        // Combine live streams for today's sessions and active engagements
        viewModelScope.launch {
            combine(
                AppRepositories.timesheet.getTodaySessionsFlow(staffUid),
                AppRepositories.engagements.getActiveEngagementsFlow()
            ) { sessions, engagements ->
                _state.value = _state.value.copy(
                    todaySessions = sessions,
                    engagements = engagements,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun resetPin() {
        viewModelScope.launch {
            try {
                AppRepositories.staff.setPendingPinReset(staffUid)
                _state.value = _state.value.copy(pinResetSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to reset PIN: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(pinResetSuccess = false, error = null)
    }

    class Factory(private val staffUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            AdminStaffDetailViewModel(staffUid) as T
    }
}
