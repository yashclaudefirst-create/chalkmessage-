package com.example.chalkmessage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chalkmessage.ui.screen.DrawingScreen
import com.example.chalkmessage.ui.screen.HistoryScreen
import com.example.chalkmessage.ui.screen.OnboardingScreen
import com.example.chalkmessage.ui.theme.ChalkMessageTheme
import com.example.chalkmessage.ui.viewmodel.DrawingViewModel
import com.example.chalkmessage.ui.viewmodel.HistoryViewModel
import com.example.chalkmessage.ui.viewmodel.OnboardingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ChalkMessageApp

        setContent {
            ChalkMessageTheme {
                ChalkApp(app)
            }
        }
    }
}

@Composable
fun ChalkApp(app: ChalkMessageApp) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            val viewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(app.userPrefs, app.firebaseRepo)
            )
            OnboardingScreen(
                viewModel = viewModel,
                onNavigateToDrawing = {
                    navController.navigate("drawing") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("drawing") {
            val viewModel: DrawingViewModel = viewModel(
                factory = DrawingViewModel.Factory(app.repository)
            )
            DrawingScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToOnboarding = { navController.navigate("onboarding") }
            )
        }

        composable("history") {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.Factory(app.repository)
            )
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
