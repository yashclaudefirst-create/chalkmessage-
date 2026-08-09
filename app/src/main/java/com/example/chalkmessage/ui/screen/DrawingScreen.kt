package com.example.chalkmessage.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.chalkmessage.ui.theme.*
import com.example.chalkmessage.ui.viewmodel.DrawingViewModel

@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel,
    onNavigateToHistory: () -> Unit
) {
    val strokes by viewModel.currentStrokes.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()

    val chalkColors = listOf(
        ChalkWhite to "White",
        ChalkYellow to "Yellow",
        ChalkPink to "Pink",
        ChalkBlue to "Blue",
        ChalkMint to "Mint"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chalk Board") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BlackboardGreen,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                ),
                actions = {
                    IconButton(onClick = viewModel::undo) {
                        Icon(Icons.Default.Undo, "Undo", tint = androidx.compose.ui.graphics.Color.White)
                    }
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Default.Delete, "Clear", tint = androidx.compose.ui.graphics.Color.White)
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "History", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = BlackboardDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color picker
                    chalkColors.forEach { (color, _) ->
                        val isSelected = currentColor == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = color,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setColor(color)
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier.padding(2.dp)
                                    } else Modifier
                                )
                        )
                    }

                    // Send button
                    FilledIconButton(
                        onClick = viewModel::sendDrawing,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BlackboardGreen)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            viewModel.startStroke(offset.x, offset.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            viewModel.addPoint(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            viewModel.endStroke()
                        }
                    )
                }
        ) {
            // Canvas renders the strokes
            Canvas(modifier = Modifier.fillMaxSize()) {
                strokes.forEach { stroke ->
                    if (stroke.points.size < 2) return@forEach

                    val path = Path().apply {
                        val first = stroke.points.first()
                        moveTo(first.x, first.y)

                        // Draw smooth curves through points
                        for (i in 1 until stroke.points.size) {
                            val point = stroke.points[i]
                            val prev = stroke.points[i - 1]
                            val midX = (prev.x + point.x) / 2
                            val midY = (prev.y + point.y) / 2
                            quadraticBezierTo(prev.x, prev.y, midX, midY)
                        }
                        // Connect to last point
                        val last = stroke.points.last()
                        lineTo(last.x, last.y)
                    }

                    val color = parseColor(stroke.colorHex)

                    // Chalk effect: draw stroke with slight jitter/texture
                    // For MVP: use a slightly irregular width
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.9f),
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Second pass for chalk texture (lighter, thinner overlay)
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.3f),
                        style = Stroke(
                            width = stroke.width * 0.7f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

// Helper to parse hex color strings
private fun parseColor(hex: String): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.White
    }
}
