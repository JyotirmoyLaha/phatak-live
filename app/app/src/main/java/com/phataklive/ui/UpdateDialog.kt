package com.phataklive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phataklive.update.UpdateChecker

/**
 * "Software Update" dialog: version, size, the release's own notes as "What's new", and
 * Download Now → progress → Install Now, or Remind Me Later.
 */
@Composable
fun UpdateDialog(
    info: UpdateChecker.UpdateInfo,
    download: UpdateChecker.DownloadState,
    t: Strings,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: (java.io.File) -> Unit,
    onOpenReleasePage: () -> Unit,
    onLater: () -> Unit,
) {
    val c = Phatak.c
    val busy = download is UpdateChecker.DownloadState.Downloading
    Dialog(onDismissRequest = { if (!busy) onLater() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(horizontal = 28.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(c.headerBottom, c.headerTop)))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))), RoundedCornerShape(28.dp)),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp, start = 22.dp, end = 22.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(c.amber), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SystemUpdate, null, tint = Color(0xFF1A1300), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(t.updateTitle, style = MaterialTheme.typography.titleLarge, color = c.onHeader)
                Spacer(Modifier.height(4.dp))
                Text(t.updateAvailable(info.version) + if (info.apkSize > 0) " · %.1f MB".format(info.apkSize / 1e6) else "",
                    style = MaterialTheme.typography.bodyMedium, color = c.onHeaderMuted, textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)) {
                Text(t.whatsNew, style = MaterialTheme.typography.labelMedium, color = c.onHeaderMuted)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    ReleaseNotes(whatsNew(info.notes))
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            when (download) {
                is UpdateChecker.DownloadState.Downloading -> {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.downloadingUpdate, style = MaterialTheme.typography.titleMedium, color = c.onHeader, modifier = Modifier.weight(1f))
                            Text("${(download.fraction * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = c.amber)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(progress = { download.fraction }, color = c.amber, trackColor = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    DialogButton(t.cancel, c.onHeader, onClick = onCancelDownload)
                }
                is UpdateChecker.DownloadState.Ready -> {
                    DialogButton(t.installNow, c.amber, bold = true) { onInstall(download.file) }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    DialogButton(t.remindLater, c.onHeader, onClick = onLater)
                }
                is UpdateChecker.DownloadState.Failed -> {
                    Text(t.updateFailed, style = MaterialTheme.typography.bodySmall, color = c.closed,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 22.dp, end = 22.dp), textAlign = TextAlign.Center)
                    DialogButton(t.tryAgain, c.amber, bold = true, onClick = onDownload)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    DialogButton(t.remindLater, c.onHeader, onClick = onLater)
                }
                UpdateChecker.DownloadState.Idle -> {
                    if (info.apkUrl != null) DialogButton(t.downloadNow, c.amber, bold = true, onClick = onDownload)
                    else DialogButton(t.openReleasePage, c.amber, bold = true, onClick = onOpenReleasePage)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    DialogButton(t.remindLater, c.onHeader, onClick = onLater)
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: Color, bold: Boolean = false, onClick: () -> Unit) {
    Text(
        text, color = color, textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 17.dp),
    )
}

/** Shows the release's "What's new" section if it has one, otherwise the notes up to the install instructions. */
internal fun whatsNew(notes: String): String {
    val lines = notes.replace("\r", "").lines()
    val start = lines.indexOfFirst { it.trimStart('#', ' ').startsWith("What's new", ignoreCase = true) }
    val from = if (start >= 0) start + 1 else 0
    val end = (from until lines.size).firstOrNull { i ->
        val l = lines[i].trim()
        l.startsWith("#") && (start >= 0 || l.contains("Install", true) || l.contains("Verify", true))
    } ?: lines.size
    return lines.subList(from, end).joinToString("\n").trim()
}

/** Minimal Markdown: #/##/### headings, - or * bullets, **bold**, `code`, > quotes. */
@Composable
private fun ReleaseNotes(md: String) {
    val c = Phatak.c
    md.lines().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.isBlank() -> Spacer(Modifier.height(6.dp))
            line.startsWith("#") -> Text(inline(line.trimStart('#').trim()), style = MaterialTheme.typography.titleLarge,
                color = c.onHeader, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> Row(Modifier.padding(vertical = 2.dp)) {
                Text("•", color = c.amber, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(10.dp))
                Text(inline(line.trimStart().drop(2)), style = MaterialTheme.typography.bodyLarge, color = c.onHeader)
            }
            line.startsWith(">") -> Text(inline(line.trimStart('>', ' ')), style = MaterialTheme.typography.bodyMedium, color = c.onHeaderMuted)
            else -> Text(inline(line), style = MaterialTheme.typography.bodyLarge, color = c.onHeader)
        }
    }
}

private fun inline(s: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val text = s.replace("`", "")
    while (i < text.length) {
        val b = text.indexOf("**", i)
        if (b < 0) { append(text.substring(i)); break }
        val e = text.indexOf("**", b + 2)
        if (e < 0) { append(text.substring(i)); break }
        append(text.substring(i, b))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(b + 2, e)) }
        i = e + 2
    }
}
