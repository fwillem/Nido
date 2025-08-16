package com.example.nido.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.nido.data.SavedPlayer
import com.example.nido.events.DialogEvent
import com.example.nido.game.GameViewModel
import com.example.nido.ui.AppScreen
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.dialogs.BlueScreenOfDeathDialog
import com.example.nido.ui.dialogs.CardSelectionDialog
import com.example.nido.ui.dialogs.GameOverDialog
import com.example.nido.ui.dialogs.QuitGameDialog
import com.example.nido.ui.dialogs.RoundOverDialog
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import java.util.UUID

@Composable
fun NidoApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    initialRoute: String = AppScreen.Routes.SPLASH // We may want to enter the app in a specific screen
) {
    val gameManager = LocalGameManager.current

    val gameState by viewModel.gameState.collectAsState()

    // Utilise initialRoute comme route de départ
    var currentRoute by rememberSaveable { mutableStateOf(initialRoute) }
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentRoute) {
            AppScreen.Routes.SPLASH -> SplashScreen(
                onExit = { currentRoute = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding)
            )
            AppScreen.Routes.LANDING -> LandingScreen(
                onSetup = { currentRoute = AppScreen.Routes.SETUP },
                onGame = {
                    val players = viewModel.savedPlayers.value.map { it.toPlayer(UUID.randomUUID().toString()) }
                    val pointLimit = viewModel.savedPointLimit.value
                    val doNotAutoPlayAI = viewModel.savedDebug.value.doNotAutoPlayerAI
                    gameManager.startNewGame(players, pointLimit, doNotAutoPlayAI)
                    currentRoute = AppScreen.Routes.GAME
                },
                onQuit = {
                    gameManager.setDialogEvent(DialogEvent.QuitGame)

                },
                modifier = modifier.padding(innerPadding)
            )
            AppScreen.Routes.SETUP -> SetupScreen(
                initialPlayers = viewModel.savedPlayers.value.map { it.toPlayer(UUID.randomUUID().toString()) },
                initialPointLimit = viewModel.savedPointLimit.value,
                debug = viewModel.savedDebug.value,
                onDone = { selectedPlayers, selectedPointLimit, debug ->
                    // Language change management
                    val previousLang = viewModel.savedDebug.value.language
                    val newLang = debug.language

                    // Settings are saved in the ViewModel
                    viewModel.savePlayers(selectedPlayers.map { SavedPlayer.fromPlayer(it) })
                    viewModel.savePointLimit(selectedPointLimit)
                    viewModel.saveDebug(debug)



                    // If the language has changed, save it and restart the app (note the language is saved both in DataStore and SharedPreferences)
                    if (newLang != previousLang && activity != null) {
                        com.example.nido.utils.LocaleUtils.saveLanguage(activity, newLang)
                        com.example.nido.utils.LocaleUtils.setAppLocaleAndRestart(activity, newLang)
                    } else {
                        currentRoute = AppScreen.Routes.LANDING
                    }
                },

                onCancel = { currentRoute = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding)
            )
            AppScreen.Routes.GAME -> MainScreen(
                onEndGame = { currentRoute = AppScreen.Routes.SCORE },
                onQuitGame = { currentRoute = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding),
                viewModel = viewModel,
                debug = viewModel.savedDebug.value
            )
            AppScreen.Routes.SCORE -> ScoreScreen(
                onContinue = { currentRoute = AppScreen.Routes.LANDING },
                onEndGame = { currentRoute = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding)
            )
        }
    }
    // ── Centralized Dialog Observer ──

    if (gameState.dialogEvent != null) {
        when (val event = gameState.dialogEvent) {

            is DialogEvent.QuitGame -> QuitGameDialog(onConfirm = { activity?.finish()}, onCancel = {})
            is DialogEvent.BlueScreenOfDeath -> BlueScreenOfDeathDialog(
                level = event.level,
                tag = event.tag,
                message = event.message,
                onExit = {  }
            )
            else -> TRACE(FATAL) { "Unknown or unexpected event : ${gameState.dialogEvent}" }
        }
    }


}
