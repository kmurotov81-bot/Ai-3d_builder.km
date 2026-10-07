package com.example.socratictutor

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
  var tutorIds = [];
  // Replaces only the tutor's expressions; anything the student typed stays.
  function setTutorGraph(list) {
    tutorIds.forEach(function(id) { calc.removeExpression({id: id}); });
    tutorIds = [];
    list.forEach(function(latex, i) {
      var id = 'tutor' + i;
      calc.setExpression({id: id, latex: latex});
      tutorIds.push(id);
    });
  }
</script></body></html>
""".trimIndent()

/**
 * Hosts the Desmos Graphing Calculator in a WebView. It stays composed
 * (even when collapsed) so the student's own expressions aren't lost.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DesmosPanel(expressions: List<String>, modifier: Modifier = Modifier) {
    val ready = remember { mutableStateOf(false) }
    val webView = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        ready.value = true
                    }
                }
                loadDataWithBaseURL(
                    "https://www.desmos.com",
                    desmosHtml(BuildConfig.DESMOS_API_KEY),
                    "text/html", "utf-8", null,
                )
                webView.value = this
            }
        },
        onRelease = { it.destroy() },
    )

    LaunchedEffect(expressions, ready.value) {
        if (ready.value) {
            webView.value?.evaluateJavascript("setTutorGraph(${JSONArray(expressions)})", null)
        }
    }
}
