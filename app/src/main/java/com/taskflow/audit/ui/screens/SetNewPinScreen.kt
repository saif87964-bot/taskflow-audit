package com.taskflow.audit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskflow.audit.data.AppRepositories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class SetNewPinState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class SetNewPinViewModel(private val uid: String) : ViewModel() {

    private val _state = MutableStateFlow(SetNewPinState())
    val state: StateFlow<SetNewPinState> = _state.asStateFlow()

    fun confirmPin(newPin: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                AppRepositories.auth.updateCurrentUserPin(newPin)
                AppRepositories.staff.clearPendingPinReset(uid)
                _state.value = _state.value.copy(isLoading = false, success = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to update PIN: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    class Factory(private val uid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SetNewPinViewModel(uid) as T
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun SetNewPinScreen(
    uid: String,
    isAdmin: Boolean,
    onComplete: () -> Unit,
    vm: SetNewPinViewModel = viewModel(factory = SetNewPinViewModel.Factory(uid))
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Two-step PIN entry: first = new PIN, second = confirmation
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var mismatchError by remember { mutableStateOf(false) }

    // Navigate on success
    LaunchedEffect(state.success) {
        if (state.success) onComplete()
    }

    val onKeyPress = { digit: String ->
        if (!isConfirmStep) {
            if (firstPin.length < 4) {
                firstPin += digit
                if (firstPin.length == 4) {
                    isConfirmStep = true
                    mismatchError = false
                }
            }
        } else {
            if (confirmPin.length < 4) {
                confirmPin += digit
                if (confirmPin.length == 4) {
                    if (confirmPin == firstPin) {
                        vm.confirmPin(confirmPin)
                    } else {
                        mismatchError = true
                        firstPin = ""
                        confirmPin = ""
                        isConfirmStep = false
                    }
                }
            }
        }
    }

    val onDelete = {
        if (!isConfirmStep) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
        }
    }

    val currentEntry = if (!isConfirmStep) firstPin else confirmPin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        // Brand icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("TF", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Set New PIN",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Your administrator has requested a PIN reset.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        Text(
            if (!isConfirmStep) "ENTER NEW PIN" else "CONFIRM NEW PIN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(16.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < currentEntry.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                )
            }
        }

        // Error messages
        val errorText = when {
            mismatchError -> "PINs do not match. Please try again."
            state.error != null -> state.error
            else -> null
        }
        if (errorText != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        } else {
            SetPinKeypad(onKey = onKeyPress, onDelete = onDelete)
        }
    }
}

@Composable
private fun SetPinKeypad(onKey: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(keys.size) { i ->
            val key = keys[i]
            if (key.isEmpty()) {
                Box(Modifier.aspectRatio(1.8f))
            } else {
                Box(
                    modifier = Modifier
                        .aspectRatio(1.8f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { if (key == "⌫") onDelete() else onKey(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
