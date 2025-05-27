package com.example.nido.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nido.R

enum class VersionOptions {
    FULL,
    SHORT,
    DATE
}

/**
 * A small text label showing the current version and/or build date.
 *
 * @param modifier Modifier to position/pad this label
 * @param option Controls what to display: FULL, SHORT (just version), or DATE (just date)
 */
@Composable
fun VersionLabel(
    modifier: Modifier = Modifier,
    option: VersionOptions = VersionOptions.FULL
) {
    val branchName = stringResource(R.string.branch_name)
    val buildTime = stringResource(R.string.build_time)
    val label = when (option) {
        VersionOptions.FULL -> "$branchName ($buildTime)"
        VersionOptions.SHORT -> branchName
        VersionOptions.DATE -> buildTime
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}
