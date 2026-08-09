package com.example.chalkmessage.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.ChalkRepository
import com.example.chalkmessage.data.model.DrawPoint
import com.example.chalkmessage.data.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DrawingViewModel(private val repository: ChalkRepository) : ViewModel() {

    // Current drawing session state
    private val _currentStrokes = MutableStateFlow<List<Stroke>>(emptyList())
    val currentStrokes: StateFlow<List<Stroke>> = _currentStrokes

    private val _currentColor = MutableStateFlow(Color.White)
    val currentColor: StateFlow<Color> = _currentColor

    private val _currentWidth = MutableStateFlow(8f)
    val currentWidth: StateFlow<Float> = _currentWidth

    // Active stroke being drawn (finger currently down)
    private var activeStrokePoints = mutableListOf<DrawPoint>()

    fun setColor(color: Color) {
        _currentColor.value = color
    }

    fun startStroke(x: Float, y: Float) {
        activeStrokePoints = mutableListOf(DrawPoint(x, y))
    }

    fun addPoint(x: Float, y: Float) {
        activeStrokePoints.add(DrawPoint(x, y))
        // Update the last stroke in the list with new points
        val updated = _currentStrokes.value.toMutableList()
        if (updated.isNotEmpty()) {
            updated[updated.lastIndex] = updated.last().copy(points = activeStrokePoints.toList())
            _currentStrokes.value = updated
        }
    }

    fun endStroke() {
        if (activeStrokePoints.size > 1) {
            val stroke = Stroke(
                points = activeStrokePoints.toList(),
                colorHex = colorToHex(_currentColor.value),
                width = _currentWidth.value
            )
            _currentStrokes.value = _currentStrokes.value + stroke
        }
        activeStrokePoints.clear()
    }

    fun undo() {
        if (_currentStrokes.value.isNotEmpty()) {
            _currentStrokes.value = _currentStrokes.value.dropLast(1)
        }
    }

    fun clear() {
        _currentStrokes.value = emptyList()
        activeStrokePoints.clear()
    }

    fun sendDrawing() {
        viewModelScope.launch {
            if (_currentStrokes.value.isNotEmpty()) {
                repository.sendMessage(_currentStrokes.value)
                clear()
            }
        }
    }

    private fun colorToHex(color: Color): String {
        val red = (color.red * 255).toInt()
        val green = (color.green * 255).toInt()
        val blue = (color.blue * 255).toInt()
        return String.format("#%02X%02X%02X", red, green, blue)
    }

    class Factory(private val repository: ChalkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DrawingViewModel(repository) as T
        }
    }
}
