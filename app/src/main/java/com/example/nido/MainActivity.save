package com.example.nido

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.nido.ui.theme.NidoTheme
import kotlin.times

enum class SortMode { FIFO, COLOR, VALUE }

enum class CardColor(val letter: Char) {
    PINK('p'),
    ORANGE('o'),
    BLUE('b'),
    RED('r'),
    GREEN('g'),
    MOCHA('m')
}

const val CARD_WIDTH = 80
const val CARD_HEIGHT = 160
const val HAND_SIZE = 9

data class Card(
    @DrawableRes val cardImageId: Int,
    val color: CardColor,
    val isPartOfCompositions: Array<Boolean> = Array(HAND_SIZE) { false },
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

class Combination(val cards: MutableList<Card> = mutableListOf()) {
    fun addCard(card: Card) {
        this.cards.add(card)
    }

    // Compute the value by sorting digits in descending order and forming a number
    val value: Int
        get() = cards
            .map { it.value }
            .sortedDescending()
            .joinToString("")
            .toInt()
}

fun calculateExpectedCombinations(hand: Hand): Int {
    var totalCombinations = 0

    // Group by Color
    val colorGroups = hand.cards.groupBy { it.color }
    for ((_, group) in colorGroups) {
        if (group.size > 1) {
            totalCombinations += (1 shl group.size) - 1 - group.size
        }
    }

    // Group by Value
    val valueGroups = hand.cards.groupBy { it.value }
    for ((_, group) in valueGroups) {
        if (group.size > 1) {
            totalCombinations += (1 shl group.size) - 1 - group.size
        }
    }

    // Add individual cards
    totalCombinations += hand.cards.size

    return totalCombinations
}

data class Hand(
    val cards: SnapshotStateList<Card> = mutableStateListOf(),
    val combinations: SnapshotStateList<Combination> = mutableStateListOf()
) {
    fun addCard(card: Card) {
        cards.add(card)
        updateCombinations()
    }

    fun findValidCombinations(cards: List<Card>): List<Combination> {
        val validCombinations = mutableListOf<Combination>()

        // Group by Color & Sort
        val colorGroups = cards.groupBy { it.color }.mapValues { it.value.sortedByDescending { card -> card.value } }

        // Group by Value & Sort
        val valueGroups = cards.groupBy { it.value }.mapValues { it.value.sortedByDescending { card -> card.color.ordinal } }

        val mainCombinations = mutableListOf<Combination>()

        colorGroups.values.forEach { group ->
            if (group.size >= 2) {
                mainCombinations.add(Combination(group.toMutableList()))
            }
        }
        valueGroups.values.forEach { group ->
            if (group.size >= 2) {
                mainCombinations.add(Combination(group.toMutableList()))
            }
        }

        validCombinations.addAll(mainCombinations)

        for (combination in mainCombinations) {
            val subsetCombinations = generateAllSubcombinations(combination.cards)
            validCombinations.addAll(subsetCombinations)
        }

        for (group in colorGroups.values) {
            if (group.size >= 2) {
                validCombinations.addAll(generateAllSubcombinations(group))
            }
        }
        for (group in valueGroups.values) {
            if (group.size >= 2) {
                validCombinations.addAll(generateAllSubcombinations(group))
            }
        }

        cards.forEach { validCombinations.add(Combination(mutableListOf(it))) }

        return validCombinations.distinctBy { it.cards.toSet() }
            .sortedByDescending { it.value }
    }

    fun generateAllSubcombinations(cards: List<Card>): List<Combination> {
        val subsets = mutableListOf<Combination>()
        val size = cards.size

        for (subsetSize in 2..size) {
            val indices = (0 until size).toList()
            val combinations = indices.combinations(subsetSize)
            for (combinationIndices in combinations) {
                val subset = combinationIndices.map { cards[it] }
                subsets.add(Combination(subset.toMutableList()))
            }
        }

        return subsets
    }

    fun <T> List<T>.combinations(k: Int): List<List<T>> {
        if (k > size) return emptyList()
        if (k == size) return listOf(this)
        if (k == 1) return map { listOf(it) }

        val result = mutableListOf<List<T>>()
        for (i in indices) {
            val elem = this[i]
            val remaining = subList(i + 1, size)
            val subCombinations = remaining.combinations(k - 1)
            for (subComb in subCombinations) {
                result.add(listOf(elem) + subComb)
            }
        }
        return result
    }

    fun removeCard(card: Card): Boolean {
        val removed = cards.remove(card)
        if (removed) updateCombinations()
        return removed
    }

    fun removeCard(index: Int = 0): Card? = cards.getOrNull(index)?.also {
        cards.removeAt(index)
        updateCombinations()
    }

    fun removeCombination(combination: Combination): Boolean {
        if (!combination.cards.all { it in cards }) return false

        combination.cards.forEach { cards.remove(it) }
        updateCombinations()
        return true
    }

    fun clear() {
        cards.clear()
        combinations.clear()
    }

    fun isEmpty(): Boolean = cards.isEmpty()
    fun count(): Int = cards.size

    fun updateCombinations() {
        combinations.clear()
        val newCombinations = findValidCombinations(cards)
        combinations.addAll(newCombinations)
    }

    override fun toString(): String = cards
        .joinToString(", ") { "${it.color.name} ${it.value}" }
        .ifEmpty { "The hand is empty" }
}

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

@Composable
fun DiscardPileView(discardPile: List<Card>, cardWidth: Dp, cardHeight: Dp) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .width(cardWidth * 3)
            .height(cardHeight)
            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (discardPile.isEmpty()) {
            Text(
                text = "DISCARD",
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier.graphicsLayer(rotationZ = -45f)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val maxCardsToShow = 5
                val overlapFraction = 0.67f
                val displayedCards = discardPile.takeLast(maxCardsToShow)
                displayedCards.forEachIndexed { index, card ->
                    val cardWidthPx = with(density) { cardWidth.toPx() }
                    val offsetX = (-index * (cardWidthPx * overlapFraction))
                    Box(
                        modifier = Modifier
                            .zIndex(index.toFloat())
                            .graphicsLayer(translationX = offsetX)
                    ) {
                        CardView(
                            card = card,
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CombinationsView(
    combinations: List<Combination>,
    cardWidth: Dp,
    cardHeight: Dp
) {
    if (combinations.isEmpty()) {
        Text("No combinations available", color = Color.Red)
        return
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalArrangement = Arrangement.Center
    ) {
        items(combinations.size) { index ->
            val combination = combinations[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Row {
                    combination.cards.forEach { card ->
                        CardView(
                            card,
                            Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                        )
                    }
                }
                Text("${combination.value}", fontSize = 16.sp, color = Color.Black)
            }
        }
    }
}

fun <T> MutableList<T>.swap(from: Int, to: Int) {
    if (from in indices && to in indices) {
        val temp = this[from]
        this[from] = this[to]
        this[to] = temp
    }
}

fun generateTestHand1(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.BLUE, 1), CardColor.BLUE, value = 1))
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 9), CardColor.MOCHA, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 2), CardColor.MOCHA, value = 2))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 5), CardColor.GREEN, value = 5))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 8), CardColor.GREEN, value = 8))
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 1), CardColor.MOCHA, value = 1))
    }
}

fun generateTestHand2(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 1), CardColor.MOCHA, value = 1))
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 9), CardColor.MOCHA, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 2), CardColor.MOCHA, value = 2))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 5), CardColor.GREEN, value = 5))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 8), CardColor.GREEN, value = 8))
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.RED, 1), CardColor.RED, value = 1))
    }
}

fun generateTestHand3(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.RED, 8), CardColor.RED, value = 8))
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.RED, 6), CardColor.RED, value = 6))
        addCard(Card(CardResources.getImage(CardColor.RED, 5), CardColor.RED, value = 5))
        addCard(Card(CardResources.getImage(CardColor.RED, 4), CardColor.RED, value = 4))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 2), CardColor.BLUE, value = 2))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 2), CardColor.MOCHA, value = 2))
    }
}

fun generateTestHand4(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 8), CardColor.BLUE, value = 8))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 7), CardColor.GREEN, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 6), CardColor.MOCHA, value = 6))
        addCard(Card(CardResources.getImage(CardColor.PINK, 5), CardColor.PINK, value = 5))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 4), CardColor.ORANGE, value = 4))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 3), CardColor.BLUE, value = 3))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 2), CardColor.GREEN, value = 2))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 1), CardColor.MOCHA, value = 1))
    }
}

fun generateTestHand5(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 9), CardColor.MOCHA, value = 9))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 8), CardColor.GREEN, value = 8))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 7), CardColor.ORANGE, value = 7))
        addCard(Card(CardResources.getImage(CardColor.PINK, 7), CardColor.PINK, value = 7))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 5), CardColor.BLUE, value = 5))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 4), CardColor.MOCHA, value = 4))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 2), CardColor.ORANGE, value = 2))
    }
}

fun generateTestHand6(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 7), CardColor.BLUE, value = 7))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 7), CardColor.GREEN, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 7), CardColor.MOCHA, value = 7))
        addCard(Card(CardResources.getImage(CardColor.PINK, 3), CardColor.PINK, value = 3))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 3), CardColor.ORANGE, value = 3))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 9), CardColor.BLUE, value = 9))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 1), CardColor.GREEN, value = 1))
    }
}

fun generateDeck(shuffle : Boolean = false): MutableList<Card> {
    val deck = mutableListOf<Card>()
    for (color in CardColor.values()) {
        for (value in 1..HAND_SIZE) {
            val cardImageId = CardResources.getImage(color, value)
            deck.add(Card(cardImageId, color, value = value))
        }
    }
    if (shuffle)
        deck.shuffle()
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

val testVectors = listOf(
    ::generateTestHand1,
    ::generateTestHand2,
    ::generateTestHand3,
    ::generateTestHand4,
    ::generateTestHand5,
    ::generateTestHand6
)

@Composable
fun PlayerRow(playerCounts: List<Int>, currentPlayerIndex: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        playerCounts.forEachIndexed { index, count ->
            if (index == currentPlayerIndex) {
                Text("🧑 You: $count cards", fontSize = 16.sp, color = Color.Yellow)
            } else {
                Text("Player ${index + 1}: $count cards", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun NidoApp(modifier: Modifier = Modifier) {
    var deck by remember { mutableStateOf(generateDeck(shuffle = true)) }
    val currentHand = remember { Hand() }
    val playmat = remember { mutableStateListOf<Card>() }
    val discardPile = remember { mutableStateListOf<Card>() }
    val playerCounts = remember { mutableStateListOf(9, 9, 9, 9) }

    var sortMode by remember { mutableStateOf(SortMode.FIFO) }
    var testVectorIndex by remember { mutableStateOf(0) }

    val switchTestVector: () -> Unit = {
        testVectorIndex = (testVectorIndex + 1) % testVectors.size
        currentHand.clear()
        testVectors[testVectorIndex]().cards.forEach { currentHand.addCard(it) }
    }

    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }

    val drawNewHand: () -> Unit = {
        currentHand.clear()
        playmat.clear()
        discardPile.clear()
        val cardsToTake = minOf(HAND_SIZE, deck.size)
        repeat(cardsToTake) { currentHand.addCard(deck.removeAt(0)) }
        if (deck.isEmpty()) deck = generateDeck(shuffle = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF006400)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            ActionButtonsRow(
                mapOf(
                    "New Hand" to drawNewHand,
                    "Remove Card" to { currentHand.removeCard()?.let { deck.add(it) } },
                    "Add Card" to { deck.firstOrNull()?.let { deck.removeAt(0); currentHand.addCard(it) } },
                    "Cycle Test Vector" to switchTestVector,
                    "Shuffle" to { deck.shuffle() },
                    "Sort Mode: ${sortMode.name}" to toggleSortMode,
                    "Test Fill" to {
                        drawNewHand()
                        playmat.clear()
                        repeat(3) { deck.firstOrNull()?.let { deck.removeAt(0); playmat.add(it) } }
                        discardPile.clear()
                        repeat(9) { deck.firstOrNull()?.let { deck.removeAt(0); discardPile.add(it) } }
                    }
                )
            )
        }

        // Player Information Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF004000)),
            contentAlignment = Alignment.Center
        ) {
            PlayerRow(playerCounts = playerCounts, currentPlayerIndex = 1)
        }

        // Middle Section: MatView (Playmat + Discard Pile)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF228B22)),
            contentAlignment = Alignment.Center
        ) {
            // matView(playmat, discardPile)
        }

        // Bottom Section: HandView (Player's Hand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF006400)),
            contentAlignment = Alignment.Center
        ) {
            HandView(
                hand = currentHand,
                cardWidth = CARD_WIDTH.dp,
                cardHeight = CARD_HEIGHT.dp,
                sortMode = sortMode,
                onDoubleClick = toggleSortMode
            )
        }
    }
}

@Composable
private fun matView(
    playmat: SnapshotStateList<Card>,
    discardPile: SnapshotStateList<Card>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Playmat Section (75% Width)
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(Color(0xFF228B22), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                LazyRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(playmat.size) { index ->
                        CardView(
                            card = playmat[index],
                            modifier = Modifier
                                .width(140.dp)
                                .height(230.dp)
                        )
                    }
                }
            }
            // Discard Pile Section (25% Width)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Gray, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                DiscardPileView(
                    discardPile = discardPile,
                    cardWidth = 80.dp,
                    cardHeight = 120.dp
                )
            }
        }
    }
}


@Composable
fun HandViewDebug(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode, // Not used here, but kept for prototype compatibility
    onDoubleClick: () -> Unit
) {
    // The outer Box fills the full width (since NidoApp places HandView inside a Box that fills width)
    // and centers its content vertically.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.CenterVertically)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner Box sizes itself only to the content (the Row)
        Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
            // Row with wrapContentWidth() ensures its width is exactly the sum of its children
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp) // No extra spacing
            ) {
                // For each card in the hand, display a magenta box
                hand.cards.forEach {
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .background(Color.Magenta)
                    ) {

                    }
                }
            }
        }
    }
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
            if (sortMode == SortMode.COLOR)
                hand.cards.sortedByComplexCriteria()
            else
                hand.cards.sortedByMode(sortMode)
        }
    }.value

    val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }

    LaunchedEffect(draggedIndex, dragOffsetX, sortMode) {
        if (sortMode == SortMode.FIFO && draggedIndex != null) {
            val rawIndexShift = (dragOffsetX / cardWidthPx).toInt()
            val potentialTargetIndex = draggedIndex!! + rawIndexShift
            targetIndex = when {
                dragOffsetX > 0 -> (potentialTargetIndex + 1).coerceIn(0, hand.cards.size)
                dragOffsetX < 0 -> potentialTargetIndex.coerceIn(0, hand.cards.size - 1)
                else -> potentialTargetIndex
            }
        } else {
            targetIndex = null
        }
    }

    // Instead of filling the entire available space, we only wrap the content.
    Box(
        modifier = Modifier
            .wrapContentSize()   // Key: size only as needed by its children
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(), // Row's width equals the sum of its children
            horizontalArrangement = Arrangement.spacedBy(0.dp) // No extra spacing between items
        ) {
            sortedCards.forEachIndexed { sortedIndex, card ->
                if (targetIndex != null &&
                    sortedIndex == targetIndex &&
                    sortMode == SortMode.FIFO
                ) {
                    // Optional insertion marker (red line)
                    Box(
                        modifier = Modifier
                            .height(cardHeight)
                            .width(6.dp)
                            .background(Color.Red)
                    )
                }
                val actualIndex = hand.cards.indexOf(card)
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
                                    }
                                },
                                onDragEnd = {
                                    if (sortMode == SortMode.FIFO &&
                                        draggedIndex != null &&
                                        targetIndex != null
                                    ) {
                                        val adjustedTarget = if (dragOffsetX > 0)
                                            (targetIndex!! - 1).coerceIn(0, hand.cards.size - 1)
                                        else targetIndex!!
                                        hand.cards.moveItem(draggedIndex!!, adjustedTarget)
                                    }
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                },
                                onDrag = { _, dragAmount ->
                                    if (sortMode == SortMode.FIFO && draggedIndex != null) {
                                        dragOffsetX += dragAmount.x
                                    }
                                }
                            )
                        }
                ) {
                    CardView(
                        card,
                        Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                    )
                }
            }
            if (targetIndex != null &&
                targetIndex == hand.cards.size &&
                sortMode == SortMode.FIFO
            ) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        actions.forEach { (label, action) ->
            Button(
                onClick = action,
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = 2.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    label,
                    fontSize = 8.sp,
                    lineHeight = 8.sp
                )
            }
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
