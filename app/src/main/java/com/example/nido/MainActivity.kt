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
import androidx.compose.foundation.lazy.LazyRow
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
            .map { it.value }  // Extract values
            .sortedDescending() // Sort from biggest to smallest
            .joinToString("") // Convert to a string of digits
            .toInt() // Convert to an integer
}

fun calculateExpectedCombinations(hand: Hand): Int {
    var totalCombinations = 0

    // **Group by Color** (Pour les séquences de cartes de même couleur)
    val colorGroups = hand.cards.groupBy { it.color }
    for ((_, group) in colorGroups) {
        if (group.size > 1) {
            totalCombinations += (1 shl group.size) - 1 - group.size // 2^N - 1 - N
        }
    }

    // **Group by Value** (Pour les groupes de cartes ayant la même valeur)
    val valueGroups = hand.cards.groupBy { it.value }
    for ((_, group) in valueGroups) {
        if (group.size > 1) {
            totalCombinations += (1 shl group.size) - 1 - group.size // 2^N - 1 - N
        }
    }

    // **Add individual cards** (Chaque carte est une combinaison unique)
    totalCombinations += hand.cards.size

    return totalCombinations
}


data class Hand(
    val cards: SnapshotStateList<Card> = mutableStateListOf(),
    val combinations: SnapshotStateList<Combination> = mutableStateListOf() // 🔹 Make observable
) {
    fun addCard(card: Card) {
        cards.add(card)
        updateCombinations() // Automatically detect new valid combinations
    }

/*
    fun findValidCombinations(cards: List<Card>): List<Combination> {
        val validCombinations = mutableListOf<Combination>()

        if (cards.isEmpty()) return validCombinations

        // 🔹 Step 1: Find color-based sequences
        val colorGroups = cards.groupBy { it.color }
        colorGroups.values.forEach { group ->
            val sortedGroup = group.sortedByDescending { it.value }

            // Add all contiguous subsequences
            for (i in sortedGroup.indices) {
                for (j in i until sortedGroup.size) {
                    validCombinations.add(Combination(sortedGroup.subList(i, j + 1).toMutableList()))
                }
            }
        }

        // 🔹 Step 2: Find same-value sets
        val valueGroups = cards.groupBy { it.value }
        valueGroups.values.forEach { group ->
            if (group.size > 1) { // Only if there are at least 2 cards with the same value
                validCombinations.add(Combination(group.toMutableList()))
            }
        }

        // 🔹 Step 3: Include each card as a single combination
        cards.forEach { card ->
            validCombinations.add(Combination(mutableListOf(card)))
        }

        // 🔹 Step 4: Remove duplicate combinations (based on unique card sets)
        val uniqueCombinations = validCombinations.distinctBy {
            it.cards.map { card -> "${card.color.letter}${card.value}" }.sorted()
        }

        // 🔹 Step 5: Sort combinations by value (highest to lowest)
        return uniqueCombinations.sortedByDescending { it.value }
    }
*/

     fun findValidCombinations(cards: List<Card>): List<Combination> {
        val validCombinations = mutableListOf<Combination>()

        // **1. Group by Color & Sort**
        val colorGroups = cards.groupBy { it.color }.mapValues { it.value.sortedByDescending { card -> card.value } }

        // **2. Group by Value & Sort**
        val valueGroups = cards.groupBy { it.value }.mapValues { it.value.sortedByDescending { card -> card.color.ordinal } }

        val mainCombinations = mutableListOf<Combination>()

        // **3. Identify the highest multi-card combinations first**
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

        // **4. Extract ALL valid subsets from the highest ones**
        for (combination in mainCombinations) {
            val subsetCombinations = generateAllSubcombinations(combination.cards)
            validCombinations.addAll(subsetCombinations)
        }

        // **5. Find missing 2-card combinations from color & value buckets**
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

        // **6. Add single-card combinations**
        cards.forEach { validCombinations.add(Combination(mutableListOf(it))) }

        // **7. Sort in descending order based on value**
        return validCombinations.distinctBy { it.cards.toSet() }
            .sortedByDescending { it.value }
    }

    // **Helper Function: Generate ALL valid subsets from a group (Not just contiguous ones!)**
    fun generateAllSubcombinations(cards: List<Card>): List<Combination> {
        val subsets = mutableListOf<Combination>()
        val size = cards.size

        for (subsetSize in 2..size) { // Only 2 or more cards
            val indices = (0 until size).toList()
            val combinations = indices.combinations(subsetSize)
            for (combinationIndices in combinations) {
                val subset = combinationIndices.map { cards[it] }
                subsets.add(Combination(subset.toMutableList()))
            }
        }

        return subsets
    }

    // **Extension Function: Generate All Combinations of a Given Size**
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
        if (removed) updateCombinations() // Ensure combinations are recalculated
        return removed
    }

    fun removeCard(index: Int = 0): Card? = cards.getOrNull(index)?.also {
        cards.removeAt(index)
        updateCombinations()
    }


    fun removeCombination(combination: Combination): Boolean {
        // Check if all cards exist before removing
        if (!combination.cards.all { it in cards }) return false

        // Remove all cards in the combination from the hand
        combination.cards.forEach { cards.remove(it) }

        updateCombinations() // Recompute combinations
        return true
    }

    fun clear() {
        cards.clear()
        combinations.clear()
    }

    fun isEmpty(): Boolean = cards.isEmpty()
    fun count(): Int = cards.size

    fun updateCombinations() {
        combinations.clear() // Clear previous combinations
        val newCombinations = findValidCombinations(cards)

        // 🔹 Instead of reassigning, add elements directly to `SnapshotStateList`
        combinations.addAll(newCombinations)
    }


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
            .background(Color.White), // Optional background
        horizontalArrangement = Arrangement.Center
    ) {
        items(combinations.size) { index ->
            val combination = combinations[index]  // Fetch combination at index
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

fun generateTestHand3(): Hand {  // 7-card sequence
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

fun generateTestHand4(): Hand {  // Full spectrum of colors
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

fun generateTestHand5(): Hand {  // Minimal overlaps case
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

fun generateTestHand6(): Hand { // Quadruple: Four cards of value 7 (Different colors)
    return Hand().apply {

        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 7), CardColor.BLUE, value = 7))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 7), CardColor.GREEN, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 7), CardColor.MOCHA, value = 7))

        // Triple: Three cards of value 3 (Different colors)
        addCard(Card(CardResources.getImage(CardColor.PINK, 3), CardColor.PINK, value = 3))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 3), CardColor.ORANGE, value = 3))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))

        // Single unique values
        addCard(Card(CardResources.getImage(CardColor.BLUE, 9), CardColor.BLUE, value = 9))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 1), CardColor.GREEN, value = 1))
    }
}


fun generateDeck(shuffle : Boolean = false): MutableList<Card> {
    val deck = mutableListOf<Card>()
    for (color in CardColor.values()) {
        for (value in 1..HAND_SIZE) {
            val cardImageId = CardResources.getImage(color, value)
            deck.add(Card(cardImageId, color,  value=value))
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
fun NidoApp(modifier: Modifier = Modifier) {
    var deck by remember { mutableStateOf(generateDeck()) }
    val currentHand = remember { Hand() }
    var sortMode by remember { mutableStateOf(SortMode.FIFO) } // Sorting mode
    var testVectorIndex by remember { mutableStateOf(0) }

    val switchTestVector: () -> Unit = {
        testVectorIndex = (testVectorIndex + 1) % testVectors.size
        currentHand.clear()
        testVectors[testVectorIndex]().cards.forEach { currentHand.addCard(it) }
    }

    val drawNewHand: () -> Unit = {
        currentHand.clear()
        val cardsToTake = minOf(HAND_SIZE, deck.size)
        repeat(cardsToTake) { currentHand.addCard(deck.removeAt(0)) }
        if (deck.isEmpty()) deck = generateDeck(shuffle = true) // Reset deck if empty
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActionButtonsRow(
            mapOf(
                "New Hand" to drawNewHand,  // Restore original behavior
                "Remove Card" to { currentHand.removeCard()?.let { deck.add(it) } },
                "Add Card" to { deck.firstOrNull()?.let { deck.removeAt(0); currentHand.addCard(it) } },
                "Cycle Test Vector" to switchTestVector, // Replace Empty with Test Cycle
                "Shuffle" to { deck.shuffle() },
                "Sort Mode: ${sortMode.name}" to {
                    sortMode = when (sortMode) {
                        SortMode.FIFO -> SortMode.COLOR
                        SortMode.COLOR -> SortMode.VALUE
                        SortMode.VALUE -> SortMode.FIFO
                    }
                }
            )
        )

        HandView(currentHand, cardWidth = 80.dp, cardHeight = 160.dp, sortMode, onDoubleClick = switchTestVector)
        CombinationsView(currentHand.combinations, cardWidth = 40.dp, cardHeight = 80.dp)

        // **Display Checksum**
        val expectedCombinations = calculateExpectedCombinations(currentHand)
        Text("Expected Combinations: $expectedCombinations", fontSize = 16.sp, color = Color.Red)
        Text("Generated Combinations: ${currentHand.combinations.size}", fontSize = 16.sp, color = Color.Blue)
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
