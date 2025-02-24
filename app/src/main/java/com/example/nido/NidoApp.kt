package com.example.nido

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.nido.data.model.Player
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.screens.ScoreScreen
import com.example.nido.ui.screens.SetupScreen
import com.example.nido.utils.Constants

@Composable
fun NidoApp(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf("SetupScreen") }
    var players by remember { mutableStateOf<List<Player>>(emptyList()) }
    var pointLimit by remember { mutableStateOf(Constants.GAME_DEFAULT_POINT_LIMIT) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            "SetupScreen" -> SetupScreen(
                onGameStart = { selectedPlayers, selectedPointLimit ->
                    players = selectedPlayers
                    pointLimit = selectedPointLimit
                    currentScreen = "GameScreen"
                }
            )

            "GameScreen" -> MainScreen(modifier = modifier)

            "ScoreScreen" -> ScoreScreen(
                players = players,   // ✅ Pass `players` correctly
                onContinue = { currentScreen = "GameScreen" },
                onEndGame = { currentScreen = "SetupScreen" }
            )
        }
    }
}
