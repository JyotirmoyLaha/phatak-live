package com.phataklive.route

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.phataklive.MainActivity
import com.phataklive.R
import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import com.phataklive.data.PhatakDb
import com.phataklive.gate.GateEngine
import com.phataklive.ui.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The trip being watched, shared by the trip service (writes) and the Route tab (reads). */
object Trip {
    data class State(
        val plan: Plan,
        /** the crossing the user set off for */
        val crossing: Crossing,
        val options: List<Option>,
        val here: LatLng?,
        val passed: Boolean,
        val startedSec: Long,
        val updatedSec: Long,
    ) {
        val chosen: Option? get() = options.firstOrNull { it.leg.crossing == crossing }
    }

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    internal fun update(s: State?) { _state.value = s }

    /** Starts watching the trip through [crossing]: an ongoing notification, updated from GPS until the crossing is behind. */
    fun start(ctx: Context, plan: Plan, crossing: Crossing, options: List<Option>) {
        val now = System.currentTimeMillis() / 1000
        _state.value = State(plan, crossing, options, null, false, now, now)
        ContextCompat.startForegroundService(ctx, Intent(ctx, TripService::class.java))
    }

    fun stop(ctx: Context) {
        _state.value = null
        ctx.stopService(Intent(ctx, TripService::class.java))
    }
}

/**
 * Foreground service (type location) that keeps a trip's crossing status up to date while the user drives with
 * Google Maps: every [TICK_MS] it re-ranks the crossings from the phone's position, refreshes live trains every few
 * minutes, and alerts when the chosen gate will be closed on arrival or another way becomes clearly faster.
 * It ends by itself once the crossing is behind, near the destination, or after [MAX_TRIP_SEC].
 */
class TripService : Service() {

    companion object {
        const val ACTION_STOP = "com.phataklive.trip.STOP"
        const val EXTRA_OPEN_ROUTE = "open_route"
        private const val CH_TRIP = "trip"
        private const val CH_ALERT = "trip_alert"
        private const val ID_TRIP = 41
        private const val ID_ALERT = 42
        private const val TICK_MS = 30_000L
        private const val LIVE_EVERY_SEC = 180
        private const val MAX_TRIP_SEC = 2 * 3600
        /** passed: came within this of the crossing, then moved [PASSED_AWAY_KM] further away */
        private const val PASSED_NEAR_KM = 0.15
        private const val PASSED_AWAY_KM = 0.25
        /** another way is suggested only when it saves at least this */
        private const val SWITCH_SAVES_SEC = 180
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private lateinit var planner: CrossingPlanner
    @Volatile private var here: LatLng? = null
    private var minKm = Double.MAX_VALUE
    private var alertedClosed = false
    private var alertedSwitch: String? = null
    /** the trip the counters above belong to (a new Navigate replaces the trip) */
    private var tripStarted = 0L
    private val listener = LocationListener { l -> here = LatLng(l.latitude, l.longitude) }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun strings() = Strings(getSharedPreferences("settings", Context.MODE_PRIVATE).getString("lang", "English")!!)

    override fun onCreate() {
        super.onCreate()
        val db = PhatakDb.get(this)
        planner = CrossingPlanner(db, GateEngine(db))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { Trip.update(null); finish(); return START_NOT_STICKY }
        val st = Trip.state.value
        if (st == null) { finish(); return START_NOT_STICKY }
        if (st.startedSec != tripStarted) {
            tripStarted = st.startedSec; minKm = Double.MAX_VALUE; alertedClosed = false; alertedSwitch = null
        }
        channels()
        try {
            ServiceCompat.startForeground(this, ID_TRIP, ongoing(st, strings()),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        } catch (e: Exception) {
            // not allowed now (location permission withdrawn, or started from the background): no trip alerts
            Trip.update(null); stopSelf(); return START_NOT_STICKY
        }
        listen()
        if (loop == null) loop = scope.launch {
            while (isActive) {
                runCatching { tick() }
                delay(TICK_MS)
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun listen() {
        val lm = getSystemService(LocationManager::class.java)
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!ok) return
        lm.removeUpdates(listener)
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            if (runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)) {
                runCatching { lm.requestLocationUpdates(p, 5_000L, 10f, listener, Looper.getMainLooper()) }
                if (here == null) runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { here = LatLng(it.latitude, it.longitude) }
            }
        }
    }

    private suspend fun tick() {
        val st = Trip.state.value ?: return finish()
        val now = System.currentTimeMillis() / 1000
        if (now - st.startedSec > MAX_TRIP_SEC) { Trip.update(null); return finish() }
        var plan = st.plan
        if (now - plan.liveFetchedSec >= LIVE_EVERY_SEC) plan = runCatching { planner.refreshLive(plan) }.getOrDefault(plan.copy(liveFetchedSec = now))
        val pos = here
        val options = planner.evaluate(plan, pos, now)
        var passed = false
        if (pos != null) {
            val d = Geo.km(pos, st.crossing.pos)
            minKm = minOf(minKm, d)
            passed = (minKm < PASSED_NEAR_KM && d > minKm + PASSED_AWAY_KM) || Geo.km(pos, plan.to.pos) < 0.2
        }
        val cur = Trip.state.value ?: return finish()   // stopped meanwhile
        if (cur.startedSec != st.startedSec) return       // a new trip replaced this one: its own tick follows
        val next = st.copy(plan = plan, options = options, here = pos, passed = passed, updatedSec = now)
        Trip.update(next)
        val t = strings()
        val nm = getSystemService(NotificationManager::class.java)
        if (passed) {
            nm.notify(ID_ALERT, alert(t.tripPassed, null))
            Trip.update(null)
            return finish()
        }
        nm.notify(ID_TRIP, ongoing(next, t))
        val chosen = next.chosen ?: return
        // the gate you are heading for will be down when you get there
        if (chosen.at == AtArrival.CLOSED && !alertedClosed) {
            alertedClosed = true
            nm.notify(ID_ALERT, alert(t.closedOnArrival(chosen.leg.crossing.name, chosen.waitSec / 60), chosen.opensAt?.let { t.opensAtTime(GateEngine.fmt(it)) }))
        } else if (chosen.at != AtArrival.CLOSED) alertedClosed = false
        // another way is now clearly faster
        val best = options.firstOrNull()
        if (best != null && best.leg.crossing != st.crossing && chosen.totalSec - best.totalSec >= SWITCH_SAVES_SEC &&
            alertedSwitch != best.leg.crossing.name) {
            alertedSwitch = best.leg.crossing.name
            nm.notify(ID_ALERT, alert(t.switchTo(best.leg.crossing.name, (chosen.totalSec - best.totalSec) / 60), null))
        }
    }

    // ------------------------------------------------------------------ notifications

    private fun channels() {
        val t = strings()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_TRIP, t.chTrip, NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ALERT, t.chAlert, NotificationManager.IMPORTANCE_HIGH))
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).putExtra(EXTRA_OPEN_ROUTE, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ongoing(st: Trip.State, t: Strings): Notification {
        val o = st.chosen
        val title = if (o == null) t.tripOn(st.crossing.name) else when (o.at) {
            AtArrival.CLOSED -> "${st.crossing.name} · ${t.closedShort(o.waitSec / 60)}"
            AtArrival.OPEN -> "${st.crossing.name} · ${t.openOnArrival}"
            AtArrival.NO_GATE -> "${st.crossing.name} · ${t.neverCloses}"
        }
        val body = o?.let { t.reachIn(it.toCrossingSec / 60, GateEngine.fmt(it.etaSec)) } ?: t.tripStarting
        val stop = PendingIntent.getService(this, 1, Intent(this, TripService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CH_TRIP)
            .setSmallIcon(R.drawable.ic_stat_gate)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(openApp())
            .addAction(0, t.stopAlerts, stop)
            .build()
    }

    private fun alert(title: String, body: String?): Notification =
        NotificationCompat.Builder(this, CH_ALERT)
            .setSmallIcon(R.drawable.ic_stat_gate)
            .setContentTitle(title)
            .apply { body?.let { setContentText(it) } }
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()

    private fun finish() {
        runCatching { getSystemService(LocationManager::class.java).removeUpdates(listener) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { getSystemService(LocationManager::class.java).removeUpdates(listener) }
        scope.cancel()
        super.onDestroy()
    }
}
