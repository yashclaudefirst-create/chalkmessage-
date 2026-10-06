package com.example.chalkmessage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.model.Board
import com.example.chalkmessage.data.remote.BoardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateBoardUiState(
    val boardName: String = "",
    val yourName: String = "",
    val isLoading: Boolean = false,
    val createdCode: String? = null,
    val error: String? = null
)

class CreateBoardViewModel(
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBoardUiState())
    val uiState: StateFlow<CreateBoardUiState> = _uiState.asStateFlow()

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
                val board: Board = boardRepository.createBoard(boardName, yourName)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    createdCode = board.code
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to create board. Please try again."
                )
            }
        }
    }

    class Factory(private val boardRepository: BoardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateBoardViewModel(boardRepository) as T
        }
    }
}
