package com.example.chalkmessage.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.chalkmessage.data.model.ChalkMessage
import com.example.chalkmessage.ui.theme.*
import com.example.chalkmessage.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val pullToRefreshState = rememberPullToRefreshState()

    // Synergize refreshing states
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Message History",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ThemeBackground)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            if (messages.isEmpty()) {
                // Empty state with cute chalk illustration
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CuteChalkIllustration(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ThemePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Draw and send your first artwork to your connected friend!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeOnBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Grid layout (2 columns) with card previews
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        SwipeToDismissCard(
                            message = message,
                            onDelete = {
                                viewModel.deleteMessage(message.id) { success ->
                                    if (success) {
                                        // Show undo snackbar
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Message deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                }
                            },
                            onCardClick = {
                                if (!message.isRead) {
                                    viewModel.markAsRead(message.id)
                                }
                            }
                        )
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = ThemeSurface,
                contentColor = ThemePrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissCard(
    message: ChalkMessage,
    onDelete: () -> Unit,
    onCardClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = ThemeError.copy(alpha = 0.8f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        content = {
            MessageCard(
                message = message,
                modifier = Modifier.clickable { onCardClick() }
            )
        }
    )
}

@Composable
private fun MessageCard(
    message: ChalkMessage,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    // Pulsing glow border for unread messages
    val infiniteTransition = rememberInfiniteTransition(label = "GlowBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val borderModifier = if (!message.isRead) {
        Modifier.border(
            width = 3.dp,
            color = ThemePrimary.copy(alpha = borderAlpha),
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = ThemeOnBackground.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier),
        colors = CardDefaults.cardColors(
            containerColor = ThemeSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.senderId == message.recipientId) "From: ${message.senderName}" else "Sent Drawing",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
                if (!message.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ThemePrimary, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Preview Board background style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(ThemeBackground, RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                MiniChalkPreview(
                    strokes = message.strokes,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(message.timestamp)),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
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
                moveTo(first.x / 4f, first.y / 4f) // Scale down coordinates for mini preview
                for (i in 1 until stroke.points.size) {
                    val point = stroke.points[i]
                    val prev = stroke.points[i - 1]
                    val midX = ((prev.x + point.x) / 2) / 4f
                    val midY = ((prev.y + point.y) / 2) / 4f
                    quadraticBezierTo(prev.x / 4f, prev.y / 4f, midX, midY)
                }
            }
            val color = try {
                Color(android.graphics.Color.parseColor(stroke.colorHex))
            } catch (e: Exception) { Color.White }

            drawPath(
                path = path,
                color = color.copy(alpha = 0.9f),
                style = Stroke(
                    width = stroke.width / 2.5f, // scale down brush width
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun CuteChalkIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 3

        // Draw a hand-drawn-style chalk circle for face
        val path = Path().apply {
            val steps = 30
            for (i in 0..steps) {
                val angle = (2 * Math.PI / steps) * i
                val x = centerX + radius * Math.cos(angle).toFloat()
                val y = centerY + radius * Math.sin(angle).toFloat()
                val jitterX = if (i == 0 || i == steps) 0f else ((-2..2).random() * 0.5f).toFloat()
                val jitterY = if (i == 0 || i == steps) 0f else ((-2..2).random() * 0.5f).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x + jitterX, y + jitterY)
            }
        }
        drawPath(path, ThemeOnBackground.copy(alpha = 0.8f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Draw cute eyes
        drawCircle(
            color = ThemePrimary.copy(alpha = 0.9f),
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(centerX - radius / 2.2f, centerY - radius / 4f)
        )
        drawCircle(
            color = ThemePrimary.copy(alpha = 0.9f),
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(centerX + radius / 2.2f, centerY - radius / 4f)
        )

        // Draw a hand-drawn-style smile path
        val smilePath = Path().apply {
            moveTo(centerX - radius / 3f, centerY + radius / 5f)
            quadraticBezierTo(centerX, centerY + radius / 2.5f, centerX + radius / 3f, centerY + radius / 5f)
        }
        drawPath(smilePath, ThemeSecondary.copy(alpha = 0.9f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}
