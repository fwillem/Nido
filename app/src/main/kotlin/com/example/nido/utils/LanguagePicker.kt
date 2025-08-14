package com.example.nido.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nido.R
import com.example.nido.ui.preview.NidoPreview
import java.util.Locale

// --- Enum AppLanguage (labels désormais dans strings.xml via locale_marker)
enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    FRENCH("fr"),
    ITALIAN("it"),
    GERMAN("de"),
    SPANISH("es");

    companion object {
        fun fromCode(code: String) = values().find { it.code == code } ?: ENGLISH
        fun all() = values().toList()
    }
}

fun AppLanguage.getLabel(context: android.content.Context): String {
    // Utilise le chemin complet pour éviter les problèmes d'import
    return context.getStringForLocale(
        com.example.nido.R.string.locale_marker,
        code
    )
}

// --- Utilitaire pour lire une string localisée pour une autre locale
fun Context.getStringForLocale(@StringRes resId: Int, localeTag: String): String {
    val locale = Locale.forLanguageTag(localeTag)
    val baseConfig = resources.configuration
    val config = Configuration(baseConfig)

    val localizedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocales(LocaleList(locale))
        createConfigurationContext(config)
    } else {
        @Suppress("DEPRECATION")
        config.locale = locale
        createConfigurationContext(config)
    }
    return localizedContext.resources.getString(resId)
}

// --- Composable pour le picker
@Composable
fun LanguagePicker(
    selectedLanguage: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val languages = remember { AppLanguage.all() }

    val labels = remember(languages, LocalConfiguration.current) {
        languages.associateWith { lang ->
            context.getStringForLocale(R.string.locale_marker, lang.code)
        }
    }

    val current = languages.find { it.code == selectedLanguage } ?: AppLanguage.ENGLISH

    Box(
        modifier = modifier.padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { expanded = true }) {
            Text(labels[current] ?: current.code)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(labels[lang] ?: lang.code) },
                    onClick = {
                        onSelected(lang.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

// @Preview(showBackground = true)
@NidoPreview(name = "Language Picker")
@Composable
fun LanguagePickerPreview() {
    var selectedLanguage by remember { mutableStateOf(AppLanguage.ENGLISH.code) }
    LanguagePicker(
        selectedLanguage = selectedLanguage,
        onSelected = { selectedLanguage = it }
    )
}
