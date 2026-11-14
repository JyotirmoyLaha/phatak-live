package com.phataklive.route

import com.phataklive.data.Gate
import com.phataklive.data.GatePass
import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import com.phataklive.data.PhatakDb
import com.phataklive.data.RailLines
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
data class Crossing(
    val kind: CrossingKind, val name: String, val named: Boolean, val pos: LatLng,
    /** the road crossing the railway there, from Google's turn-by-turn step ("GT Rd"): tells unnamed crossings apart */
    val road: String? = null,
)

/** A crossing further along the same road, after the one the user chooses ([toSec]: from the start, Google's time share). */
data class Later(val crossing: Crossing, val toSec: Int)

/**
 * One way: Google's route for the whole trip ([viaSec] / [viaM], road [line]) whose first railway crossing is
 * [crossing], reached after [toCrossingSec]; [later] are the crossings after it on the same road.
 * [navStop]: the stop Navigate passes to Google Maps to go this way (just past the crossing); null when this is the
 * route Google Maps takes by itself. All times are Google's for exactly the link Navigate opens.
 */
data class Leg(
    val crossing: Crossing, val toCrossingSec: Int, val viaSec: Int, val viaM: Int, val navStop: LatLng?,
    val line: List<LatLng>, val later: List<Later>,
    /** Google's name for the roads of this way ("NH114 and Shibda Rd"), to tell ways apart */
    val road: String? = null,
) {
    val own: Boolean get() = navStop == null
    /** every crossing on this way, in order */
    val crossings: List<Crossing> get() = listOf(crossing) + later.map { it.crossing }
}

data class Place(val name: String, val sub: String?, val pos: LatLng)

data class Plan(
    val from: Place,
    /** the start is the phone's own location (Navigate lets Google Maps start from where the phone is) */
    val fromIsMe: Boolean,
    val to: Place,
    val mode: Mode,
    val legs: List<Leg>,
    /** Google's road crosses no railway line we know: start and destination are on the same side */
    val sameSide: Boolean,
    /** every railway line near the trip came from Google; false: some are straight-line guesses, so "no crossing" can't be said */
    val railsChecked: Boolean,
    /** the typed start is on the railway itself (a gate, a station) and the phone is not there: Google picks the side */
    val startOnRailway: Boolean,
    /** railway lines near the trip, for the in-app map */
    val rails: List<List<LatLng>>,
    val liveStations: List<Station>,
    val live: Map<String, GateEngine.LiveBoard>,
    val liveFetchedSec: Long,
    val createdSec: Long,
)

enum class AtArrival { OPEN, CLOSED, NO_GATE }

/** What you are likely to find at one crossing, reaching it at [etaSec]. */
data class AtCrossing(val crossing: Crossing, val etaSec: Long, val waitSec: Int, val at: AtArrival, val opensAt: Long?)

/** One way as it stands now: when you reach the crossing, what you find there, and the whole trip time. */
data class Option(
    val leg: Leg,
    val toCrossingSec: Int,
    val etaSec: Long,
    val waitSec: Int,
    /** whole trip, including the likely waits at every gate on the way */
    val totalSec: Int,
    val at: AtArrival,
    /** gate down on arrival: when it opens */
    val opensAt: Long?,
    /** when the gate goes down next after you could pass */
    val nextClose: Long?,
    val trains: List<GatePass>,
    /** the train times behind this answer are confirmed live */
    val live: Boolean,
    /** this is the road Google Maps takes by itself */
    val googleWay: Boolean,
    /** the crossings after the first one, on the same road */
    val later: List<AtCrossing> = emptyList(),
)

/**
 * Ways across the railway between two places, using Google Maps only:
 *  1. Google's own route, and the railway lines near it from Google's train directions between neighbouring stations;
 *  2. where Google's road line meets a railway line is a crossing, named or not; the first one is the choice to make;
 *  3. other roads across near it come from routes Google makes through points along the railway, with the same
 *     number of crossings (no detours across and back);
 *  4. every other way is checked again with the exact link Navigate opens, so Google Maps drives exactly what the app
 *     shows; a way that Google would drive differently is dropped.
 * Safety: a crossing is a gate (can close for trains) unless Google Maps has a bridge or underpass right at it and no
 * gate anywhere near; times are estimates, and the screen says so.
 */
class CrossingPlanner(private val db: PhatakDb, private val engine: GateEngine) {

    companion object {
        /** Google lookups at once: more than this and Google starts answering with error pages */
        private const val PARALLEL = 3
        /** railway points tried as "go through here", at most */
        private const val MAX_PROBES = 12
        private const val PROBE_STEP_KM = 0.3
        /** other roads across are looked for this far from the first crossing on Google's route */
        private const val PROBE_NEAR_KM = 2.5
        /** other ways checked with their Navigate link, besides Google's own */
        private const val MAX_OTHER_WAYS = 3
        /** Navigate's stop: this far past the crossing, on the same road */
        private const val STOP_AFTER_KM = 0.04
        /** a gate's name on the map this close to a crossing belongs to it */
        private const val GATE_NAME_KM = 0.15
        /** a bridge or underpass must be this close, and no gate within [NO_GATE_KM], to be called one */
        private const val BRIDGE_NAME_KM = 0.06
        private const val NO_GATE_KM = 0.3
        /** a start closer than this to a railway line is on it (a gate, a station): which side is unknown */
        private const val ON_RAILWAY_KM = 0.06
        /** a route that gets this far from another somewhere is on another road */
        private const val OTHER_ROAD_KM = 0.3
        /** crossings this close are one crossing area: only one way through it is shown */
        private const val SAME_AREA_KM = 0.5
        /** crossings with the same name closer than this: only the faster way is shown */
        private const val SAME_NAME_KM = 3.0
        /** a crossing this close to the start is where you are */
        private const val AT_KM = 0.03
        /** longest trip the crossing check covers */
        const val MAX_TRIP_KM = 80.0
        private const val MAX_SECTIONS = 30


        private val bridgeWords = Regex(
            "(over\\s*-?\\s*bridge|\\bR\\.?\\s?O\\.?\\s?B\\b|flyover|fly\\s*over|under\\s*-?\\s*pass|\\bR\\.?\\s?U\\.?\\s?B\\b|subway|under\\s*bridge|" +
                "foot\\s*-?\\s*over\\s*-?\\s*bridge|\\bF\\.?\\s?O\\.?\\s?B\\b|ओवर\\s*ब्रिज|अंडर\\s*पास|ওভার\\s*ব্রিজ|আন্ডার\\s*পাস)",
            RegexOption.IGNORE_CASE,
        )
        private val footWords = Regex("(foot\\s*-?\\s*over|\\bF\\.?\\s?O\\.?\\s?B\\b|foot\\s*bridge|pedestrian)", RegexOption.IGNORE_CASE)
        private val underWords = Regex("(under\\s*-?\\s*pass|\\bR\\.?\\s?U\\.?\\s?B\\b|subway|under\\s*bridge|अंडर\\s*पास|আন্ডার\\s*পাস)", RegexOption.IGNORE_CASE)
        /** names that are railway bridges by themselves; "flyover", "underpass" and "subway" also span roads and rivers */
        private val railBridge = Regex("(\\bR\\.?\\s?O\\.?\\s?B\\b|\\bR\\.?\\s?U\\.?\\s?B\\b|rail|railway|\\brly\\b|level\\s*crossing|\\bL\\.?\\s?C\\b|station|phatak|फाटक|ফাটক|over\\s*-?\\s*bridge|ओवर\\s*ब्रिज|ওভার\\s*ব্রিজ)", RegexOption.IGNORE_CASE)
        /** shops and eateries named "Subway", "Flyover Dhaba", "Over Bridge Hotel"; bridges still being built */
        private val notACrossing = Regex("(restaurant|food|cafe|café|dhaba|hotel|shop|store|market|bazar|bazaar|sweet|bakery|bar\\b|lodge|bus\\s*stop|stand\\b|school|hospital|clinic|bank|atm|petrol|pump|garage|repair|under\\s*construction|proposed|work\\s*site|construction)", RegexOption.IGNORE_CASE)

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

        /** a road named as passing over / under the railway in a turn-by-turn step */
        private val stepOver = Regex("""(flyover|fly\s*over|overpass|over\s*-?\s*bridge|\bR\.?\s?O\.?\s?B\b|rail(way)?\s*bridge)""", RegexOption.IGNORE_CASE)
        private val stepUnder = Regex("""(underpass|under\s*pass|subway|\bR\.?\s?U\.?\s?B\b|under\s*bridge)""", RegexOption.IGNORE_CASE)

        /** The road a step names: "Continue onto Shahdara Flyover" -> "Shahdara Flyover" (after onto / on / via). */
        /** Any road a step names: "Turn right onto GT Rd" -> "GT Rd"; null for "Head south" or a roundabout. */
        internal fun roadName(text: String): String? =
            Regex("""(?:onto|on|along|via)\s+(?:the\s+)?(.+?)(?:\s+(?:toward|towards|for|to)\b.*)?$""", RegexOption.IGNORE_CASE)
                .find(text.lineSequence().first())?.groupValues?.get(1)?.trim()
                ?.takeIf { it.length in 2..60 && !it.contains("roundabout", ignoreCase = true) && it.any { c -> c.isLetter() } }

        internal fun stepRoad(text: String): String? =
            Regex("""(?:onto|on|via|take)\s+(?:the\s+)?(.+?)(?:\s+(?:toward|towards|for|to)\b.*)?$""", RegexOption.IGNORE_CASE)
                .find(text.lineSequence().first())?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() && (stepOver.containsMatchIn(it) || stepUnder.containsMatchIn(it)) }

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
         * Points to try along the railway where [keep] says, every [PROBE_STEP_KM], at most [MAX_PROBES] spread
         * evenly; points closer than 150 m (overlapping sections) count once.
         */
        internal fun probePoints(rails: List<List<LatLng>>, keep: (LatLng) -> Boolean): List<LatLng> {
            val pts = ArrayList<LatLng>()
            for (rail in rails) {
                val len = Lines.length(rail)
                var s = PROBE_STEP_KM / 2
                while (s < len) {
                    val p = Lines.pointAt(rail, s)
                    if (keep(p) && pts.none { Geo.km(it, p) < 0.15 }) pts += p
                    s += PROBE_STEP_KM
                }
            }
            if (pts.size <= MAX_PROBES) return pts
            return (0 until MAX_PROBES).map { pts[Math.round(it * (pts.size - 1).toDouble() / (MAX_PROBES - 1)).toInt()] }
        }

        /**
         * What a crossing is, from names on Google Maps near it. A gate name nearby makes it a gate; a bridge or
         * underpass counts only right at the crossing with no gate anywhere near (a new bridge is often built beside an
         * old gate); anything else is an unnamed gate, because a gate that may close is the safe assumption.
         */
        internal fun classify(pos: LatLng, gates: List<Pair<String, LatLng>>, bridges: List<Triple<String, LatLng, CrossingKind>>,
                              nearStation: String?, stepText: String? = null): Crossing {
            val g = gates.filter { Geo.km(it.second, pos) <= GATE_NAME_KM }.minByOrNull { Geo.km(it.second, pos) }
            if (g != null) return Crossing(CrossingKind.GATE, g.first, true, pos)
            val gateNear = gates.any { Geo.km(it.second, pos) <= NO_GATE_KM }
            val b = bridges.filter { Geo.km(it.second, pos) <= BRIDGE_NAME_KM }.minByOrNull { Geo.km(it.second, pos) }
            if (b != null && !gateNear) return Crossing(b.third, b.first, true, pos)
            // Google's own turn-by-turn step at the crossing names the road: "Continue onto Shahdara Flyover"
            if (!gateNear && stepText != null) {
                val road = stepRoad(stepText)
                val kind = when {
                    stepUnder.containsMatchIn(stepText) -> CrossingKind.UNDERPASS
                    stepOver.containsMatchIn(stepText) -> CrossingKind.OVERBRIDGE
                    else -> null
                }
                if (kind != null) return Crossing(kind, road ?: nearStation ?: "", road != null, pos)
            }
            return Crossing(CrossingKind.GATE, nearStation ?: "", false, pos, stepText?.let { roadName(it) })
        }
    }

    /** Steps reported while planning, for the progress line. */
    enum class Step { CROSSINGS, ROUTES, TRAINS }

    class NoRoute : Exception("no route")
    class TooFar : Exception("trip too long")

    private class Way(val route: GoogleDirections.Route, val hits: List<Lines.Hit>, val stop: LatLng?)

    /**
     * [knownLive]: live station boards already fetched (Nearby scan), reused when fresh. [gps]: the phone's position,
     * used instead of a typed start that lies on the railway when the phone is right there.
     */
    suspend fun plan(from: Place, fromIsMe: Boolean, to: Place, mode: Mode, knownLive: Map<String, GateEngine.LiveBoard>,
                     gps: LatLng?, onStep: (Step) -> Unit): Plan = coroutineScope {
        val d = to.pos
        if (Geo.km(from.pos, d) > MAX_TRIP_KM) throw TooFar()
        val sem = Semaphore(PARALLEL)
        // every Google call runs on the IO threads, whoever calls it: never on the screen thread
        suspend fun <T> google(block: () -> T): T? = withContext(Dispatchers.IO) {
            sem.withPermit { runCatching { block() }.onFailure { android.util.Log.w("PhatakLive", "route: Google call failed", it) }.getOrNull() }
        }

        // 1. Google's own route, then the railway lines near it
        onStep(Step.CROSSINGS)
        var start = from
        var startIsMe = fromIsMe
        var own = google { GoogleDirections.routes(listOf(start.pos, d), mode) }?.filter { it.line.size >= 2 }
        if (own.isNullOrEmpty()) throw NoRoute()

        val span = Geo.km(start.pos, d)
        val pad = 0.05
        val box = doubleArrayOf(min(start.pos.lat, d.lat) - pad, max(start.pos.lat, d.lat) + pad, min(start.pos.lng, d.lng) - pad, max(start.pos.lng, d.lng) + pad)
        val ownLines = own.map { it.line }
        // sections near Google's routes: the ones its road crosses first, then the closest
        val sections = withContext(Dispatchers.IO) { db.railSections(box[0], box[1], box[2], box[3]) }
            .map { (a, b) ->
                val straight = listOf(a.pos, b.pos)
                val crosses = ownLines.any { Lines.crossings(it, listOf(straight)).isNotEmpty() }
                Triple(a, b, if (crosses) 0.0 else min(Lines.distanceKm(a.pos, ownLines), Lines.distanceKm(b.pos, ownLines)))
            }
            .filter { it.third <= 3.0 }
            .sortedBy { it.third }.take(MAX_SECTIONS)
        var fallback = false
        val rails = sections.map { (a, b, _) ->
            async(Dispatchers.IO) {
                RailLines.get(a.code, b.code) ?: google { GoogleDirections.trainRoutes(a.pos, b.pos) }?.let { pickRailLine(it, a.pos, b.pos) }
                    ?.also { RailLines.put(a.code, b.code, it) }
            } to listOf(a.pos, b.pos)
        }.map { (job, straight) -> job.await() ?: straight.also { fallback = true } }

        // a typed start on the railway itself (a gate, a station): the phone's position tells the side, if it is there
        var startOnRailway = false
        if (!fromIsMe && rails.isNotEmpty() && Lines.distanceKm(from.pos, rails) < ON_RAILWAY_KM) {
            if (gps != null && Geo.km(gps, from.pos) < 1.5) {
                start = from.copy(pos = gps); startIsMe = true
                own = google { GoogleDirections.routes(listOf(start.pos, d), mode) }?.filter { it.line.size >= 2 }
                if (own.isNullOrEmpty()) throw NoRoute()
            } else startOnRailway = true
        }
        val o = start.pos
        val maxDetour = max(2.5, 0.5 * min(span, 15.0))
        fun detour(p: LatLng) = Geo.km(o, p) + Geo.km(p, d) - Geo.km(o, d)

        // names on the map, near Google's route (labels; and bridge or underpass instead of gate)
        val ownRoute = own.first()
        val ownHits = Lines.crossings(ownRoute.line, rails)
        // the first crossing's area is searched once, wider (below); later crossings each get a small search
        val nameJobs = ownHits.drop(1).take(3).map { h ->
            async(Dispatchers.IO) { google { GoogleMaps.searchGates(h.pos, 1.0) } ?: emptyList() }
        }
        val center = ownHits.firstOrNull()?.pos ?: o
        val bridgeNameJobs = bridgeQueries.map { q -> async(Dispatchers.IO) { google { GoogleMaps.searchPlaces(q, center, PROBE_NEAR_KM + 1) } ?: emptyList() } }

        if (ownHits.isEmpty()) {
            // Google's road crosses no railway line: say so only if every railway line near it was checked
            onStep(Step.TRAINS)
            val now = System.currentTimeMillis() / 1000
            return@coroutineScope Plan(start, startIsMe, to, mode, emptyList(), sameSide = !fallback, railsChecked = !fallback,
                startOnRailway = startOnRailway, rails = rails, liveStations = emptyList(), live = emptyMap(), liveFetchedSec = now, createdSec = now)
        }

        // 2. other roads across near the first crossing: routes through points along the railway there
        onStep(Step.ROUTES)
        val first = ownHits.first().pos
        val probes = probePoints(rails) { Geo.km(it, first) <= PROBE_NEAR_KM && Geo.km(it, first) > 0.1 && detour(it) <= maxDetour }
        val candidates = ArrayList<Way>()
        // a way is another way if it crosses somewhere else, or crosses at the same place on a different road
        fun sameWay(a: Way, hits: List<Lines.Hit>, line: List<LatLng>) =
            Geo.km(a.hits[0].pos, hits[0].pos) < 0.08 && Lines.deviationKm(line, a.route.line) < OTHER_ROAD_KM
        fun consider(r: GoogleDirections.Route) {
            val hits = Lines.crossings(r.line, rails)
            // same number of crossings as Google's own road (no detour across and back)
            if (hits.size != ownHits.size) return
            // Google's own way again
            if (Geo.km(hits[0].pos, first) < 0.08 && Lines.deviationKm(r.line, ownRoute.line) < OTHER_ROAD_KM) return
            val same = candidates.indexOfFirst { sameWay(it, hits, r.line) }
            if (same < 0) candidates += Way(r, hits, null)
            else if (r.durationSec < candidates[same].route.durationSec) candidates[same] = Way(r, hits, null)
        }
        own.drop(1).forEach { consider(it) }   // Google's other roads: they need a stop too, Navigate takes the first route
        probes.map { p -> async(Dispatchers.IO) { google { GoogleDirections.routes(listOf(o, p, d), mode).firstOrNull() } } }
            .awaitAll().filterNotNull().filter { it.line.size >= 2 }.forEach { consider(it) }

        // 3. check each other way with the exact link Navigate opens
        val limit = ownRoute.durationSec * 5 / 2 + 600
        val others = candidates.filter { it.route.durationSec <= limit }.sortedBy { it.route.durationSec }.take(MAX_OTHER_WAYS)
            .map { w ->
                async(Dispatchers.IO) {
                    // the stop that makes Google Maps take this way: just past its crossing when it crosses elsewhere,
                    // else on the stretch of road where it leaves Google's own route
                    val sameCrossing = Geo.km(w.hits[0].pos, first) < 0.08
                    val stop = if (!sameCrossing) Lines.pointAt(w.route.line, w.hits[0].atKm + STOP_AFTER_KM)
                        else Lines.farthestFrom(w.route.line, ownRoute.line)
                    val r = google { GoogleDirections.routes(listOf(o, stop, d), mode).firstOrNull() } ?: return@async null
                    val hits = Lines.crossings(r.line, rails)
                    // Google Maps must cross first exactly here, as many times as Google's own road, and (same crossing)
                    // still on the other road
                    val ok = hits.size == ownHits.size && Geo.km(hits[0].pos, w.hits[0].pos) < 0.08 &&
                        (!sameCrossing || Lines.deviationKm(r.line, ownRoute.line) >= OTHER_ROAD_KM)
                    if (ok) Way(r, hits, stop) else null
                }
            }.awaitAll().filterNotNull()
        val ways = listOf(Way(ownRoute, ownHits, null)) + others

        val gateNames = nameJobs.awaitAll().flatten().map { it.name to it.pos } +
            (google { GoogleMaps.searchGates(center, PROBE_NEAR_KM + 1) } ?: emptyList()).map { it.name to it.pos }
        val bridgeNames = bridgeNameJobs.awaitAll().flatten().mapNotNull { p ->
            val nearStation = db.nearbyStations(p.pos, 1.5, 1, passengerOnly = false).isNotEmpty()
            bridgeKind(p.name, p.categories, nearStation)?.takeIf { it != CrossingKind.FOOTBRIDGE || mode == Mode.WALK }?.let { Triple(p.name, p.pos, it) }
        }
        fun crossingAt(pos: LatLng, r: GoogleDirections.Route, atKm: Double) =
            classify(pos, gateNames, bridgeNames, db.nearbyStations(pos, 5.0, 1).firstOrNull()?.name, r.stepAt(atKm))

        // time to the first gate: Google's route from the start to just before it; later ones by share of the trip
        val legs = ways.map { w ->
            async(Dispatchers.IO) {
                val r = w.route
                val len = Lines.length(r.line).coerceAtLeast(1e-6)
                fun share(km: Double) = (r.durationSec * km / len).toInt().coerceIn(0, r.durationSec)
                val h = w.hits[0]
                val c = crossingAt(h.pos, r, h.atKm)
                val toSec = when {
                    Geo.km(o, h.pos) < AT_KM -> 0
                    c.kind != CrossingKind.GATE -> share(h.atKm)
                    else -> google { GoogleDirections.routes(listOf(o, Lines.pointAt(r.line, h.atKm - 0.02)), mode).firstOrNull()?.durationSec } ?: share(h.atKm)
                }
                Leg(c, toSec.coerceIn(0, r.durationSec), r.durationSec, r.distanceM, w.stop, r.line,
                    w.hits.drop(1).map { Later(crossingAt(it.pos, r, it.atKm), share(it.atKm)) }, r.summary?.takeIf { it.isNotBlank() })
            }
        }.awaitAll()
            // one crossing area, one way: crossings closer than SAME_AREA_KM (two tracks, two roads at the same spot)
            // would show twice under the same name; Google's own way first, then the fastest, are kept
            .sortedWith(compareBy({ if (it.own) 0 else 1 }, { it.viaSec }))
            .fold(ArrayList<Leg>()) { kept, l ->
                if (kept.none { k ->
                        val d = Geo.km(k.crossing.pos, l.crossing.pos)
                        when {
                            // the same crossing: a duplicate unless it is really another road
                            d < 0.08 -> Lines.deviationKm(l.line, k.line) < OTHER_ROAD_KM
                            // two crossings in one area, or two under the same name nearby ("near X" twice), confuse
                            d < SAME_AREA_KM -> true
                            else -> k.crossing.name == l.crossing.name && k.crossing.named == l.crossing.named && d < SAME_NAME_KM
                        }
                    }) kept += l
                kept
            }

        // 4. live trains at the stations next to the gates
        onStep(Step.TRAINS)
        val stations = legs.flatMap { it.crossings }.filter { it.kind == CrossingKind.GATE }
            .mapNotNull { db.nearbyStations(it.pos, 10.0, 1).firstOrNull() }
            .distinctBy { it.code }.take(6)
        val now = System.currentTimeMillis() / 1000
        val reuse = knownLive.filter { (code, b) -> stations.any { it.code == code } && now - b.fetchedEpochSec < 300 }
        val live = reuse + fetchLive(stations.filter { it.code !in reuse })

        Plan(start, startIsMe, to, mode, legs, sameSide = false, railsChecked = !fallback, startOnRailway = startOnRailway,
            rails = rails, liveStations = stations, live = live, liveFetchedSec = now, createdSec = now)
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

    /** What a road user reaching crossing [c] at [eta] is likely to find. */
    private fun atCrossing(c: Crossing, eta: Long, plan: Plan, pos: LatLng, now: Long): Pair<AtCrossing, GateEngine.Companion.Arrival?> {
        if (c.kind != CrossingKind.GATE) return AtCrossing(c, eta, 0, AtArrival.NO_GATE, null) to null
        val st = engine.gateStatus(Gate(c.name, c.pos, null, "route"), pos, plan.live, now)
        val a = GateEngine.arrival(st.passes, eta)
        val trains = a.trains.ifEmpty { st.passes.filter { a.nextClose != null && it.closeEpochSec >= a.nextClose }.take(2) }
        return AtCrossing(c, eta, a.waitSec.toInt(), if (a.waitSec > 0) AtArrival.CLOSED else AtArrival.OPEN, a.opensAt) to a.copy(trains = trains)
    }

    /** Time still to go to [c], first planned as [planned] from the start: shrinks with the straight-line distance left. */
    private fun remaining(planned: Int, c: Crossing, plan: Plan, here: LatLng?): Int {
        val startKm = Geo.km(plan.from.pos, c.pos)
        if (here == null || startKm < AT_KM) return planned
        return (planned * (Geo.km(here, c.pos) / startKm)).toInt().coerceIn(0, planned * 2 + 600)
    }

    /**
     * Every way as it stands at [now], best first. [here]: the phone's live position. Waits at gates further along
     * push the arrival at the next ones back, and count in the whole trip time.
     */
    fun evaluate(plan: Plan, here: LatLng?, now: Long): List<Option> {
        val pos = here ?: plan.from.pos
        return plan.legs.map { leg ->
            val toSec = remaining(leg.toCrossingSec, leg.crossing, plan, here)
            val (first, arrival) = atCrossing(leg.crossing, now + toSec, plan, pos, now)
            var delay = first.waitSec
            val later = leg.later.map { l ->
                val (at, _) = atCrossing(l.crossing, now + remaining(l.toSec, l.crossing, plan, here) + delay, plan, pos, now)
                delay += at.waitSec
                at
            }
            val after = (leg.viaSec - leg.toCrossingSec).coerceAtLeast(0)
            val trains = arrival?.trains ?: emptyList()
            Option(
                leg, toSec, first.etaSec, first.waitSec, toSec + after + delay, first.at, first.opensAt, arrival?.nextClose, trains,
                live = trains.isNotEmpty() && trains.all { it.timeSource == TimeSource.LIVE },
                googleWay = leg.own, later = later,
            )
        }
            // a minute either way is noise: then Google's own road (no extra stop), then a bridge (never closes)
            .sortedWith(compareBy({ it.totalSec / 60 }, { if (it.googleWay) 0 else 1 }, { if (it.leg.crossing.kind == CrossingKind.GATE) 1 else 0 }, { it.totalSec }))
    }
}
