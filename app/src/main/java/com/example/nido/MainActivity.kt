package com.example.nido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.example.nido.ui.theme.NidoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NidoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NidoApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}


