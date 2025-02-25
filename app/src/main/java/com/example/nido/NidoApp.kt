package com.example.nido

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.screens.ScoreScreen
import com.example.nido.ui.screens.SetupScreen
import com.example.nido.game.GameManager

@Composable
fun NidoApp(modifier: Modifier = Modifier) {
    var currentScreen by rememberSaveable { mutableStateOf("SetupScreen") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            "SetupScreen" -> SetupScreen(
                onGameStart = { selectedPlayers, selectedPointLimit ->
                    GameManager.startNewGame(selectedPlayers, selectedPointLimit)
                    currentScreen = "GameScreen"
                }
            )

            "GameScreen" -> MainScreen(
                onEndGame = { currentScreen = "ScoreScreen" },
                modifier = modifier)

            "ScoreScreen" -> ScoreScreen(   // ✅ No need to pass `players`
                onContinue = { currentScreen = "GameScreen" },
                onEndGame = { currentScreen = "SetupScreen" }
            )
        }
    }
}
