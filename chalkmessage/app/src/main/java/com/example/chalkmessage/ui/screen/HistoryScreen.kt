package com.example.chalkmessage.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.chalkmessage.data.model.ChalkMessage
import com.example.chalkmessage.ui.theme.BlackboardGreen
import com.example.chalkmessage.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageCard(message)
            }
        }
    }
}

@Composable
private fun MessageCard(message: ChalkMessage) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BlackboardGreen
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (message.senderId == message.recipientId) "From: ${message.senderName}" else "Sent",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Mini preview canvas
            MiniChalkPreview(
                strokes = message.strokes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(message.timestamp)),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MiniChalkPreview(
    strokes: List<com.example.chalkmessage.data.model.Stroke>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path().apply {
                val first = stroke.points.first()
                moveTo(first.x, first.y)
                for (i in 1 until stroke.points.size) {
                    val point = stroke.points[i]
                    val prev = stroke.points[i - 1]
                    val midX = (prev.x + point.x) / 2
                    val midY = (prev.y + point.y) / 2
                    quadraticBezierTo(prev.x, prev.y, midX, midY)
                }
            }
            val color = try {
                Color(android.graphics.Color.parseColor(stroke.colorHex))
            } catch (e: Exception) { Color.White }

            drawPath(
                path = path,
                color = color.copy(alpha = 0.9f),
                style = Stroke(
                    width = stroke.width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
