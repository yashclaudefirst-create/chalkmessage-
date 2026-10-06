package com.example.chalkmessage.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chalkmessage.ui.components.ChalkOutlineButton
import com.example.chalkmessage.ui.components.ChalkPink
import com.example.chalkmessage.ui.components.ChalkWhite
import com.example.chalkmessage.ui.components.ChalkYellow
import com.example.chalkmessage.ui.components.ChalkboardBackground
import com.example.chalkmessage.ui.components.ChalkHeart
import com.example.chalkmessage.ui.components.ChalkStar
import com.example.chalkmessage.ui.theme.PatrickHandFontFamily
import com.example.chalkmessage.ui.viewmodel.SplashState
import com.example.chalkmessage.ui.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state is SplashState.Initial) {
            viewModel.signInAndInit()
        }
    }

    LaunchedEffect(state) {
        if (state is SplashState.Success) {
            onDone()
        }
    }

    ChalkboardBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(140.dp))

            // Canvas 160dp box drawing a large chalk heart with a small star
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                ChalkHeart(
                    size = 140.dp,
                    color = ChalkWhite,
                    strokeDp = 3f
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    ChalkStar(
                        size = 28.dp,
                        color = ChalkYellow,
                        strokeDp = 2.5f
                    )
                }
            }

            Text(
                text = "Drawn to You",
                style = TextStyle(
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 40.sp,
                    color = ChalkWhite
                ),
                modifier = Modifier.padding(top = 24.dp)
            )

            if (state is SplashState.Error) {
                val errorMsg = (state as SplashState.Error).message
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMsg,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = ChalkPink
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ChalkOutlineButton(
                    text = "Retry",
                    onClick = { viewModel.signInAndInit() },
                    modifier = Modifier.size(width = 120.dp, height = 44.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Private. Intimate. Yours.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = ChalkWhite.copy(alpha = 0.7f)
                ),
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}
