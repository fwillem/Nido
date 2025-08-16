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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.events.UiEventBridge
import com.example.nido.game.GameViewModel
import com.example.nido.game.IGameManager
import com.example.nido.game.getGameManagerInstance
import com.example.nido.ui.AppScreen
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.theme.NidoTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply saved locale as early as possible so all resources load with the right language
        val lang = com.example.nido.utils.LocaleUtils.getSavedLanguage(newBase)
            ?: com.example.nido.utils.AppLanguage.ENGLISH.code
        val context = com.example.nido.utils.LocaleUtils.setLocale(newBase, lang)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Locale already applied in attachBaseContext
        super.onCreate(savedInstanceState)

        // Fullscreen / edge-to-edge setup
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Optional: jump directly to landing when restarting the app with a flag
        val forceLanding = intent.getBooleanExtra("force_landing", false)

        setContent {
            NidoTheme {
                // ViewModel is a structural placeholder (per your architecture notes)
                val viewModel: GameViewModel = viewModel()

                // GameManager is the single source of truth for state/logic
                val gameManager: IGameManager = getGameManagerInstance()
                gameManager.initialize(viewModel)

                // Provide GameManager to the composition
                CompositionLocalProvider(LocalGameManager provides gameManager) {

                    // Bridge NON-Compose / NON-reducer UI events (TRACE, network, services) into the two dialog pipes.
                    // IMPORTANT: This does NOT carry GameEvent; gameplay must go through the reducer/dispatcher.
                    DisposableEffect(Unit) {
                        UiEventBridge.setListener { event ->
                            when (event) {
                                is AppDialogEvent  -> gameManager.setAppDialogEvent(event)   // Global dialogs (handled in NidoApp)
                                is GameDialogEvent -> gameManager.setGameDialogEvent(event)  // In-game dialogs (handled in MainScreen)
                                else -> Unit // Ignore unsupported payloads
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
