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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class OnboardingViewModel(
    private val userPrefs: UserPrefs,
    private val boardRepository: BoardRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingState>(OnboardingState.Loading)
    val uiState: StateFlow<OnboardingState> = _uiState

    sealed class OnboardingState {
        object Loading : OnboardingState()
        object NeedsName : OnboardingState()
        data class Ready(
            val userId: String,
            val inviteCode: String,
            val error: String? = null,
            val isConnecting: Boolean = false
        ) : OnboardingState()
        object Connected : OnboardingState()
    }

    init {
        checkOnboardingStatus()
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            val name = userPrefs.userName.first()
            val connected = userPrefs.currentBoardId.first()
            val hasSkipped = userPrefs.hasSkippedConnection.first()
            when {
                name.isNullOrEmpty() -> _uiState.value = OnboardingState.NeedsName
                connected.isNullOrEmpty() && !hasSkipped -> {
                    val uid = supabase.auth.currentUserOrNull()?.id
                        ?: userPrefs.userId.first()
                        ?: UUID.randomUUID().toString()
                    val code = userPrefs.inviteCode.first() ?: ""
                    _uiState.value = OnboardingState.Ready(
                        userId = uid,
                        inviteCode = code
                    )
                }
                else -> _uiState.value = OnboardingState.Connected
            }
        }
    }

    fun createUser(name: String) {
        viewModelScope.launch {
            _uiState.value = OnboardingState.Loading
            if (supabase.auth.currentSessionOrNull() == null) {
                try {
                    supabase.auth.signInAnonymously()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val userId = supabase.auth.currentUserOrNull()?.id ?: UUID.randomUUID().toString()
            val inviteCode = (100000..999999).random().toString()

            userPrefs.saveUser(userId, name, inviteCode)
            userPrefs.setHasSkippedConnection(false)
            _uiState.value = OnboardingState.Ready(userId, inviteCode)
        }
    }

    fun connectToUser(boardCode: String) {
        val currentState = _uiState.value
        if (currentState is OnboardingState.Ready) {
            _uiState.value = currentState.copy(isConnecting = true, error = null)
        }
        viewModelScope.launch {
            try {
                if (supabase.auth.currentSessionOrNull() == null) {
                    supabase.auth.signInAnonymously()
                }

                val boardId = boardRepository.joinBoard(boardCode)
                userPrefs.setCurrentBoardId(boardId)
                userPrefs.setHasSkippedConnection(false)
                _uiState.value = OnboardingState.Connected
            } catch (e: Exception) {
                if (currentState is OnboardingState.Ready) {
                    _uiState.value = currentState.copy(
                        isConnecting = false,
                        error = e.localizedMessage ?: "Failed to join board."
                    )
                }
            }
        }
    }

    fun skipConnection() {
        viewModelScope.launch {
            userPrefs.setHasSkippedConnection(true)
            _uiState.value = OnboardingState.Connected
        }
    }

    fun clearError() {
        val currentState = _uiState.value
        if (currentState is OnboardingState.Ready) {
            _uiState.value = currentState.copy(error = null)
        }
    }

    class Factory(
        private val userPrefs: UserPrefs,
        private val boardRepository: BoardRepository,
        private val supabase: SupabaseClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(userPrefs, boardRepository, supabase) as T
        }
    }
}
