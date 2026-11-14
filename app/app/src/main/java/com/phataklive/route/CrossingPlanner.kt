package com.phataklive.route

import com.phataklive.data.Gate
import com.phataklive.data.GatePass
import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import com.phataklive.data.PhatakDb
import com.phataklive.data.Station
import com.phataklive.data.TimeSource
import com.phataklive.gate.GateEngine
import com.phataklive.net.GoogleDirections
import com.phataklive.net.GoogleDirections.Mode
import com.phataklive.net.GoogleMaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

enum class CrossingKind { GATE, OVERBRIDGE, UNDERPASS, FOOTBRIDGE }

/**
 * A place where a road crosses the railway, found from Google's road and rail lines (not from names).
 * [named]: [name] is the place's own name on Google Maps; otherwise [name] is the nearest station ("crossing near …").
 */
data class Crossing(val kind: CrossingKind, val name: String, val named: Boolean, val pos: LatLng)

/**
 * One way across: Google's route for the whole trip through [crossing] ([viaSec] / [viaM]), and the time from the start
 * to the crossing ([toCrossingSec]). [navStop]: the stop Navigate passes to Google Maps to go this way (just past the
 * crossing); null when this is Google's own route, which needs no stop. The times are Google's for exactly that link.
 */
data class Leg(val crossing: Crossing, val toCrossingSec: Int, val viaSec: Int, val viaM: Int, val navStop: LatLng?) {
    val own: Boolean get() = navStop == null
}

data class Place(val name: String, val sub: String?, val pos: LatLng)

data class Plan(
    val from: Place,
    /** the start is the phone's own location (live position can then replace it) */
    val fromIsMe: Boolean,
    val to: Place,
    val mode: Mode,
    val legs: List<Leg>,
    /** Google's road does not cross the railway at all: start and destination are on the same side */
    val sameSide: Boolean,
    val liveStations: List<Station>,
    val live: Map<String, GateEngine.LiveBoard>,
    val liveFetchedSec: Long,
    val createdSec: Long,
)

enum class AtArrival { OPEN, CLOSED, NO_GATE }

/** One way across, as it stands now: when you reach the crossing, what you find there, and the whole trip time. */
data class Option(
    val leg: Leg,
    val toCrossingSec: Int,
    val etaSec: Long,
    val waitSec: Int,
    val totalSec: Int,
    val at: AtArrival,
    /** gate down on arrival: when it opens */
    val opensAt: Long?,
    /** when the gate goes down next after you could pass */
    val nextClose: Long?,
    val trains: List<GatePass>,
    /** the train times behind this answer are confirmed live (bridges: always true) */
    val live: Boolean,
    /** this is the road Google Maps takes by itself */
    val googleWay: Boolean,
)

/**
 * Best way across the railway between two places, using Google Maps only:
 *  1. the railway lines around the trip come from Google's train directions between neighbouring stations;
 *  2. roads come from Google's own route plus routes forced through points along the railway;
 *  3. every place a road line meets a rail line is a crossing, named or not;
 *  4. each way is checked again with the exact link Navigate opens, so the app shows only what Google Maps will drive.
 * A crossing is a gate (can close for trains) unless Google Maps has a bridge or underpass right there.
 */
class CrossingPlanner(private val db: PhatakDb, private val engine: GateEngine) {

    companion object {
        /** Google lookups at once: more than this and Google starts answering with error pages */
        private const val PARALLEL = 3
        /** railway points tried as "go through here", at most */
        private const val MAX_PROBES = 14
        private const val PROBE_STEP_KM = 0.3
        /** other ways checked with their Navigate link, besides Google's own */
        private const val MAX_OTHER_WAYS = 4
        /** Navigate's stop: this far past the crossing, on the same road */
        private const val STOP_AFTER_KM = 0.04
        /** a name on the map this close to a crossing belongs to it */
        private const val NAME_KM = 0.15
        /** a crossing this close to the start is where you are */
        private const val AT_KM = 0.03

        /** Railway lines by section, kept for the app's lifetime (they don't move). */
        private val railCache = ConcurrentHashMap<String, List<LatLng>>()

        private val bridgeWords = Regex(
            "(over\\s*-?\\s*bridge|\\bR\\.?\\s?O\\.?\\s?B\\b|flyover|fly\\s*over|under\\s*-?\\s*pass|\\bR\\.?\\s?U\\.?\\s?B\\b|subway|under\\s*bridge|" +
                "foot\\s*-?\\s*over\\s*-?\\s*bridge|\\bF\\.?\\s?O\\.?\\s?B\\b|ओवर\\s*ब्रिज|अंडर\\s*पास|ওভার\\s*ব্রিজ|আন্ডার\\s*পাস)",
            RegexOption.IGNORE_CASE,
        )
        private val footWords = Regex("(foot\\s*-?\\s*over|\\bF\\.?\\s?O\\.?\\s?B\\b|foot\\s*bridge|pedestrian)", RegexOption.IGNORE_CASE)
        private val underWords = Regex("(under\\s*-?\\s*pass|\\bR\\.?\\s?U\\.?\\s?B\\b|subway|under\\s*bridge|अंडर\\s*पास|আন্ডার\\s*পাস)", RegexOption.IGNORE_CASE)
        /** names that are railway bridges by themselves; "flyover", "underpass" and "subway" also span roads and rivers */
        private val railBridge = Regex("(\\bR\\.?\\s?O\\.?\\s?B\\b|\\bR\\.?\\s?U\\.?\\s?B\\b|rail|railway|\\brly\\b|level\\s*crossing|\\bL\\.?\\s?C\\b|station|phatak|फाटक|ফাটক|over\\s*-?\\s*bridge|ओवर\\s*ब्रिज|ওভার\\s*ব্রিজ)", RegexOption.IGNORE_CASE)
        /** shops and eateries named "Subway", "Flyover Dhaba", "Over Bridge Hotel" */
        private val notACrossing = Regex("(restaurant|food|cafe|café|dhaba|hotel|shop|store|market|bazar|bazaar|sweet|bakery|bar\\b|lodge|bus\\s*stop|stand\\b|school|hospital|clinic|bank|atm|petrol|pump|garage|repair)", RegexOption.IGNORE_CASE)

        /** Bridge or underpass by its Maps name and categories; null when it is not one, or not over / under a railway. */
        fun bridgeKind(name: String, categories: List<String>, nearStation: Boolean): CrossingKind? {
            if (!bridgeWords.containsMatchIn(name)) return null
            if (notACrossing.containsMatchIn(name) || categories.any { notACrossing.containsMatchIn(it) }) return null
            if (!railBridge.containsMatchIn(name) && !nearStation) return null
            return when {
                footWords.containsMatchIn(name) -> CrossingKind.FOOTBRIDGE
                underWords.containsMatchIn(name) -> CrossingKind.UNDERPASS
                else -> CrossingKind.OVERBRIDGE
            }
        }

        private val bridgeQueries = listOf("railway over bridge", "railway underpass", "RUB railway subway")

        /**
         * The railway line of one section from Google's train routes: the one that starts and ends at the two stations
         * and is not much longer than the straight line (a train route, not a detour by road). Null if none fits.
         */
        internal fun pickRailLine(routes: List<GoogleDirections.Route>, a: LatLng, b: LatLng): List<LatLng>? =
            routes.sortedByDescending { it.line.size }.firstOrNull { r ->
                r.line.size >= 2 && Geo.km(r.line.first(), a) < 0.4 && Geo.km(r.line.last(), b) < 0.4 &&
                    Lines.length(r.line) < 1.35 * Geo.km(a, b) + 0.3
            }?.line

        /**
         * Points to try along the railway inside the trip's corridor, every [PROBE_STEP_KM], at most [MAX_PROBES]
         * spread evenly; points closer than 150 m (overlapping sections) count once.
         */
        internal fun probePoints(rails: List<List<LatLng>>, inCorridor: (LatLng) -> Boolean): List<LatLng> {
            val pts = ArrayList<LatLng>()
            for (rail in rails) {
                val len = Lines.length(rail)
                var s = PROBE_STEP_KM / 2
                while (s < len) {
                    val p = Lines.pointAt(rail, s)
                    if (inCorridor(p) && pts.none { Geo.km(it, p) < 0.15 }) pts += p
                    s += PROBE_STEP_KM
                }
            }
            if (pts.size <= MAX_PROBES) return pts
            return (0 until MAX_PROBES).map { pts[Math.round(it * (pts.size - 1).toDouble() / (MAX_PROBES - 1)).toInt()] }
        }
    }

    /** Steps reported while planning, for the progress line. */
    enum class Step { CROSSINGS, ROUTES, TRAINS }

    class NoRoute : Exception("no route")

    private class Found(val pos: LatLng, var route: GoogleDirections.Route, var hitKm: Double, var own: Boolean)

    /** [knownLive]: live station boards already fetched (Nearby scan), reused when fresh. */
    suspend fun plan(from: Place, fromIsMe: Boolean, to: Place, mode: Mode, knownLive: Map<String, GateEngine.LiveBoard>,
                     onStep: (Step) -> Unit): Plan = coroutineScope {
        val o = from.pos; val d = to.pos
        val span = Geo.km(o, d)
        val near = span <= 15.0
        // long trips: the crossings ahead, near the start
        val maxDetour = if (near) max(2.5, 0.5 * span) else (0.3 * span).coerceAtMost(8.0)
        fun detour(p: LatLng) = Geo.km(o, p) + Geo.km(p, d) - span
        val sem = Semaphore(PARALLEL)
        suspend fun <T> google(block: () -> T): T? = sem.withPermit { runCatching { block() }.getOrNull() }

        // 1. railway lines around the trip, Google's own route, and names on the map (labels only)
        onStep(Step.CROSSINGS)
        val ownJob = async(Dispatchers.IO) { google { GoogleDirections.routes(listOf(o, d), mode) } }
        val center = if (near) LatLng((o.lat + d.lat) / 2, (o.lng + d.lng) / 2) else o
        val radius = if (near) (span / 2 + 2.5).coerceIn(3.0, 15.0) else 8.0
        val gateNamesJob = async(Dispatchers.IO) { google { GoogleMaps.searchGates(center, radius) } ?: emptyList() }
        val bridgeNameJobs = bridgeQueries.map { q -> async(Dispatchers.IO) { google { GoogleMaps.searchPlaces(q, center, radius) } ?: emptyList() } }

        val pad = 0.06
        val box = if (near) doubleArrayOf(min(o.lat, d.lat) - pad, max(o.lat, d.lat) + pad, min(o.lng, d.lng) - pad, max(o.lng, d.lng) + pad)
            else doubleArrayOf(o.lat - 0.08, o.lat + 0.08, o.lng - 0.08, o.lng + 0.08)
        val sections = withContext(Dispatchers.IO) { db.railSections(box[0], box[1], box[2], box[3]) }
            .map { (a, b) ->
                // a section the straight line of the trip runs across is always kept, however far its stations are
                val across = Lines.segmentT(o, d, a.pos, b.pos, kotlin.math.cos(Math.toRadians(o.lat))) != null
                Triple(a, b, if (across) 0.0 else min(detour(a.pos), detour(b.pos)))
            }
            .filter { it.third <= maxDetour + 3 }
            .sortedBy { it.third }.take(12).map { it.first to it.second }
        val rails = sections.map { (a, b) ->
            async(Dispatchers.IO) {
                val key = "${a.code}|${b.code}"
                railCache[key] ?: (google { GoogleDirections.trainRoutes(a.pos, b.pos) }?.let { pickRailLine(it, a.pos, b.pos) }
                    ?.also { railCache[key] = it } ?: listOf(a.pos, b.pos))
            }
        }.awaitAll()
        val own = ownJob.await()?.filter { it.line.size >= 2 }
        if (own.isNullOrEmpty()) throw NoRoute()

        // 2. roads across: Google's own route, then routes through points along the railway
        onStep(Step.ROUTES)
        val found = ArrayList<Found>()
        fun add(r: GoogleDirections.Route, isOwn: Boolean): Int {
            val hits = Lines.crossings(r.line, rails)
            if (hits.size != 1) return hits.size        // none, or across and back: not one way across
            val h = hits[0]
            val f = found.firstOrNull { Geo.km(it.pos, h.pos) < 0.08 }
            when {
                f == null -> found += Found(h.pos, r, h.atKm, isOwn)
                isOwn && !f.own -> { f.route = r; f.hitKm = h.atKm; f.own = true }
                !f.own && r.durationSec < f.route.durationSec -> { f.route = r; f.hitKm = h.atKm }
            }
            return 1
        }
        val ownHits = own.map { add(it, true) }
        // Google's own road doesn't cross the railway: both places are on the same side
        val sameSide = ownHits.first() == 0
        val probes = probePoints(rails) { detour(it) <= maxDetour }
        val probeRoutes = probes.map { p -> async(Dispatchers.IO) { google { GoogleDirections.routes(listOf(o, p, d), mode).firstOrNull() } } }.awaitAll()
        probeRoutes.filterNotNull().filter { it.line.size >= 2 }.forEach { add(it, false) }

        // 3. check each way with the exact link Navigate will open
        val ownSec = own.minOf { it.durationSec }
        val ownWays = found.filter { it.own }.map { f -> Triple(f, f.route, null as LatLng?) }
        val others = found.filter { !it.own && it.route.durationSec <= ownSec * 5 / 2 + 600 }
            .sortedBy { it.route.durationSec }.take(MAX_OTHER_WAYS)
            .map { f ->
                async(Dispatchers.IO) {
                    val stop = Lines.pointAt(f.route.line, f.hitKm + STOP_AFTER_KM)
                    val r = google { GoogleDirections.routes(listOf(o, stop, d), mode).firstOrNull() } ?: return@async null
                    val hits = Lines.crossings(r.line, rails)
                    // Google Maps must cross exactly here and nowhere else, or this way would confuse more than help
                    if (hits.size != 1 || Geo.km(hits[0].pos, f.pos) >= 0.08) null else Triple(f, r, stop)
                }
            }.awaitAll().filterNotNull()
        val ways = ownWays + others

        // names on the map (labels, and bridge or underpass instead of gate)
        val gateNames = gateNamesJob.await()
        val bridgeNames = bridgeNameJobs.awaitAll().flatten().mapNotNull { p ->
            val nearStation = db.nearbyStations(p.pos, 1.5, 1, passengerOnly = false).isNotEmpty()
            bridgeKind(p.name, p.categories, nearStation)?.takeIf { it != CrossingKind.FOOTBRIDGE || mode == Mode.WALK }?.let { Triple(p.name, p.pos, it) }
        }
        fun crossingAt(pos: LatLng): Crossing {
            val g = gateNames.filter { Geo.km(it.pos, pos) <= NAME_KM }.minByOrNull { Geo.km(it.pos, pos) }
            // a gate on the map wins: never call a crossing "never closes" when a gate may be there
            if (g != null) return Crossing(CrossingKind.GATE, g.name, true, pos)
            val b = bridgeNames.filter { Geo.km(it.second, pos) <= NAME_KM }.minByOrNull { Geo.km(it.second, pos) }
            if (b != null) return Crossing(b.third, b.first, true, pos)
            val st = db.nearbyStations(pos, 5.0, 1).firstOrNull()
            return Crossing(CrossingKind.GATE, st?.name ?: "", false, pos)
        }

        // time to each gate: Google's route from the start to just before it (bridges: share of the trip by distance)
        val legs = ways.map { (f, r, stop) ->
            async(Dispatchers.IO) {
                val c = crossingAt(f.pos)
                val hitKm = Lines.crossings(r.line, rails).firstOrNull()?.atKm ?: f.hitKm
                val share = (r.durationSec * hitKm / Lines.length(r.line).coerceAtLeast(1e-6)).toInt()
                val toSec = when {
                    Geo.km(o, f.pos) < AT_KM -> 0
                    c.kind != CrossingKind.GATE -> share
                    else -> google { GoogleDirections.routes(listOf(o, Lines.pointAt(r.line, hitKm - 0.02)), mode).firstOrNull()?.durationSec } ?: share
                }
                Leg(c, toSec.coerceIn(0, r.durationSec), r.durationSec, r.distanceM, stop)
            }
        }.awaitAll()

        // 4. live trains at the stations next to the gates
        onStep(Step.TRAINS)
        val stations = legs.filter { it.crossing.kind == CrossingKind.GATE }
            .mapNotNull { db.nearbyStations(it.crossing.pos, 10.0, 1).firstOrNull() }
            .distinctBy { it.code }.take(4)
        val now = System.currentTimeMillis() / 1000
        val reuse = knownLive.filter { (code, b) -> stations.any { it.code == code } && now - b.fetchedEpochSec < 300 }
        val live = reuse + fetchLive(stations.filter { it.code !in reuse })

        Plan(from, fromIsMe, to, mode, legs, sameSide && legs.isEmpty(), stations, live, now, now)
    }

    private suspend fun fetchLive(stations: List<Station>): Map<String, GateEngine.LiveBoard> = coroutineScope {
        stations.map { s ->
            async(Dispatchers.IO) {
                runCatching { s.code to GateEngine.LiveBoard(System.currentTimeMillis() / 1000, GoogleMaps.stationDepartures(s.name, s.code)) }.getOrNull()
            }
        }.awaitAll().filterNotNull().toMap()
    }

    /** Fresh live trains for a plan (every few minutes during a trip); the old boards stay if this fails. */
    suspend fun refreshLive(plan: Plan): Plan {
        val fresh = fetchLive(plan.liveStations)
        return plan.copy(live = plan.live + fresh, liveFetchedSec = System.currentTimeMillis() / 1000)
    }

    // ------------------------------------------------------------------ ranking (offline, every few seconds)

    /**
     * Every way across as it stands at [now], best first. [here]: the phone's live position; the time left to each
     * crossing shrinks with the straight-line distance left to it (Google is not asked again on every tick).
     */
    fun evaluate(plan: Plan, here: LatLng?, now: Long): List<Option> {
        val pos = here ?: plan.from.pos
        return plan.legs.map { leg ->
            val c = leg.crossing
            val startKm = Geo.km(plan.from.pos, c.pos)
            val after = (leg.viaSec - leg.toCrossingSec).coerceAtLeast(0)
            val toSec = if (here == null || startKm < AT_KM) leg.toCrossingSec
                else (leg.toCrossingSec * (Geo.km(pos, c.pos) / startKm)).toInt().coerceIn(0, leg.toCrossingSec * 2 + 600)
            val eta = now + toSec
            if (c.kind != CrossingKind.GATE) {
                Option(leg, toSec, eta, 0, toSec + after, AtArrival.NO_GATE, null, null, emptyList(), live = true, googleWay = leg.own)
            } else {
                val st = engine.gateStatus(Gate(c.name, c.pos, null, "route"), pos, plan.live, now)
                val a = GateEngine.arrival(st.passes, eta)
                val trains = a.trains.ifEmpty { st.passes.filter { a.nextClose != null && it.closeEpochSec >= a.nextClose }.take(2) }
                Option(
                    leg, toSec, eta, a.waitSec.toInt(), toSec + a.waitSec.toInt() + after,
                    if (a.waitSec > 0) AtArrival.CLOSED else AtArrival.OPEN, a.opensAt, a.nextClose, trains,
                    live = trains.isNotEmpty() && trains.all { it.timeSource == TimeSource.LIVE },
                    googleWay = leg.own,
                )
            }
        }
            // a minute either way is noise: then Google's own road (no extra stop), then a bridge (never closes)
            .sortedWith(compareBy({ it.totalSec / 60 }, { if (it.googleWay) 0 else 1 }, { if (it.leg.crossing.kind == CrossingKind.GATE) 1 else 0 }, { it.totalSec }))
    }
}
