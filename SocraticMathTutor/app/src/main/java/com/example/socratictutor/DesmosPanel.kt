package com.example.socratictutor

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray

private fun desmosHtml(apiKey: String) = """
<!DOCTYPE html>
<html><head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<script src="https://www.desmos.com/api/v1.9/calculator.js?apiKey=$apiKey"></script>
<style>html,body,#c{margin:0;padding:0;height:100%;width:100%}</style>
</head><body><div id="c"></div>
<script>
  var calc = Desmos.GraphingCalculator(document.getElementById('c'), {
    keypad: false, expressions: true, settingsMenu: false, zoomButtons: true
  });
  function setGraph(list) {
    list.forEach(function(latex, i) { calc.setExpression({id: 'tutor' + i, latex: latex}); });
  }
</script></body></html>
""".trimIndent()

/** Full-screen dialog opened by "See it on the graph"; it is not part of the permanent layout. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GraphDialog(expressions: List<String>, onClose: () -> Unit, onFailure: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
                }
                var failed by remember { mutableStateOf(false) }
                var ready by remember { mutableStateOf(false) }
                var web by remember { mutableStateOf<WebView?>(null) }
                if (failed) {
                    Text(stringResource(R.string.err_graph), Modifier.padding(16.dp))
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) { ready = true }
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                        error: android.webkit.WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) { failed = true; onFailure() }
                                    }
                                }
                                loadDataWithBaseURL(
                                    "https://www.desmos.com", desmosHtml(BuildConfig.DESMOS_API_KEY),
                                    "text/html", "utf-8", null,
                                )
                                web = this
                            }
                        },
                        onRelease = { it.destroy() },
                    )
                    LaunchedEffect(ready, expressions) {
                        if (ready) web?.evaluateJavascript("setGraph(${JSONArray(expressions)})", null)
                    }
                }
            }
        }
    }
}
