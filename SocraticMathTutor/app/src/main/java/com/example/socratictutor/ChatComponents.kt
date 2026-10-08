@file:OptIn(ExperimentalLayoutApi::class)

package com.example.socratictutor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBanner(error: UiError, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(errorText(error), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    }
}

@Composable
fun LoadingCard(kind: LoadingKind) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(loadingText(kind), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun MessageBubble(
    msg: ChatMessage,
    isLatestTutor: Boolean,
    canHint: Boolean,
    enabled: Boolean,
    onWhy: () -> Unit,
    onStuck: () -> Unit,
    onGraph: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isStudent = msg.role == Role.STUDENT
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isStudent) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isStudent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                msg.image?.let { PhotoThumb(it, large = true) }
                msg.problem?.let {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(10.dp)) {
                            Text(stringResource(R.string.problem_label), style = MaterialTheme.typography.labelMedium)
                            MathText(it, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (msg.text.isNotBlank()) MathText(msg.text, style = MaterialTheme.typography.bodyLarge)
                msg.question?.let {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        MathText(
                            it, Modifier.padding(10.dp), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                if (!isStudent) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isLatestTutor && msg.type == ReplyType.PROBLEM_CONFIRMATION) {
                            Button(onClick = onConfirm, enabled = enabled) { Text(stringResource(R.string.confirm_btn)) }
                        }
                        if (msg.canUseWhy) {
                            OutlinedButton(onClick = onWhy, enabled = enabled) { Text(stringResource(R.string.why_did_we_do_that)) }
                        }
                        if (isLatestTutor && canHint && msg.type != ReplyType.FINAL) {
                            OutlinedButton(onClick = onStuck, enabled = enabled) { Text(stringResource(R.string.im_stuck)) }
                        }
                        if (msg.graph.isNotEmpty()) {
                            OutlinedButton(onClick = onGraph) { Text(stringResource(R.string.graph_btn)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoThumb(bmp: Bitmap, large: Boolean) {
    Image(
        bmp.asImageBitmap(),
        contentDescription = stringResource(R.string.photo_desc),
        modifier = (if (large) Modifier.fillMaxWidth().height(160.dp) else Modifier.size(44.dp))
            .clip(RoundedCornerShape(10.dp)),
        contentScale = if (large) ContentScale.Fit else ContentScale.Crop,
    )
}

@Composable
fun SolvedCard(s: SolvedSummary, onSimilar: () -> Unit, onNew: () -> Unit, onPracticeTopic: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.solved_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (s.concepts.isNotEmpty()) {
                Text(stringResource(R.string.solved_learned), style = MaterialTheme.typography.titleSmall)
                s.concepts.forEach { MathText("•  $it", style = MaterialTheme.typography.bodyMedium) }
            }
            Text(stringResource(R.string.solved_difficulty, difficultyLabel(s.difficulty)))
            Text(stringResource(R.string.solved_hints, s.hintsUsed))
            Text(stringResource(R.string.solved_mistakes, s.mistakes))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSimilar) { Text(stringResource(R.string.try_similar)) }
                OutlinedButton(onClick = onNew) { Text(stringResource(R.string.new_problem)) }
                if (s.topic != Topic.UNKNOWN) {
                    OutlinedButton(onClick = onPracticeTopic) { Text(stringResource(R.string.practice_topic)) }
                }
            }
        }
    }
}

@Composable
fun InputBar(
    input: String,
    onInput: (String) -> Unit,
    pending: Bitmap?,
    checkWork: Boolean,
    enabled: Boolean,
    onToggleCheck: () -> Unit,
    onClearImage: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.padding(8.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onCamera) { Text(stringResource(R.string.camera)) }
                TextButton(onClick = onGallery) { Text(stringResource(R.string.gallery)) }
                FilterChip(
                    selected = checkWork,
                    onClick = onToggleCheck,
                    label = { Text(stringResource(R.string.show_my_mistake)) },
                )
                if (pending != null) {
                    PhotoThumb(pending, large = false)
                    TextButton(onClick = onClearImage) { Text(stringResource(R.string.remove)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(if (checkWork) R.string.check_work_placeholder else R.string.type_answer_placeholder)) },
                    maxLines = 4,
                )
                val sendDesc = stringResource(R.string.send)
                IconButton(
                    onClick = onSend,
                    enabled = enabled && (input.isNotBlank() || pending != null),
                    modifier = Modifier.semantics { contentDescription = sendDesc },
                ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) }
            }
        }
    }
}
