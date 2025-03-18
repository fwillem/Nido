package com.example.nido

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
import com.example.nido.game.GameManager
import com.example.nido.game.IGameManager
import com.example.nido.ui.LocalGameManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Set the content of the activity
        setContent {
            NidoTheme {
                val viewModel: GameViewModel = viewModel()

                // ✅ Use dependency injection for GameManager
                val gameManager: IGameManager = GameManager
                gameManager.initialize(viewModel)

                // ✅ Provide GameManager using CompositionLocalProvider
                CompositionLocalProvider(LocalGameManager provides gameManager) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NidoApp(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
