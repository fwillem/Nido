import androidx.compose.foundation.layout.size
import androidx.compose.material3.value
import com.example.nido.logic.GameRules

data class Hand(
    val cards: SnapshotStateList<Card> = mutableStateListOf(),
    val combinations: SnapshotStateList<Combination> = mutableStateListOf()
) {
    fun addCard(card: Card) {
        cards.add(card)
        updateCombinations()
    }

    fun removeCard(card: Card): Boolean {
        val removed = cards.remove(card)
        if (removed) updateCombinations()
        return removed
    }

    fun clear() {
        cards.clear()
        combinations.clear()
    }

    fun updateCombinations() {
        combinations.clear()
        val newCombinations = GameRules.findValidCombinations(cards)  // 🔹 Now uses GameRules
        combinations.addAll(newCombinations)
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
