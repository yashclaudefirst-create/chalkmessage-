package com.example.chalkmessage.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chalkmessage.data.BoardMemberUi
import com.example.chalkmessage.data.model.Stroke
import com.example.chalkmessage.ui.viewmodel.BoardDrawingViewModel
import com.example.chalkmessage.ui.viewmodel.SendState

val ChalkWhite = Color(0xFFFFFFFF)
val ChalkYellow = Color(0xFFFFF176)
val ChalkPink = Color(0xFFF48FB1)
val ChalkBlue = Color(0xFF81D4FA)
val ChalkMint = Color(0xFFA5D6A7)

val ChalkPalette = listOf(ChalkWhite, ChalkYellow, ChalkPink, ChalkBlue, ChalkMint)

val ChalkboardBackground = Color(0xFF1A261A)
val ChalkboardUnderline = Color(0x66FFFFFF)
val MemberRingColors = listOf(
    Color(0xFFE57373), Color(0xFF81D4FA), Color(0xFFA5D6A7),
    Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFFFF8A65)
)

@Composable
fun MainChalkboardScreen(
    viewModel: BoardDrawingViewModel,
    modifier: Modifier = Modifier
) {
    val members by viewModel.members.collectAsState()
    val typingUserName by viewModel.typingUserName.collectAsState()
    val currentStrokes by viewModel.currentStrokes.collectAsState()
    val activeStroke by viewModel.activeStroke.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val brushWidth by viewModel.brushWidth.collectAsState()
    val isEraser by viewModel.isEraser.collectAsState()
    val sendState by viewModel.sendState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ChalkboardBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Section
                TopHeaderSection(
                    boardName = viewModel.boardName,
                    members = members,
                    typingUserName = typingUserName
                )

                // Full-bleed Chalkboard Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    ChalkboardCanvas(
                        strokes = currentStrokes,
                        activeStroke = activeStroke,
                        onStartStroke = { x, y -> viewModel.startStroke(x, y) },
                        onAddPoint = { x, y -> viewModel.addPoint(x, y) },
                        onEndStroke = { viewModel.endStroke() }
                    )
                }

                // Bottom Drawing Toolbar
                BottomDrawingToolbar(
                    currentColor = currentColor,
                    brushWidth = brushWidth,
                    isEraser = isEraser,
                    sendState = sendState,
                    onColorSelected = { viewModel.setColor(it) },
                    onBrushWidthChanged = { viewModel.setBrushWidth(it) },
                    onEraserToggled = { viewModel.toggleEraser() },
                    onClearCanvas = { viewModel.clear() },
                    onSend = { viewModel.send() }
                )
            }
        }
    }
}

@Composable
private fun TopHeaderSection(
    boardName: String,
    members: List<BoardMemberUi>,
    typingUserName: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular member avatars
        if (members.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-8).dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                members.take(6).forEachIndexed { index, member ->
                    MemberAvatar(
                        member = member,
                        ringColor = MemberRingColors[index % MemberRingColors.size]
                    )
                }
            }
        }

        // Board Name with thin underline
        Text(
            text = boardName,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(120.dp)
                .height(1.dp)
                .background(ChalkboardUnderline)
        )

        // Animated "X is drawing..." row
        AnimatedVisibility(
            visible = typingUserName != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            typingUserName?.let { name ->
                DrawingIndicatorRow(userName = name)
            }
        }
    }
}

@Composable
private fun MemberAvatar(
    member: BoardMemberUi,
    ringColor: Color
) {
    val initials = member.name.take(2).uppercase()
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(2.dp, ringColor, CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
            .background(Color(0xFF2C3E30)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DrawingIndicatorRow(userName: String) {
    val transition = rememberInfiniteTransition(label = "drawing_dots")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$userName is drawing...",
            color = Color(0xFFFFF59D),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.alpha(alphaAnim)
        )
    }
}

@Composable
private fun ChalkboardCanvas(
    strokes: List<Stroke>,
    activeStroke: Stroke?,
    onStartStroke: (Float, Float) -> Unit,
    onAddPoint: (Float, Float) -> Unit,
    onEndStroke: () -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onStartStroke(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onAddPoint(change.position.x, change.position.y)
                    },
                    onDragEnd = {
                        onEndStroke()
                    },
                    onDragCancel = {
                        onEndStroke()
                    }
                )
            }
    ) {
        // Draw completed strokes
        strokes.forEach { stroke ->
            drawChalkStroke(stroke)
        }
        // Draw active in-progress stroke
        activeStroke?.let { stroke ->
            drawChalkStroke(stroke)
        }
    }
}

private fun DrawScope.drawChalkStroke(stroke: Stroke) {
    if (stroke.points.size < 2) {
        // Single point tap rendering
        stroke.points.firstOrNull()?.let { point ->
            val color = parseStrokeColor(stroke.colorHex)
            drawCircle(
                color = color.copy(alpha = 0.9f),
                radius = stroke.width / 2f,
                center = Offset(point.x, point.y)
            )
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = (stroke.width * 0.7f) / 2f,
                center = Offset(point.x, point.y)
            )
        }
        return
    }

    val path = Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        for (i in 1 until stroke.points.size) {
            val p = stroke.points[i]
            lineTo(p.x, p.y)
        }
    }

    val color = parseStrokeColor(stroke.colorHex)

    // Double-pass rendering for chalky texture effect
    // 1. Solid pass at full width / 0.9 alpha
    drawPath(
        path = path,
        color = color.copy(alpha = 0.9f),
        style = DrawStroke(
            width = stroke.width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 2. Lighter pass at 0.7x width / 0.3 alpha
    drawPath(
        path = path,
        color = color.copy(alpha = 0.3f),
        style = DrawStroke(
            width = stroke.width * 0.7f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun parseStrokeColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> 0xFFFFFFFF.toInt()
        }
        Color(colorInt)
    } catch (e: Exception) {
        Color.White
    }
}

@Composable
private fun BottomDrawingToolbar(
    currentColor: Color,
    brushWidth: Float,
    isEraser: Boolean,
    sendState: SendState,
    onColorSelected: (Color) -> Unit,
    onBrushWidthChanged: (Float) -> Unit,
    onEraserToggled: () -> Unit,
    onClearCanvas: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF141E15),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Row 1: Brush size slider & Eraser / Clear controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brush size indicator & slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size((brushWidth.coerceIn(4f, 20f)).dp)
                            .clip(CircleShape)
                            .background(if (isEraser) Color.Gray else currentColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = brushWidth,
                        onValueChange = onBrushWidthChanged,
                        valueRange = 2f..20f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White.copy(alpha = 0.8f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Eraser button
                IconButton(
                    onClick = onEraserToggled,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isEraser) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixNormal,
                        contentDescription = "Eraser",
                        tint = if (isEraser) Color.Yellow else Color.White
                    )
                }

                // Clear button
                IconButton(
                    onClick = onClearCanvas,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Canvas",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: 5 Chalk color dots & Send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Color dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChalkPalette.forEach { color ->
                        val isSelected = !isEraser && color == currentColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(color) }
                        )
                    }
                }

                // Send button with loading spinner
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .clickable(enabled = sendState !is SendState.Sending) { onSend() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (sendState is SendState.Sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Send",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Drawing",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
