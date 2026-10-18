package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.game.model.GameMode
import com.example.ui.game.GameViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.match.MatchScreen
import com.example.ui.online.OnlineLobbyScreen
import com.example.ui.setup.SetupScreen
import com.example.ui.theme.DotBattleTheme
import com.example.ui.tutorial.HowToPlayScreen
import com.example.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DotBattleTheme {
                DotBattleApp()
            }
        }
    }
}

@Composable
fun DotBattleApp(gameViewModel: GameViewModel = viewModel()) {
    val navController = rememberNavController()
    val settings by gameViewModel.settings.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                onPlayOnline = { navController.navigate("online_lobby") },
                onPlayVsAi = { navController.navigate("setup_vs_ai") },
                onMultiAi = { navController.navigate("setup_multi_ai") },
                onHowToPlay = { navController.navigate("how_to_play") },
                onSettings = { navController.navigate("settings") }
            )
        }

        composable("setup_vs_ai") {
            SetupScreen(
                mode = GameMode.VS_AI,
                defaultBoardSize = settings.defaultBoardSize,
                defaultAiDifficulty = settings.defaultAiDifficulty,
                onBack = { navController.popBackStack() },
                onStartGame = { boardSize, players ->
                    gameViewModel.startNewGame(GameMode.VS_AI, boardSize, players)
                    navController.navigate("match")
                }
            )
        }

        composable("setup_multi_ai") {
            SetupScreen(
                mode = GameMode.MULTI_AI,
                defaultBoardSize = settings.defaultBoardSize,
                defaultAiDifficulty = settings.defaultAiDifficulty,
                onBack = { navController.popBackStack() },
                onStartGame = { boardSize, players ->
                    gameViewModel.startNewGame(GameMode.MULTI_AI, boardSize, players)
                    navController.navigate("match")
                }
            )
        }

        composable("online_lobby") {
            OnlineLobbyScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() },
                onStartOnlineGame = { boardSize, players ->
                    gameViewModel.startNewGame(GameMode.ONLINE, boardSize, players)
                    navController.navigate("match")
                }
            )
        }

        composable("match") {
            MatchScreen(
                viewModel = gameViewModel,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("how_to_play") {
            HowToPlayScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
