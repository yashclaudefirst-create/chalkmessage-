package com.example.chalkmessage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.chalkmessage.data.ChalkRepository
import com.example.chalkmessage.data.model.ChalkMessage
import kotlinx.coroutines.flow.StateFlow

class HistoryViewModel(repository: ChalkRepository) : ViewModel() {
    val messages: StateFlow<List<ChalkMessage>> = repository.allMessages as StateFlow<List<ChalkMessage>>

    class Factory(private val repository: ChalkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
