package com.example.nido.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nido.R

/**
 * A small text label showing the current Git tag (or short SHA) injected via R.string.git_tag.
 *
 * @param modifier Modifier to position/pad this label
 */
@Composable
fun VersionLabel(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.git_tag),
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}
