package com.example.chalkmessage.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chalkmessage.ui.theme.*
import com.example.chalkmessage.ui.viewmodel.OnboardingViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToDrawing: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Side effect: navigate when connected
    LaunchedEffect(state) {
        if (state is OnboardingViewModel.OnboardingState.Connected) {
            onNavigateToDrawing()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ThemeBackground
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                if (targetState is OnboardingViewModel.OnboardingState.Ready) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "OnboardingStepTransition"
        ) { currentState ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (currentState) {
                    is OnboardingViewModel.OnboardingState.Loading -> {
                        CircularProgressIndicator(color = ThemePrimary)
                    }
                    is OnboardingViewModel.OnboardingState.NeedsName -> {
                        NameSetupScreen(onSubmit = { viewModel.createUser(it) })
                    }
                    is OnboardingViewModel.OnboardingState.Ready -> {
                        ConnectScreen(
                            userId = currentState.userId,
                            inviteCode = currentState.inviteCode,
                            error = currentState.error,
                            isConnecting = currentState.isConnecting,
                            onConnect = { viewModel.connectToUser(it) },
                            onSkip = { viewModel.skipConnection() },
                            onClearError = { viewModel.clearError() }
                        )
                    }
                    else -> {
                        // Connected transitions handled by LaunchedEffect
                        CircularProgressIndicator(color = ThemePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign? = null,
    durationMillisPerChar: Int = 50
) {
    var visibleText by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        visibleText = ""
        for (i in text.indices) {
            visibleText += text[i]
            kotlinx.coroutines.delay(durationMillisPerChar.toLong())
        }
    }
    Text(
        text = visibleText,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign
    )
}

@Composable
private fun NameSetupScreen(onSubmit: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var buttonPressed by remember { mutableStateOf(false) }

    val buttonScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Chalk Title
        TypewriterText(
            text = "Drawn to You",
            style = MaterialTheme.typography.headlineLarge,
            color = ThemePrimary,
            textAlign = TextAlign.Center,
            durationMillisPerChar = 100
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A Chalk Message Adventure",
            style = MaterialTheme.typography.titleLarge,
            color = ThemeSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.titleLarge,
            color = ThemeOnBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Custom Chalk-Styled Underline Input Field
        ChalkTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Enter your name..."
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    buttonPressed = true
                    onSubmit(name.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(buttonScale),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemePrimary,
                contentColor = ThemeBackground
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Get Started",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = ThemeBackground
            )
        }
    }
}

@Composable
private fun ConnectScreen(
    userId: String,
    inviteCode: String,
    error: String?,
    isConnecting: Boolean,
    onConnect: (String) -> Unit,
    onSkip: () -> Unit,
    onClearError: () -> Unit
) {
    var partnerCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var connectPressed by remember { mutableStateOf(false) }
    val connectButtonScale by animateFloatAsState(
        targetValue = if (connectPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ConnectButtonScale"
    )

    var skipPressed by remember { mutableStateOf(false) }
    val skipButtonScale by animateFloatAsState(
        targetValue = if (skipPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "SkipButtonScale"
    )

    // Show error toast if any error occurs
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            onClearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TypewriterText(
            text = "Your Invite Code",
            style = MaterialTheme.typography.headlineMedium,
            color = ThemePrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Chalk-styled card for invite code
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeSurface, shape = RoundedCornerShape(12.dp))
                .border(2.dp, ThemeOnBackground, RoundedCornerShape(12.dp))
                .clickable {
                    clipboardManager.setText(AnnotatedString(inviteCode))
                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = inviteCode,
                    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 2.sp),
                    color = ThemePrimary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = ThemeOnBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Connect to Friend",
            style = MaterialTheme.typography.titleLarge,
            color = ThemeSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Animated input field with bounce focus state or outline pulse
        var isFocused by remember { mutableStateOf(false) }
        val animatedBorderWidth by animateDpAsState(
            targetValue = if (isFocused) 3.dp else 1.5.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "AnimatedBorderWidth"
        )
        val animatedBorderColor by animateColorAsState(
            targetValue = if (isFocused) ThemeSecondary else ThemeOnBackground.copy(alpha = 0.6f),
            animationSpec = spring(),
            label = "AnimatedBorderColor"
        )

        BasicTextField(
            value = partnerCode,
            onValueChange = { partnerCode = it.uppercase().trim() },
            textStyle = MaterialTheme.typography.titleLarge.copy(
                color = ThemeOnBackground,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(ThemeBackground, RoundedCornerShape(12.dp))
                .border(animatedBorderWidth, animatedBorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (partnerCode.isEmpty()) {
                        Text(
                            text = "Friend's Invite Code",
                            color = ThemeOnBackground.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (partnerCode.isNotBlank() && !isConnecting) {
                    connectPressed = true
                    onConnect(partnerCode)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(connectButtonScale),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeSecondary,
                contentColor = ThemeBackground
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isConnecting
        ) {
            if (isConnecting) {
                CircularProgressIndicator(color = ThemeBackground, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    "Connect",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemeBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Skip for now" styled as a chalk underline text button
        Box(
            modifier = Modifier
                .clickable {
                    skipPressed = true
                    onSkip()
                }
                .scale(skipButtonScale)
                .padding(8.dp)
        ) {
            Text(
                text = "Skip for now",
                style = MaterialTheme.typography.titleLarge,
                color = ThemeOnBackground.copy(alpha = 0.8f),
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    val pointsCount = 8
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(0f, y)
                    for (i in 1..pointsCount) {
                        val x = (size.width / pointsCount) * i
                        val jitter = ((-1..1).random()).toFloat()
                        path.lineTo(x, y + jitter)
                    }
                    drawPath(
                        path = path,
                        color = ThemeOnBackground.copy(alpha = 0.6f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            )
        }
    }
}

@Composable
fun ChalkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.titleLarge.copy(color = ThemeOnBackground, textAlign = TextAlign.Center),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.titleLarge,
                        color = ThemeOnBackground.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 3.dp.toPx()
                val y = size.height - strokeWidth / 2
                val pointsCount = 20
                val path = androidx.compose.ui.graphics.Path()
                path.moveTo(0f, y)
                for (i in 1..pointsCount) {
                    val x = (size.width / pointsCount) * i
                    val jitter = if (i == pointsCount) 0f else ((-2..2).random()).toFloat()
                    path.lineTo(x, y + jitter)
                }
                drawPath(
                    path = path,
                    color = ThemeOnBackground,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
    )
}
