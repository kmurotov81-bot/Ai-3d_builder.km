@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.socratictutor

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/** Shared chat UI for Tutor and Practice. [empty] is shown until the first message exists. */
@Composable
fun ChatScreen(
    vm: TutorViewModel,
    title: String,
    showInputWhenEmpty: Boolean,
    onPracticeTopic: (Topic) -> Unit,
    empty: @Composable () -> Unit,
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { vm.attachImage(context, it) } else ImageUtils.clearTempPhotos(context)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.attachImage(context, it) }
    }

    fun launchCamera() {
        try {
            val dir = File(context.cacheDir, "photos").apply { mkdirs() }
            val file = File.createTempFile("problem_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            vm.reportError(AiErrorKind.CAMERA_UNAVAILABLE)
        } catch (e: Exception) {
            vm.reportError(AiErrorKind.CAMERA_UNAVAILABLE)
        }
    }

    LaunchedEffect(state.messages.size, state.loading, state.solved) {
        val extra = (if (state.loading != null) 1 else 0) + (if (state.solved != null) 1 else 0)
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex + extra)
    }

    val latestTutorId = state.messages.lastOrNull { it.role == Role.TUTOR }?.id
    val isEmpty = state.messages.isEmpty() && state.loading == null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = { TextButton(onClick = vm::newProblem) { Text(stringResource(R.string.new_problem)) } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            Box(Modifier.weight(1f)) {
                if (isEmpty) {
                    empty()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.messages, key = { it.id }) { msg ->
                            MessageBubble(
                                msg = msg,
                                isLatestTutor = msg.id == latestTutorId && state.solved == null,
                                canHint = state.canHint,
                                enabled = state.loading == null,
                                onWhy = { vm.askWhy(msg) },
                                onStuck = vm::requestHint,
                                onGraph = { vm.openGraph(msg.graph) },
                                onConfirm = vm::confirmProblem,
                            )
                        }
                        state.loading?.let { item { LoadingCard(it) } }
                        state.solved?.let { s ->
                            item {
                                SolvedCard(
                                    s = s,
                                    onSimilar = vm::similarProblem,
                                    onNew = vm::newProblem,
                                    onPracticeTopic = { onPracticeTopic(s.topic) },
                                )
                            }
                        }
                    }
                }
            }

            state.error?.let { ErrorBanner(it, vm::dismissError) }

            if (!isEmpty || showInputWhenEmpty) {
                InputBar(
                    input = input,
                    onInput = { input = it },
                    pending = state.pendingImage,
                    checkWork = state.checkWork,
                    enabled = state.loading == null,
                    onToggleCheck = { vm.setCheckWork(!state.checkWork) },
                    onClearImage = vm::clearImage,
                    onCamera = ::launchCamera,
                    onGallery = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onSend = { vm.submit(input); input = "" },
                )
            }
        }
    }

    if (state.graphOpen) {
        GraphDialog(state.graph, onClose = vm::closeGraph, onFailure = { vm.reportError(AiErrorKind.GRAPH_FAILED) })
    }
}
