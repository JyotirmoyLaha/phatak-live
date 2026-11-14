package com.phataklive.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.phataklive.data.LatLng
import com.phataklive.route.AtArrival
import com.phataklive.route.CrossingKind
import com.phataklive.route.Leg
import com.phataklive.route.Option
import com.phataklive.route.Plan
import kotlin.math.cos
import kotlin.math.min

/**
 * The chosen way on a simple map drawn by the app from Google Maps' own road and railway lines: the route, the railway,
 * every crossing on the way coloured by what you're likely to find, start, destination and your live position. Drawn
 * here (no web page), so it always shows and always matches what the app says. Pinch to zoom, drag to move.
 */
@Composable
fun RouteMapDialog(
    plan: Plan, leg: Leg, option: Option?, t: Strings,
    onNavigate: () -> Unit, onClose: () -> Unit,
    status: @Composable () -> Unit,
) {
    val c = Phatak.c
    val ctx = LocalContext.current
    var you by remember { mutableStateOf<LatLng?>(null) }
    // live position while the map is open
    DisposableEffect(Unit) {
        val lm = ctx.getSystemService(LocationManager::class.java)
        val listener = LocationListener { l -> you = LatLng(l.latitude, l.longitude) }
        val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        @SuppressLint("MissingPermission")
        fun start() {
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)) {
                    runCatching { lm.requestLocationUpdates(p, 3_000L, 5f, listener, Looper.getMainLooper()) }
                    if (you == null) runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { you = LatLng(it.latitude, it.longitude) }
                }
            }
        }
        if (ok) start()
        onDispose { runCatching { lm.removeUpdates(listener) } }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(c.bgBase)) {
            Row(Modifier.fillMaxWidth().background(c.headerTop).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, t.close, tint = Color.White) }
                Text(leg.crossing.label(t), color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                var zoom by remember { mutableFloatStateOf(1f) }
                var pan by remember { mutableStateOf(Offset.Zero) }
                TripCanvas(plan, leg, option, you, zoom, pan, t, Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { centroid, p, z, _ ->
                        val nz = (zoom * z).coerceIn(0.5f, 12f)
                        // zoom around the fingers
                        pan = (pan - centroid) * (nz / zoom) + centroid + p
                        zoom = nz
                    }
                })
                Text(t.mapDrawnNote, style = MaterialTheme.typography.labelSmall, color = c.muted,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
                SmallFloatingActionButton(onClick = { zoom = 1f; pan = Offset.Zero }, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                    Icon(Icons.Rounded.CenterFocusStrong, t.mapFit)
                }
            }
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().padding(16.dp)) {
                status()
                Button(onClick = onNavigate, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.amber, contentColor = Color(0xFF1A1300))) {
                    Icon(Icons.Rounded.Navigation, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(t.navigateBtn)
                }
            }
        }
    }
}

@Composable
private fun TripCanvas(plan: Plan, leg: Leg, option: Option?, you: LatLng?, zoom: Float, pan: Offset, t: Strings, modifier: Modifier) {
    val c = Phatak.c
    val measurer = rememberTextMeasurer()
    val routeColor = Color(0xFF2F7CF6)
    val railColor = if (c.dark) Color(0xFFB8C0CC) else Color(0xFF3A4150)
    val label = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val states = (option?.let { listOf(it.at) + it.later.map { l -> l.at } }) ?: emptyList()

    Canvas(modifier) {
        // fit the route (and start / destination) with a margin; longitude scaled for latitude
        val pts = leg.line + plan.from.pos + plan.to.pos
        val minLat = pts.minOf { it.lat }; val maxLat = pts.maxOf { it.lat }
        val minLng = pts.minOf { it.lng }; val maxLng = pts.maxOf { it.lng }
        val k = cos(Math.toRadians((minLat + maxLat) / 2))
        val w = ((maxLng - minLng) * k).coerceAtLeast(1e-5); val h = (maxLat - minLat).coerceAtLeast(1e-5)
        val margin = 48.dp.toPx()
        val scale = min((size.width - 2 * margin) / w, (size.height - 2 * margin) / h).toFloat()
        val ox = (size.width - w.toFloat() * scale) / 2; val oy = (size.height - h.toFloat() * scale) / 2
        fun xy(p: LatLng): Offset {
            val base = Offset(ox + ((p.lng - minLng) * k).toFloat() * scale, oy + (maxLat - p.lat).toFloat() * scale)
            return base * zoom + pan
        }
        fun path(line: List<LatLng>) = Path().apply { line.forEachIndexed { i, p -> val o = xy(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }

        // railway: a dark line with light cross-ties
        for (rail in plan.rails) {
            if (rail.size < 2) continue
            val p = path(rail)
            drawPath(p, railColor, style = Stroke(5.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round))
            drawPath(p, c.bgBase, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))))
        }
        // the way
        drawPath(path(leg.line), routeColor, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        fun dot(p: LatLng, fill: Color, r: Float, ring: Color = Color.White) {
            val o = xy(p); drawCircle(ring, r + 3.dp.toPx(), o); drawCircle(fill, r, o)
        }
        fun text(s: String, p: LatLng, dy: Float) {
            val layout = measurer.measure(s, label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = (size.width * 0.6f).toInt()))
            val o = xy(p)
            val tl = Offset((o.x - layout.size.width / 2f).coerceIn(4f, size.width - layout.size.width - 4f),
                (o.y + dy).coerceIn(4f, size.height - layout.size.height - 4f))
            drawRoundRect(c.glassStrong, tl - Offset(4.dp.toPx(), 2.dp.toPx()),
                androidx.compose.ui.geometry.Size(layout.size.width + 8.dp.toPx(), layout.size.height + 4.dp.toPx()),
                androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            drawText(layout, topLeft = tl)
        }

        // crossings, coloured by what you're likely to find
        leg.crossings.forEachIndexed { i, cr ->
            val col = when {
                cr.kind != CrossingKind.GATE -> c.open
                states.getOrNull(i) == AtArrival.CLOSED -> c.closed
                states.getOrNull(i) == AtArrival.OPEN -> c.open
                else -> c.soon
            }
            dot(cr.pos, col, 8.dp.toPx())
            text(cr.label(t), cr.pos, -30.dp.toPx())
        }
        leg.navStop?.let { dot(it, routeColor, 4.dp.toPx()) }
        dot(plan.from.pos, Color(0xFF1F9D55), 7.dp.toPx())
        text(if (plan.fromIsMe) t.myLocation else plan.from.name, plan.from.pos, 12.dp.toPx())
        dot(plan.to.pos, Color(0xFFE5484D), 7.dp.toPx())
        text(plan.to.name, plan.to.pos, 12.dp.toPx())
        // you, live
        you?.let { p ->
            val o = xy(p)
            drawCircle(routeColor.copy(alpha = 0.22f), 18.dp.toPx(), o)
            drawCircle(Color.White, 8.dp.toPx(), o)
            drawCircle(routeColor, 6.dp.toPx(), o)
        }
    }
}
