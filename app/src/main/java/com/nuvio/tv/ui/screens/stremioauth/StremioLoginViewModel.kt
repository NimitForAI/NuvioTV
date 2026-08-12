package com.nuvio.tv.ui.screens.stremioauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.repository.StremioAuthRepository
import com.nuvio.tv.data.repository.StremioAuthResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StremioLoginUiState(
    val email: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

@HiltViewModel
class StremioLoginViewModel @Inject constructor(
    private val repository: StremioAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StremioLoginUiState())
    val uiState: StateFlow<StremioLoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = if (state.isRegisterMode) {
                repository.register(state.email, state.password)
            } else {
                repository.login(state.email, state.password)
            }
            when (result) {
                is StremioAuthResultState.Success ->
                    _uiState.update { it.copy(isSubmitting = false, loggedIn = true) }
                is StremioAuthResultState.Failure ->
                    _uiState.update { it.copy(isSubmitting = false, error = result.message) }
            }
        }
    }
}
