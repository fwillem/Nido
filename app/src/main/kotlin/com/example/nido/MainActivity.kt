package com.example.nido

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.example.nido.game.GameViewModel
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.screens.NidoApp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nido.game.IGameManager
import com.example.nido.game.getGameManagerInstance
import com.example.nido.ui.LocalGameManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.nido.ui.AppScreen
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*

class MainActivity : ComponentActivity() {

    // ATTENTION : ne rien lire/écrire dans DataStore ici pour la langue !
    override fun attachBaseContext(newBase: Context) {
        val lang = com.example.nido.utils.LocaleUtils.getSavedLanguage(newBase) ?: com.example.nido.utils.AppLanguage.ENGLISH.code
        TRACE(VERBOSE) { "BOOOO LocaleDebug attachBaseContext read lang=$lang" }

        android.util.Log.d("BOOOO LocaleDebug", "attachBaseContext read lang=$lang")
        val context = com.example.nido.utils.LocaleUtils.setLocale(newBase, lang)
        super.attachBaseContext(context)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        // NE PAS faire de setLocale ici : déjà fait via attachBaseContext !
        super.onCreate(savedInstanceState)

        // Setup système d'affichage et de navigation
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Récupère si besoin le paramètre pour forcer la LandingScreen
        val forceLanding = intent.getBooleanExtra("force_landing", false)

        setContent {
            NidoTheme {
                val viewModel: GameViewModel = viewModel()
                val gameManager: IGameManager = getGameManagerInstance()
                gameManager.initialize(viewModel)

                TRACE(VERBOSE) {"HUGEBUG ORIGIN ViewModel Number of Players ${viewModel.gameState.value.players.size}"}
                TRACE(VERBOSE) {"HUGEBUG ORIGIN gameManager Number of Players ${gameManager.gameState.value.players.size}"}
                TRACE(VERBOSE) { "HUGEBUG ORIGIN ViewModel gameState ref: ${System.identityHashCode(viewModel.gameState.value)}" }
                TRACE(VERBOSE) { "HUGEBUG ORIGIN GameManager gameState ref: ${System.identityHashCode(gameManager.gameState.value)}" }

                CompositionLocalProvider(LocalGameManager provides gameManager) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        var initialRoute = AppScreen.Routes.SPLASH
                       // if (forceLanding) initialRoute = AppScreen.Routes.LANDING


                        TRACE(VERBOSE) { "HUGEBUG ViewModel gameState ref: ${System.identityHashCode(viewModel.gameState.value)}, ${viewModel.gameState.value.players.size}" }
                        TRACE(VERBOSE) { "HUGEBUG GameManager gameState ref: ${System.identityHashCode(gameManager.gameState.value)}, ${gameManager.gameState.value.players.size}" }


                        NidoApp(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding),
                            initialRoute = initialRoute
                        )
                    }
                }
            }
        }
    }
}
