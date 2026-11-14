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
import kotlin.math.abs
import kotlin.math.max

enum class CrossingKind { GATE, OVERBRIDGE, UNDERPASS, FOOTBRIDGE }

/** A place where the road crosses the railway: a gate (can be closed for a train) or a bridge / underpass (never). */
data class Crossing(val kind: CrossingKind, val name: String, val pos: LatLng, val placeId: String?)

/**
 * A crossing's roads from Google Maps, as they were when the trip was planned: [viaSec] / [viaM] for the whole trip
 * through it, [toCrossingSec] from the start to the crossing (asked for gates, whose wait depends on the arrival time).
 */
data class Leg(val crossing: Crossing, val toCrossingSec: Int?, val viaSec: Int, val viaM: Int)

data class Place(val name: String, val sub: String?, val pos: LatLng)

data class Plan(
    val from: Place,
    /** the start is the phone's own location (live position can then replace it) */
    val fromIsMe: Boolean,
    val to: Place,
    val mode: Mode,
    val legs: List<Leg>,
    /** Google's own fastest route, which may go through any of the legs or none */
    val directSec: Int?,
    val directM: Int?,
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
    /** this is probably the road Google itself picks */
    val googleWay: Boolean,
)

/**
 * Best way across the railway between two places. Crossings come from Google Maps (gates, overbridges, underpasses
 * near the line between the two places); each is timed with a Google route forced through it, and a gate's wait is
 * the closing window the gate engine expects at the minute you would reach it.
 */
class CrossingPlanner(private val db: PhatakDb, private val engine: GateEngine) {

    companion object {
        const val MAX_GATES = 5
        const val MAX_BRIDGES = 3
        /** Google lookups at once: enough to be quick, few enough not to look like a flood */
        private const val PARALLEL = 5
        /** a crossing this close to the start is where you are */
        private const val AT_KM = 0.05

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

        /** Google's own road is much quicker than any crossing found: the trip may not cross the line, or crosses where the map has no name. */
        fun directMuchFaster(plan: Plan): Boolean {
            val ds = plan.directSec ?: return false
            val best = plan.legs.minOfOrNull { it.viaSec } ?: return true
            return best > ds + max(180, ds / 4)
        }

        private fun bridgeQueries(mode: Mode) = listOf("railway over bridge", "ROB rail over bridge", "railway underpass", "RUB railway subway") +
            if (mode == Mode.WALK) listOf("foot over bridge railway") else emptyList()
    }

    // ------------------------------------------------------------------ planning (online)

    /** Steps reported while planning, for the progress line. */
    enum class Step { CROSSINGS, ROUTES, TRAINS }

    class NoRoute : Exception("no route")

    /** [knownLive]: live station boards already fetched (Nearby scan), reused when fresh. */
    suspend fun plan(from: Place, fromIsMe: Boolean, to: Place, mode: Mode, knownLive: Map<String, GateEngine.LiveBoard>,
                     onStep: (Step) -> Unit): Plan = coroutineScope {
        val o = from.pos; val d = to.pos
        val span = Geo.km(o, d)
        val gate = Semaphore(PARALLEL)
        suspend fun <T> google(block: () -> T): T? = gate.withPermit { runCatching { block() }.getOrNull() }

        // 1. crossings near the line between the two places (for a long trip: the ones ahead, near the start)
        onStep(Step.CROSSINGS)
        val near = span <= 15.0
        val center = if (near) LatLng((o.lat + d.lat) / 2, (o.lng + d.lng) / 2) else o
        val radius = if (near) (span / 2 + 2.5).coerceIn(3.0, 15.0) else 8.0
        val maxDetour = if (near) max(2.5, 0.5 * span) else (0.3 * span).coerceAtMost(8.0)
        val directJob = async(Dispatchers.IO) { google { GoogleDirections.routes(listOf(o, d), mode).minByOrNull { it.durationSec } } }
        val gateJob = async(Dispatchers.IO) { google { GoogleMaps.searchGates(center, radius) } ?: emptyList() }
        val bridgeJobs = bridgeQueries(mode).map { q -> async(Dispatchers.IO) { google { GoogleMaps.searchPlaces(q, center, radius) } ?: emptyList() } }

        fun detour(p: LatLng) = Geo.km(o, p) + Geo.km(p, d) - span
        fun clash(list: List<Crossing>, name: String, p: LatLng) =
            list.any { Geo.km(it.pos, p) < 0.08 || (it.name.equals(name, true) && Geo.km(it.pos, p) < 0.3) }

        val gates = ArrayList<Crossing>()
        for (g in gateJob.await()) {
            if (detour(g.pos) > maxDetour || clash(gates, g.name, g.pos)) continue
            gates += Crossing(CrossingKind.GATE, g.name, g.pos, g.placeId)
        }
        val bridges = ArrayList<Crossing>()
        for (p in bridgeJobs.awaitAll().flatten()) {
            if (detour(p.pos) > maxDetour || clash(gates + bridges, p.name, p.pos)) continue
            if (GoogleMaps.isGateName(p.name) && !bridgeWords.containsMatchIn(p.name)) continue
            val nearStation = db.nearbyStations(p.pos, 1.5, 1, passengerOnly = false).isNotEmpty()
            val kind = bridgeKind(p.name, p.categories, nearStation) ?: continue
            if (kind == CrossingKind.FOOTBRIDGE && mode != Mode.WALK) continue
            bridges += Crossing(kind, p.name, p.pos, p.placeId)
        }
        val crossings = gates.sortedBy { detour(it.pos) }.take(MAX_GATES) + bridges.sortedBy { detour(it.pos) }.take(MAX_BRIDGES)

        // 2. a Google route through each crossing; gates also from the start to the gate, for the minute you reach it
        onStep(Step.ROUTES)
        val legs = crossings.map { c ->
            async(Dispatchers.IO) {
                val atStart = Geo.km(o, c.pos) < AT_KM
                val viaJob = async { google { GoogleDirections.routes(if (atStart) listOf(o, d) else listOf(o, c.pos, d), mode).firstOrNull() } }
                val toJob = async {
                    when {
                        c.kind != CrossingKind.GATE -> null
                        atStart -> 0
                        else -> google { GoogleDirections.routes(listOf(o, c.pos), mode).firstOrNull()?.durationSec }
                    }
                }
                val via = viaJob.await() ?: return@async null
                val to = toJob.await()
                // a gate whose arrival time is unknown can't be timed: better left out than shown wrong
                if (c.kind == CrossingKind.GATE && to == null) return@async null
                Leg(c, to?.coerceAtMost(via.durationSec), via.durationSec, via.distanceM)
            }
        }.awaitAll().filterNotNull()
        val direct = directJob.await()
        if (legs.isEmpty() && direct == null) throw NoRoute()

        // 3. live trains at the stations next to the gates
        onStep(Step.TRAINS)
        val stations = legs.filter { it.crossing.kind == CrossingKind.GATE }
            .mapNotNull { db.nearbyStations(it.crossing.pos, 10.0, 1).firstOrNull() }
            .distinctBy { it.code }.take(4)
        val now = System.currentTimeMillis() / 1000
        val reuse = knownLive.filter { (code, b) -> stations.any { it.code == code } && now - b.fetchedEpochSec < 300 }
        val live = reuse + fetchLive(stations.filter { it.code !in reuse })

        Plan(from, fromIsMe, to, mode, legs, direct?.durationSec, direct?.distanceM, stations, live, now, now)
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
        val list = plan.legs.map { leg ->
            val c = leg.crossing
            val startKm = Geo.km(plan.from.pos, c.pos)
            // bridges: share of the trip time by distance (no wait depends on it)
            val baseTo = leg.toCrossingSec ?: if (startKm < AT_KM) 0
                else (leg.viaSec * startKm / (startKm + Geo.km(c.pos, plan.to.pos)).coerceAtLeast(0.001)).toInt()
            val after = (leg.viaSec - baseTo).coerceAtLeast(0)
            val toSec = if (here == null || startKm < AT_KM) baseTo
                else (baseTo * (Geo.km(pos, c.pos) / startKm)).toInt().coerceIn(0, baseTo * 2 + 600)
            val eta = now + toSec
            if (c.kind != CrossingKind.GATE) {
                Option(leg, toSec, eta, 0, toSec + after, AtArrival.NO_GATE, null, null, emptyList(), live = true, googleWay = false)
            } else {
                val st = engine.gateStatus(Gate(c.name, c.pos, c.placeId, "maps"), pos, plan.live, now)
                val a = GateEngine.arrival(st.passes, eta)
                val trains = a.trains.ifEmpty { st.passes.filter { a.nextClose != null && it.closeEpochSec >= a.nextClose }.take(2) }
                Option(
                    leg, toSec, eta, a.waitSec.toInt(), toSec + a.waitSec.toInt() + after,
                    if (a.waitSec > 0) AtArrival.CLOSED else AtArrival.OPEN, a.opensAt, a.nextClose, trains,
                    live = trains.isNotEmpty() && trains.all { it.timeSource == TimeSource.LIVE },
                    googleWay = false,
                )
            }
        }
        // the crossing whose forced route matches Google's own fastest one is most likely the road Google takes
        val google = plan.directSec?.let { ds ->
            list.minByOrNull { abs(it.leg.viaSec - ds) }?.takeIf {
                abs(it.leg.viaSec - ds) <= max(60, ds / 10) && abs(it.leg.viaM - (plan.directM ?: it.leg.viaM)) <= max(150, (plan.directM ?: 0) / 12)
            }
        }
        return list.map { if (it === google) it.copy(googleWay = true) else it }
            // a minute either way is noise: then a bridge (never closes) beats a gate
            .sortedWith(compareBy({ it.totalSec / 60 }, { if (it.leg.crossing.kind == CrossingKind.GATE) 1 else 0 }, { it.totalSec }))
    }
}
