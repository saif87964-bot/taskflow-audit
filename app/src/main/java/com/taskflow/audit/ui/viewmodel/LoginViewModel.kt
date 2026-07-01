package com.taskflow.audit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.audit.data.AppRepositories
import com.taskflow.audit.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToStaffHome: String? = null,   // staffUid
    val navigateToAdmin: String? = null,         // adminUid
    val requiresBiometric: Boolean = false,
    val savedShortId: String? = null,
    val offerBiometricEnroll: Boolean = false,  // show "enable fingerprint?" dialog after PIN login
    val pendingUid: String? = null,
    val pendingIsAdmin: Boolean = false
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Check if a returning user can use biometric
        val auth = AppRepositories.auth
        if (auth.isLoggedIn && auth.isBiometricEnabled()) {
            _uiState.value = _uiState.value.copy(
                requiresBiometric = true,
                savedShortId = auth.getSavedShortId()
            )
        }
    }

    fun signIn(shortId: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = AppRepositories.auth.signIn(shortId, pin)) {
                is AuthResult.Success -> {
                    val uid = result.user.uid
                    val biometricNotYetEnabled = !AppRepositories.auth.isBiometricEnabled()
                    if (biometricNotYetEnabled) {
                        // Offer biometric enrollment — screen will show dialog if hardware available
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
                // Token expired — fall back to PIN
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
            navigateToAdmin = null
        )
    }
}
