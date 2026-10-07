package com.example.chalkmessage.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chalkmessage.ui.components.ChalkButton
import com.example.chalkmessage.ui.components.ChalkHeart
import com.example.chalkmessage.ui.components.ChalkPink
import com.example.chalkmessage.ui.components.ChalkTextField
import com.example.chalkmessage.ui.components.ChalkWhite
import com.example.chalkmessage.ui.components.ChalkYellow
import com.example.chalkmessage.ui.components.ChalkboardBackground
import com.example.chalkmessage.ui.theme.PatrickHandFontFamily
import com.example.chalkmessage.ui.viewmodel.JoinBoardViewModel

@Composable
fun JoinBoardScreen(
    viewModel: JoinBoardViewModel,
    onJoinedBoard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.joinedBoardId) {
        if (uiState.joinedBoardId != null) {
            onJoinedBoard()
        }
    }

    ChalkboardBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top decorative hearts
                Row(modifier = Modifier.fillMaxWidth()) {
                    ChalkHeart(size = 16.dp, color = ChalkWhite.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.weight(1f))
                    ChalkHeart(size = 16.dp, color = ChalkPink.copy(alpha = 0.6f))
                }

                Text(
                    text = "Join Board",
                    style = TextStyle(
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        color = ChalkWhite
                    ),
                    modifier = Modifier.padding(top = 24.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                ChalkTextField(
                    value = uiState.yourName,
                    onValueChange = { viewModel.onYourNameChanged(it) },
                    label = "Your name"
                )

                Spacer(modifier = Modifier.height(24.dp))

                ChalkTextField(
                    value = uiState.code,
                    onValueChange = { viewModel.onCodeChanged(it) },
                    label = "Board Code (e.g. 123456)"
                )

                Spacer(modifier = Modifier.height(40.dp))

                val canJoin = uiState.code.isNotBlank() && uiState.yourName.isNotBlank() && !uiState.isLoading
                ChalkButton(
                    text = "JOIN BOARD",
                    enabled = canJoin,
                    onClick = { viewModel.joinBoard() }
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ChalkPink
                        ),
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom decorative hearts
                Row(modifier = Modifier.fillMaxWidth()) {
                    ChalkHeart(size = 12.dp, color = ChalkWhite.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(4.dp))
                    ChalkHeart(size = 12.dp, color = ChalkPink.copy(alpha = 0.5f))
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = ChalkYellow,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}
