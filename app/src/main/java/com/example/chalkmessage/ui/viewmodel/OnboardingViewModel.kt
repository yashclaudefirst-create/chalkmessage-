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
            val connected = userPrefs.connectedTo.first()
            val hasSkipped = userPrefs.hasSkippedConnection.first()
            when {
                name.isNullOrEmpty() -> _uiState.value = OnboardingState.NeedsName
                connected.isNullOrEmpty() && !hasSkipped -> {
                    _uiState.value = OnboardingState.Ready(
                        userId = userPrefs.userId.first() ?: "",
                        inviteCode = userPrefs.inviteCode.first() ?: ""
                    )
                }
                else -> _uiState.value = OnboardingState.Connected
            }
        }
    }

    fun createUser(name: String) {
        viewModelScope.launch {
            _uiState.value = OnboardingState.Loading
            val userId = UUID.randomUUID().toString()
            val inviteCode = firebaseRepo.generateInviteCode()
            val fcmToken = firebaseRepo.getFcmToken()

            try {
                // Store user profile in Firestore
                firebaseRepo.createUserProfile(
                    userId = userId,
                    name = name,
                    inviteCode = inviteCode,
                    fcmToken = fcmToken
                )
                // Save locally
                userPrefs.saveUser(userId, name, inviteCode)
                userPrefs.setHasSkippedConnection(false) // default to false
                _uiState.value = OnboardingState.Ready(userId, inviteCode)
            } catch (e: Exception) {
                // If firestore fails (e.g. offline), still save locally to allow offline MVP usage
                userPrefs.saveUser(userId, name, inviteCode)
                _uiState.value = OnboardingState.Ready(userId, inviteCode, error = "Created profile locally (Offline)")
            }
        }
    }

    fun connectToUser(inviteCode: String) {
        val currentState = _uiState.value
        if (currentState is OnboardingState.Ready) {
            _uiState.value = currentState.copy(isConnecting = true, error = null)
        }
        viewModelScope.launch {
            val myId = userPrefs.userId.first()
            if (myId.isNullOrEmpty()) {
                if (currentState is OnboardingState.Ready) {
                    _uiState.value = currentState.copy(isConnecting = false, error = "User ID missing. Try restarting app.")
                }
                return@launch
            }

            try {
                val partnerUserId = firebaseRepo.lookupInviteCode(inviteCode)
                if (partnerUserId == null) {
                    if (currentState is OnboardingState.Ready) {
                        _uiState.value = currentState.copy(isConnecting = false, error = "Invalid Invite Code")
                    }
                } else if (partnerUserId == myId) {
                    if (currentState is OnboardingState.Ready) {
                        _uiState.value = currentState.copy(isConnecting = false, error = "You cannot connect with yourself")
                    }
                } else {
                    // Create joint connection document
                    firebaseRepo.createConnection(myId, partnerUserId)
                    // Save locally
                    userPrefs.setConnectedPartner(partnerUserId)
                    userPrefs.setHasSkippedConnection(false)
                    _uiState.value = OnboardingState.Connected
                }
            } catch (e: Exception) {
                if (currentState is OnboardingState.Ready) {
                    _uiState.value = currentState.copy(isConnecting = false, error = "Connection failed: ${e.message}")
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
        private val firebaseRepo: FirebaseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(userPrefs, firebaseRepo) as T
        }
    }
}
