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


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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