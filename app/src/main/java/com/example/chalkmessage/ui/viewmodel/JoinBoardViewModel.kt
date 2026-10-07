package com.example.chalkmessage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.local.UserPrefs
import com.example.chalkmessage.data.remote.BoardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class JoinBoardUiState(
    val code: String = "",
    val yourName: String = "",
    val isLoading: Boolean = false,
    val joinedBoardId: String? = null,
    val error: String? = null
)

class JoinBoardViewModel(
    private val boardRepository: BoardRepository,
    private val userPrefs: UserPrefs,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinBoardUiState())
    val uiState: StateFlow<JoinBoardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedName = userPrefs.userName.first() ?: ""
            _uiState.value = _uiState.value.copy(yourName = savedName)
        }
    }

    fun onCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(code = code, error = null)
    }

    fun onYourNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(yourName = name, error = null)
    }

    fun joinBoard() {
        val currentState = _uiState.value
        val code = currentState.code.trim()
        val yourName = currentState.yourName.trim()

        if (code.isBlank() || yourName.isBlank()) {
            _uiState.value = currentState.copy(error = "Please enter both your name and board code.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (supabase.auth.currentSessionOrNull() == null) {
                    supabase.auth.signInAnonymously()
                }

                val boardId = boardRepository.joinBoard(code)
                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

                userPrefs.saveUser(currentUserId, yourName, code)
                userPrefs.setCurrentBoardId(boardId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    joinedBoardId = boardId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to join board."
                )
            }
        }
    }

    class Factory(
        private val boardRepository: BoardRepository,
        private val userPrefs: UserPrefs,
        private val supabase: SupabaseClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JoinBoardViewModel(boardRepository, userPrefs, supabase) as T
        }
    }
}
