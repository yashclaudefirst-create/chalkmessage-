package com.example.chalkmessage.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chalkmessage.ui.viewmodel.OnboardingViewModel

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
        color = MaterialTheme.colorScheme.background
    ) {
        when (val currentState = state) {
            is OnboardingViewModel.OnboardingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is OnboardingViewModel.OnboardingState.NeedsName -> {
                NameSetupScreen(onSubmit = { viewModel.createUser(it) })
            }
            is OnboardingViewModel.OnboardingState.Ready -> {
                ConnectScreen(
                    userId = currentState.userId,
                    inviteCode = currentState.inviteCode,
                    onConnect = { viewModel.connectToUser(it) }
                )
            }
            else -> {} // Connected state handled by LaunchedEffect
        }
    }
}

@Composable
private fun NameSetupScreen(onSubmit: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Chalk Message",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { if (name.isNotBlank()) onSubmit(name.trim()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun ConnectScreen(
    userId: String,
    inviteCode: String,
    onConnect: (String) -> Unit
) {
    var partnerCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your Invite Code", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = inviteCode,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Enter a friend's code:", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = partnerCode,
            onValueChange = { partnerCode = it.uppercase() },
            label = { Text("Friend's Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { if (partnerCode.isNotBlank()) onConnect(partnerCode) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }
    }
}
