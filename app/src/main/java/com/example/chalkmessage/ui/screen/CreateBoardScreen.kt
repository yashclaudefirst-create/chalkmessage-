package com.example.chalkmessage.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chalkmessage.ui.components.ChalkArrow
import com.example.chalkmessage.ui.components.ChalkButton
import com.example.chalkmessage.ui.components.ChalkCodeDisplay
import com.example.chalkmessage.ui.components.ChalkHeart
import com.example.chalkmessage.ui.components.ChalkOutlineButton
import com.example.chalkmessage.ui.components.ChalkPink
import com.example.chalkmessage.ui.components.ChalkTextField
import com.example.chalkmessage.ui.components.ChalkWhite
import com.example.chalkmessage.ui.components.ChalkYellow
import com.example.chalkmessage.ui.components.ChalkboardBackground
import com.example.chalkmessage.ui.theme.PatrickHandFontFamily
import com.example.chalkmessage.ui.viewmodel.CreateBoardViewModel

@Composable
fun CreateBoardScreen(
    viewModel: CreateBoardViewModel,
    onBoardCreated: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                    text = "Create Board",
                    style = TextStyle(
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        color = ChalkWhite
                    ),
                    modifier = Modifier.padding(top = 24.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState.createdCode == null) {
                    ChalkTextField(
                        value = uiState.boardName,
                        onValueChange = { viewModel.onBoardNameChanged(it) },
                        label = "Name your board"
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    ChalkTextField(
                        value = uiState.yourName,
                        onValueChange = { viewModel.onYourNameChanged(it) },
                        label = "Your name"
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    val canCreate = uiState.boardName.isNotBlank() && uiState.yourName.isNotBlank() && !uiState.isLoading
                    ChalkButton(
                        text = "CREATE",
                        enabled = canCreate,
                        onClick = { viewModel.createBoard() }
                    )

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = ChalkPink
                            ),
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                } else {
                    val code = uiState.createdCode!!
                    val shareText = "Join my board \"${uiState.boardName}\" on Drawn to You with code: $code"

                    ChalkArrow(size = 32.dp, color = ChalkWhite.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Share this code with your person",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ChalkWhite.copy(alpha = 0.6f)
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ChalkCodeDisplay(code = code)

                    Spacer(modifier = Modifier.height(32.dp))

                    ChalkOutlineButton(
                        text = "Copy & Share",
                        icon = Icons.Default.Share,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Board Code", code)
                            clipboard.setPrimaryClip(clip)

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            val chooser = Intent.createChooser(sendIntent, "Share Board Code")
                            context.startActivity(chooser)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ChalkOutlineButton(
                        text = "Share via WhatsApp",
                        icon = Icons.Default.Share,
                        iconTint = Color(0xFF25D366),
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(sendIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp isn't installed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ChalkButton(
                        text = "START DRAWING",
                        onClick = onBoardCreated
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom-start two tiny hearts
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
