package com.phataklive.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Google's own map page for the trip through one crossing, inside the app: the route is drawn by Google, and the
 * page's location button follows the phone's live position (the app's location permission is passed to the page).
 * Below it, the crossing's status at the minute you would reach it, and Navigate for turn-by-turn in Google Maps.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RouteMapDialog(
    url: String, title: String, t: Strings,
    onNavigate: () -> Unit, onClose: () -> Unit,
    status: @Composable () -> Unit,
) {
    val c = Phatak.c
    var progress by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().background(c.headerTop).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, t.close, tint = Color.White) }
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setGeolocationEnabled(true)
                            webChromeClient = object : WebChromeClient() {
                                // only Google's map page gets the position, and only while this screen is open
                                override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                                    val google = origin != null && Regex("^https://([a-z0-9-]+\\.)*google\\.(com|co\\.in)(:\\d+)?/?$").matches(origin)
                                    callback?.invoke(origin, google, false)
                                }
                                override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                            }
                            webViewClient = object : WebViewClient() {
                                // stay on the web map: "open in app" (intent://) and other schemes are not followed
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val scheme = request?.url?.scheme ?: return true
                                    return scheme != "https" && scheme != "http"
                                }
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { progress = 5 }
                            }
                            loadUrl(url)
                        }
                    },
                    onRelease = { it.stopLoading(); it.destroy() },
                )
                if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().padding(16.dp)) {
                status()
                Text(t.mapLiveHint, style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(top = 6.dp))
                Button(onClick = onNavigate, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.amber, contentColor = Color(0xFF1A1300))) {
                    Icon(Icons.Rounded.Navigation, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(t.navigateBtn)
                }
            }
        }
    }
}
