@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.phataklive.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.DirectionsRailway
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import com.phataklive.BuildConfig
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phataklive.LocationAccess
import com.phataklive.MAX_RADIUS_KM
import com.phataklive.MIN_RADIUS_KM
import com.phataklive.RADIUS_PRESETS_KM
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import com.phataklive.R
import com.phataklive.MainViewModel
import com.phataklive.Problem
import com.phataklive.UiState
import com.phataklive.data.GateState
import com.phataklive.data.GateStatus
import com.phataklive.data.StationBoard
import com.phataklive.data.TimeSource
import com.phataklive.gate.GateEngine
import com.phataklive.search.LiveSearch
import com.phataklive.update.UpdateChecker
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// ===================================================================================== root

@Composable
fun MainScreen(
    vm: MainViewModel,
    onRequestLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    val s by vm.state.collectAsState()
    val t = remember(s.lang) { Strings(s.lang) }
    var skippedLocation by rememberSaveable { mutableStateOf(false) }

    if (s.access != LocationAccess.GRANTED && !skippedLocation) {
        Onboarding(s, t, onRequestLocation, onOpenAppSettings, onSkip = { skippedLocation = true }, onLang = vm::setLang)
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // a trip notification was tapped
    LaunchedEffect(s.openRoute) { if (s.openRoute) { tab = 3; vm.routeOpened() } }
    // everything under the bottom bar is the blur source: lists scroll behind the frosted bar
    val haze = remember { HazeState() }
    // glass panels are not Surfaces, so text colour comes from here (otherwise it would default to black)
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
        AmbientBackground()
        Column(Modifier.fillMaxSize()) {
            Header(s, t, onRefresh = vm::refresh, onSettings = { showSettings = true })
            AnimatedContent(targetState = tab, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "tab") { which ->
                // pull down to refresh: search tabs repeat their last search, otherwise the nearby scan runs again
                val repeatGate = which == 1 && s.gateQuery.isNotBlank()
                val repeatTrain = which == 2 && s.trainQuery.isNotBlank()
                val repeatRoute = which == 3 && s.routeTo != null
                val refreshing = when {
                    repeatGate -> s.gateSearching
                    repeatTrain -> s.trainSearching
                    repeatRoute -> s.routeStep != null
                    else -> s.loading
                }
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        when {
                            repeatGate -> vm.searchGate(s.gateQuery)
                            repeatTrain -> vm.searchTrain(s.trainQuery)
                            repeatRoute -> vm.planRoute()
                            else -> vm.refresh()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (which) {
                        0 -> NearbyTab(s, t, vm, onRequestLocation, onOpenLocationSettings)
                        1 -> GateSearchTab(s, t, vm)
                        2 -> TrainTab(s, t, vm)
                        else -> RouteTab(s, t, vm)
                    }
                }
            }
        }
        }
        BottomBar(tab, t, haze, Modifier.align(Alignment.BottomCenter)) { tab = it }
    }
    }
    if (showSettings) SettingsSheet(s, t, vm, onFindGate = { showSettings = false; tab = 1 }) { showSettings = false }
    AppUpdate(t)
}

/** List padding that keeps the last card clear of the floating bottom bar. */
@Composable
internal fun listPadding(): PaddingValues {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp + nav)
}

/** Offers a newer release from GitHub (checked once per launch in MainActivity). */
@Composable
private fun AppUpdate(t: Strings) {
    val info by UpdateChecker.available.collectAsState()
    val dl by UpdateChecker.download.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val update = info ?: return
    UpdateDialog(
        update, dl, t,
        onDownload = { scope.launch { UpdateChecker.downloadApk(ctx) } },
        onCancelDownload = { UpdateChecker.cancelDownload() },
        onInstall = { file ->
            if (!UpdateChecker.install(ctx, file)) Toast.makeText(ctx, t.allowInstall, Toast.LENGTH_LONG).show()
        },
        onOpenReleasePage = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl))) },
        onLater = { UpdateChecker.dismiss() },
    )
}

/**
 * Floating frosted-glass tab bar: the list scrolls behind it and shows through blurred, with a fine grain
 * (Android 12+; on Android 10-11 the glass is a dense tint instead).
 */
@Composable
private fun BottomBar(tab: Int, t: Strings, haze: HazeState, modifier: Modifier, onTab: (Int) -> Unit) {
    val c = Phatak.c
    val shape = RoundedCornerShape(26.dp)
    val items = listOf(Triple(t.tabNearby, Icons.Rounded.Traffic, 0), Triple(t.tabSearch, Icons.Rounded.Search, 1),
        Triple(t.tabTrains, Icons.Rounded.DirectionsRailway, 2), Triple(t.tabRoute, Icons.Rounded.AltRoute, 3))
    Row(
        modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().height(66.dp)
            .clip(shape)
            .hazeEffect(haze, HazeStyle(backgroundColor = c.bgBase, tint = HazeTint(c.chrome), blurRadius = 26.dp,
                noiseFactor = 0.10f, fallbackTint = HazeTint(c.chromeFallback)))
            .border(1.dp, glassEdge(), shape)
            .selectableGroup()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (label, icon, i) ->
            val sel = tab == i
            val fg = when { !sel -> c.muted; c.dark -> c.amber; else -> MaterialTheme.colorScheme.onSurface }
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp))
                    .background(if (sel) c.amber.copy(alpha = if (c.dark) 0.14f else 0.32f) else Color.Transparent)
                    .selectable(selected = sel, role = Role.Tab, onClick = { onTab(i) }),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ===================================================================================== onboarding

@Composable
private fun Onboarding(s: UiState, t: Strings, onRequest: () -> Unit, onSettings: () -> Unit, onSkip: () -> Unit, onLang: (String) -> Unit) {
    val c = Phatak.c
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.headerTop, c.headerBottom)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { LangSwitch(s.lang, onLang) }
            Spacer(Modifier.weight(0.6f))
            BarrierIllustration(Modifier.fillMaxWidth().height(170.dp))
            Spacer(Modifier.height(28.dp))
            Text("Phatak Live", color = c.amber, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(t.heroTitle, color = c.onHeader, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(t.heroSub, color = c.onHeaderMuted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            listOf(t.feat1, t.feat2, t.feat3).forEach { f ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = c.amber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(f, color = c.onHeader, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.weight(1f))
            if (s.access == LocationAccess.DENIED || s.access == LocationAccess.BLOCKED) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(t.deniedTitle, color = c.onHeader, style = MaterialTheme.typography.titleMedium)
                        Text(if (s.access == LocationAccess.BLOCKED) t.blockedBody else t.deniedBody, color = c.onHeaderMuted,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Button(
                onClick = if (s.access == LocationAccess.BLOCKED) onSettings else onRequest,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.amber, contentColor = Color(0xFF1A1300)),
            ) {
                Icon(Icons.Rounded.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text(when (s.access) { LocationAccess.BLOCKED -> t.openSettings; LocationAccess.DENIED -> t.tryAgain; else -> t.allowLocation },
                    style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = c.onHeaderMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(t.locationWhy, color = c.onHeaderMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (s.access == LocationAccess.DENIED || s.access == LocationAccess.BLOCKED) {
                TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(t.continueWithout, color = c.onHeader)
                }
            }
        }
    }
}

/** Animated level crossing: posts, a red/white boom that lowers and lifts, and a blinking signal. */
@Composable
private fun BarrierIllustration(modifier: Modifier) {
    val c = Phatak.c
    val anim = rememberInfiniteTransition(label = "barrier")
    val angle by anim.animateFloat(-62f, 0f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "boom")
    val blink by anim.animateFloat(0.25f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "blink")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val ground = h * 0.86f
        // road + rails
        drawRoundRect(Color.White.copy(alpha = 0.10f), Offset(0f, ground), Size(w, h * 0.06f), CornerRadius(8f))
        for (k in 0..9) drawRect(Color.White.copy(alpha = 0.18f), Offset(w * 0.08f + k * w * 0.09f, ground + h * 0.015f), Size(w * 0.05f, h * 0.03f))
        // post
        val px = w * 0.16f
        drawRoundRect(c.amber, Offset(px - 9f, h * 0.30f), Size(18f, ground - h * 0.30f), CornerRadius(6f))
        // signal lamp
        drawCircle(Color(0xFF2A0F12), 22f, Offset(px + 34f, h * 0.24f))
        drawCircle(Color(0xFFFF3B4E).copy(alpha = blink), 14f, Offset(px + 34f, h * 0.24f))
        // boom
        val pivot = Offset(px, h * 0.52f)
        rotate(angle, pivot) {
            val len = w * 0.74f
            drawRoundRect(Color.White, Offset(pivot.x, pivot.y - 11f), Size(len, 22f), CornerRadius(11f))
            var x = pivot.x + 24f
            while (x < pivot.x + len - 30f) {
                drawRoundRect(Color(0xFFE5383B), Offset(x, pivot.y - 11f), Size(len * 0.09f, 22f), CornerRadius(4f))
                x += len * 0.18f
            }
            drawCircle(Color(0xFFFF3B4E).copy(alpha = blink), 9f, Offset(pivot.x + len - 14f, pivot.y))
        }
        drawCircle(c.headerTop, 13f, pivot); drawCircle(c.amber, 8f, pivot)
    }
}

// ===================================================================================== header

@Composable
private fun Header(s: UiState, t: Strings, onRefresh: () -> Unit, onSettings: () -> Unit) {
    val c = Phatak.c
    val shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.headerTop, c.headerBottom)))
            // a warm lamp glow at the right, the same amber as the signal
            .background(Brush.radialGradient(listOf(c.amber.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(Float.POSITIVE_INFINITY, 0f), radius = 700f))
            .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.14f))), shape)
            .statusBarsPadding().padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Text("Phatak", color = c.onHeader, style = MaterialTheme.typography.headlineSmall)
            Text(" Live", color = c.amber, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(8.dp))
            if (s.lastRefreshSec != null && !s.loading) LivePulse(c.open)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Tune, t.settings, tint = c.onHeader) }
            val spin = rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "r")
            IconButton(onClick = onRefresh, enabled = !s.loading) {
                Icon(Icons.Rounded.Refresh, "Refresh", tint = c.onHeader, modifier = if (s.loading) Modifier.rotate(spin.value) else Modifier)
            }
        }
        Row(
            Modifier.padding(top = 8.dp, end = 10.dp).headerGlass()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.LocationOn, null, tint = c.amber, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            val where = when {
                s.locating && s.user == null -> t.locating
                s.user == null -> "—"
                else -> s.address ?: "%.4f, %.4f".format(s.user.lat, s.user.lng)
            }
            Text(where, color = c.onHeader, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            s.accuracyM?.let { Text("  · ${t.accuracy(it.toInt())}", color = c.onHeaderMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
        Text(
            listOfNotNull("${s.radiusKm} km", s.lastRefreshSec?.let { "${t.updated} ${GateEngine.fmt(it)}" }).joinToString("  ·  "),
            color = c.onHeaderMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
    }
}

/** Language, theme and search radius, out of the header: a bottom sheet opened from the header's settings button. */
@Composable
private fun SettingsSheet(s: UiState, t: Strings, vm: MainViewModel, onFindGate: () -> Unit, onClose: () -> Unit) {
    val c = Phatak.c
    // always fully open (no half-height stop), so when the keyboard comes up the whole sheet rides above it by itself
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        // the sheet stops above the keyboard and scrolls, so the custom radius field and its note stay visible while typing
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding()) {
            Text(t.settings, style = MaterialTheme.typography.headlineSmall)
            SettingTitle(t.language, null)
            Segmented(listOf(Triple("English", "English", null), Triple("Hindi", "हिन्दी", null), Triple("Bengali", "বাংলা", null)),
                s.lang, vm::setLang)
            SettingTitle(t.theme, t.themeHint)
            Segmented(listOf(Triple(ThemeMode.AUTO, t.themeAuto, Icons.Rounded.BrightnessAuto),
                Triple(ThemeMode.LIGHT, t.themeLight, Icons.Rounded.LightMode), Triple(ThemeMode.DARK, t.themeDark, Icons.Rounded.DarkMode)),
                s.theme, vm::setTheme)
            SettingTitle(t.radius, t.radiusHint)
            RadiusSetting(s, t, vm, sheet, onFindGate)
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Phatak Live ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.weight(1f))
                Button(onClick = onClose, shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.amber, contentColor = Color(0xFF1A1300))) { Text(t.done) }
            }
        }
    }
}

/**
 * 10 km · 20 km · Custom. Custom opens a number field limited to [MIN_RADIUS_KM]..[MAX_RADIUS_KM]; a bigger
 * number is refused with a yellow note pointing to Find gate, which reaches any town or station in India.
 */
@Composable
private fun RadiusSetting(s: UiState, t: Strings, vm: MainViewModel, sheet: SheetState, onFindGate: () -> Unit) {
    val isCustom = s.radiusKm !in RADIUS_PRESETS_KM
    var customOpen by rememberSaveable { mutableStateOf(isCustom) }
    var text by rememberSaveable { mutableStateOf(if (isCustom) "${s.radiusKm}" else "") }
    val keyboard = LocalSoftwareKeyboardController.current
    val bring = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val km = text.toIntOrNull()
    val valid = km != null && km in MIN_RADIUS_KM..MAX_RADIUS_KM
    val problem = when {
        text.isEmpty() -> null
        km != null && km > MAX_RADIUS_KM -> t.radiusTooBig
        !valid -> t.radiusTooSmall
        else -> null
    }
    fun apply() {
        if (!valid) return
        keyboard?.hide()
        if (km != s.radiusKm) vm.setRadius(km!!)
    }

    Segmented(
        RADIUS_PRESETS_KM.map { Triple("$it", "$it km", null) } +
            Triple("custom", if (isCustom) "${s.radiusKm} km" else t.radiusCustom, Icons.Rounded.Tune),
        if (customOpen) "custom" else "${s.radiusKm}",
    ) { v ->
        if (v == "custom") customOpen = true
        else { customOpen = false; vm.setRadius(v.toInt()) }
    }
    if (!customOpen) return
    LaunchedEffect(focused, imeVisible, problem) {
        if (!focused) return@LaunchedEffect
        if (sheet.currentValue != SheetValue.Expanded) sheet.expand()   // lift the sheet without a manual drag
        delay(300)   // let the keyboard finish sliding in, then scroll the field and its note into view
        if (sheet.currentValue != SheetValue.Expanded) sheet.expand()
        bring.bringIntoView()
    }
    Column(Modifier.bringIntoViewRequester(bring)) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            // digits only (Hindi / Bengali digits become 0-9); 6 digits are plenty to say "too far" without overflowing
            onValueChange = { v -> text = v.mapNotNull { ch -> Character.digit(ch, 10).takeIf { it >= 0 }?.let { '0' + it } }.take(6).joinToString("") },
            modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
            singleLine = true,
            placeholder = { Text(t.radiusCustomHint) },
            suffix = { Text("km") },
            isError = problem != null,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { apply() }),
        )
        Spacer(Modifier.width(10.dp))
        Button(onClick = { apply() }, enabled = valid && km != s.radiusKm, shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Phatak.c.amber, contentColor = Color(0xFF1A1300))) { Text(t.apply) }
    }
    problem?.let { msg -> RadiusWarning(msg, action = if (km != null && km > MAX_RADIUS_KM) t.tabSearch else null, onAction = onFindGate) }
    }
}

/** Yellow note under the custom radius field. */
@Composable
private fun RadiusWarning(text: String, action: String?, onAction: () -> Unit) {
    val c = Phatak.c
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(shape).background(c.amber.copy(alpha = 0.18f))
            .border(1.dp, c.amber, shape).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.WarningAmber, null, tint = c.soon, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        action?.let {
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Rounded.Search, null, tint = c.soon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(it, color = c.soon)
            }
        }
    }
}

@Composable
private fun SettingTitle(title: String, hint: String?) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 22.dp))
    hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Phatak.c.muted, modifier = Modifier.padding(top = 2.dp)) }
    Spacer(Modifier.height(10.dp))
}

/** One choice out of a few, as a glass track with the chosen option lit in signal amber. */
@Composable
internal fun Segmented(options: List<Triple<String, String, ImageVector?>>, selected: String, onSelect: (String) -> Unit) {
    val c = Phatak.c
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp), strong = true).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label, icon) ->
            val sel = value == selected
            val fg = if (sel) Color(0xFF1A1300) else MaterialTheme.colorScheme.onSurface
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (sel) c.amber else Color.Transparent)
                    .selectable(selected = sel, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.let { Icon(it, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** Frosted pill on the navy header: a little white haze with a fine light edge. */
private fun Modifier.headerGlass(): Modifier {
    val pill = RoundedCornerShape(50)
    return clip(pill).background(Color.White.copy(alpha = 0.11f)).border(1.dp, Color.White.copy(alpha = 0.14f), pill)
}

@Composable
private fun LangSwitch(lang: String, onLang: (String) -> Unit) {
    val c = Phatak.c
    Row(Modifier.headerGlass().padding(3.dp)) {
        listOf("English" to "EN", "Hindi" to "हि", "Bengali" to "বা").forEach { (l, label) ->
            val sel = lang == l
            Text(label, color = if (sel) Color(0xFF1A1300) else c.onHeader, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) c.amber else Color.Transparent)
                    .clickable { onLang(l) }.padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun LivePulse(color: Color) {
    val a by rememberInfiniteTransition(label = "pulse").animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(9.dp).clip(CircleShape).background(color.copy(alpha = a)))
}

// ===================================================================================== nearby

@Composable
internal fun nowSec(): Long {
    val v by produceState(System.currentTimeMillis() / 1000) { while (true) { delay(10_000); value = System.currentTimeMillis() / 1000 } }
    return v
}

@Composable
private fun NearbyTab(s: UiState, t: Strings, vm: MainViewModel, onRequestLocation: () -> Unit, onOpenLocationSettings: () -> Unit) {
    val now = nowSec()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (s.access != LocationAccess.GRANTED) item {
            Banner(Icons.Rounded.LocationOff, t.deniedTitle, t.deniedBody, t.allowLocation, onRequestLocation)
        }
        if (s.gpsOff) item { Banner(Icons.Rounded.LocationOff, t.gpsOff, t.gpsOffBody, t.turnOn, onOpenLocationSettings) }
        if (s.offline) item { Banner(Icons.Rounded.CloudOff, null, t.offline, null, null) }
        item { SectionTitle(t.gatesNear, if (s.loading) t.searching else "${s.gates.size} · ${s.radiusKm} km") }
        when {
            s.loading && s.gates.isEmpty() -> items(2) { Skeleton(150.dp) }
            s.gates.isEmpty() && s.user != null -> item { EmptyNote(t.noGates) }
            else -> {
                s.gates.firstOrNull()?.let { g -> item(key = "hero") { GateHero(g, t, now) } }
                items(s.gates.drop(1), key = { "g" + it.gate.pos + it.gate.name }) { GateCard(it, t, now) }
            }
        }
        if (s.boards.isNotEmpty()) item { SectionTitle(t.stations, "${s.stations.size}") }
        items(s.boards, key = { it.station.code }) { BoardCard(it, t) }
        item { InfoBox(t) }
    }
}

@Composable
internal fun Banner(icon: ImageVector, title: String?, body: String, action: String?, onAction: (() -> Unit)?) {
    val c = Phatak.c
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp), tint = c.soon).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.soon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null && onAction != null) FilledTonalButton(onClick = onAction) { Text(action) }
    }
}

/** "How gate times work": the timing rule and LIVE / TIMETABLE explained, in a tidy info box. */
@Composable
private fun InfoBox(t: Strings) {
    val c = Phatak.c
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).glass(RoundedCornerShape(22.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = c.amber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(t.infoTitle, style = MaterialTheme.typography.titleMedium)
        }
        listOf(
            Icons.Rounded.Schedule to t.info1,
            Icons.Rounded.Sensors to t.info2,
            Icons.Rounded.WarningAmber to t.info3,
        ).forEach { (icon, text) ->
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                Icon(icon, null, tint = c.muted, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, sub: String) {
    Row(Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Text(sub, style = MaterialTheme.typography.bodySmall, color = Phatak.c.muted)
    }
}

@Composable
internal fun EmptyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).padding(16.dp))
}

@Composable
internal fun Skeleton(height: androidx.compose.ui.unit.Dp) {
    val a by rememberInfiniteTransition(label = "sk").animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxWidth().height(height).graphicsLayer { alpha = a }.glass(RoundedCornerShape(22.dp)))
}

internal fun problemText(p: Problem, t: Strings): String = when (p) {
    Problem.WaitingForLocation -> t.waitingLocation
    is Problem.NoGateFound -> t.noGateFound(p.query)
    is Problem.NoTrainFound -> t.noTrainFound(p.query)
    is Problem.PlaceNotFound -> t.placeNotFound(p.query)
    Problem.RouteFailed -> t.routeFailed
    Problem.NoCrossings -> t.noCrossings
    Problem.TooFar -> t.tooFar(com.phataklive.route.CrossingPlanner.MAX_TRIP_KM.toInt())
}

// ===================================================================================== gates

internal data class Look(val fg: Color, val bg: Color, val label: String)

@Composable
internal fun look(st: GateState, t: Strings): Look {
    val c = Phatak.c
    return when (st) {
        GateState.CLOSED -> Look(c.closed, c.closedBg, t.closed)
        GateState.CLOSING_SOON -> Look(c.soon, c.soonBg, t.closingSoon)
        GateState.OPEN -> Look(c.open, c.openBg, t.open)
        GateState.UNKNOWN -> Look(c.muted, MaterialTheme.colorScheme.surfaceVariant, "?")
    }
}

/** "Opens in 4 min" / "Closes in 12 min" / "Clear for the next 3 hours". */
private fun countdown(g: GateStatus, t: Strings, now: Long): Pair<String, String> = when {
    g.state == GateState.CLOSED && g.windowOpen != null -> t.opensIn to t.duration(((g.windowOpen - now) / 60).toInt().coerceAtLeast(0))
    g.windowClose != null -> t.closesIn to t.duration(((g.windowClose - now) / 60).toInt().coerceAtLeast(0))
    else -> "" to t.clearFor
}

@Composable
internal fun StateLight(color: Color, pulsing: Boolean, size: androidx.compose.ui.unit.Dp) {
    val scale by rememberInfiniteTransition(label = "light").animateFloat(1f, 1.9f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "s")
    val alpha by rememberInfiniteTransition(label = "lighta").animateFloat(0.45f, 0f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "a")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 2)) {
        if (pulsing) Box(Modifier.size(size).scale(scale).clip(CircleShape).background(color.copy(alpha = alpha)))
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

@Composable
private fun GateHero(g: GateStatus, t: Strings, now: Long) {
    val l = look(g.state, t)
    val c = Phatak.c
    val (label, value) = countdown(g, t, now)
    GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, tint = l.fg) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StateLight(l.fg, pulsing = g.state != GateState.OPEN, size = 14.dp)
                Text(l.label, color = l.fg, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                if (g.window.isNotEmpty()) SourceChip(g.verified, t)
            }
            Text(g.gate.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("%.1f km".format(g.distanceKm) + (g.nearestStation?.let { " · ${t.near} ${it.name}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium, color = c.muted)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
                    Text(value, style = MaterialTheme.typography.headlineLarge, color = l.fg)
                }
                OpenInMaps(g, t)
            }
            if (g.windowClose != null && g.windowOpen != null) {
                Spacer(Modifier.height(12.dp))
                WindowBar(g.windowClose, g.windowOpen, now, l.fg, t)
            }
            if (g.window.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = l.fg.copy(alpha = 0.15f))
                g.window.take(3).forEach { p -> TrainLine(p.trainNo, p.trainName, p.passEpochSec, p.delayMin, p.timeSource, t) }
            }
        }
    }
}

/** Timeline of the closing window with a "now" marker: closes ──[■■■■]── reopens. */
@Composable
private fun WindowBar(close: Long, open: Long, now: Long, color: Color, t: Strings) {
    val span = (open - close).coerceAtLeast(60).toFloat()
    val start = minOf(now, close) - 120
    val end = maxOf(open, now) + 120
    val total = (end - start).toFloat()
    Column {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val w = size.width; val h = size.height
            drawRoundRect(color.copy(alpha = 0.15f), Offset(0f, h * 0.3f), Size(w, h * 0.4f), CornerRadius(h))
            val x0 = (close - start) / total * w
            drawRoundRect(color, Offset(x0, h * 0.15f), Size(span / total * w, h * 0.7f), CornerRadius(h))
            val xn = ((now - start) / total * w).coerceIn(0f, w)
            drawCircle(Color.White, h * 0.55f, Offset(xn, h / 2)); drawCircle(color, h * 0.35f, Offset(xn, h / 2))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("${t.closes} ${GateEngine.fmt(close)}", style = MaterialTheme.typography.bodySmall, color = Phatak.c.muted, modifier = Modifier.weight(1f))
            Text("${t.reopens} ${GateEngine.fmt(open)}", style = MaterialTheme.typography.bodySmall, color = Phatak.c.muted)
        }
    }
}

@Composable
internal fun TrainLine(no: String, name: String, time: Long, delayMin: Int, src: TimeSource, t: Strings) {
    val c = Phatak.c
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.DirectionsRailway, null, tint = c.muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("$no $name", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("~${GateEngine.fmt(time)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        DelayChip(delayMin, src, t)
    }
}

@Composable
internal fun DelayChip(delayMin: Int, src: TimeSource, t: Strings) {
    val c = Phatak.c
    val (fg, txt) = when {
        src == TimeSource.SCHEDULED -> c.muted to t.timetableOnly
        delayMin > 0 -> c.closed to t.delayed(delayMin)
        else -> c.open to t.onTime
    }
    Text(txt, color = fg, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(fg.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 3.dp))
}

@Composable
internal fun SourceChip(verified: Boolean, t: Strings) {
    val c = Phatak.c
    val col = if (verified) c.live else c.muted
    Row(Modifier.clip(RoundedCornerShape(50)).background(col.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (verified) { LivePulse(col); Spacer(Modifier.width(5.dp)) }
        Text(if (verified) t.live else t.scheduled, color = col, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun OpenInMaps(g: GateStatus, t: Strings) {
    val ctx = LocalContext.current
    FilledTonalButton(onClick = {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${g.gate.pos.lat},${g.gate.pos.lng}" +
            (g.gate.placeId?.let { "&query_place_id=$it" } ?: ""))))
    }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Icon(Icons.Rounded.Directions, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(t.navigate)
    }
}

@Composable
private fun GateCard(g: GateStatus, t: Strings, now: Long) {
    val l = look(g.state, t)
    val c = Phatak.c
    val (label, value) = countdown(g, t, now)
    var expanded by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth(), radius = 22.dp, onClick = { expanded = !expanded }) {
        Row {
            Column(Modifier.padding(14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(l.fg))
                    Spacer(Modifier.width(8.dp))
                    Text(l.label, color = l.fg, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(l.bg).padding(horizontal = 9.dp, vertical = 3.dp))
                    Spacer(Modifier.width(8.dp))
                    if (g.window.isNotEmpty()) SourceChip(g.verified, t)
                    Spacer(Modifier.weight(1f))
                    Text("%.1f km".format(g.distanceKm), style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Text(g.gate.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(if (label.isNotEmpty()) "$label $value" else value, g.nearestStation?.let { "${t.near} ${it.name}" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = c.muted)
                AnimatedVisibility(expanded) {
                    Column {
                        if (g.windowClose != null && g.windowOpen != null) { Spacer(Modifier.height(10.dp)); WindowBar(g.windowClose, g.windowOpen, now, l.fg, t) }
                        g.window.take(3).forEach { p -> TrainLine(p.trainNo, p.trainName, p.passEpochSec, p.delayMin, p.timeSource, t) }
                        Spacer(Modifier.height(8.dp)); OpenInMaps(g, t)
                    }
                }
            }
        }
    }
}

// ===================================================================================== stations

@Composable
private fun BoardCard(b: StationBoard, t: Strings) {
    val c = Phatak.c
    var expanded by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth(), radius = 22.dp, onClick = { expanded = !expanded }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DirectionsRailway, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.station.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${b.station.code} · %.1f km".format(b.station.distanceKm), style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                SourceChip(b.liveAvailable, t)
            }
            val rows = if (expanded) b.trains else b.trains.take(3)
            if (rows.isEmpty()) Text(t.noTrain3h, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(top = 8.dp))
            rows.forEach { r ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(GateEngine.fmt(r.timeEpochSec), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(78.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${r.trainNo} ${r.trainName}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val extra = listOfNotNull(if (!r.halts) t.noStop else null, r.platform).joinToString(" · ")
                        if (extra.isNotEmpty()) Text(extra, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    DelayChip(r.delayMin, r.source, t)
                }
            }
            if (!expanded && b.trains.size > 3) Text("+${b.trains.size - 3} ${t.more}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

// ===================================================================================== search tabs

@Composable
private fun SearchField(
    id: String, hint: String, busy: Boolean, initial: String, onClear: () -> Unit,
    onType: (String) -> Unit = {}, onSearch: (String) -> Unit,
) {
    // keyed by a fixed id, so switching language doesn't wipe what the user typed
    var q by rememberSaveable(id) { mutableStateOf(initial) }
    // a picked suggestion puts its name in the box
    LaunchedEffect(initial) { if (initial.isNotEmpty()) q = initial }
    OutlinedTextField(
        value = q, modifier = Modifier.fillMaxWidth(), singleLine = true,
        onValueChange = { q = it; onType(it); if (it.isBlank()) onClear() },   // emptied by hand: results go too
        placeholder = { Text(hint) }, shape = RoundedCornerShape(18.dp),
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            if (q.isNotEmpty()) IconButton(onClick = { q = ""; onClear() }) { Icon(Icons.Rounded.Close, null) }
            else if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { if (q.isNotBlank()) onSearch(q) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Phatak.c.glassStrong, unfocusedContainerColor = Phatak.c.glassStrong,
            unfocusedBorderColor = Phatak.c.cardBorder, focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun Suggestions(list: List<LiveSearch.Suggestion>, t: Strings, onPick: (LiveSearch.Suggestion) -> Unit) {
    val c = Phatak.c
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).glass(RoundedCornerShape(18.dp), strong = true)) {
        list.forEachIndexed { i, sug ->
            if (i > 0) HorizontalDivider(color = c.cardBorder)
            val (icon, title, sub, km) = when (sug) {
                is LiveSearch.Suggestion.GatePlace -> Quad(Icons.Rounded.Traffic, sug.gate.name, sug.address, sug.distanceKm)
                is LiveSearch.Suggestion.NearStation -> Quad(Icons.Rounded.DirectionsRailway, t.gatesNearStation(sug.station.name), sug.station.code, sug.distanceKm)
            }
            Row(Modifier.fillMaxWidth().clickable { onPick(sug) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = c.muted, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Text(if (km < 10) "%.1f km".format(km) else "%.0f km".format(km), style = MaterialTheme.typography.labelMedium, color = c.muted)
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun GateSearchTab(s: UiState, t: Strings, vm: MainViewModel) {
    val now = nowSec()
    val focus = LocalFocusManager.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(t.searchGateTitle, style = MaterialTheme.typography.headlineSmall)
            Text(t.searchGateSub, style = MaterialTheme.typography.bodyMedium, color = Phatak.c.muted, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            SearchField("gate", t.gateHint, s.gateSearching, s.gateQuery, onClear = vm::clearGateSearch, onType = vm::onGateTyping) { q ->
                focus.clearFocus(); vm.setGateQuery(q); vm.searchGate(q)
            }
            if (s.gateSuggestions.isNotEmpty()) Suggestions(s.gateSuggestions, t) { focus.clearFocus(); vm.pickSuggestion(it) }
        }
        s.gateProblem?.let { item { EmptyNote(problemText(it, t)) } }
        if (s.gateSearching && s.gateResults == null) items(2) { Skeleton(110.dp) }
        s.gateResults?.let { list ->
            list.firstOrNull()?.let { item(key = "hero") { GateHero(it, t, now) } }
            items(list.drop(1), key = { "s" + it.gate.pos + it.gate.name }) { GateCard(it, t, now) }
        }
    }
}

@Composable
private fun TrainTab(s: UiState, t: Strings, vm: MainViewModel) {
    val c = Phatak.c
    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(t.trainTitle, style = MaterialTheme.typography.headlineSmall)
            Text(t.trainSub, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            SearchField("train", t.trainHint, s.trainSearching, "", onClear = vm::clearTrain) { vm.searchTrain(it) }
        }
        s.trainProblem?.let { item { EmptyNote(problemText(it, t)) } }
        if (s.trainSearching && s.train == null) item { Skeleton(180.dp) }
        s.train?.let { tr ->
            if (!tr.exactMatch && tr.alternatives.isNotEmpty()) item {
                Column {
                    Text(t.didYouMean, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        tr.alternatives.forEach { (no, name) ->
                            OutlinedButton(onClick = { vm.searchTrain(no) }, shape = RoundedCornerShape(50)) {
                                Text("$no $name", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth(), radius = 26.dp) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr.no, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp))
                            Spacer(Modifier.weight(1f))
                            SourceChip(tr.live, t)
                        }
                        Text(tr.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
                        val (col, txt) = when {
                            !tr.live -> c.muted to t.timetableOnly
                            tr.delayMin > 0 -> c.closed to t.delayed(tr.delayMin)
                            else -> c.open to t.onTime
                        }
                        Text(txt, style = MaterialTheme.typography.headlineLarge, color = col, modifier = Modifier.padding(top = 6.dp))
                        tr.lastSeen?.takeIf { tr.live }?.let {
                            Text("${t.lastSeen} $it", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(t.nextStops, style = MaterialTheme.typography.labelMedium, color = c.muted)
                        tr.next.forEachIndexed { i, n ->
                            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(if (i == 0) c.amber else c.cardBorder))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(n.station, style = MaterialTheme.typography.titleMedium)
                                    Text(listOfNotNull(n.code, n.platform).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = c.muted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(GateEngine.fmt(n.est), style = MaterialTheme.typography.titleMedium)
                                    if (n.est != n.sched) Text(GateEngine.fmt(n.sched), style = MaterialTheme.typography.bodySmall, color = c.muted,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

