package com.example.chalkmessage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chalkmessage.ui.theme.PatrickHandFontFamily
import kotlin.random.Random

val ChalkboardBgColor = Color(0xFF1C1F1D)
val ChalkWhite = Color(0xFFF2F0E6)
val ChalkYellow = Color(0xFFF2C94C)
val ChalkPink = Color(0xFFF4A7C3)
val ChalkCream = Color(0xFFF3E5AB)
val ChalkBlue = Color(0xFFA8C5E8)
val ChalkMint = Color(0xFFA8E6CF)

@Composable
fun ChalkboardBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChalkboardBgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = Random(42)
            // ~120 noise dots
            repeat(120) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                val radius = (1 + random.nextFloat() * 2).dp.toPx()
                val alpha = 0.04f + random.nextFloat() * 0.06f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
            // 3 faint smudge circles
            val smudges = listOf(
                Triple(0.2f, 0.3f, 60.dp.toPx()),
                Triple(0.8f, 0.2f, 80.dp.toPx()),
                Triple(0.5f, 0.7f, 50.dp.toPx())
            )
            for ((rx, ry, rPx) in smudges) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = rPx,
                    center = Offset(size.width * rx, size.height * ry)
                )
            }
        }
        content()
    }
}

@Composable
fun ChalkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bgColor = if (enabled) ChalkYellow else ChalkYellow.copy(alpha = 0.5f)
    val textColor = ChalkboardBgColor.copy(alpha = if (enabled) 1f else 0.6f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textColor
            )
        )
    }
}

@Composable
fun ChalkOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = ChalkWhite
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, ChalkWhite, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ChalkWhite
                )
            )
        }
    }
}

@Composable
fun ChalkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 18.sp,
                color = ChalkWhite
            ),
            cursorBrush = SolidColor(ChalkYellow),
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 18.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        val lineColor = if (isFocused) ChalkYellow else ChalkWhite.copy(alpha = 0.6f)
        val strokeWidth = if (isFocused) 2.dp else 2.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(strokeWidth)
                .background(lineColor)
        )
    }
}

@Composable
fun ChalkCodeDisplay(
    code: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val charStr = code.getOrNull(index)?.toString() ?: ""
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.5.dp, ChalkWhite, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = charStr,
                    style = TextStyle(
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        color = ChalkWhite
                    )
                )
            }
        }
    }
}

@Composable
fun ChalkHeart(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = ChalkWhite,
    strokeDp: Float = 2.5f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.8f)
            cubicTo(w * 0.1f, h * 0.5f, w * 0.05f, h * 0.15f, w * 0.35f, h * 0.15f)
            cubicTo(w * 0.48f, h * 0.15f, w * 0.5f, h * 0.3f, w * 0.5f, h * 0.3f)
            cubicTo(w * 0.5f, h * 0.3f, w * 0.52f, h * 0.15f, w * 0.65f, h * 0.15f)
            cubicTo(w * 0.95f, h * 0.15f, w * 0.9f, h * 0.5f, w * 0.5f, h * 0.8f)
            close()
        }
        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * 0.8f),
            style = Stroke(width = strokeDp.dp.toPx())
        )
    }
}

@Composable
fun ChalkSquiggle(
    modifier: Modifier = Modifier,
    width: Dp = 60.dp,
    height: Dp = 20.dp,
    color: Color = ChalkWhite,
    strokeDp: Float = 2.5f
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(0f, h * 0.5f)
            cubicTo(w * 0.25f, 0f, w * 0.25f, h, w * 0.5f, h * 0.5f)
            cubicTo(w * 0.75f, 0f, w * 0.75f, h, w, h * 0.5f)
        }
        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * 0.8f),
            style = Stroke(width = strokeDp.dp.toPx())
        )
    }
}

@Composable
fun ChalkStar(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = ChalkYellow,
    strokeDp: Float = 2.5f
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val outerR = this.size.width / 2f * 0.9f
        val innerR = outerR * 0.4f

        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (i * 36 - 90) * (Math.PI / 180.0)
            val x = (cx + r * kotlin.math.cos(angle)).toFloat()
            val y = (cy + r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * 0.8f),
            style = Stroke(width = strokeDp.dp.toPx())
        )
    }
}

@Composable
fun ChalkArrow(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = ChalkWhite,
    strokeDp: Float = 2.5f
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokePx = strokeDp.dp.toPx()

        // Curved shaft pointing down
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.1f)
            cubicTo(w * 0.7f, h * 0.2f, w * 0.3f, h * 0.7f, w * 0.5f, h * 0.85f)
        }
        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * 0.8f),
            style = Stroke(width = strokePx)
        )

        // Arrowhead
        val head1 = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.68f)
        }
        val head2 = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            lineTo(w * 0.65f, h * 0.7f)
        }
        drawPath(head1, color = color.copy(alpha = color.alpha * 0.8f), style = Stroke(width = strokePx))
        drawPath(head2, color = color.copy(alpha = color.alpha * 0.8f), style = Stroke(width = strokePx))
    }
}
