package com.example.nido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.ui.theme.NidoTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp


enum class SortMode { FIFO, COLOR, VALUE }

enum class CardColor(val letter: Char) {
    PINK('p'),
    ORANGE('o'),
    BLUE('b'),
    RED('r'),
    GREEN('g'),
    MOCHA('m')
}

data class Card(
    @DrawableRes val cardImageId: Int,
    val color: CardColor,
    val value: Int
)

val backCoverCard = Card(
    cardImageId = R.drawable.back_cover,
    color = CardColor.MOCHA,
    value = 0
)

// Object storing all image resources at compile time
object CardResources {
    val images = mapOf(
        // PINK (p)
        "p_1" to R.drawable.nido_card_p_1,
        "p_2" to R.drawable.nido_card_p_2,
        "p_3" to R.drawable.nido_card_p_3,
        "p_4" to R.drawable.nido_card_p_4,
        "p_5" to R.drawable.nido_card_p_5,
        "p_6" to R.drawable.nido_card_p_6,
        "p_7" to R.drawable.nido_card_p_7,
        "p_8" to R.drawable.nido_card_p_8,
        "p_9" to R.drawable.nido_card_p_9,

        // ORANGE (o)
        "o_1" to R.drawable.nido_card_o_1,
        "o_2" to R.drawable.nido_card_o_2,
        "o_3" to R.drawable.nido_card_o_3,
        "o_4" to R.drawable.nido_card_o_4,
        "o_5" to R.drawable.nido_card_o_5,
        "o_6" to R.drawable.nido_card_o_6,
        "o_7" to R.drawable.nido_card_o_7,
        "o_8" to R.drawable.nido_card_o_8,
        "o_9" to R.drawable.nido_card_o_9,

        // BLUE (b)
        "b_1" to R.drawable.nido_card_b_1,
        "b_2" to R.drawable.nido_card_b_2,
        "b_3" to R.drawable.nido_card_b_3,
        "b_4" to R.drawable.nido_card_b_4,
        "b_5" to R.drawable.nido_card_b_5,
        "b_6" to R.drawable.nido_card_b_6,
        "b_7" to R.drawable.nido_card_b_7,
        "b_8" to R.drawable.nido_card_b_8,
        "b_9" to R.drawable.nido_card_b_9,

        // RED (r)
        "r_1" to R.drawable.nido_card_r_1,
        "r_2" to R.drawable.nido_card_r_2,
        "r_3" to R.drawable.nido_card_r_3,
        "r_4" to R.drawable.nido_card_r_4,
        "r_5" to R.drawable.nido_card_r_5,
        "r_6" to R.drawable.nido_card_r_6,
        "r_7" to R.drawable.nido_card_r_7,
        "r_8" to R.drawable.nido_card_r_8,
        "r_9" to R.drawable.nido_card_r_9,

        // GREEN (g)
        "g_1" to R.drawable.nido_card_g_1,
        "g_2" to R.drawable.nido_card_g_2,
        "g_3" to R.drawable.nido_card_g_3,
        "g_4" to R.drawable.nido_card_g_4,
        "g_5" to R.drawable.nido_card_g_5,
        "g_6" to R.drawable.nido_card_g_6,
        "g_7" to R.drawable.nido_card_g_7,
        "g_8" to R.drawable.nido_card_g_8,
        "g_9" to R.drawable.nido_card_g_9,

        // MOCHA (m)
        "m_1" to R.drawable.nido_card_m_1,
        "m_2" to R.drawable.nido_card_m_2,
        "m_3" to R.drawable.nido_card_m_3,
        "m_4" to R.drawable.nido_card_m_4,
        "m_5" to R.drawable.nido_card_m_5,
        "m_6" to R.drawable.nido_card_m_6,
        "m_7" to R.drawable.nido_card_m_7,
        "m_8" to R.drawable.nido_card_m_8,
        "m_9" to R.drawable.nido_card_m_9
    )

    fun getImage(color: CardColor, value: Int): Int {
        return images["${color.letter}_$value"]
            ?: throw IllegalStateException("Missing resource for card: ${color.letter}_$value")
    }
}


data class Hand(val cards: SnapshotStateList<Card> = mutableStateListOf()) {

    fun addCard(card: Card) {
        cards.add(card)
    }


    fun removeCard(card: Card): Boolean {
        return cards.remove(card)
    }

    // Improved `removeCard` function
    fun removeCard(index: Int = 0): Card? = cards.getOrNull(index)?.also { cards.removeAt(index) }


    fun clear() {
        cards.clear()
    }

    fun isEmpty(): Boolean = cards.isEmpty()
    fun count(): Int = cards.size


    override fun toString(): String = cards
        .joinToString(", ") { "${it.color.name} ${it.value}" }
        .ifEmpty { "The hand is empty" }
}

// Function to sort cards based on the selected sorting mode
/*
fun List<Card>.sortedByMode(mode: SortMode): List<Card> {
    return when (mode) {
        SortMode.FIFO -> this // No sorting, keep as is
        SortMode.COLOR -> this.sortedWith(compareBy({ it.color.ordinal }, { it.value }))
        SortMode.VALUE -> this.sortedBy { it.value }
    }
}
*/
fun List<Card>.sortedByMode(mode: SortMode): List<Card> {
    return when (mode) {
        SortMode.FIFO -> this // No sorting, keep as is
        SortMode.COLOR -> this // Will be handled by complex criterias
        SortMode.VALUE -> this.sortedByDescending { it.value } // Sort all cards by descending value
    }
}

fun List<Card>.sortedByComplexCriteria(): List<Card> {
    val groupedByColor = this.groupBy { it.color }

    val colorOrder = groupedByColor.entries.sortedByDescending { entry ->
        entry.value.joinToString("") { it.value.toString() }.toIntOrNull() ?: 0 // Handle potential parsing errors
    }.map { it.key }

    val sortedCards = mutableListOf<Card>()

    for (color in colorOrder) {
        val cardsOfColor = groupedByColor[color] ?: emptyList()
        sortedCards.addAll(cardsOfColor.sortedByDescending { it.value })
    }

    return sortedCards
}


@Composable
fun HandView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode,
    onDoubleClick: () -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    val sortedCards = remember(sortMode) {
        derivedStateOf {
            if (sortMode == SortMode.COLOR) {
                hand.cards.sortedByComplexCriteria()
            } else {
                hand.cards.sortedByMode(sortMode) // Use your existing sortedByMode for FIFO and VALUE
            }
        }
    }.value


    val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }

    LaunchedEffect(draggedIndex, dragOffsetX, sortMode) {
        if (sortMode == SortMode.FIFO && draggedIndex != null) {
            val rawIndexShift = (dragOffsetX / cardWidthPx).toInt()
            val potentialTargetIndex = draggedIndex!! + rawIndexShift

            // 🔹 Fix: Adjust for rightward movement
            targetIndex = when {
                dragOffsetX > 0 -> (potentialTargetIndex + 1).coerceIn(
                    0,
                    hand.cards.size
                ) // Adjust one step forward
                dragOffsetX < 0 -> potentialTargetIndex.coerceIn(
                    0,
                    hand.cards.size - 1
                )  // Keep normal behavior for leftward drag
                else -> potentialTargetIndex
            }
        } else {
            targetIndex = null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleClick() }) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            sortedCards.forEachIndexed { sortedIndex, card ->
                val actualIndex = hand.cards.indexOf(card)

                val showRedLine =
                    targetIndex != null && sortedIndex == targetIndex && sortMode == SortMode.FIFO

                if (showRedLine) {
                    Box(
                        modifier = Modifier
                            .height(cardHeight)
                            .width(6.dp)
                            .background(Color.Red)
                    )
                }

                Box(
                    modifier = Modifier
                        .zIndex(if (draggedIndex == actualIndex) 1f else 0f)
                        .graphicsLayer {
                            if (draggedIndex == actualIndex) {
                                alpha = 0.5f
                                translationX = dragOffsetX
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    if (sortMode == SortMode.FIFO) {
                                        draggedIndex = actualIndex
                                    //    println("Drag started on actualIndex: $draggedIndex, sortedIndex: $sortedIndex, card: ${card.color} ${card.value}")
                                    }
                                },

                                onDragEnd = {
                                    if (sortMode == SortMode.FIFO && draggedIndex != null && targetIndex != null) {
                                        val adjustedTarget = if (dragOffsetX > 0) {
                                            // 🔹 Fix: Adjust insertion back by -1 for rightward movement
                                            (targetIndex!! - 1).coerceIn(0, hand.cards.size - 1)
                                        } else {
                                            targetIndex!!
                                        }

                                      //  println("Drag ended. Moving from $draggedIndex to $adjustedTarget")
                                        hand.cards.moveItem(draggedIndex!!, adjustedTarget)
                                    }

                                    draggedIndex = null
                                    dragOffsetX = 0f
                                },


                                onDragCancel = {
                                 //   println("Drag cancelled.")
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                //    println("DragCancel: draggedIndex = $draggedIndex, targetIndex = $targetIndex, dragOffsetX = $dragOffsetX")
                                },
                                onDrag = { change, dragAmount ->
                                    if (sortMode == SortMode.FIFO && draggedIndex != null) {
                                        dragOffsetX += dragAmount.x
                                    }
                                }
                            )
                        }
                ) {
                    CardView(card, Modifier
                        .width(cardWidth)
                        .height(cardHeight))
                }
            }

            val showEndRedLine =
                targetIndex != null && targetIndex == hand.cards.size && sortMode == SortMode.FIFO
            if (showEndRedLine) {
                Box(
                    modifier = Modifier
                        .height(cardHeight)
                        .width(6.dp)
                        .background(Color.Red)
                )
            }
        }

    }
}

// Extension function to swap two elements in a mutable list
fun <T> MutableList<T>.swap(from: Int, to: Int) {
    if (from in indices && to in indices) {
        val temp = this[from]
        this[from] = this[to]
        this[to] = temp
    }
}


fun generateDeck(): MutableList<Card> {
    val deck = mutableListOf<Card>()
    for (color in CardColor.values()) {
        for (value in 1..9) {
            val cardImageId = CardResources.getImage(color, value)
            deck.add(Card(cardImageId, color, value))
        }
    }
    return deck
}

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

@Composable
fun NidoApp(modifier: Modifier = Modifier) {
    var deck by remember { mutableStateOf(generateDeck()) }
    val currentHand = remember { Hand() }
    var sortMode by remember { mutableStateOf(SortMode.FIFO) } // Track sorting mode

    // Function to toggle sorting mode
    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActionButtonsRow(
            mapOf(
                "New Hand" to {
                    currentHand.clear()
                    val cardsToTake = minOf(9, deck.size)
                    repeat(cardsToTake) {
                        currentHand.addCard(deck.removeAt(0))
                    }
                },
                "Remove Card" to {
                    currentHand.removeCard()?.let { deck.add(it) }
                },
                "Add Card" to {
                    deck.firstOrNull()?.let {
                        deck.removeAt(0)
                        currentHand.addCard(it)
                    }
                },
                "Empty" to {
                    repeat(currentHand.count()) {
                        currentHand.removeCard()?.let { deck.add(it) }
                    }
                },
                "Shuffle" to { deck.shuffle() },
                "Sort Mode: ${sortMode.name}" to toggleSortMode // Single function call!
            )
        )

        // Pass the same toggle function to both HandView calls


        HandView(
            currentHand,
            cardWidth = 80.dp,
            cardHeight = 160.dp,
            sortMode,
            onDoubleClick = toggleSortMode
        )

    }
}


@Composable
fun CardView(
    card: Card, modifier: Modifier = Modifier
        .width(140.dp)
        .height(230.dp)
) {
    Column {
        Box(
            modifier = modifier
        ) {
            Image(
                painter = painterResource(id = card.cardImageId),
                contentDescription = "Card Number ${card.value}, Color ${card.color.name}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ActionButtonsRow(actions: Map<String, () -> Unit>) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        actions.forEach { (label, action) ->
            Button(onClick = action) { Text(label) }
        }
    }
}

fun <T> MutableList<T>.moveItem(fromIndex: Int, toIndex: Int) {
    if (fromIndex in indices && toIndex in indices) {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}


@Preview(
    name = "Landscape Preview",
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
fun GreetingPreview() {
    NidoTheme {
        NidoApp()
    }
}
