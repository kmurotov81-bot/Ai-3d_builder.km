@file:OptIn(ExperimentalLayoutApi::class)

package com.example.socratictutor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun TutorScreen(vm: TutorViewModel, onPracticeTopic: (Topic) -> Unit) {
    ChatScreen(
        vm = vm,
        title = stringResource(R.string.mode_tutor),
        showInputWhenEmpty = true,
        onPracticeTopic = onPracticeTopic,
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.empty_tutor_title), style = MaterialTheme.typography.titleMedium)
            Text("\n" + stringResource(R.string.empty_tutor_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PracticeScreen(
    vm: TutorViewModel,
    settings: SettingsData,
    pendingTopic: Topic?,
    onTopicConsumed: () -> Unit,
    onPracticeTopic: (Topic) -> Unit,
) {
    LaunchedEffect(pendingTopic) {
        if (pendingTopic != null) {
            vm.startPractice(pendingTopic, settings.difficulty)
            onTopicConsumed()
        }
    }
    ChatScreen(
        vm = vm,
        title = stringResource(R.string.mode_practice),
        showInputWhenEmpty = false,
        onPracticeTopic = onPracticeTopic,
    ) {
        PracticeSetup(settings.difficulty) { topic, diff -> vm.startPractice(topic, diff) }
    }
}

@Composable
private fun PracticeSetup(defaultDifficulty: Difficulty, onStart: (Topic?, Difficulty) -> Unit) {
    var topic by rememberSaveable { mutableStateOf<Topic?>(null) }
    var diff by rememberSaveable { mutableStateOf(defaultDifficulty) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.practice_recommended), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.practice_recommended_desc), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { onStart(null, diff) }) { Text(stringResource(R.string.practice_generate_auto)) }
            }
        }
        Text(stringResource(R.string.practice_subject), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Topic.selectable.forEach { t ->
                FilterChip(selected = topic == t, onClick = { topic = if (topic == t) null else t }, label = { Text(topicLabel(t)) })
            }
        }
        Text(stringResource(R.string.practice_difficulty), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = diff == d, onClick = { diff = d }, label = { Text(difficultyLabel(d)) })
            }
        }
        OutlinedButton(onClick = { onStart(topic, diff) }, enabled = topic != null, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.practice_generate))
        }
    }
}
