package com.example.nido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.ui.theme.NidoTheme
import kotlin.text.get




/***************************************************************************************************************************
 *
 */

package com.example.nido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.screens.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NidoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}


/***************************************************************************************************************************
 *
 */
enum class SortMode { FIFO, COLOR, VALUE }

const val CARD_WIDTH = 80
const val CARD_HEIGHT = 160
const val HAND_SIZE = 9
fun List<Card>.sortedByMode(mode: SortMode): List<Card> {
    return when (mode) {
        SortMode.FIFO -> this
        SortMode.COLOR -> this
        SortMode.VALUE -> this.sortedByDescending { it.value }
    }
}

fun List<Card>.sortedByComplexCriteria(): List<Card> {
    val groupedByColor = this.groupBy { it.color }
    val colorOrder = groupedByColor.entries.sortedByDescending { entry ->
        entry.value.joinToString("") { it.value.toString() }.toIntOrNull() ?: 0
    }.map { it.key }
    val sortedCards = mutableListOf<Card>()
    for (color in colorOrder) {
        val cardsOfColor = groupedByColor[color] ?: emptyList()
        sortedCards.addAll(cardsOfColor.sortedByDescending { it.value })
    }
    return sortedCards
}

fun <T> MutableList<T>.swap(from: Int, to: Int) {
    if (from in indices && to in indices) {
        val temp = this[from]
        this[from] = this[to]
        this[to] = temp
    }
}




fun <T> MutableList<T>.moveItem(fromIndex: Int, toIndex: Int) {
    if (fromIndex in indices && toIndex in indices) {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}
