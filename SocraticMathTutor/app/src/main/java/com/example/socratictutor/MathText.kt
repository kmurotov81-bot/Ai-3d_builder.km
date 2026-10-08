package com.example.socratictutor

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

private fun hex(c: Color): String =
    "#%02x%02x%02x".format((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())

private fun mathHtml(text: String, color: Color, sizePx: Float): String = """
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/contrib/auto-render.min.js"></script>
<style>
html,body{margin:0;padding:0;background:transparent;color:${hex(color)};
font-family:sans-serif;font-size:${sizePx}px;line-height:1.45;word-wrap:break-word}
</style></head><body><div id="t"></div>
<script>
  var el = document.getElementById('t');
  el.textContent = ${JSONObject.quote(text)};
  try { renderMathInElement(el, {delimiters:[
    {left:'$$',right:'$$',display:true},{left:'$',right:'$',display:false},
    {left:'\\(',right:'\\)',display:false},{left:'\\[',right:'\\]',display:true}]}); } catch (e) {}
</script></body></html>
"""

fun hasMath(text: String): Boolean = text.contains('$') || text.contains("\\(") || text.contains("\\[")

/**
 * Text with proper math typesetting (KaTeX in a WebView) when the text contains LaTeX,
 * plain Compose Text otherwise. KaTeX is loaded from a CDN, so offline the raw LaTeX stays readable.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (!hasMath(text)) {
        Text(text, modifier = modifier, style = style, color = color)
        return
    }
    var heightDp by remember(text) { mutableStateOf(40) }
    val sizePx = if (style.fontSize.isSpecified) style.fontSize.value else 16f
    AndroidView(
        modifier = modifier.fillMaxWidth().height(heightDp.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0)
                settings.javaScriptEnabled = true
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.evaluateJavascript("document.documentElement.scrollHeight") { v ->
                            v?.toIntOrNull()?.let { heightDp = it + 4 }
                        }
                    }
                }
                loadDataWithBaseURL("https://cdn.jsdelivr.net", mathHtml(text, color, sizePx), "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
    )
}
