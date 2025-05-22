package com.example.nido.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommentsView(actions: Map<String, () -> Unit>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text zone - 80%
        Box(
            modifier = Modifier.weight(0.8f, fill = true)
        ) {
            Text(
                text = "Hello",
                fontSize = 16.sp,
                lineHeight = 8.sp,
                modifier = Modifier.padding(end = 4.dp, start = 36.dp)
            )
        }

        // Vertical Divider (Material3 only provides horizontal, but this works)
        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )


        // Buttons zone - 20%
        Row(
            modifier = Modifier.weight(0.2f, fill = true),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
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
}
