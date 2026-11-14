package com.phataklive.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.phataklive.data.LatLng
import com.phataklive.net.GoogleMaps
import com.phataklive.route.AtArrival
import com.phataklive.route.CrossingKind
import com.phataklive.route.Leg
import com.phataklive.route.Option
import com.phataklive.route.Plan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Google's map pictures (tiles, the ones the Google Maps website shows), kept in memory while the app runs.
 * Each tile is a 512 px picture of one square of the world at one zoom level (Web Mercator, like every web map).
 */
private object Tiles {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 160
    }
    private val failed = HashSet<String>()
    private val sem = Semaphore(6)

    @Synchronized fun get(key: String): ImageBitmap? = cache[key]
    @Synchronized private fun put(key: String, img: ImageBitmap) { cache[key] = img }
    @Synchronized private fun hasFailed(key: String) = key in failed
    @Synchronized private fun fail(key: String) { failed += key }

    suspend fun load(z: Int, x: Int, y: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = "$z/$x/$y"
        get(key) ?: if (hasFailed(key)) null else sem.withPermit {
            val img = runCatching {
                val req = Request.Builder()
                    .url("https://mt${(x + y) % 4}.google.com/vt/lyrs=m&hl=en&gl=in&scale=2&x=$x&y=$y&z=$z")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36")
                    .build()
                GoogleMaps.http.newCall(req).execute().use { r ->
                    val bytes = r.body?.bytes()
                    if (!r.isSuccessful || bytes == null) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }.getOrNull()
            if (img != null) put(key, img) else fail(key)
            img
        }
    }
}

/** Web Mercator: longitude / latitude to 0..1 across the world map. */
private fun wx(lng: Double) = (lng + 180.0) / 360.0
private fun wy(lat: Double): Double {
    val r = Math.toRadians(lat.coerceIn(-85.0, 85.0))
    return (1 - ln(tan(r) + 1 / cos(r)) / PI) / 2
}

/** Map view: world point (0..1) -> screen, as the base fit, then the user's zoom and pan. */
private class View(val w: Float, val h: Float, val c0x: Double, val c0y: Double, val s0: Double, val zoom: Float, val pan: Offset) {
    val scale = s0 * zoom   // screen px per world unit
    fun screen(x: Double, y: Double) = Offset(((x - c0x) * s0 + w / 2).toFloat() * zoom + pan.x, ((y - c0y) * s0 + h / 2).toFloat() * zoom + pan.y)
    fun screen(p: LatLng) = screen(wx(p.lng), wy(p.lat))
    fun worldX(sx: Float) = ((sx - pan.x) / zoom - w / 2) / s0 + c0x
    fun worldY(sy: Float) = ((sy - pan.y) / zoom - h / 2) / s0 + c0y
    /** tile zoom whose pictures are about 256 dp wide on screen */
    fun tileZoom(density: Float) = log2(scale / (256.0 * density)).roundToInt().coerceIn(3, 19)
}

/**
 * The chosen way on the real Google map: Google's map pictures underneath, and on top the route, the railway, every
 * crossing on the way coloured by what you're likely to find, start, destination and your live position. Pinch to
 * zoom, drag to move. The window keeps clear of the status and gesture bars, so Navigate is always fully visible.
 */
@Composable
fun RouteMapDialog(
    plan: Plan, leg: Leg, option: Option?, t: Strings,
    /** the gesture / navigation bar height, measured in the app window: some phones don't report it to pop-ups */
    bottomInset: androidx.compose.ui.unit.Dp,
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

    // decorFitsSystemWindows: the window itself stops at the status and gesture bars, on every phone
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().background(c.headerTop).padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, t.close, tint = Color.White) }
                Text(leg.crossing.label(t), color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                var zoom by remember { mutableFloatStateOf(1f) }
                var pan by remember { mutableStateOf(Offset.Zero) }
                TripMap(plan, leg, option, you, zoom, pan, t, Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { centroid, p, z, _ ->
                        val nz = (zoom * z).coerceIn(0.3f, 40f)
                        // zoom around the fingers
                        pan = (pan - centroid) * (nz / zoom) + centroid + p
                        zoom = nz
                    }
                })
                SmallFloatingActionButton(onClick = { zoom = 1f; pan = Offset.Zero }, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                    Icon(Icons.Rounded.CenterFocusStrong, t.mapFit)
                }
            }
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 12.dp)) {
                status()
                Button(onClick = onNavigate, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.amber, contentColor = Color(0xFF1A1300))) {
                    Icon(Icons.Rounded.Navigation, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text(t.navigateBtn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                // keep Navigate clear of the gesture bar on every phone
                Spacer(Modifier.height(bottomInset.coerceAtLeast(20.dp) + 16.dp))
            }
        }
    }
}

@Composable
private fun TripMap(plan: Plan, leg: Leg, option: Option?, you: LatLng?, zoom: Float, pan: Offset, t: Strings, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current.density
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        // fit the route, start and destination with a margin
        val pts = leg.line + plan.from.pos + plan.to.pos
        val minX = pts.minOf { wx(it.lng) }; val maxX = pts.maxOf { wx(it.lng) }
        val minY = pts.minOf { wy(it.lat) }; val maxY = pts.maxOf { wy(it.lat) }
        val margin = 56 * density
        val s0 = min((w - 2 * margin).coerceAtLeast(1f) / (maxX - minX).coerceAtLeast(1e-9), (h - 2 * margin).coerceAtLeast(1f) / (maxY - minY).coerceAtLeast(1e-9))
            .coerceAtMost(256.0 * density * (1 shl 18))
        val view = View(w, h, (minX + maxX) / 2, (minY + maxY) / 2, s0, zoom, pan)

        // the map pictures on screen now, loaded as needed
        val z = view.tileZoom(density)
        val n = 1 shl z
        val x0 = floor(view.worldX(0f) * n).toInt().coerceIn(0, n - 1); val x1 = floor(view.worldX(w) * n).toInt().coerceIn(0, n - 1)
        val y0 = floor(view.worldY(0f) * n).toInt().coerceIn(0, n - 1); val y1 = floor(view.worldY(h) * n).toInt().coerceIn(0, n - 1)
        val wanted = buildList { for (x in x0..x1) for (y in y0..y1) add(Triple(z, x, y)) }.take(80)
        var loaded by remember { mutableIntStateOf(0) }   // bumps a redraw when a picture arrives
        LaunchedEffect(wanted) {
            for ((tz, tx, ty) in wanted) if (Tiles.get("$tz/$tx/$ty") == null && Tiles.load(tz, tx, ty) != null) loaded++
        }
        TripCanvas(plan, leg, option, you, view, wanted, loaded, t)
    }
}

@Composable
private fun TripCanvas(plan: Plan, leg: Leg, option: Option?, you: LatLng?, view: View, tiles: List<Triple<Int, Int, Int>>, loaded: Int, t: Strings) {
    val c = Phatak.c
    val measurer = rememberTextMeasurer()
    val routeColor = Color(0xFF1A73E8)
    val railColor = Color(0xFF3C4043)
    val label = TextStyle(color = Color(0xFF202124), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val credit = TextStyle(color = Color(0xFF5F6368), fontSize = 10.sp)
    val states = (option?.let { listOf(it.at) + it.later.map { l -> l.at } }) ?: emptyList()

    Canvas(Modifier.fillMaxSize()) {
        if (loaded < 0) return@Canvas   // reads [loaded]: a newly arrived picture redraws the map
        drawRect(Color(0xFFE8EAED))
        // Google's map pictures
        for ((z, x, y) in tiles) {
            val img = Tiles.get("$z/$x/$y") ?: continue
            val n = (1 shl z).toDouble()
            val tl = view.screen(x / n, y / n); val br = view.screen((x + 1) / n, (y + 1) / n)
            drawImage(img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                dstSize = IntSize((br.x - tl.x).roundToInt() + 1, (br.y - tl.y).roundToInt() + 1))
        }
        fun path(line: List<LatLng>) = Path().apply { line.forEachIndexed { i, p -> val o = view.screen(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }

        // the railway the app checked, over Google's own drawing of it
        for (rail in plan.rails) {
            if (rail.size < 2) continue
            val p = path(rail)
            drawPath(p, railColor.copy(alpha = 0.75f), style = Stroke(4.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round))
            drawPath(p, Color.White, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))))
        }
        // the way: Google Maps blue with a white edge
        val way = path(leg.line)
        drawPath(way, Color.White, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(way, routeColor, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        fun dot(p: LatLng, fill: Color, r: Float) { val o = view.screen(p); drawCircle(Color.White, r + 3.dp.toPx(), o); drawCircle(fill, r, o) }
        fun text(s: String, p: LatLng, dy: Float) {
            val layout = measurer.measure(s, label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = (size.width * 0.6f).toInt().coerceAtLeast(1)))
            val o = view.screen(p)
            val maxX = (size.width - layout.size.width - 6f).coerceAtLeast(6f)
            val maxY = (size.height - layout.size.height - 6f).coerceAtLeast(6f)
            val tl = Offset((o.x - layout.size.width / 2f).coerceIn(6f, maxX), (o.y + dy).coerceIn(6f, maxY))
            drawRoundRect(Color.White.copy(alpha = 0.92f), tl - Offset(5.dp.toPx(), 3.dp.toPx()),
                Size(layout.size.width + 10.dp.toPx(), layout.size.height + 6.dp.toPx()), CornerRadius(8.dp.toPx()))
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
            dot(cr.pos, col, 9.dp.toPx())
            text(cr.label(t), cr.pos, -34.dp.toPx())
        }
        dot(plan.from.pos, Color(0xFF188038), 7.dp.toPx())
        text(if (plan.fromIsMe) t.myLocation else plan.from.name, plan.from.pos, 14.dp.toPx())
        dot(plan.to.pos, Color(0xFFD93025), 8.dp.toPx())
        text(plan.to.name, plan.to.pos, 14.dp.toPx())
        // you, live
        you?.let { p ->
            val o = view.screen(p)
            drawCircle(routeColor.copy(alpha = 0.20f), 20.dp.toPx(), o)
            drawCircle(Color.White, 9.dp.toPx(), o)
            drawCircle(routeColor, 6.5.dp.toPx(), o)
        }
        // credit for the map pictures
        val cl = measurer.measure("Map data ©2026 Google", credit)
        drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(4.dp.toPx(), size.height - cl.size.height - 8.dp.toPx()),
            Size(cl.size.width + 8.dp.toPx(), cl.size.height + 4.dp.toPx()), CornerRadius(4.dp.toPx()))
        drawText(cl, topLeft = Offset(8.dp.toPx(), size.height - cl.size.height - 6.dp.toPx()))
    }
}
