package com.example.nido.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.nido.game.GameViewModel // Import GameViewModel

@Composable
fun NidoApp(viewModel: GameViewModel, modifier: Modifier = Modifier) { // Receive viewModel
    var currentScreen by rememberSaveable { mutableStateOf("SetupScreen") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            "SetupScreen" -> SetupScreen(
                onGameStart = { selectedPlayers, selectedPointLimit ->
                    viewModel.gameManager.startNewGame(selectedPlayers, selectedPointLimit) // Use viewModel
                    currentScreen = "GameScreen"
                },
                modifier = modifier.padding(innerPadding)
            )

            "GameScreen" -> MainScreen(
                onEndGame = { currentScreen = "ScoreScreen" },
                modifier = modifier.padding(innerPadding),
                viewModel = viewModel // Pass viewModel to MainScreen
            )

            "ScoreScreen" -> ScoreScreen(
                onContinue = { currentScreen = "SetupScreen" },
                onEndGame = { currentScreen = "SetupScreen" },
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}