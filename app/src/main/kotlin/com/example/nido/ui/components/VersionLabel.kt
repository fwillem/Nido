package com.example.nido.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nido.R

// Variable used to check correctness version read on github by external agent such as AI agents
val gitHubDebug = "20/08/2025 10:48:00"

enum class VersionOptions {
    FULL,
    SHORT,
    DATE,
    TAG
}

/**
 * A small text label showing the current version and/or build date.
 *
 * @param modifier Modifier to position/pad this label
 * @param option Controls what to display: FULL, SHORT (just branch),
 *               DATE (just build time), or TAG (just git tag which displays savepoints also)
 */
@Composable
fun VersionLabel(
    modifier: Modifier = Modifier,
    option: VersionOptions = VersionOptions.FULL
) {
    val branchName = stringResource(R.string.branch_name)
    val buildTime = stringResource(R.string.build_time)
    val gitTag = stringResource(R.string.git_tag)

    val label = when (option) {
        VersionOptions.FULL -> "$branchName ($buildTime)"
        VersionOptions.SHORT -> branchName
        VersionOptions.DATE -> buildTime
        VersionOptions.TAG -> gitTag
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}
