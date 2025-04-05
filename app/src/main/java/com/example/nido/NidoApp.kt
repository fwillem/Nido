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
import com.example.nido.ui.AppScreenSaver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState // Potentially needed for explicit typing
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.screens.SetupScreen
import com.example.nido.ui.screens.ScoreScreen

@Composable
fun NidoApp(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val gameManager = LocalGameManager.current

    // Use rememberSaveable with the AppScreenSaver. No need for mutableStateOf here.
  //  var currentScreen: AppScreen by rememberSaveable(saver = AppScreenSaver) { AppScreen.Setup }

    var currentScreen: AppScreen by rememberSaveable(saver = AppScreenSaver) {
        mutableStateOf<AppScreen>(AppScreen.Setup) // Add <AppScreen> here
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            is AppScreen.Setup -> SetupScreen(
                onGameStart = { selectedPlayers, selectedPointLimit ->
                    gameManager.startNewGame(selectedPlayers, selectedPointLimit)
                    currentScreen = AppScreen.Game
                },
                modifier = modifier.padding(innerPadding)
            )
            is AppScreen.Game -> MainScreen(
                onEndGame = { currentScreen = AppScreen.Score },
                modifier = modifier.padding(innerPadding),
                viewModel = viewModel
            )
            is AppScreen.Score -> ScoreScreen(
                onContinue = { currentScreen = AppScreen.Setup },
                onEndGame = { currentScreen = AppScreen.Setup },
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}
