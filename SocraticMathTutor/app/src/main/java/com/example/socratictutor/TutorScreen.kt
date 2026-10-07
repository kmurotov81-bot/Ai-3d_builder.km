package com.example.socratictutor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(vm: TutorViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { vm.attachImage(context, it) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.attachImage(context, it) }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File.createTempFile("problem_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        takePicture.launch(uri)
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        val extra = if (state.isLoading) 1 else 0
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex + extra)
    }

    val latestTutorId = state.messages.lastOrNull { it.role == Role.TUTOR }?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Socratic Math Tutor") },
                actions = {
                    TextButton(onClick = vm::toggleGraph) { Text(if (state.showGraph) "Hide graph" else "Graph") }
                    TextButton(onClick = vm::newProblem) { Text("New") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            // Always composed so the student's own Desmos work survives collapsing.
            DesmosPanel(
                expressions = state.graph,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (state.showGraph) 300.dp else 0.dp),
            )

            Box(Modifier.weight(1f)) {
                if (state.messages.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.messages, key = { it.id }) { msg ->
                            MessageBubble(
                                msg = msg,
                                isLatestTutor = msg.id == latestTutorId,
                                enabled = !state.isLoading,
                                onWhy = { vm.askWhy(msg) },
                                onStuck = vm::imStuck,
                                onGraph = { vm.openGraph(msg.graph) },
                            )
                        }
                        if (state.isLoading) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("  Thinking it through with you…", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            state.error?.let {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = vm::dismissError) { Text("OK") }
                    }
                }
            }

            InputBar(
                input = input,
                onInput = { input = it },
                pending = state.pendingImage,
                enabled = !state.isLoading,
                onClearImage = vm::clearImage,
                onCamera = ::launchCamera,
                onGallery = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onSend = {
                    vm.submit(input)
                    input = ""
                },
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Let's work through it together.", style = MaterialTheme.typography.titleMedium)
        Text(
            "\nSnap a photo of a calculus or algebra problem, or type it below. " +
                "I won't hand you the answer. We'll take it one step at a time, " +
                "and you can always ask \"Why did we do that?\"",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubble(
    msg: ChatMessage,
    isLatestTutor: Boolean,
    enabled: Boolean,
    onWhy: () -> Unit,
    onStuck: () -> Unit,
    onGraph: () -> Unit,
) {
    val isStudent = msg.role == Role.STUDENT
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isStudent) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isStudent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                msg.image?.let {
                    Image(
                        it.asImageBitmap(), contentDescription = "Problem photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                if (msg.text.isNotBlank()) Text(msg.text, style = MaterialTheme.typography.bodyLarge)

                msg.question?.let {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            it,
                            Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (!isStudent) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (msg.kind == Kind.STEP || msg.kind == Kind.FINAL) {
                            OutlinedButton(onClick = onWhy, enabled = enabled) { Text("Why did we do that?") }
                        }
                        if (isLatestTutor && msg.question != null && msg.kind != Kind.FINAL) {
                            OutlinedButton(onClick = onStuck, enabled = enabled) { Text("I'm stuck") }
                        }
                        if (msg.graph.isNotEmpty()) {
                            OutlinedButton(onClick = onGraph) { Text("See it on the graph") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    onInput: (String) -> Unit,
    pending: Bitmap?,
    enabled: Boolean,
    onClearImage: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCamera) { Text("Camera") }
                TextButton(onClick = onGallery) { Text("Gallery") }
                if (pending != null) {
                    Image(
                        pending.asImageBitmap(), contentDescription = "Attached photo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    TextButton(onClick = onClearImage) { Text("Remove") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a problem or your answer…") },
                    maxLines = 4,
                )
                IconButton(
                    onClick = onSend,
                    enabled = enabled && (input.isNotBlank() || pending != null),
                ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
            }
        }
    }
}
