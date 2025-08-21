package com.example.nido.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NumberWheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 5,
    itemHeight: Dp = 36.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    val clampedSelected = selectedIndex.coerceIn(0, (values.size - 1).coerceAtLeast(0))
    val slots = (visibleItemsCount.coerceAtLeast(3) or 1)
    val middle = (slots - 1) / 2
    val totalHeight = itemHeight * slots
    val sidePadding = itemHeight * middle
    val density = LocalDensity.current

    val initialFirstVisible = (clampedSelected - middle).coerceIn(0, (values.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisible)

    // Keep list aligned with external selection (centered)
    LaunchedEffect(clampedSelected, values) {
        val targetFirst = (clampedSelected - middle).coerceIn(0, (values.size - 1).coerceAtLeast(0))
        if (listState.firstVisibleItemIndex != targetFirst) {
            listState.scrollToItem(targetFirst)
        }
    }

    // When scroll stops, snap to the nearest item at center and report selection
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    LaunchedEffect(isScrolling) {
        if (!isScrolling && values.isNotEmpty()) {
            val first = listState.firstVisibleItemIndex
            val offsetPx = listState.firstVisibleItemScrollOffset
            val itemHeightPx = with(density) { itemHeight.toPx() }
            val nearestTop = if (offsetPx >= itemHeightPx / 2f) first + 1 else first
            val centeredIndex = (nearestTop + middle).coerceIn(0, values.lastIndex)
            if (centeredIndex != clampedSelected) onSelectedIndexChange(centeredIndex)

            // Snap scroll so the centered index is perfectly centered
            val targetFirst = (centeredIndex - middle).coerceIn(0, values.lastIndex)
            if (targetFirst != listState.firstVisibleItemIndex) {
                listState.scrollToItem(targetFirst)
            }
        }
    }

    Box(modifier = modifier.height(totalHeight), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = values.size > 1,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = sidePadding)
        ) {
            itemsIndexed(values) { index, label ->
                val isSelected by remember(clampedSelected, index) { derivedStateOf { index == clampedSelected } }
                val style = if (isSelected) {
                    textStyle.copy(fontWeight = FontWeight.Bold)
                } else {
                    textStyle
                }
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, style = style, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
