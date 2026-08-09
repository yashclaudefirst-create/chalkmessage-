package com.example.chalkmessage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.local.UserPrefs
import com.example.chalkmessage.data.remote.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class OnboardingViewModel(
    private val userPrefs: UserPrefs,
    private val firebaseRepo: FirebaseRepository
) : ViewModel() {

    // StateFlow is like useState but observable (components can collect it)
    private val _uiState = MutableStateFlow<OnboardingState>(OnboardingState.Loading)
    val uiState: StateFlow<OnboardingState> = _uiState

    sealed class OnboardingState {
        object Loading : OnboardingState()
        object NeedsName : OnboardingState()
        data class Ready(val userId: String, val inviteCode: String) : OnboardingState()
        object Connected : OnboardingState()
    }

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val name = userPrefs.userName.first()
            val connected = userPrefs.connectedTo.first()
            when {
                name.isNullOrEmpty() -> _uiState.value = OnboardingState.NeedsName
                connected.isNullOrEmpty() -> _uiState.value = OnboardingState.Ready(
                    userId = userPrefs.userId.first() ?: "",
                    inviteCode = userPrefs.inviteCode.first() ?: ""
                )
                else -> _uiState.value = OnboardingState.Connected
            }
        }
    }

    fun createUser(name: String) {
        viewModelScope.launch {
            val userId = UUID.randomUUID().toString()
            val inviteCode = firebaseRepo.generateInviteCode()
            userPrefs.saveUser(userId, name, inviteCode)
            _uiState.value = OnboardingState.Ready(userId, inviteCode)
        }
    }

    fun connectToUser(inviteCode: String) {
        viewModelScope.launch {
            // For MVP: store the code directly as the partner ID
            // In production, you'd look up the code in Firestore to get the real user ID
            val myId = userPrefs.userId.first() ?: return@launch
            userPrefs.addConnection(inviteCode)
            _uiState.value = OnboardingState.Connected
        }
    }

    // Factory: needed because ViewModels need constructor arguments
    class Factory(
        private val userPrefs: UserPrefs,
        private val firebaseRepo: FirebaseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(userPrefs, firebaseRepo) as T
        }
    }
}
