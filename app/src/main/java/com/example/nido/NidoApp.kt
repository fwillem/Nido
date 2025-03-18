package com.example.nido.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.nido.game.GameViewModel
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.screens.ScoreScreen
import com.example.nido.ui.screens.SetupScreen

@Composable
fun NidoApp(viewModel: GameViewModel, modifier: Modifier = Modifier) { // Receive viewModel

    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager

    var currentScreen by rememberSaveable { mutableStateOf("SetupScreen") }


    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            "SetupScreen" -> SetupScreen(
                onGameStart = { selectedPlayers, selectedPointLimit ->
                    gameManager.startNewGame(selectedPlayers, selectedPointLimit) // Use viewModel
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