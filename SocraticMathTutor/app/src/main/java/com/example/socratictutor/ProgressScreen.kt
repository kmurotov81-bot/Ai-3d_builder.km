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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(data: ProgressData, settings: SettingsData, today: Long, onPractice: (Topic) -> Unit) {
    val level = settings.level ?: ProgressEngine.estimateLevel(data, null)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (data.isEmpty) {
            Text(stringResource(R.string.progress_empty))
            return@Column
        }
        val acc = ProgressEngine.accuracy(data)?.let { "${(it * 100).toInt()}%" } ?: "—"
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(stringResource(R.string.stat_solved), "${data.solved}", Modifier.weight(1f))
            Stat(stringResource(R.string.stat_accuracy), acc, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(stringResource(R.string.stat_level), levelLabel(level), Modifier.weight(1f))
            Stat(stringResource(R.string.stat_streak), "${ProgressEngine.streak(data, today)}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(stringResource(R.string.stat_hints), "${data.hintsTotal}", Modifier.weight(1f))
            Stat(stringResource(R.string.stat_mistakes), "${data.mistakesTotal}", Modifier.weight(1f))
        }

        Section(stringResource(R.string.recommendations)) {
            ProgressEngine.recommendations(data).forEach { rec ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val text = when (rec.reason) {
                        RecReason.START -> stringResource(R.string.rec_start)
                        RecReason.WEAK -> stringResource(R.string.rec_weak, topicLabel(rec.topic ?: Topic.UNKNOWN))
                        RecReason.IMPROVE -> stringResource(R.string.rec_improve, topicLabel(rec.topic ?: Topic.UNKNOWN))
                        RecReason.KEEP_GOING -> stringResource(R.string.rec_keep)
                    }
                    Text(text)
                    rec.topic?.let { t ->
                        OutlinedButton(onClick = { onPractice(t) }) { Text(stringResource(R.string.practice_topic)) }
                    }
                }
            }
        }

        Section(stringResource(R.string.mastered_topics)) {
            val m = ProgressEngine.masteredTopics(data)
            if (m.isEmpty()) Text(stringResource(R.string.none_yet))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                m.forEach { AssistChip(onClick = {}, label = { Text(topicLabel(it)) }) }
            }
        }

        Section(stringResource(R.string.weak_topics)) {
            val w = ProgressEngine.weakTopics(data)
            if (w.isEmpty()) Text(stringResource(R.string.none_yet))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                w.forEach { AssistChip(onClick = {}, label = { Text(topicLabel(it)) }) }
            }
        }

        Section(stringResource(R.string.recent_mistakes)) {
            if (data.mistakes.isEmpty()) Text(stringResource(R.string.none_yet))
            data.mistakes.takeLast(5).reversed().forEach {
                Text("• " + topicLabel(Topic.fromKey(it.topic)) + ": " + it.note, style = MaterialTheme.typography.bodySmall)
            }
        }

        val badges = ProgressEngine.badges(data, today)
        Section(stringResource(R.string.badges)) {
            if (badges.isEmpty()) Text(stringResource(R.string.none_yet))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                badges.forEach { AssistChip(onClick = {}, label = { Text(stringResource(badgeLabel(it))) }) }
            }
        }

        if (data.tests.isNotEmpty()) {
            Section(stringResource(R.string.recent_tests)) {
                data.tests.takeLast(3).reversed().forEach {
                    Text(stringResource(R.string.test_line, it.correct, it.total, formatClock(it.seconds)))
                }
            }
        }
    }
}

private fun badgeLabel(b: Badge): Int = when (b) {
    Badge.FIRST_SOLVE -> R.string.badge_first_solve
    Badge.FIVE_SOLVED -> R.string.badge_five
    Badge.TEN_SOLVED -> R.string.badge_ten
    Badge.NO_HINTS -> R.string.badge_no_hints
    Badge.STREAK_3 -> R.string.badge_streak3
    Badge.STREAK_7 -> R.string.badge_streak7
    Badge.MASTERED_TOPIC -> R.string.badge_mastered
    Badge.FIRST_TEST -> R.string.badge_first_test
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
