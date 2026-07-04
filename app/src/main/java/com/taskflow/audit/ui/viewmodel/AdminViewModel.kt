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
    val todaySessions: List<TimeSessionDocument> = emptyList(),
    val engagements: List<EngagementDocument> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val createStaffSuccess: Boolean = false,
    val updateStaffSuccess: Boolean = false,
    val actionError: String? = null,
    val resetPinSuccess: String? = null
) {
    /** Sessions currently running (staff checked in right now). */
    val activeSessions: List<TimeSessionDocument> get() = todaySessions.filter { it.isActive }

    val checkedInCount: Int get() = activeSessions.map { it.staffId }.distinct().size

    /** Staff with no session at all today — genuinely never logged in. */
    val notLoggedToday: List<StaffDocument> get() =
        staffList.filter { s -> todaySessions.none { it.staffId == s.uid } &&
            s.shortId != "admin" }
}

class AdminViewModel(private val adminUid: String) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state: StateFlow<AdminDashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                AppRepositories.staff.getAllStaffFlow(),
                AppRepositories.timesheet.getTodayAllSessionsFlow(),
                AppRepositories.engagements.getActiveEngagementsFlow()
            ) { staff, sessions, engs ->
                _state.value = _state.value.copy(
                    staffList = staff,
                    todaySessions = sessions,
                    engagements = engs,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun createStaff(shortId: String, fullName: String, role: String, colorHex: String, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                val id = shortId.trim().lowercase()
                if (AppRepositories.staff.getStaffByShortId(id) != null ||
                    _state.value.staffList.any { it.shortId == id }
                ) {
                    _state.value = _state.value.copy(
                        actionError = "Short ID '$id' is already taken. Choose another."
                    )
                    return@launch
                }
                val uid = AppRepositories.auth.createStaff(id, fullName, role, colorHex, isAdmin)
                AppRepositories.staff.createStaffDoc(uid, id, fullName, role, colorHex, isAdmin)
                _state.value = _state.value.copy(createStaffSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionError = "Failed to create staff: ${e.message}")
            }
        }
    }

    fun resetStaffPin(uid: String) {
        viewModelScope.launch {
            try {
                AppRepositories.staff.setPendingPinReset(uid)
                val name = _state.value.staffList.find { it.uid == uid }?.fullName ?: "Staff"
                _state.value = _state.value.copy(resetPinSuccess = name)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionError = "Failed to reset PIN: ${e.message}")
            }
        }
    }

    fun updateStaff(uid: String, fullName: String, role: String, colorHex: String, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                AppRepositories.staff.updateStaff(uid, fullName, role, colorHex, isAdmin)
                _state.value = _state.value.copy(updateStaffSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(actionError = "Failed to update staff: ${e.message}")
            }
        }
    }

    fun clearActionResult() {
        _state.value = _state.value.copy(
            createStaffSuccess = false,
            updateStaffSuccess = false,
            actionError = null,
            resetPinSuccess = null
        )
    }

    fun clearError() = _state.value.let { _state.value = it.copy(error = null) }

    class Factory(private val adminUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            AdminViewModel(adminUid) as T
    }
}
