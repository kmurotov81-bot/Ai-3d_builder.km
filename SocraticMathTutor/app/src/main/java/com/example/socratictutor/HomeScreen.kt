@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.socratictutor

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun HomeScreen(
    tutor: TutorViewModel,
    streak: Int,
    onOpenTutor: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenTest: () -> Unit,
) {
    val state by tutor.state.collectAsState()
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(Mode.TUTOR) }
    var input by rememberSaveable { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { tutor.attachImage(context, it) } else ImageUtils.clearTempPhotos(context)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { tutor.attachImage(context, it) }
    }
    fun launchCamera() {
        try {
            val dir = File(context.cacheDir, "photos").apply { mkdirs() }
            val file = File.createTempFile("problem_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            tutor.reportError(AiErrorKind.CAMERA_UNAVAILABLE)
        } catch (e: Exception) {
            tutor.reportError(AiErrorKind.CAMERA_UNAVAILABLE)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium)
        if (streak > 0) {
            Text(stringResource(R.string.streak_days, streak), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }

        ModeCard(R.string.mode_tutor, R.string.mode_tutor_desc, selected == Mode.TUTOR) { selected = Mode.TUTOR }
        ModeCard(R.string.mode_practice, R.string.mode_practice_desc, selected == Mode.PRACTICE) { selected = Mode.PRACTICE }
        ModeCard(R.string.mode_test, R.string.mode_test_desc, selected == Mode.TEST) { selected = Mode.TEST }

        if (selected == Mode.TUTOR) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.problem_placeholder)) },
                minLines = 2,
                maxLines = 5,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = ::launchCamera) { Text(stringResource(R.string.camera)) }
                TextButton(onClick = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.gallery)) }
                if (state.pendingImage != null) {
                    PhotoThumb(state.pendingImage!!, large = false)
                    TextButton(onClick = tutor::clearImage) { Text(stringResource(R.string.remove)) }
                }
            }
            state.error?.let { ErrorBanner(it, tutor::dismissError) }
        }

        Button(
            onClick = {
                when (selected) {
                    Mode.TUTOR -> {
                        if (input.isNotBlank() || state.pendingImage != null) {
                            tutor.newProblem()
                            tutor.submit(input)
                            input = ""
                        }
                        onOpenTutor()
                    }
                    Mode.PRACTICE -> onOpenPractice()
                    Mode.TEST -> onOpenTest()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.start_solving)) }

        OutlinedButton(
            onClick = { tutor.setCheckWork(true); onOpenTutor() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.show_my_mistake)) }
    }
}

@Composable
private fun ModeCard(title: Int, desc: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(desc), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
