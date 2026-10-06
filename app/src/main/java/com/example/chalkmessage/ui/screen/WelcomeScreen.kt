package com.example.chalkmessage.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chalkmessage.ui.components.ChalkButton
import com.example.chalkmessage.ui.components.ChalkHeart
import com.example.chalkmessage.ui.components.ChalkOutlineButton
import com.example.chalkmessage.ui.components.ChalkPink
import com.example.chalkmessage.ui.components.ChalkSquiggle
import com.example.chalkmessage.ui.components.ChalkStar
import com.example.chalkmessage.ui.components.ChalkWhite
import com.example.chalkmessage.ui.components.ChalkboardBackground
import com.example.chalkmessage.ui.theme.PatrickHandFontFamily
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    onCreateBoard: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ChalkboardBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Decorative doodles (alpha 0.5)
                // Top-start 40dp/16dp: two small ChalkHearts (18dp, white + pink)
                Box(modifier = Modifier.offset(x = 16.dp, y = 40.dp)) {
                    Row {
                        ChalkHeart(size = 18.dp, color = ChalkWhite.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.width(4.dp))
                        ChalkHeart(size = 18.dp, color = ChalkPink.copy(alpha = 0.5f))
                    }
                }

                // Top-end: ChalkSquiggle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 16.dp)
                ) {
                    ChalkSquiggle(width = 50.dp, color = ChalkWhite.copy(alpha = 0.5f))
                }

                // Mid-start: ChalkStar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                ) {
                    ChalkStar(size = 24.dp, color = ChalkWhite.copy(alpha = 0.5f))
                }

                // Mid-end: curly ChalkSquiggle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                ) {
                    ChalkSquiggle(width = 40.dp, color = ChalkPink.copy(alpha = 0.5f))
                }

                // Center Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.15f))

                    // ChalkHeart pair: two overlapping 28dp hearts (pink + white, offset 14dp)
                    Box(modifier = Modifier.size(width = 42.dp, height = 28.dp)) {
                        ChalkHeart(
                            size = 28.dp,
                            color = ChalkPink,
                            modifier = Modifier.offset(x = 0.dp)
                        )
                        ChalkHeart(
                            size = 28.dp,
                            color = ChalkWhite,
                            modifier = Modifier.offset(x = 14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Drawn to You",
                        style = TextStyle(
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 20.sp,
                            color = ChalkWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = "Welcome to",
                        style = TextStyle(
                            fontFamily = PatrickHandFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            color = ChalkWhite,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = "Drawn to You",
                        style = TextStyle(
                            fontFamily = PatrickHandFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp,
                            color = ChalkWhite,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hand-drawn underline: Canvas width 200dp height 10dp drawing a slightly wavy pink (#F4A7C3) stroke 3dp
                    Canvas(modifier = Modifier.size(width = 200.dp, height = 10.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(0f, h * 0.5f)
                            cubicTo(w * 0.3f, 0f, w * 0.7f, h, w, h * 0.4f)
                        }
                        drawPath(
                            path = path,
                            color = ChalkPink,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    Text(
                        text = "Intimate, real-time shared digital chalkboard for you and your favorite person.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = ChalkWhite.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .padding(top = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    ChalkButton(
                        text = "Create a Board",
                        onClick = onCreateBoard
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ChalkOutlineButton(
                        text = "Join a Board",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Joining arrives in the next step.")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.weight(0.15f))
                }
            }
        }
    }
}
