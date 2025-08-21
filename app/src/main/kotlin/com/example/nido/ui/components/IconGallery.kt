package com.example.nido.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.nido.ui.theme.NidoTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconGallery(
    icons: List<Pair<String, ImageVector>>,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val filtered = if (query.isBlank()) icons
    else icons.filter { it.first.contains(query, ignoreCase = true) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search icon") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(filtered, key = { it.first }) { (name, vector) ->
                ElevatedCard(
                    onClick = {
                        clipboard.setText(AnnotatedString("Icons.Filled.$name"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = name,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// Une liste d’icônes manuelle (ajoute-en d’autres selon ton besoin)
fun sampleIcons(): List<Pair<String, ImageVector>> = listOf(
    "Add" to Icons.Filled.Add,
    "Remove" to Icons.Filled.Remove,
    "Home" to Icons.Filled.Home,
    "Star" to Icons.Filled.Star,
    "Settings" to Icons.Filled.Settings,
    "Favorite" to Icons.Filled.Favorite,
    "Search" to Icons.Filled.Search,
    "Info" to Icons.Filled.Info,
    "Delete" to Icons.Filled.Delete,
    "ArrowBack" to Icons.Filled.ArrowBack,
    "ArrowForward" to Icons.Filled.ArrowForward,
    "Close" to Icons.Filled.Close,
)

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "Icon Gallery – Sample")
@Composable
private fun IconGalleryPreview() {
    NidoTheme {
        IconGallery(
            icons = sampleIcons(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
