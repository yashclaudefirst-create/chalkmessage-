package com.example.chalkmessage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.local.UserPrefs
import com.example.chalkmessage.data.model.Board
import com.example.chalkmessage.data.remote.BoardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CreateBoardUiState(
    val boardName: String = "",
    val yourName: String = "",
    val isLoading: Boolean = false,
    val createdCode: String? = null,
    val createdBoardId: String? = null,
    val error: String? = null
)

class CreateBoardViewModel(
    private val boardRepository: BoardRepository,
    private val userPrefs: UserPrefs,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBoardUiState())
    val uiState: StateFlow<CreateBoardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedName = userPrefs.userName.first() ?: ""
            _uiState.value = _uiState.value.copy(yourName = savedName)
        }
    }

    fun onBoardNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(boardName = name, error = null)
    }

    fun onYourNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(yourName = name, error = null)
    }

    fun createBoard() {
        val currentState = _uiState.value
        val boardName = currentState.boardName.trim()
        val yourName = currentState.yourName.trim()

        if (boardName.isBlank() || yourName.isBlank()) {
            _uiState.value = currentState.copy(error = "Please fill in both fields.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (supabase.auth.currentSessionOrNull() == null) {
                    supabase.auth.signInAnonymously()
                }

                val board: Board = boardRepository.createBoard(boardName, yourName)
                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

                userPrefs.saveUser(currentUserId, yourName, board.code)
                userPrefs.setCurrentBoardId(board.id)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    createdCode = board.code,
                    createdBoardId = board.id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to create board. Please try again."
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
            return CreateBoardViewModel(boardRepository, userPrefs, supabase) as T
        }
    }
}
