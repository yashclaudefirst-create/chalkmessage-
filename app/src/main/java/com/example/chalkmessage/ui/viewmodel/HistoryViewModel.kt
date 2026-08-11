package com.example.chalkmessage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.ChalkRepository
import com.example.chalkmessage.data.model.ChalkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: ChalkRepository) : ViewModel() {

    val messages: StateFlow<List<ChalkMessage>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var lastDeletedMessage: ChalkMessage? = null
    private var lastDeletedIsIncoming: Boolean = true

    fun refreshMessages() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncIncomingMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteMessage(id: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val backup = repository.deleteMessageAndReturnBackup(id)
                if (backup != null) {
                    lastDeletedMessage = backup.first
                    lastDeletedIsIncoming = backup.second
                    onDone(true)
                } else {
                    onDone(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(false)
            }
        }
    }

    fun undoDelete(onDone: () -> Unit = {}) {
        val backupMsg = lastDeletedMessage ?: return
        val backupIsIncoming = lastDeletedIsIncoming
        viewModelScope.launch {
            try {
                repository.insertMessage(backupMsg, backupIsIncoming)
                lastDeletedMessage = null
                onDone()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            try {
                repository.markAsRead(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Factory(private val repository: ChalkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
