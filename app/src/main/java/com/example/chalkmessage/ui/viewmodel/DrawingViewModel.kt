package com.example.chalkmessage.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chalkmessage.data.ChalkRepository
import com.example.chalkmessage.data.model.DrawPoint
import com.example.chalkmessage.data.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DrawingViewModel(private val repository: ChalkRepository) : ViewModel() {

    // Current drawing session state
    private val _currentStrokes = MutableStateFlow<List<Stroke>>(emptyList())
    val currentStrokes: StateFlow<List<Stroke>> = _currentStrokes

    // Redo stack for undo/redo functionality
    private val redoStrokes = mutableListOf<Stroke>()

    private val _currentColor = MutableStateFlow(Color.White)
    val currentColor: StateFlow<Color> = _currentColor

    private val _currentWidth = MutableStateFlow(8f)
    val currentWidth: StateFlow<Float> = _currentWidth

    // Loading & state flows
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _showConnectionWarning = MutableStateFlow(false)
    val showConnectionWarning: StateFlow<Boolean> = _showConnectionWarning

    private val _messageSent = MutableStateFlow(false)
    val messageSent: StateFlow<Boolean> = _messageSent

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Active stroke being drawn (finger currently down)
    private var activeStrokePoints = mutableListOf<DrawPoint>()

    fun setColor(color: Color) {
        _currentColor.value = color
    }

    fun setWidth(width: Float) {
        _currentWidth.value = width
    }

    fun startStroke(x: Float, y: Float) {
        // Clear redo stack on new draw action
        redoStrokes.clear()
        activeStrokePoints = mutableListOf(DrawPoint(x, y))
        // Temporarily add a stroke that will be updated in addPoint
        val stroke = Stroke(
            points = activeStrokePoints.toList(),
            colorHex = colorToHex(_currentColor.value),
            width = _currentWidth.value
        )
        _currentStrokes.value = _currentStrokes.value + stroke
    }

    fun addPoint(x: Float, y: Float) {
        activeStrokePoints.add(DrawPoint(x, y))
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
            val newList = _currentStrokes.value.dropLast(1) + stroke
            // Enforce max stack size of 20 strokes
            if (newList.size > 20) {
                _currentStrokes.value = newList.takeLast(20)
            } else {
                _currentStrokes.value = newList
            }
        } else {
            // Remove the temporary single point stroke if they just tapped
            if (_currentStrokes.value.isNotEmpty()) {
                _currentStrokes.value = _currentStrokes.value.dropLast(1)
            }
        }
        activeStrokePoints.clear()
    }

    fun undo() {
        if (_currentStrokes.value.isNotEmpty()) {
            val last = _currentStrokes.value.last()
            redoStrokes.add(last)
            _currentStrokes.value = _currentStrokes.value.dropLast(1)
        }
    }

    fun redo() {
        if (redoStrokes.isNotEmpty()) {
            val lastRedo = redoStrokes.removeAt(redoStrokes.lastIndex)
            val newList = _currentStrokes.value + lastRedo
            if (newList.size > 20) {
                _currentStrokes.value = newList.takeLast(20)
            } else {
                _currentStrokes.value = newList
            }
        }
    }

    fun clear() {
        _currentStrokes.value = emptyList()
        redoStrokes.clear()
        activeStrokePoints.clear()
    }

    fun sendDrawing(context: Context) {
        viewModelScope.launch {
            if (_currentStrokes.value.isEmpty()) {
                _errorMessage.value = "Cannot send an empty drawing!"
                return@launch
            }

            _isSending.value = true
            _errorMessage.value = null

            val hasInternet = isNetworkAvailable(context)

            try {
                if (!hasInternet) {
                    _errorMessage.value = "No internet connection. Please check your network."
                    _isSending.value = false
                    return@launch
                }

                repository.sendMessage(_currentStrokes.value)
                _messageSent.value = true
                clear()
            } catch (e: IllegalArgumentException) {
                _showConnectionWarning.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun resetConnectionWarning() {
        _showConnectionWarning.value = false
    }

    fun resetMessageSent() {
        _messageSent.value = false
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun colorToHex(color: Color): String {
        val red = (color.red * 255).toInt()
        val green = (color.green * 255).toInt()
        val blue = (color.blue * 255).toInt()
        return String.format("#%02X%02X%02X", red, green, blue)
    }

    /*
     * ==========================================
     *  AI PREPARATION PLACEHOLDER (For Future)
     * ==========================================
     * TODO: Integrate Gemini/Vertex AI for Android.
     * Features planned:
     * 1. Smart Chalk Suggestions: Analyze the current strokes on the canvas
     *    and suggest doodles or next paths to draw.
     * 2. Autocomplete drawings: Help users finish beautiful cursive handwritings
     *    or doodles based on their stroke history.
     * 3. AI Chalk-style filter: Refine raw drawings to look like perfect classical chalkboard art.
     *
     * Example future implementation:
     * suspend fun analyzeDrawingWithAI(strokes: List<Stroke>): AISuggestion {
     *     val bitmap = renderStrokesToBitmap(strokes)
     *     return generativeModel.generateContent(
     *         content {
     *             image(bitmap)
     *             text("Suggest 3 cute chalk drawing concepts based on this doodle.")
     *         }
     *     )
     * }
     */

    class Factory(private val repository: ChalkRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DrawingViewModel(repository) as T
        }
    }
}
