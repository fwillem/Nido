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
import androidx.compose.runtime.DisposableEffect
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
import com.example.nido.events.UiEventBridge
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent

class MainActivity : ComponentActivity() {


    override fun attachBaseContext(newBase: Context) {
        val lang = com.example.nido.utils.LocaleUtils.getSavedLanguage(newBase) ?: com.example.nido.utils.AppLanguage.ENGLISH.code

        val context = com.example.nido.utils.LocaleUtils.setLocale(newBase, lang)
        super.attachBaseContext(context)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        // We do not set the locale here, as it is already set in attachBaseContext
        super.onCreate(savedInstanceState)

        // Setup Game Mode (i.e. full screen, no system bars)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // In case of restart, we may want to jump directly to the landing page
        val forceLanding = intent.getBooleanExtra("force_landing", false)

        setContent {
            NidoTheme {
                val viewModel: GameViewModel = viewModel()
                val gameManager: IGameManager = getGameManagerInstance()
                gameManager.initialize(viewModel)

                CompositionLocalProvider(LocalGameManager provides gameManager) {
                    // Bridge: Non-Compose sources -> UI layer
                    DisposableEffect(Unit) {
                        UiEventBridge.setListener { e ->
                            when (e) {
                                is AppDialogEvent  -> gameManager.setAppDialogEvent(e)
                                is GameDialogEvent -> gameManager.setGameDialogEvent(e)
                            }
                        }
                        onDispose {
                            UiEventBridge.setListener(null)
                        }
                    }

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        var initialRoute = AppScreen.Routes.SPLASH
                        if (forceLanding) initialRoute = AppScreen.Routes.LANDING


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
