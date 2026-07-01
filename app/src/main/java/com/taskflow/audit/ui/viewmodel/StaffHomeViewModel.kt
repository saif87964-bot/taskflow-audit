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

data class StaffHomeState(
    val staff: StaffDocument? = null,
    val activeSession: TimeSessionDocument? = null,
    val todaySessions: List<TimeSessionDocument> = emptyList(),
    val currentEngagement: EngagementDocument? = null,
    val engagements: List<EngagementDocument> = emptyList(),
    val pendingTaskCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val hoursToday: Double get() = todaySessions.sumOf { it.durationMinutes } / 60.0 +
        (activeSession?.let {
            (System.currentTimeMillis() / 1000 - (it.startTime?.seconds ?: 0)) / 3600.0
        } ?: 0.0)

    val isCheckedIn: Boolean get() = activeSession != null
}

class StaffHomeViewModel(private val staffUid: String) : ViewModel() {

    private val _state = MutableStateFlow(StaffHomeState())
    val state: StateFlow<StaffHomeState> = _state.asStateFlow()

    private var activeSessionId: String? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val staffDoc = AppRepositories.staff.getStaffByUid(staffUid)
            _state.value = _state.value.copy(staff = staffDoc)
        }
        viewModelScope.launch {
            AppRepositories.engagements.getActiveEngagementsFlow().collect { engs ->
                _state.value = _state.value.copy(engagements = engs)
            }
        }
        viewModelScope.launch {
            combine(
                AppRepositories.timesheet.getTodaySessionsFlow(staffUid),
                AppRepositories.tasks.getTasksForStaffFlow(staffUid)
            ) { sessions, tasks ->
                val active = sessions.firstOrNull { it.isActive }
                activeSessionId = active?.id
                val engagement = active?.let {
                    AppRepositories.engagements.getEngagementById(it.engagementId)
                }
                val pending = tasks.count { it.status != "DONE" }
                _state.value = _state.value.copy(
                    activeSession = active,
                    todaySessions = sessions.filter { !it.isActive },
                    currentEngagement = engagement,
                    pendingTaskCount = pending,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun checkIn(engagementId: String) {
        viewModelScope.launch {
            try {
                val sessionId = AppRepositories.timesheet.checkIn(staffUid, engagementId)
                activeSessionId = sessionId
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Check-in failed: ${e.message}")
            }
        }
    }

    fun checkOut() {
        val sessionId = activeSessionId ?: return
        viewModelScope.launch {
            try {
                val elapsed = _state.value.activeSession?.startTime?.let {
                    ((System.currentTimeMillis() / 1000 - it.seconds) / 60).toInt()
                } ?: 0
                AppRepositories.timesheet.checkOut(sessionId, elapsed)
                activeSessionId = null
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Check-out failed: ${e.message}")
            }
        }
    }

    fun switchEngagement(newEngagementId: String) {
        viewModelScope.launch {
            try {
                val currentId = activeSessionId
                val elapsed = _state.value.activeSession?.startTime?.let {
                    ((System.currentTimeMillis() / 1000 - it.seconds) / 60).toInt()
                } ?: 0
                if (currentId != null) {
                    val newId = AppRepositories.timesheet.switchEngagement(
                        currentId, staffUid, newEngagementId, elapsed
                    )
                    activeSessionId = newId
                } else {
                    val newId = AppRepositories.timesheet.checkIn(staffUid, newEngagementId)
                    activeSessionId = newId
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Switch failed: ${e.message}")
            }
        }
    }

    fun clearError() = _state.value.let { _state.value = it.copy(error = null) }

    class Factory(private val staffUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            StaffHomeViewModel(staffUid) as T
    }
}
