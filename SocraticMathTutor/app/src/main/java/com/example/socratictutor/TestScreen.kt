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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TestScreen(vm: TestViewModel, onExit: () -> Unit) {
    val st by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        st.error?.let { ErrorBanner(it, vm::dismissError) }
        when (st.phase) {
            TestPhase.SETUP -> TestSetup(st, vm, onExit)
            TestPhase.LOADING -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.test_loading), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            TestPhase.RUNNING -> TestRunning(st, vm)
            TestPhase.REVIEW -> TestReview(st, vm, onExit)
        }
    }
}

@Composable
private fun TestSetup(st: TestUiState, vm: TestViewModel, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.test_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.test_no_help_note), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.practice_subject), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = st.topic == null, onClick = { vm.setTopic(null) }, label = { Text(stringResource(R.string.test_mixed)) })
            Topic.selectable.forEach { t ->
                FilterChip(selected = st.topic == t, onClick = { vm.setTopic(t) }, label = { Text(topicLabel(t)) })
            }
        }
        Text(stringResource(R.string.practice_difficulty), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = st.difficulty == d, onClick = { vm.setDifficulty(d) }, label = { Text(difficultyLabel(d)) })
            }
        }
        Text(stringResource(R.string.test_questions_count), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(5, 10).forEach { n ->
                FilterChip(selected = st.count == n, onClick = { vm.setCount(n) }, label = { Text("$n") })
            }
        }
        Button(onClick = vm::start, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.test_start)) }
        TextButton(onClick = { vm.reset(); onExit() }) { Text(stringResource(R.string.close)) }
    }
}

@Composable
private fun TestRunning(st: TestUiState, vm: TestViewModel) {
    val q = st.questions[st.index]
    val low = st.secondsLeft <= 60
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.test_question_of, st.index + 1, st.questions.size),
                Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            )
            Text(
                formatClock(st.secondsLeft),
                style = MaterialTheme.typography.titleMedium,
                color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { (st.index + 1).toFloat() / st.questions.size },
            modifier = Modifier.fillMaxWidth(),
        )
        MathText(q.question, style = MaterialTheme.typography.titleMedium)
        q.choices.forEachIndexed { i, c ->
            val sel = st.answers.getOrNull(st.index) == i
            Row(
                Modifier.fillMaxWidth().selectable(selected = sel, role = SemRole.RadioButton, onClick = { vm.select(i) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = sel, onClick = null)
                MathText(c, Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::previous, enabled = st.index > 0) { Text(stringResource(R.string.test_previous)) }
            Button(onClick = vm::next) {
                Text(stringResource(if (st.index == st.questions.lastIndex) R.string.test_finish else R.string.test_next))
            }
        }
    }
}

@Composable
private fun TestReview(st: TestUiState, vm: TestViewModel, onExit: () -> Unit) {
    val r = st.result ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.test_results), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.test_score, r.correct, r.total), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.test_accuracy, (r.accuracy * 100).toInt()))
                Text(stringResource(R.string.test_time_used, formatClock(r.secondsUsed)))
                Text(stringResource(R.string.test_weak_areas), style = MaterialTheme.typography.titleSmall)
                if (r.weakTopics.isEmpty()) Text(stringResource(R.string.test_all_good))
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.weakTopics.forEach { Text("• " + topicLabel(it)) }
                }
            }
        }
        Text(stringResource(R.string.test_unverified_note), style = MaterialTheme.typography.bodySmall)
        r.results.forEachIndexed { i, qr ->
            val q = qr.question
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.test_question_of, i + 1, r.total) + "  ·  " +
                            topicLabel(q.topic) + "  ·  " + difficultyLabel(q.difficulty) + "  ·  " +
                            stringResource(if (qr.isCorrect) R.string.test_correct else R.string.test_incorrect),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (qr.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    MathText(q.question, style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.test_your_answer), style = MaterialTheme.typography.labelMedium)
                    if (qr.selected == null) Text(stringResource(R.string.test_unanswered))
                    else MathText(q.choices[qr.selected])
                    Text(stringResource(R.string.test_correct_answer), style = MaterialTheme.typography.labelMedium)
                    MathText(q.choices[q.correctIndex])
                    Text(stringResource(R.string.test_explanation), style = MaterialTheme.typography.labelMedium)
                    MathText(q.explanation)
                    if (!qr.isCorrect) q.commonMistake?.let {
                        Text(stringResource(R.string.test_mistake_analysis), style = MaterialTheme.typography.labelMedium)
                        MathText(it)
                    }
                }
            }
        }
        Button(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.test_again)) }
        OutlinedButton(onClick = { vm.reset(); onExit() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.test_done)) }
    }
}
