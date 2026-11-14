package com.phataklive.search

import com.phataklive.data.Fuzzy
import com.phataklive.data.Gate
import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import com.phataklive.data.PhatakDb
import com.phataklive.data.Station
import com.phataklive.gate.GateEngine
import com.phataklive.net.GoogleMaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

/**
 * Online part of Phatak Live: which Google Maps lookups to run, running them in parallel,
 * and the live status of a single train.
 */
class LiveSearch(private val db: PhatakDb) {

    sealed class Call {
        /** gates Google knows within [radiusKm] of [center] (the user when null) */
        data class GateSearch(val radiusKm: Double, val center: LatLng? = null) : Call()
        /** a Maps search biased to [center] (the user when null); [radiusKm] null = anywhere in India */
        data class MapsSearch(val query: String, val center: LatLng? = null, val radiusKm: Double? = 30.0) : Call()
        data class StationLive(val name: String, val code: String) : Call()
    }

    /** Automatic scan: gates around the user, gates near the two nearest stations, live trains at the four nearest. */
    fun nearbyPlan(radiusKm: Int, stations: List<Station>): List<Call> =
        listOf<Call>(Call.GateSearch(radiusKm.toDouble())) +
            stations.take(2).map { Call.MapsSearch("railway gate near ${it.name}", radiusKm = maxOf(radiusKm, 30).toDouble()) } +
            stations.take(4).map { Call.StationLive(it.name, it.code) }

    private fun english(q: String) = q.any { it in 'a'..'z' || it in 'A'..'Z' }

    /**
     * Typed gate search, anywhere in India. The user's own words go to Google Maps as typed (Maps understands Hindi
     * and Bengali script and corrects spelling), plus a "railway gate" variant; stations whose name matches the
     * place part of the query, even misspelled, add the gates around them.
     */
    fun gatePlan(query: String, stations: List<Station>): List<Call> {
        val q = query.trim()
        return (listOf(Call.MapsSearch(q, radiusKm = null)) +
            if (english(q)) mapsVariants(q).map { Call.MapsSearch(it, radiusKm = null) }
            else listOf(Call.MapsSearch("$q রেল গেট phatak", radiusKm = null))) +
            stations.take(2).map { Call.MapsSearch("railway gate near ${it.name}", it.pos, 8.0) } +
            listOfNotNull(stations.firstOrNull()?.let { Call.GateSearch(6.0, it.pos) })
    }

    /** Google names gates either way, so both are asked: "<place> railway gate" and "<place> level crossing". */
    private fun mapsVariants(q: String): List<String> {
        val place = Fuzzy.placePart(q).ifBlank { q }
        return listOf("$place railway gate", "$place level crossing").distinct()
    }

    /**
     * Drops gates Google adds for weak queries (any gate near the map centre): a gate is kept when its name looks
     * like the typed place or it lies near a station that matches. Without a matching station everything is kept.
     */
    fun relevant(gates: List<Gate>, query: String, stations: List<Station>): List<Gate> {
        if (stations.isEmpty()) return gates
        val place = Fuzzy.placePart(query).ifBlank { query }
        return gates.filter { g ->
            Fuzzy.nameScore(place, g.name) >= 0.6 || g.name.contains(place, ignoreCase = true) ||
                stations.any { Geo.km(it.pos, g.pos) <= 15.0 }
        }
    }

    /** Gates near one station (a picked "Gates near … station" suggestion). */
    fun stationGatePlan(st: Station): List<Call> =
        listOf(Call.GateSearch(6.0, st.pos), Call.MapsSearch("railway gate near ${st.name}", st.pos, 8.0))

    /** Best name match to [query] first (in steps, so close spellings tie), then nearest to [from]. */
    fun rank(gates: List<Gate>, query: String, from: LatLng): List<Gate> {
        val q = Fuzzy.grams(query)
        return gates.sortedWith(
            compareByDescending<Gate> { (Fuzzy.similarity(q, Fuzzy.grams(it.name)) * 5).toInt() }.thenBy { Geo.km(from, it.pos) },
        )
    }

    sealed class Suggestion {
        data class GatePlace(val gate: Gate, val address: String?, val distanceKm: Double) : Suggestion()
        data class NearStation(val station: Station, val distanceKm: Double) : Suggestion()
    }

    /**
     * Suggestions while the user types: gates Google Maps finds for the words so far (anywhere, spelling corrected)
     * and stations whose name is close to the place part, as "Gates near … station".
     */
    suspend fun suggest(query: String, user: LatLng): List<Suggestion> = coroutineScope {
        val q = query.trim()
        val stationsJob = async(Dispatchers.IO) { runCatching { db.findStations(Fuzzy.placePart(q).ifBlank { q }, 2) }.getOrDefault(emptyList()) }
        // the words as typed go too: Google corrects spelling best on the full text ("bali raliway crosing")
        val queries = (listOf(q) + if (english(q)) mapsVariants(q) else emptyList()).distinct()
        val placeJobs = queries.map { mq -> async(Dispatchers.IO) { runCatching { GoogleMaps.searchPlaces(mq, user, null) }.getOrDefault(emptyList()) } }
        val gates = placeJobs.flatMap { it.await() }.filter { GoogleMaps.isGateName(it.name) }
            .distinctBy { "${it.name.lowercase()}|${(it.pos.lat * 500).toInt()}|${(it.pos.lng * 500).toInt()}" }
            .map { Gate(it.name, it.pos, it.placeId, "maps") to it.address }
        val stations = stationsJob.await()
        val ranked = rank(relevant(gates.map { it.first }, q, stations), q, user).take(4)
        ranked.map { g -> Suggestion.GatePlace(g, gates.first { it.first === g }.second, Geo.km(user, g.pos)) } +
            stations.map { Suggestion.NearStation(it, Geo.km(user, it.pos)) }
    }

    data class SearchResult(val gates: List<Gate>, val live: Map<String, GateEngine.LiveBoard>, val errors: Map<String, String>)

    suspend fun run(calls: List<Call>, user: LatLng, radiusKm: Double): SearchResult = coroutineScope {
        val gates = ArrayList<Gate>()
        val live = HashMap<String, GateEngine.LiveBoard>()
        val errors = HashMap<String, String>()
        val jobs = calls.map { c ->
            async(Dispatchers.IO) {
                c to runCatching {
                    when (c) {
                        is Call.GateSearch -> GoogleMaps.searchGates(c.center ?: user, c.radiusKm)
                        is Call.MapsSearch -> GoogleMaps.searchPlaces(c.query, c.center ?: user, c.radiusKm)
                            .filter { p -> GoogleMaps.isGateName(p.name) }
                            .map { Gate(it.name, it.pos, it.placeId, "maps") }
                        is Call.StationLive -> GateEngine.LiveBoard(System.currentTimeMillis() / 1000, GoogleMaps.stationDepartures(c.name, c.code))
                    }
                }
            }
        }
        for ((c, r) in jobs.awaitAll()) {
            val v = r.getOrNull()
            when {
                r.isFailure && c is Call.StationLive -> errors[c.code] = r.exceptionOrNull()?.message ?: "network error"
                v is List<*> -> v.filterIsInstance<Gate>().forEach { g ->
                    if (gates.none { Geo.km(it.pos, g.pos) < 0.08 || (it.name.equals(g.name, true) && Geo.km(it.pos, g.pos) < 0.3) }) gates += g
                }
                v is GateEngine.LiveBoard && c is Call.StationLive -> live[c.code] = v
            }
        }
        SearchResult(gates, live, errors)
    }

    // ------------------------------------------------------------------ live train status

    data class NextStop(val station: String, val code: String, val sched: Long, val est: Long, val platform: String?)

    data class TrainStatus(
        val no: String, val name: String, val live: Boolean, val delayMin: Int, val lastSeen: String?,
        val next: List<NextStop>,
        /** false when the typed name only matched approximately; [alternatives] are other close trains */
        val exactMatch: Boolean = true, val alternatives: List<Pair<String, String>> = emptyList(),
    )

    /** Hindi (०-९) and Bengali (০-৯) digits count as digits too. */
    private fun fiveDigits(s: String): String? =
        Regex("\\d{5}").find(s.map { if (it.isDigit()) '0' + Character.digit(it, 10) else it }.joinToString(""))?.value

    /** A train by number or name: its current run from the timetable, verified against Google's live departures. */
    suspend fun trainStatus(query: String): TrainStatus? {
        val match = withContext(Dispatchers.IO) { db.findTrain(fiveDigits(query), query) } ?: return null
        val no = match.no
        val (days, route) = withContext(Dispatchers.IO) { db.route(no) }
        val halts = route.filterIndexed { i, s -> s.halt > 0 || i == 0 || i == route.lastIndex }
        if (halts.size < 2) return null
        val now = System.currentTimeMillis() / 1000
        val today = LocalDate.now(GateEngine.IST)
        fun epoch(start: LocalDate, absMin: Int) = start.atStartOfDay(GateEngine.IST).toEpochSecond() + absMin * 60L
        // the run that is on the way now, else the next one to start
        val start = (-4..2).map { today.plusDays(it.toLong()) }
            .filter { (days shr (it.dayOfWeek.value - 1)) and 1 == 1 }
            .firstOrNull { epoch(it, halts.last().arrAbs) + 3600 >= now } ?: return null
        val upcoming = halts.filter { epoch(start, it.depAbs) >= now - 600 }.ifEmpty { listOf(halts.last()) }

        // live: Google's departures at the next two stops
        var delayMin = 0
        var live = false
        var lastSeen: String? = halts.lastOrNull { epoch(start, it.depAbs) < now }?.name
        val platforms = HashMap<String, String>()
        coroutineScope {
            upcoming.take(2).map { s -> async(Dispatchers.IO) { s to runCatching { GoogleMaps.stationDepartures(s.name, s.code) }.getOrNull() } }
                .awaitAll()
        }.forEach { (s, rows) ->
            val row = rows?.filter { it.realtime && it.trainNo == no }
                ?.minByOrNull { abs((it.schedEpochSec ?: it.estEpochSec) - epoch(start, s.depAbs)) } ?: return@forEach
            if (!live) {
                live = true
                delayMin = row.delayMin
                row.trainPos?.let { p -> db.nearbyStations(p, 15.0, 1).firstOrNull()?.let { lastSeen = it.name } }
            }
            row.platform?.let { platforms[s.code] = it }
        }
        val next = upcoming.take(3).map { s ->
            val sched = epoch(start, s.depAbs)
            NextStop(s.name, s.code, sched, sched + delayMin * 60L, platforms[s.code])
        }
        return TrainStatus(no, match.name, live, delayMin, if (live) lastSeen else null, next, match.exact, match.alternatives)
    }
}
