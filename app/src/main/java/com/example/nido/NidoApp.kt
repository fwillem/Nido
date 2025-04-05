package com.example.nido.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.nido.game.GameViewModel
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.AppScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState // Potentially needed for explicit typing
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.screens.SetupScreen
import com.example.nido.ui.screens.ScoreScreen



@Composable
fun NidoApp(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val gameManager = LocalGameManager.current

    // Store the String route, use the constant for the initial value
    var currentRoute by rememberSaveable { mutableStateOf(AppScreen.Routes.SETUP) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Navigate based on the route string constant
        when (currentRoute) {
            AppScreen.Routes.SETUP -> SetupScreen(
                onGameStart = { selectedPlayers, selectedPointLimit ->
                    gameManager.startNewGame(selectedPlayers, selectedPointLimit)
                    currentRoute = AppScreen.Routes.GAME // Navigate using constant
                },
                modifier = modifier.padding(innerPadding)
            )
            AppScreen.Routes.GAME -> MainScreen(
                onEndGame = { currentRoute = AppScreen.Routes.SCORE }, // Navigate using constant
                modifier = modifier.padding(innerPadding),
                viewModel = viewModel
            )
            AppScreen.Routes.SCORE -> ScoreScreen(
                onContinue = { currentRoute = AppScreen.Routes.SETUP }, // Navigate using constant
                onEndGame = { currentRoute = AppScreen.Routes.SETUP }, // Navigate using constant
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}