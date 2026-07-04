package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.repository.AuthResult
import com.taskflow.audit.data.repository.StaffRepository
import com.taskflow.audit.security.EncryptedPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/** Lightweight staff entry shown on the login screen (cached locally). */
data class DirectoryEntry(
    val shortId: String,
    val name: String,
    val role: String,
    val colorHex: String
)

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToStaffHome: String? = null,
    val navigateToAdmin: String? = null,
    val navigateToPinChange: String? = null,
    val requiresBiometric: Boolean = false,
    val savedShortId: String? = null,
    val offerBiometricEnroll: Boolean = false,
    val pendingUid: String? = null,
    val pendingIsAdmin: Boolean = false,
    val directory: List<DirectoryEntry> = emptyList()
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        val auth = AppRepositories.auth
        if (auth.isLoggedIn && auth.isBiometricEnabled()) {
            _uiState.value = _uiState.value.copy(
                requiresBiometric = true,
                savedShortId = auth.getSavedShortId()
            )
        }
        _uiState.value = _uiState.value.copy(directory = loadCachedDirectory())
    }

    private fun loadCachedDirectory(): List<DirectoryEntry> = try {
        val json = EncryptedPrefs.getString(EncryptedPrefs.KEY_STAFF_DIRECTORY) ?: ""
        if (json.isEmpty()) emptyList() else {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DirectoryEntry(
                    shortId = o.getString("shortId"),
                    name = o.getString("name"),
                    role = o.getString("role"),
                    colorHex = o.getString("colorHex")
                )
            }
        }
    } catch (_: Exception) { emptyList() }

    /**
     * Fetches the current staff list and caches it locally so the login
     * screen stays in sync when staff are added or edited in-app.
     * Best-effort: failures never block login.
     */
    private suspend fun refreshDirectoryCache() {
        try {
            withTimeoutOrNull(4_000) {
                val staff = StaffRepository().getAllStaffOnce()
                if (staff.isEmpty()) return@withTimeoutOrNull
                val arr = JSONArray()
                staff.sortedBy { it.fullName }.forEach { s ->
                    arr.put(JSONObject().apply {
                        put("shortId", s.shortId)
                        put("name", s.fullName)
                        put("role", s.role)
                        put("colorHex", s.colorHex)
                    })
                }
                EncryptedPrefs.putString(EncryptedPrefs.KEY_STAFF_DIRECTORY, arr.toString())
            }
        } catch (_: Exception) { /* non-fatal */ }
    }

    fun signIn(shortId: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = AppRepositories.auth.signIn(shortId, pin)) {
                is AuthResult.Success -> {
                    val uid = result.user.uid

                    // Refresh the login staff directory while we're authenticated
                    refreshDirectoryCache()

                    // Check if this user has a pending PIN reset
                    val staffRepo = StaffRepository()
                    val staffDoc = staffRepo.getStaffByUid(uid)
                    if (staffDoc?.pendingPinReset == true) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            navigateToPinChange = uid,
                            pendingIsAdmin = result.isAdmin
                        )
                        return@launch
                    }

                    val biometricNotYetEnabled = !AppRepositories.auth.isBiometricEnabled()
                    if (biometricNotYetEnabled) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            offerBiometricEnroll = true,
                            pendingUid = uid,
                            pendingIsAdmin = result.isAdmin
                        )
                    } else if (result.isAdmin) {
                        _uiState.value = _uiState.value.copy(isLoading = false, navigateToAdmin = uid)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, navigateToStaffHome = uid)
                    }
                }
                is AuthResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            val valid = AppRepositories.auth.refreshToken()
            if (!valid) {
                _uiState.value = _uiState.value.copy(requiresBiometric = false)
                return@launch
            }
            val uid = AppRepositories.auth.currentUser?.uid ?: return@launch
            val staffDoc = AppRepositories.staff.getStaffByUid(uid)
            if (staffDoc?.isAdmin == true) {
                _uiState.value = _uiState.value.copy(navigateToAdmin = uid)
            } else {
                _uiState.value = _uiState.value.copy(navigateToStaffHome = uid)
            }
        }
    }

    fun onBiometricDismissed() {
        _uiState.value = _uiState.value.copy(requiresBiometric = false)
    }

    fun enableBiometricAndProceed() {
        AppRepositories.auth.enableBiometric()
        proceedAfterEnrollDecision()
    }

    fun skipBiometricAndProceed() {
        proceedAfterEnrollDecision()
    }

    private fun proceedAfterEnrollDecision() {
        val uid = _uiState.value.pendingUid ?: return
        val isAdmin = _uiState.value.pendingIsAdmin
        _uiState.value = _uiState.value.copy(offerBiometricEnroll = false, pendingUid = null)
        if (isAdmin) _uiState.value = _uiState.value.copy(navigateToAdmin = uid)
        else _uiState.value = _uiState.value.copy(navigateToStaffHome = uid)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearNavigation() {
        _uiState.value = _uiState.value.copy(
            navigateToStaffHome = null,
            navigateToAdmin = null,
            navigateToPinChange = null
        )
    }
}
