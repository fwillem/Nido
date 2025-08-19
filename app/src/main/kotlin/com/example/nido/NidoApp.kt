package com.example.nido

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.nido.data.SavedPlayer
import com.example.nido.events.AppDialogEvent
import com.example.nido.game.GameViewModel
import com.example.nido.ui.AppScreen
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.dialogs.BlueScreenOfDeathDialog
import com.example.nido.ui.dialogs.QuitGameDialog
import com.example.nido.ui.screens.LandingScreen
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.screens.ScoreScreen
import com.example.nido.ui.screens.SetupScreen
import com.example.nido.ui.screens.SplashScreen
import com.example.nido.utils.LocaleUtils
import com.example.nido.utils.copyDebugReport
import com.example.nido.utils.hardRestartApp
import java.util.UUID

/**
 * Hardcoded flag controlling BSOD behavior:
 *
 * - true  => "terrifying" BSOD UI + HARD RESTART the app when user confirms
 * - false => "cool" BSOD UI + CRASH the app when user confirms
 */
private const val BSOD_IS_TERRIFYING: Boolean = true

@Composable
fun NidoApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    initialRoute: String = AppScreen.Routes.SPLASH // allows entering the app at a specific screen
) {
    val gameManager = LocalGameManager.current
    val gameState by viewModel.gameState.collectAsState()

    // Current navigation route
    var currentRoute = rememberSaveable { mutableStateOf(initialRoute) }

    // Activity handle (used for hard restarts)
    val context = LocalContext.current
    val activity = context as? Activity

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentRoute.value) {
            AppScreen.Routes.SPLASH -> SplashScreen(
                onExit = { currentRoute.value = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding)
            )

            AppScreen.Routes.LANDING -> LandingScreen(
                onSetup = { currentRoute.value = AppScreen.Routes.SETUP },
                onGame = {
                    val players = viewModel.savedPlayers.value.map {
                        it.toPlayer(UUID.randomUUID().toString())
                    }
                    val pointLimit = viewModel.savedPointLimit.value
                    val doNotAutoPlayAI = viewModel.savedDebug.value.doNotAutoPlayerAI
                    gameManager.startNewGame(players, pointLimit, doNotAutoPlayAI)
                    currentRoute.value = AppScreen.Routes.GAME
                },
                onQuit = {
                    gameManager.setAppDialogEvent(AppDialogEvent.QuitApp)
                },
                modifier = modifier.padding(innerPadding)
            )

            AppScreen.Routes.SETUP -> SetupScreen(
                initialPlayers = viewModel.savedPlayers.value.map {
                    it.toPlayer(UUID.randomUUID().toString())
                },
                initialPointLimit = viewModel.savedPointLimit.value,
                debug = viewModel.savedDebug.value,
                onDone = { selectedPlayers, selectedPointLimit, debug ->

                    // handle language change (hard restart if changed)
                    val previousLang = viewModel.savedDebug.value.language
                    val newLang = debug.language

                    // persist settings
                    viewModel.savePlayers(selectedPlayers.map { SavedPlayer.fromPlayer(it) })
                    viewModel.savePointLimit(selectedPointLimit)
                    viewModel.saveDebug(debug)

                    if (newLang != previousLang && activity != null) {
                        LocaleUtils.saveLanguage(activity, newLang)
                        LocaleUtils.setAppLocaleAndHardRestart(activity, newLang)
                    } else {
                        currentRoute.value = AppScreen.Routes.LANDING
                    }
                },
                onCancel = { currentRoute.value = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding)
            )

            AppScreen.Routes.GAME -> MainScreen(
                onEndGame = { currentRoute.value = AppScreen.Routes.SCORE },
                onQuitGame = { currentRoute.value = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding),
                viewModel = viewModel,
                debug = viewModel.savedDebug.value
            )

            AppScreen.Routes.SCORE -> ScoreScreen(
                onContinue = { currentRoute.value = AppScreen.Routes.LANDING },
                onEndGame = { currentRoute.value = AppScreen.Routes.LANDING },
                modifier = modifier.padding(innerPadding)
            )
        }
    }

    // ── Centralized Dialog Observer ──
    val appEvent = gameState.appDialogEvent
    if (appEvent != null) {
        when (appEvent) {
            is AppDialogEvent.QuitApp -> QuitGameDialog(
                onConfirm = {
                    gameManager.clearAppDialogEvent()
                    activity?.finish()
                },
                onCancel = { gameManager.clearAppDialogEvent() }
            )

            is AppDialogEvent.BlueScreenOfDeath -> {
                val isTerrifying = BSOD_IS_TERRIFYING

                BlueScreenOfDeathDialog(
                    tag = appEvent.tag,
                    message = appEvent.message,
                    isTerrifying = isTerrifying,
                    onExit = {
                        gameManager.clearAppDialogEvent()
                        val act = activity
                        if (isTerrifying) {
                            if (act != null) hardRestartApp(act, forceLanding = true)
                            else error(appEvent.message())
                        } else {
                            error(appEvent.message())
                        }
                    },
                    onCopyReport = {
                        // User explicitly asked for a rich report → copy ONLY (no restart/crash)
                        activity?.let { act ->
                            copyDebugReport(
                                act,
                                tag = appEvent.tag,
                                message = appEvent.message(),
                                gameStateDump = gameState.toString() // or a compact dump you prefer
                            )
                        }
                    }
                )
            }
        }
    }

}
