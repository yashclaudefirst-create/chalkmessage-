package com.example.chalkmessage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashState {
    object Initial : SplashState()
    object Loading : SplashState()
    object Success : SplashState()
    data class Error(val message: String) : SplashState()
}

class SplashViewModel(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Initial)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    fun signInAndInit() {
        viewModelScope.launch {
            _state.value = SplashState.Loading
            val startTime = System.currentTimeMillis()
            try {
                if (supabase.auth.currentSessionOrNull() == null) {
                    supabase.auth.signInAnonymously()
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingDelay = 1200L - elapsedTime
                if (remainingDelay > 0) {
                    delay(remainingDelay)
                }
                _state.value = SplashState.Success
            } catch (e: Exception) {
                _state.value = SplashState.Error(e.localizedMessage ?: "Sign-in failed. Please try again.")
            }
        }
    }

    class Factory(private val supabase: SupabaseClient) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SplashViewModel(supabase) as T
        }
    }
}
