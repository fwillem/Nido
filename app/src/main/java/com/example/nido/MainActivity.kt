package com.example.nido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nido.game.GameViewModel
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.screens.NidoApp
import androidx.lifecycle.viewmodel.compose.viewModel // CORRECT IMPORT
import com.example.nido.game.GameManager
import androidx.core.view.WindowCompat // 🚀
import androidx.core.view.WindowInsetsCompat // 🚀
import androidx.core.view.WindowInsetsControllerCompat // 🚀

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        /**
         * This whole section is about enabling immersive sticky mode :
         * description : https://developer.android.com/develop/ui/views/layout/immersive#sticky
         *
         */
        WindowCompat.setDecorFitsSystemWindows(window, false) // 🚀 Allow content behind system bars
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView) // 🚀
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE // 🚀
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars()) // 🚀 Hide system bars for full screen

        // Set the content of the activity
        setContent {
            NidoTheme {
                // Get the ViewModel *here*, in the Activity
                val viewModel: GameViewModel = viewModel()
                GameManager.initialize(viewModel)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pass the viewModel to NidoApp
                    NidoApp(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
