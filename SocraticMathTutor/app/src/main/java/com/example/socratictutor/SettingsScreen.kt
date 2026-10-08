@file:OptIn(ExperimentalLayoutApi::class)

package com.example.socratictutor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    s: SettingsData,
    onChange: ((SettingsData) -> SettingsData) -> Unit,
    onDeleteProgress: () -> Unit,
    onLanguageChanged: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Heading(R.string.set_level)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = s.level == null, onClick = { onChange { it.copy(level = null) } }, label = { Text(stringResource(R.string.level_auto)) })
            Level.entries.forEach { l ->
                FilterChip(selected = s.level == l, onClick = { onChange { it.copy(level = l) } }, label = { Text(levelLabel(l)) })
            }
        }

        Heading(R.string.set_difficulty)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = s.difficulty == d, onClick = { onChange { it.copy(difficulty = d) } }, label = { Text(difficultyLabel(d)) })
            }
        }

        Heading(R.string.set_style)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TutorStyle.entries.forEach { t ->
                val label = when (t) {
                    TutorStyle.GENTLE -> R.string.style_gentle
                    TutorStyle.CONCISE -> R.string.style_concise
                    TutorStyle.CHALLENGING -> R.string.style_challenging
                }
                FilterChip(selected = s.style == t, onClick = { onChange { it.copy(style = t) } }, label = { Text(stringResource(label)) })
            }
        }

        Heading(R.string.set_theme)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ThemeMode.entries.forEach { m ->
                val label = when (m) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                }
                FilterChip(selected = s.theme == m, onClick = { onChange { it.copy(theme = m) } }, label = { Text(stringResource(label)) })
            }
        }

        Heading(R.string.set_sat)
        ToggleRow(R.string.sat_calculator, s.satCalculator) { v -> onChange { it.copy(satCalculator = v) } }
        ToggleRow(R.string.sat_time_tips, s.satTimeTips) { v -> onChange { it.copy(satTimeTips = v) } }

        Heading(R.string.set_language)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("system" to R.string.lang_system, "en" to R.string.lang_en, "uz" to R.string.lang_uz).forEach { (code, label) ->
                FilterChip(
                    selected = s.language == code,
                    onClick = { if (s.language != code) { onChange { it.copy(language = code) }; onLanguageChanged() } },
                    label = { Text(stringResource(label)) },
                )
            }
        }

        Heading(R.string.set_account)
        Text(stringResource(R.string.account_info), style = MaterialTheme.typography.bodyMedium)

        Heading(R.string.set_privacy)
        Text(stringResource(R.string.privacy_info), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete_data)) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { onDeleteProgress(); confirmDelete = false }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Heading(id: Int) {
    Text(stringResource(id), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ToggleRow(label: Int, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
