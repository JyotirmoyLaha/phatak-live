package com.phataklive

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phataklive.data.Fuzzy
import com.phataklive.data.Gate
import com.phataklive.data.GateStatus
import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import com.phataklive.data.PhatakDb
import com.phataklive.data.Station
import com.phataklive.data.StationBoard
import com.phataklive.gate.GateEngine
import com.phataklive.net.GoogleMaps
import com.phataklive.search.LiveSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

enum class LocationAccess { UNKNOWN, GRANTED, DENIED, BLOCKED }

/** Messages are keys, not text, so the screen shows them in the language the user picked. */
sealed class Problem {
    data object WaitingForLocation : Problem()
    data class NoGateFound(val query: String) : Problem()
    data class NoTrainFound(val query: String) : Problem()
}

data class UiState(
    // location
    val access: LocationAccess = LocationAccess.UNKNOWN,
    val gpsOff: Boolean = false,
    val locating: Boolean = false,
    val user: LatLng? = null,
    val accuracyM: Float? = null,
    val address: String? = null,
    // settings
    val radiusKm: Int = 15,
    val lang: String = "English",
    /** "auto" | "light" | "dark" (ui.ThemeMode) */
    val theme: String = "auto",
    // automatic scan
    val stations: List<Station> = emptyList(),
    val gates: List<GateStatus> = emptyList(),
    val boards: List<StationBoard> = emptyList(),
    val loading: Boolean = false,
    val lastRefreshSec: Long? = null,
    val offline: Boolean = false,
    // gate search
    val gateQuery: String = "",
    val gateSearching: Boolean = false,
    val gateResults: List<GateStatus>? = null,
    val gateProblem: Problem? = null,
    val gateSuggestions: List<LiveSearch.Suggestion> = emptyList(),
    // train search
    val trainQuery: String = "",
    val trainSearching: Boolean = false,
    val train: LiveSearch.TrainStatus? = null,
    val trainProblem: Problem? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PhatakDb.get(app)
    private val engine = GateEngine(db)
    private val search = LiveSearch(db)
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(UiState(
        lang = prefs.getString("lang", defaultLang())!!,
        radiusKm = prefs.getInt("radius", 15),
        theme = prefs.getString("theme", "auto")!!,
    ))
    val state: StateFlow<UiState> = _state

    private var gates: List<Gate> = emptyList()
    private var live: Map<String, GateEngine.LiveBoard> = emptyMap()
    private var liveErrors: Map<String, String> = emptyMap()
    private var refreshJob: Job? = null
    private var gateJob: Job? = null
    private var suggestJob: Job? = null
    private var trainJob: Job? = null
    @Volatile private var foreground = true

    /** biases suggestions to India before the first location fix */
    private val INDIA_CENTRE = LatLng(22.9, 79.0)

    /** First launch follows the phone's language: Hindi or Bengali phones start in that language. */
    private fun defaultLang() = when (Locale.getDefault().language) { "hi" -> "Hindi"; "bn" -> "Bengali"; else -> "English" }

    init {
        // gate states tick every 20 s (no network); online data refreshes every 3 min while the app is open
        viewModelScope.launch {
            while (isActive) {
                delay(20_000)
                if (!foreground) continue
                recompute()
                val last = _state.value.lastRefreshSec ?: continue
                if (now() - last > 180 && !_state.value.loading) refresh()
            }
        }
    }

    private fun now() = System.currentTimeMillis() / 1000

    fun onForeground(fg: Boolean) {
        foreground = fg
        if (fg) {
            val s = _state.value
            if (s.access == LocationAccess.GRANTED && (s.lastRefreshSec == null || now() - s.lastRefreshSec > 180 || s.gpsOff)) refresh()
        }
    }

    fun onLocationAccess(access: LocationAccess) {
        if (access == _state.value.access) return          // onResume reports GRANTED every time; onForeground handles refreshes
        _state.update { it.copy(access = access) }
        if (access == LocationAccess.GRANTED) refresh()
    }

    private fun hasPermission() = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

    fun setRadius(km: Int) {
        prefs.edit().putInt("radius", km).apply()
        _state.update { it.copy(radiusKm = km) }
        refresh()
    }

    fun setLang(lang: String) {
        prefs.edit().putString("lang", lang).apply()
        _state.update { it.copy(lang = lang) }
    }

    fun setTheme(mode: String) {
        prefs.edit().putString("theme", mode).apply()
        _state.update { it.copy(theme = mode) }
    }

    // ------------------------------------------------------------------ location

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? {
        val lm = getApplication<Application>().getSystemService(LocationManager::class.java)
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        val fresh = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val signal = androidx.core.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(lm, provider, signal, ContextCompat.getMainExecutor(getApplication())) {
                    if (cont.isActive) cont.resume(it)
                }
            }
        }
        return fresh ?: lm.getLastKnownLocation(provider) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    private fun locationEnabled(): Boolean =
        LocationManagerCompat.isLocationEnabled(getApplication<Application>().getSystemService(LocationManager::class.java))

    private suspend fun placeName(p: LatLng): String? = withContext(Dispatchers.IO) {
        runCatching { GoogleMaps.placeName(p) }.getOrNull() ?: runCatching {
            @Suppress("DEPRECATION")
            Geocoder(getApplication(), Locale.ENGLISH).getFromLocation(p.lat, p.lng, 1)?.firstOrNull()?.let { a ->
                listOfNotNull(a.subLocality ?: a.locality, a.adminArea).distinct().joinToString(", ")
            }
        }.getOrNull()
    }

    // ------------------------------------------------------------------ automatic scan

    fun refresh() {
        if (!hasPermission()) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (!locationEnabled()) {
                _state.update { it.copy(gpsOff = true, loading = false, locating = false) }
                return@launch
            }
            _state.update { it.copy(loading = true, locating = true, gpsOff = false) }
            val loc = currentLocation()
            if (loc == null) {
                _state.update { it.copy(loading = false, locating = false, gpsOff = !locationEnabled()) }
                return@launch
            }
            val user = LatLng(loc.latitude, loc.longitude)
            val radius = _state.value.radiusKm
            _state.update { it.copy(user = user, accuracyM = loc.accuracy, locating = false) }
            val stations = withContext(Dispatchers.IO) { db.nearbyStations(user, radius.toDouble()) }
            _state.update { it.copy(stations = stations) }
            val place = placeName(user)
            _state.update { it.copy(address = place ?: it.address) }

            val res = search.run(search.nearbyPlan(radius, stations), user, radius.toDouble())
            gates = res.gates.filter { Geo.km(user, it.pos) <= radius }
            live = res.live
            liveErrors = res.errors
            _state.update { it.copy(offline = res.live.isEmpty() && res.errors.isNotEmpty(), lastRefreshSec = now()) }
            recompute()
            _state.update { it.copy(loading = false) }
        }
    }

    private suspend fun recompute() {
        val s = _state.value
        val user = s.user ?: return
        val t = now()
        val (statuses, boards) = withContext(Dispatchers.Default) {
            val st = gates.map { engine.gateStatus(it, user, live, t) }
                .sortedWith(compareBy({ it.state.ordinal }, { it.distanceKm }))
            val bd = s.stations.take(6).map { engine.board(it, live[it.code], liveErrors[it.code], t) }
            st to bd
        }
        _state.update { it.copy(gates = statuses, boards = boards) }
    }

    // ------------------------------------------------------------------ gate search

    fun setGateQuery(q: String) = _state.update { it.copy(gateQuery = q) }

    /** Suggestions under the search box, refreshed while the user types (after a short pause). */
    fun onGateTyping(q: String) {
        suggestJob?.cancel()
        if (q.trim().length < 3) {
            _state.update { it.copy(gateSuggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(350)
            val from = _state.value.user ?: INDIA_CENTRE
            val list = search.suggest(q, from)
            _state.update { it.copy(gateSuggestions = list) }
        }
    }

    fun searchGate(query: String) {
        if (query.isBlank()) return
        val q = query.trim()
        runGateSearch(q) { user ->
            val stations = withContext(Dispatchers.IO) { db.findStations(Fuzzy.placePart(q).ifBlank { q }, 2) }
            val res = search.run(search.gatePlan(q, stations), user, 30.0)
            search.rank(search.relevant(res.gates, q, stations), q, user).take(6) to res.live
        }
    }

    fun pickSuggestion(s: LiveSearch.Suggestion) = when (s) {
        is LiveSearch.Suggestion.GatePlace -> runGateSearch(s.gate.name) { listOf(s.gate) to emptyMap() }
        is LiveSearch.Suggestion.NearStation -> runGateSearch(s.station.name) { user ->
            val res = search.run(search.stationGatePlan(s.station), user, 30.0)
            res.gates.sortedBy { Geo.km(s.station.pos, it.pos) }.take(6) to res.live
        }
    }

    /** Runs [find] for the gates to show, then fetches live trains near them and works out each gate's status. */
    private fun runGateSearch(label: String, find: suspend (LatLng) -> Pair<List<Gate>, Map<String, GateEngine.LiveBoard>>) {
        val user = _state.value.user ?: run {
            _state.update { it.copy(gateProblem = Problem.WaitingForLocation) }
            return
        }
        suggestJob?.cancel()
        gateJob?.cancel()
        gateJob = viewModelScope.launch {
            _state.update { it.copy(gateQuery = label, gateSuggestions = emptyList(), gateSearching = true, gateResults = null, gateProblem = null) }
            val (gates, foundLive) = find(user)
            // live data for the stations next to the found gates
            val stationsNear = withContext(Dispatchers.IO) {
                gates.mapNotNull { g -> db.nearbyStations(g.pos, 10.0, 1).firstOrNull() }.distinctBy { it.code }
            }
            val missing = stationsNear.filter { it.code !in live && it.code !in foundLive }.take(3)
            val extra = search.run(missing.map { LiveSearch.Call.StationLive(it.name, it.code) }, user, 30.0).live
            val all = live + foundLive + extra
            val t = now()
            val found = withContext(Dispatchers.Default) { gates.map { engine.gateStatus(it, user, all, t) } }
            _state.update {
                it.copy(gateSearching = false, gateResults = found, gateProblem = if (found.isEmpty()) Problem.NoGateFound(label) else null)
            }
        }
    }

    /** Clears the search box's results, and stops a search still running so its results can't pop back in. */
    fun clearGateSearch() {
        suggestJob?.cancel()
        gateJob?.cancel()
        _state.update { it.copy(gateResults = null, gateQuery = "", gateProblem = null, gateSearching = false, gateSuggestions = emptyList()) }
    }

    // ------------------------------------------------------------------ train search

    fun searchTrain(query: String) {
        if (query.isBlank()) return
        trainJob?.cancel()
        trainJob = viewModelScope.launch {
            _state.update { it.copy(trainQuery = query.trim(), trainSearching = true, train = null, trainProblem = null) }
            val r = search.trainStatus(query.trim())
            _state.update {
                if (r == null) it.copy(trainSearching = false, trainProblem = Problem.NoTrainFound(query.trim()))
                else it.copy(trainSearching = false, train = r)
            }
        }
    }

    fun clearTrain() {
        trainJob?.cancel()
        _state.update { it.copy(trainQuery = "", train = null, trainProblem = null, trainSearching = false) }
    }
}
