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

data class AdminDashboardState(
    val staffList: List<StaffDocument> = emptyList(),
    val activeSessions: List<TimeSessionDocument> = emptyList(),
    val engagements: List<EngagementDocument> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val checkedInCount: Int get() = activeSessions.map { it.staffId }.distinct().size
    val notLoggedToday: List<StaffDocument> get() =
        staffList.filter { s -> activeSessions.none { it.staffId == s.uid } &&
            s.shortId != "admin" }
}

class AdminViewModel(private val adminUid: String) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                AppRepositories.staff.getAllStaffFlow(),
                AppRepositories.timesheet.getActiveSessionsFlow(),
                AppRepositories.engagements.getActiveEngagementsFlow()
            ) { staff, sessions, engs ->
                _state.value = _state.value.copy(
                    staffList = staff,
                    activeSessions = sessions,
                    engagements = engs,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun clearError() = _state.value.let { _state.value = it.copy(error = null) }

    class Factory(private val adminUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            AdminViewModel(adminUid) as T
    }
}
