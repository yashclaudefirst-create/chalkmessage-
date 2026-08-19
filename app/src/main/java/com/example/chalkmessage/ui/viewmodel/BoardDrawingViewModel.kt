package com.example.chalkmessage.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.BoardMemberUi
import com.example.chalkmessage.data.BoardRepository
import com.example.chalkmessage.data.DrawingDto
import com.example.chalkmessage.data.model.DrawPoint
import com.example.chalkmessage.data.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface SendState {
    object Idle : SendState
    object Sending : SendState
    object Sent : SendState
    data class Failed(val message: String? = null) : SendState
}

class BoardDrawingViewModel(
    private val repository: BoardRepository,
    val boardId: String,
    val boardName: String,
    val myName: String,
    val myColorHex: String
) : ViewModel() {

    private val _members = MutableStateFlow<List<BoardMemberUi>>(emptyList())
    val members: StateFlow<List<BoardMemberUi>> = _members.asStateFlow()

    // NOTE: typingUserName requires Supabase Realtime Presence (broadcast/presence)
    // to track active typing/drawing users in real time. It is separate from
    // postgres_changes listeners. Stubbed as null for now as per design requirements.
    private val _typingUserName = MutableStateFlow<String?>(null)
    val typingUserName: StateFlow<String?> = _typingUserName.asStateFlow()

    private val _recentDrawings = MutableStateFlow<List<DrawingDto>>(emptyList())
    val recentDrawings: StateFlow<List<DrawingDto>> = _recentDrawings.asStateFlow()

    private val _currentStrokes = MutableStateFlow<List<Stroke>>(emptyList())
    val currentStrokes: StateFlow<List<Stroke>> = _currentStrokes.asStateFlow()

    private val _activeStroke = MutableStateFlow<Stroke?>(null)
    val activeStroke: StateFlow<Stroke?> = _activeStroke.asStateFlow()

    private val initialColor = parseHexColor(myColorHex)
    private val _currentColor = MutableStateFlow(initialColor)
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    private val _brushWidth = MutableStateFlow(8f)
    val brushWidth: StateFlow<Float> = _brushWidth.asStateFlow()

    private val _isEraser = MutableStateFlow(false)
    val isEraser: StateFlow<Boolean> = _isEraser.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    private val chalkboardBackgroundColor = Color(0xFF1A261A)

    init {
        loadBoardMembers()
        loadRecentDrawings()
        listenForNewDrawings()
    }

    private fun loadBoardMembers() {
        viewModelScope.launch {
            try {
                val memberList = repository.getBoardMembers(boardId)
                _members.value = memberList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRecentDrawings() {
        viewModelScope.launch {
            try {
                val drawings = repository.getDrawings(boardId)
                _recentDrawings.value = drawings
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun listenForNewDrawings() {
        viewModelScope.launch {
            try {
                repository.listenForNewDrawings(boardId)
                    .catch { e -> e.printStackTrace() }
                    .collect { newDrawing ->
                        _recentDrawings.value = listOf(newDrawing) + _recentDrawings.value
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setColor(color: Color) {
        _currentColor.value = color
        _isEraser.value = false
    }

    fun toggleEraser() {
        _isEraser.value = !_isEraser.value
    }

    fun setBrushWidth(width: Float) {
        _brushWidth.value = width.coerceIn(2f, 20f)
    }

    fun startStroke(x: Float, y: Float) {
        val colorHex = if (_isEraser.value) {
            colorToHex(chalkboardBackgroundColor)
        } else {
            colorToHex(_currentColor.value)
        }
        val newStroke = Stroke(
            points = listOf(DrawPoint(x, y)),
            colorHex = colorHex,
            width = _brushWidth.value
        )
        _activeStroke.value = newStroke
    }

    fun addPoint(x: Float, y: Float) {
        val current = _activeStroke.value ?: return
        val updatedPoints = current.points + DrawPoint(x, y)
        _activeStroke.value = current.copy(points = updatedPoints)
    }

    fun endStroke() {
        val completed = _activeStroke.value
        if (completed != null && completed.points.isNotEmpty()) {
            _currentStrokes.value = _currentStrokes.value + completed
        }
        _activeStroke.value = null
    }

    fun clear() {
        _currentStrokes.value = emptyList()
        _activeStroke.value = null
    }

    fun send() {
        if (_sendState.value is SendState.Sending) return
        val strokesToSend = _currentStrokes.value
        if (strokesToSend.isEmpty()) return

        _sendState.value = SendState.Sending
        viewModelScope.launch {
            try {
                val json = Json.encodeToString(strokesToSend)
                val hex = colorToHex(_currentColor.value)
                repository.postDrawing(
                    boardId = boardId,
                    userName = myName,
                    userColor = hex,
                    strokesJson = json
                )
                _sendState.value = SendState.Sent
                clear()
            } catch (e: Exception) {
                e.printStackTrace()
                _sendState.value = SendState.Failed(e.localizedMessage ?: "Failed to send drawing")
            }
        }
    }

    fun resetSendState() {
        _sendState.value = SendState.Idle
    }

    private fun colorToHex(color: Color): String {
        val argb = color.toArgb()
        return String.format("#%08X", argb)
    }

    private fun parseHexColor(hex: String): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorInt = when (cleanHex.length) {
                6 -> (0xFF000000 or cleanHex.toLong(16)).toInt()
                8 -> cleanHex.toLong(16).toInt()
                else -> 0xFFFFFFFF.toInt()
            }
            Color(colorInt)
        } catch (e: Exception) {
            Color.White
        }
    }

    class Factory(
        private val repository: BoardRepository,
        private val boardId: String,
        private val boardName: String,
        private val myName: String,
        private val myColorHex: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BoardDrawingViewModel::class.java)) {
                return BoardDrawingViewModel(repository, boardId, boardName, myName, myColorHex) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
