package com.phataklive.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phataklive.MainViewModel
import com.phataklive.RouteField
import com.phataklive.UiState
import com.phataklive.data.Geo
import com.phataklive.gate.GateEngine
import com.phataklive.net.GoogleDirections
import com.phataklive.route.AtArrival
import com.phataklive.route.Crossing
import com.phataklive.route.Leg
import com.phataklive.route.CrossingKind
import com.phataklive.route.CrossingPlanner
import com.phataklive.route.Option
import com.phataklive.route.Place
import com.phataklive.route.Plan
import com.phataklive.route.Trip

/** Minutes for display: travel rounds to the nearest minute, a wait rounds up (never promise a gate early). */
private fun roundMin(sec: Int) = (sec + 30) / 60
private fun waitMin(sec: Int) = (sec + 59) / 60

/** A crossing's name for people: its name on the map, or "Rail crossing near <station>". */
fun Crossing.label(t: Strings): String = when {
    named -> name
    name.isNotEmpty() -> t.crossingNear(name)
    else -> t.railCrossing
}

/**
 * Opens turn-by-turn in the Google Maps app (or any app that handles the link): Google's own route with no stop, or
 * with the stop just past the chosen crossing that the plan checked, so Maps drives exactly the way the app showed.
 */
private fun openNavigation(ctx: Context, plan: Plan, leg: Leg) {
    val uri = GoogleDirections.navigationUri(if (plan.fromIsMe) null else plan.from.pos, leg.navStop, plan.to.pos, plan.mode)
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
    } catch (e: ActivityNotFoundException) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

/**
 * Route tab: from (my location, a typed place, or a gate I'm at) to a destination; every gate, overbridge and
 * underpass on the way ranked by the whole trip time including the wait at each gate. Navigate starts trip alerts and
 * hands over to Google Maps; Map shows Google's map inside the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouteTab(s: UiState, t: Strings, vm: MainViewModel) {
    val c = Phatak.c
    nowSec()   // recompose every 10 s so countdown text stays fresh
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val trip by Trip.state.collectAsState()
    val plan = s.routePlan
    // while a trip of this plan runs, its live-position ranking is the freshest
    val tripOfPlan = trip?.takeIf { plan != null && it.plan.createdSec == plan.createdSec }
    val options = tripOfPlan?.options?.takeIf { it.isNotEmpty() } ?: s.routeOptions
    var mapCrossing by remember { mutableStateOf<Crossing?>(null) }
    // a few ways only: the best, and one or two others
    // what is typed in the two boxes; a picked place (or "I'm at" gate) fills its box
    var fromText by rememberSaveable { mutableStateOf(s.routeFrom?.name ?: "") }
    var toText by rememberSaveable { mutableStateOf(s.routeTo?.name ?: "") }
    LaunchedEffect(s.routeFrom) { s.routeFrom?.let { fromText = it.name } }
    LaunchedEffect(s.routeTo) { s.routeTo?.let { toText = it.name } }
    var pending by remember { mutableStateOf<Crossing?>(null) }

    fun go(crossing: Crossing) {
        val p = s.routePlan ?: return
        val leg = p.legs.firstOrNull { it.crossing == crossing } ?: return
        Trip.start(ctx, p, crossing, options)
        openNavigation(ctx, p, leg)
    }
    // Android 13+: notifications need permission; asked once, when the first trip starts
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(ctx, t.allowNotifications, Toast.LENGTH_LONG).show()
        pending?.let { go(it) }
        pending = null
    }
    fun navigate(crossing: Crossing) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pending = crossing
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else go(crossing)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(t.routeTitle, style = MaterialTheme.typography.headlineSmall)
            Text(t.routeSub, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            val mine = listOfNotNull(t.myLocation, s.address?.takeIf { it.isNotBlank() }).joinToString(" · ")
            PlaceBox(fromText, t.from, mine, if (s.routeFrom == null) Icons.Rounded.MyLocation else Icons.Rounded.Place,
                onType = { fromText = it; vm.onRouteTyping(RouteField.FROM, it) },
                onSubmit = { focus.clearFocus(); vm.submitRoutePlace(RouteField.FROM, it) },
                onClear = { fromText = ""; vm.pickRoutePlace(RouteField.FROM, null) })
            if (s.routeSuggestFor == RouteField.FROM && s.routeSuggest.isNotEmpty())
                PlaceSuggestions(s.routeSuggest, s) { focus.clearFocus(); vm.pickRoutePlace(RouteField.FROM, it) }
            // "I'm at" a gate found nearby: the usual case at a closed gate
            if (s.routeFrom == null && s.gates.isNotEmpty()) {
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t.atGate, style = MaterialTheme.typography.labelMedium, color = c.muted, modifier = Modifier.align(Alignment.CenterVertically))
                    s.gates.take(3).forEach { g ->
                        val l = look(g.state, t)
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).glass(RoundedCornerShape(50), strong = true)
                                .clickable { vm.pickRoutePlace(RouteField.FROM, Place(g.gate.name, null, g.gate.pos)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(l.fg))
                            Spacer(Modifier.width(6.dp))
                            Text(g.gate.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp))
                        }
                    }
                }
            }
        }
        item {
            PlaceBox(toText, t.to, t.toHint, Icons.Rounded.Flag,
                onType = { toText = it; vm.onRouteTyping(RouteField.TO, it) },
                onSubmit = { focus.clearFocus(); vm.submitRoutePlace(RouteField.TO, it) },
                onClear = { toText = ""; vm.clearRouteTo() })
            if (s.routeSuggestFor == RouteField.TO && s.routeSuggest.isNotEmpty())
                PlaceSuggestions(s.routeSuggest, s) { focus.clearFocus(); vm.pickRoutePlace(RouteField.TO, it) }
        }
        item { ModePicker(s.routeMode, t, vm::setRouteMode) }
        item {
            val busy = s.routeStep != null
            Button(
                onClick = { focus.clearFocus(); vm.findBest(fromText, toText) }, enabled = (s.routeTo != null || toText.isNotBlank()) && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.amber, contentColor = Color(0xFF1A1300)),
            ) {
                if (busy) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1A1300)); Spacer(Modifier.width(10.dp)) }
                else { Icon(Icons.Rounded.AltRoute, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
                Text(when (s.routeStep) {
                    CrossingPlanner.Step.CROSSINGS -> t.stepCrossings
                    CrossingPlanner.Step.ROUTES -> t.stepRoutes
                    CrossingPlanner.Step.TRAINS -> t.stepTrains
                    null -> t.findBest
                }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trip?.let { tr ->
            item { Banner(Icons.Rounded.NotificationsActive, t.tripOn(tr.crossing.label(t)), t.tripOnSub, t.stopAlerts) { Trip.stop(ctx) } }
        }
        s.routeProblem?.let { item { EmptyNote(problemText(it, t)) } }
        if (s.routeStep != null) items(3) { Skeleton(150.dp) }
        if (plan != null && s.routeStep == null) {
            if (plan.sameSide) item { EmptyNote(t.sameSide) }
            items(options.take(3), key = { "r" + it.leg.crossing.pos }) { o ->
                OptionCard(o, best = o === options.first(), t = t,
                    tripOn = tripOfPlan?.crossing == o.leg.crossing,
                    onNavigate = { navigate(o.leg.crossing) }, onMap = { mapCrossing = o.leg.crossing })
            }
            if (options.isNotEmpty()) item {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.WarningAmber, null, tint = c.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.routeSafety, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }
        }
    }

    val shown = mapCrossing
    if (shown != null && plan != null) {
        val o = options.firstOrNull { it.leg.crossing == shown }
        val stop = plan.legs.firstOrNull { it.crossing == shown }?.navStop
        RouteMapDialog(
            url = GoogleDirections.webUrl(listOfNotNull(plan.from.pos, stop, plan.to.pos), plan.mode),
            title = shown.label(t), t = t,
            onNavigate = { mapCrossing = null; navigate(shown) },
            onClose = { mapCrossing = null },
        ) { if (o != null) StatusLine(o, t) }
    }
}

// ------------------------------------------------------------------ inputs

@Composable
private fun PlaceBox(
    q: String, label: String, hint: String, icon: ImageVector,
    onType: (String) -> Unit, onSubmit: (String) -> Unit, onClear: () -> Unit,
) {
    OutlinedTextField(
        value = q, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), singleLine = true,
        onValueChange = { onType(it); if (it.isBlank()) onClear() },
        label = { Text(label) }, placeholder = { Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shape = RoundedCornerShape(18.dp),
        leadingIcon = { Icon(icon, null) },
        trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, null) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { if (q.isNotBlank()) onSubmit(q) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Phatak.c.glassStrong, unfocusedContainerColor = Phatak.c.glassStrong,
            unfocusedBorderColor = Phatak.c.cardBorder, focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun PlaceSuggestions(list: List<Place>, s: UiState, onPick: (Place) -> Unit) {
    val c = Phatak.c
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).glass(RoundedCornerShape(18.dp), strong = true)) {
        list.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(color = c.cardBorder)
            Row(Modifier.fillMaxWidth().clickable { onPick(p) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, tint = c.muted, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    p.sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                s.user?.let { u ->
                    val km = Geo.km(u, p.pos)
                    Text(if (km < 10) "%.1f km".format(km) else "%.0f km".format(km), style = MaterialTheme.typography.labelMedium, color = c.muted)
                }
            }
        }
    }
}

@Composable
private fun ModePicker(mode: GoogleDirections.Mode, t: Strings, onPick: (GoogleDirections.Mode) -> Unit) {
    val c = Phatak.c
    val modes = listOf(
        Triple(GoogleDirections.Mode.TWO_WHEELER, t.modeBike, Icons.Rounded.TwoWheeler),
        Triple(GoogleDirections.Mode.CAR, t.modeCar, Icons.Rounded.DirectionsCar),
        Triple(GoogleDirections.Mode.WALK, t.modeWalk, Icons.Rounded.DirectionsWalk),
    )
    Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp), strong = true).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        modes.forEach { (m, label, icon) ->
            val sel = m == mode
            val fg = if (sel) Color(0xFF1A1300) else MaterialTheme.colorScheme.onSurface
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (sel) c.amber else Color.Transparent)
                    .selectable(selected = sel, role = Role.RadioButton, onClick = { onPick(m) }).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                Text(label, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ------------------------------------------------------------------ results

private fun kindIcon(k: CrossingKind) = when (k) {
    CrossingKind.GATE -> Icons.Rounded.Traffic
    CrossingKind.OVERBRIDGE -> Icons.Rounded.AltRoute
    CrossingKind.UNDERPASS -> Icons.Rounded.Subway
    CrossingKind.FOOTBRIDGE -> Icons.Rounded.DirectionsWalk
}

private fun kindLabel(k: CrossingKind, t: Strings) = when (k) {
    CrossingKind.GATE -> t.kindGate
    CrossingKind.OVERBRIDGE -> t.kindOver
    CrossingKind.UNDERPASS -> t.kindUnder
    CrossingKind.FOOTBRIDGE -> t.kindFoot
}

@Composable
private fun stateColor(o: Option): Color = if (o.at == AtArrival.CLOSED) Phatak.c.closed else Phatak.c.open

/** What you find at the crossing: open (and when it closes next), closed with the wait, or no gate at all. */
@Composable
private fun StatusLine(o: Option, t: Strings) {
    val col = stateColor(o)
    val text = when (o.at) {
        AtArrival.CLOSED -> t.closedWait(waitMin(o.waitSec), o.opensAt?.let { GateEngine.fmt(it) } ?: "?")
        AtArrival.OPEN -> listOfNotNull(t.openOnArrival, o.nextClose?.let { t.nextCloseAt(GateEngine.fmt(it)) }).joinToString(" · ")
        AtArrival.NO_GATE -> t.neverCloses
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(col))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = col, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionCard(o: Option, best: Boolean, t: Strings, tripOn: Boolean, onNavigate: () -> Unit, onMap: () -> Unit) {
    val c = Phatak.c
    val col = stateColor(o)
    val cr = o.leg.crossing
    GlassCard(Modifier.fillMaxWidth(), radius = if (best) 26.dp else 22.dp, tint = if (best) col else null) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(col.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(kindIcon(cr.kind), null, tint = col)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(cr.label(t), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(kindLabel(cr.kind, t), style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                if (best) Text(t.best, color = Color(0xFF1A1300), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(c.amber).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Text(t.total(roundMin(o.totalSec)), style = if (best) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(4.dp))
            StatusLine(o, t)
            Text(t.reachIn(roundMin(o.toCrossingSec), GateEngine.fmt(o.etaSec)), style = MaterialTheme.typography.bodySmall, color = c.muted,
                modifier = Modifier.padding(top = 4.dp))
            if (o.googleWay || (cr.kind == CrossingKind.GATE && o.trains.isNotEmpty()) || tripOn) {
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (tripOn) Pill(t.tripAlertsOn, c.amber, Icons.Rounded.NotificationsActive)
                    if (o.googleWay) Pill(t.googleWay, MaterialTheme.colorScheme.primary, null)
                    if (cr.kind == CrossingKind.GATE && o.trains.isNotEmpty()) SourceChip(o.live, t)
                }
            }
            if (o.at == AtArrival.CLOSED) o.trains.take(2).forEach { p -> TrainLine(p.trainNo, p.trainName, p.passEpochSec, p.delayMin, p.timeSource, t) }
            // Google Maps shows our stop just past the crossing as a stop: say so, so nobody pulls over there
            if (o.leg.navStop != null) Text(t.navStopNote, style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNavigate, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = c.amber, contentColor = Color(0xFF1A1300))) {
                    Icon(Icons.Rounded.Navigation, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(t.navigateBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(onClick = onMap, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Map, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(t.mapBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, col: Color, icon: ImageVector?) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(col.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(it, null, tint = col, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, color = col, style = MaterialTheme.typography.labelSmall)
    }
}
