package com.example.chalkmessage.ui.screen

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.chalkmessage.ui.theme.*
import com.example.chalkmessage.ui.viewmodel.DrawingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val strokes by viewModel.currentStrokes.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val brushWidth by viewModel.currentWidth.collectAsState()

    val isSending by viewModel.isSending.collectAsState()
    val showConnectionWarning by viewModel.showConnectionWarning.collectAsState()
    val messageSent by viewModel.messageSent.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Immersive Mode effect
    val window = (context as? Activity)?.window
    LaunchedEffect(Unit) {
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Restore System Bars on dispose
    DisposableEffect(Unit) {
        onDispose {
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Handle VM events
    LaunchedEffect(showConnectionWarning) {
        if (showConnectionWarning) {
            val result = snackbarHostState.showSnackbar(
                message = "Connect with a friend first to send messages!",
                actionLabel = "Connect",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                onNavigateToOnboarding()
            }
            viewModel.resetConnectionWarning()
        }
    }

    LaunchedEffect(messageSent) {
        if (messageSent) {
            Toast.makeText(context, "Chalk drawing sent successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetMessageSent()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    val chalkColors = listOf(
        ChalkWhite to "White",
        ChalkYellow to "Yellow",
        ChalkPink to "Pink",
        ChalkBlue to "Blue",
        ChalkMint to "Mint"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chalkboard Canvas",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeBackground,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Undo with animated press
                    var undoPressed by remember { mutableStateOf(false) }
                    val undoScale by animateFloatAsState(if (undoPressed) 0.8f else 1f, label = "UndoScale")
                    IconButton(
                        onClick = {
                            undoPressed = true
                            viewModel.undo()
                        },
                        modifier = Modifier.scale(undoScale)
                    ) {
                        Icon(Icons.Default.Undo, "Undo", tint = Color.White)
                    }
                    LaunchedEffect(undoPressed) {
                        if (undoPressed) {
                            kotlinx.coroutines.delay(100)
                            undoPressed = false
                        }
                    }

                    // Redo with animated press
                    var redoPressed by remember { mutableStateOf(false) }
                    val redoScale by animateFloatAsState(if (redoPressed) 0.8f else 1f, label = "RedoScale")
                    IconButton(
                        onClick = {
                            redoPressed = true
                            viewModel.redo()
                        },
                        modifier = Modifier.scale(redoScale)
                    ) {
                        Icon(Icons.Default.Redo, "Redo", tint = Color.White)
                    }
                    LaunchedEffect(redoPressed) {
                        if (redoPressed) {
                            kotlinx.coroutines.delay(100)
                            redoPressed = false
                        }
                    }

                    // Clear
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Default.Delete, "Clear", tint = Color.White)
                    }

                    // History
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "History", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = ThemeBackground,
                modifier = Modifier.height(130.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Brush size slider (2px to 20px)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Size:", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = brushWidth,
                            onValueChange = viewModel::setWidth,
                            valueRange = 2f..20f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = ThemePrimary,
                                activeTrackColor = ThemePrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${brushWidth.toInt()}px", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color picker as floating bubbles with bounce animation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            chalkColors.forEach { (color, _) ->
                                val isSelected = currentColor == color
                                val bubbleScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.25f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioHighBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "BubbleScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .scale(bubbleScale)
                                        .background(color = color, shape = CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setColor(color) }
                                )
                            }
                        }

                        // Send button with paper airplane animation
                        var sendPressed by remember { mutableStateOf(false) }
                        val airplaneTranslationX by animateFloatAsState(
                            targetValue = if (sendPressed || isSending) 150f else 0f,
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            label = "AirplaneFlight"
                        )
                        val airplaneTranslationY by animateFloatAsState(
                            targetValue = if (sendPressed || isSending) -150f else 0f,
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            label = "AirplaneFlightY"
                        )
                        val airplaneAlpha by animateFloatAsState(
                            targetValue = if (sendPressed || isSending) 0f else 1f,
                            animationSpec = tween(500),
                            label = "AirplaneAlpha"
                        )

                        val buttonScale by animateFloatAsState(
                            targetValue = if (sendPressed) 0.9f else 1f,
                            label = "ButtonScale"
                        )

                        FilledIconButton(
                            onClick = {
                                sendPressed = true
                                viewModel.sendDrawing(context)
                            },
                            modifier = Modifier.scale(buttonScale),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = ThemePrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = ThemeBackground,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = airplaneTranslationX
                                        translationY = airplaneTranslationY
                                        alpha = airplaneAlpha
                                    }
                            )
                        }

                        // Reset send animation after flight finishes
                        LaunchedEffect(sendPressed, isSending) {
                            if (sendPressed && !isSending) {
                                kotlinx.coroutines.delay(800)
                                sendPressed = false
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Chalk texture background (subtle noise/grain overlay)
                .drawBehind {
                    // Solid dark chalkboard background
                    drawRect(ThemeBackground)

                    // Grain noise details
                    val dotCount = 5000
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        this.color = android.graphics.Color.WHITE
                        alpha = 10 // highly subtle dust/chalk grain texture
                    }
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    // Use a seeded or consistent look
                    val random = java.util.Random(1337)
                    for (i in 0 until dotCount) {
                        val x = random.nextFloat() * size.width
                        val y = random.nextFloat() * size.height
                        val radius = 0.5f + random.nextFloat() * 1.5f
                        nativeCanvas.drawCircle(x, y, radius, paint)
                    }
                }
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

                    // Chalk effect: double pass for beautiful soft glow rendering
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.85f),
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Second pass for chalk texture (lighter, thinner, soft glow overlay)
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.35f),
                        style = Stroke(
                            width = stroke.width * 0.7f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Loading state overlay while sending to Firebase
            if (isSending) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = ThemePrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Sending drawing...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

// Helper to parse hex color strings
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.White
    }
}
