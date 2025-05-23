package com.example.nido.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.nido.data.SavedPlayer
import com.example.nido.game.GameViewModel
import com.example.nido.ui.AppScreen
import com.example.nido.ui.LocalGameManager
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import java.util.UUID


@Composable
fun NidoApp(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val gameManager = LocalGameManager.current

    // Store the String route, use the constant for the initial value
    var currentRoute by rememberSaveable { mutableStateOf(AppScreen.Routes.SPLASH) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Navigate based on the route string constant
        when (currentRoute) {
            AppScreen.Routes.SPLASH -> SplashScreen(
                onTimeout = { currentRoute = AppScreen.Routes.SETUP }, // Navigate using constant
                modifier = modifier.padding(innerPadding)
            )
            AppScreen.Routes.LANDING -> LandingScreen(
                onSetup = { currentRoute = AppScreen.Routes.SETUP }, // Navigate using constant
                onGame = { currentRoute = AppScreen.Routes.GAME }, // Navigate using constant
                modifier = modifier.padding(innerPadding)
            )
            AppScreen.Routes.SETUP -> SetupScreen(
                initialPlayers = viewModel.savedPlayers.value.map { it.toPlayer(UUID.randomUUID().toString()) },
                initialPointLimit = viewModel.savedPointLimit.value,
                onGameStart = { selectedPlayers, selectedPointLimit ->

                    //  Save user preferences before starting the game
                    viewModel.savePlayers(selectedPlayers.map { SavedPlayer.fromPlayer(it) })
                    viewModel.savePointLimit(selectedPointLimit)
                    TRACE(INFO) { "Saved SetupScreen preferences: players = $selectedPlayers, pointLimit = $selectedPointLimit" }

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