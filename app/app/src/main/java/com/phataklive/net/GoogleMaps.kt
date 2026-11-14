package com.phataklive.net

import com.phataklive.data.Gate
import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import com.phataklive.data.LiveDeparture
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Reads the JSON that google.com/search?tbm=map returns to the Maps web client (no API key).
 * Unofficial and undocumented: the layout was verified on 2026-11-13 and can change.
 */
object GoogleMaps {
    internal val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/130.0 Mobile Safari/537.36"

    data class Place(val name: String, val pos: LatLng, val categories: List<String>, val placeId: String?, val address: String?)

    /**
     * Network calls must never run on the screen thread: that freezes the app (Android shows "not responding").
     * A mistake fails at once here instead of freezing. (In plain JVM unit tests there is no Looper: nothing to check.)
     */
    internal fun requireBackground() {
        val main = runCatching { android.os.Looper.getMainLooper() }.getOrNull() ?: return
        check(!main.isCurrentThread) { "network call on the screen thread" }
    }

    private fun fetch(query: String, near: LatLng?, spanMeters: Int): JSONArray {
        requireBackground()
        val q = URLEncoder.encode(query, "UTF-8")
        val pb = if (near != null)
            "&pb=!4m12!1m3!1d$spanMeters!2d${near.lng}!3d${near.lat}!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!7i20!10b1"
        else ""
        val req = Request.Builder()
            .url("https://www.google.com/search?tbm=map&hl=en&gl=in&q=$q$pb")
            .header("User-Agent", UA)
            .header("Accept-Language", "en-IN,en;q=0.9")
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("Google Maps HTTP ${r.code}")
            var text = r.body!!.string().substringBefore("/*\"\"*/")
            if (text.startsWith(")]}'")) text = text.substring(4)
            return JSONArray(text.trim())
        }
    }

    /**
     * Places matching [query] around [near]. Google only biases by location, so with a [radiusKm] results are
     * distance-filtered; with `null` (typed searches) a place anywhere in India is kept. Google corrects spelling
     * ("bali raliway crosing" finds Bally Railway Crossing).
     */
    fun searchPlaces(query: String, near: LatLng, radiusKm: Double?): List<Place> {
        val data = fetch(query, near, ((radiusKm ?: 30.0) * 2000).toInt().coerceIn(2000, 60000))
        fun place(p: JSONArray?): Place? {
            p ?: return null
            val name = p.at(11) as? String ?: return null
            val lat = (p.at(9, 2) as? Number)?.toDouble() ?: return null
            val lng = (p.at(9, 3) as? Number)?.toDouble() ?: return null
            val pos = LatLng(lat, lng)
            if (radiusKm != null && Geo.km(near, pos) > radiusKm) return null
            val cats = (p.at(13) as? JSONArray)?.let { a -> (0 until a.length()).mapNotNull { a.optString(it, null) } } ?: emptyList()
            return Place(name, pos, cats, p.at(78) as? String, p.at(39) as? String)
        }
        // a query naming one place gets that place's card instead of a list
        val list = data.at(0, 1) as? JSONArray
        val found = (0 until (list?.length() ?: 0)).mapNotNull { place(list!!.at(it, 14) as? JSONArray) }
        return found.ifEmpty { listOfNotNull(place(data.at(6) as? JSONArray)) }
    }

    /**
     * Places for what is typed so far, from the autocomplete of the Google Maps search box (google.com/s?tbm=map&suggest=p):
     * built for half-typed words ("bard" -> Bardhaman). Only real places (with a position) are kept; search phrases
     * people typed ("gonna kotar somoy lagbe") are not places and are dropped. Biased to [near].
     */
    fun autocomplete(query: String, near: LatLng): List<Place> {
        requireBackground()
        val q = URLEncoder.encode(query, "UTF-8")
        val pb = "!2i1!4m12!1m3!1d20000!2d${near.lng}!3d${near.lat}!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!7i20!10b1"
        val req = Request.Builder()
            .url("https://www.google.com/s?tbm=map&suggest=p&gs_ri=maps&hl=en&gl=in&authuser=0&q=$q&pb=$pb")
            .header("User-Agent", UA)
            .header("Accept-Language", "en-IN,en;q=0.9")
            .build()
        val body = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("Google Maps HTTP ${r.code}")
            r.body!!.string()
        }
        return parseAutocomplete(body)
    }

    internal fun parseAutocomplete(body: String): List<Place> {
        val json = JSONArray((if (body.startsWith(")]}'")) body.substring(4) else body).trim())
        val list = json.at(0, 1) as? JSONArray ?: return emptyList()
        fun findPos(n: Any?): LatLng? {
            val a = n as? JSONArray ?: return null
            if (a.length() == 4 && a.isNull(0) && a.opt(2) is Number && a.opt(3) is Number) return LatLng(a.getDouble(2), a.getDouble(3))
            for (i in 0 until a.length()) findPos(a.opt(i))?.let { return it }
            return null
        }
        return (0 until list.length()).mapNotNull { i ->
            val s = list.optJSONArray(i)?.optJSONArray(22) ?: return@mapNotNull null
            val text = s.at(0, 0) as? String ?: return@mapNotNull null
            val pos = findPos(s) ?: return@mapNotNull null
            // "Bardhaman Bus Stand, Goda P, West Bengal": the name, then where it is
            val name = text.substringBefore(", ").trim()
            Place(name, pos, emptyList(), null, text.substringAfter(", ", "").ifBlank { null })
        }
    }

    private val gateWords = Regex(
        "(level\\s*crossing|\\bl\\.?\\s?c\\.?\\b|lc\\s*gate|rail(way)?\\s*(gate|crossing|phatak)|phatak|crossing\\s*gate|label\\s*crossing)",
        RegexOption.IGNORE_CASE,
    )

    // Things that contain the words above but are not level crossings: station entrances ("New Delhi Railway Station
    // Gate No. 2"), streets named after a gate ("Railway Gate Rd"), and shops or stops nearby.
    private val notAGate = Regex(
        "(\\bstation\\b|\\broad\\b|\\brd\\b|\\bstreet\\b|\\bst\\.?$|\\blane\\b|\\bmarg\\b|\\bcolony\\b|\\bnagar\\b|\\bmetro\\b|" +
            "\\bbus\\b|\\bstop\\b|\\bhotel\\b|\\bschool\\b|\\bmarket\\b|\\bbaza+r\\b|\\bhaat\\b|\\bshop\\b|\\bstore\\b|\\bapartment|\\bsociety\\b|\\bparking\\b)",
        RegexOption.IGNORE_CASE,
    )

    /** True for names of rail gates / level crossings ("LC gate no 24", "Rail Phatak"), not "Gateway Hotel" or "Railway Gate Rd". */
    fun isGateName(name: String): Boolean = gateWords.containsMatchIn(name) && !notAGate.containsMatchIn(name)

    /** Rail gates (level crossings) Google Maps knows around [near]. */
    fun searchGates(near: LatLng, radiusKm: Double): List<Gate> {
        val found = ArrayList<Gate>()
        for (q in listOf("railway gate", "level crossing", "railway crossing gate", "rail phatak")) {
            val places = runCatching { searchPlaces(q, near, radiusKm) }.getOrDefault(emptyList())
            for (p in places) {
                if (!isGateName(p.name)) continue
                // same crossing listed twice: very close, or same name a little apart
                if (found.any { Geo.km(it.pos, p.pos) < 0.08 || (it.name.equals(p.name, true) && Geo.km(it.pos, p.pos) < 0.3) }) continue
                found += Gate(p.name, p.pos, p.placeId, "maps")
            }
        }
        return found
    }

    private val plusCode = Regex("^[23456789CFGHJMPQRVWX]{4,8}\\+[23456789CFGHJMPQRVWX]{2,3},? (.+)$")

    /** Place name for a coordinate: Google labels it "7MRV+2P Bolpur, West Bengal" (plus code + locality). */
    fun placeName(p: LatLng): String? {
        fun walk(n: Any?): Sequence<String> = when (n) {
            is JSONArray -> (0 until n.length()).asSequence().flatMap { walk(n.opt(it)) }
            is String -> sequenceOf(n)
            else -> emptySequence()
        }
        return walk(fetch("${p.lat},${p.lng}", null, 0)).firstNotNullOfOrNull { plusCode.matchEntire(it)?.groupValues?.get(1) }
    }

    /** Live departures Google shows on a railway station's Maps card. */
    fun stationDepartures(stationName: String, stationCode: String): List<LiveDeparture> {
        // adding the station code makes Google miss the station card, so it is only the last try
        var place: JSONArray? = null
        for (q in listOf("$stationName railway station", "$stationName station", "$stationName railway station $stationCode")) {
            val data = fetch(q, null, 0)
            val list = data.at(0, 1) as? JSONArray
            place = (0 until (list?.length() ?: 0)).asSequence()
                .mapNotNull { list!!.at(it, 14) as? JSONArray }
                .firstOrNull { it.at(62, 1, 0, 2) != null }
            if (place != null) break
        }
        val trains = place?.at(62, 1, 0, 2) as? JSONArray ?: return emptyList()
        val out = ArrayList<LiveDeparture>()
        for (i in 0 until trains.length()) {
            val t = trains.optJSONArray(i) ?: continue
            val name = t.at(1, 0, 0) as? String ?: continue
            val row = t.at(1, 0, 3, 0) as? JSONArray ?: continue
            val dep = row.optJSONArray(0) ?: continue
            val est = (dep.at(0, 0) as? Number)?.toLong() ?: continue
            val sched = (dep.at(6, 0) as? Number)?.toLong()
            val flag = dep.at(4) as? Number
            val lat = (dep.at(10, 1, 0, 0, 1, 2) as? Number)?.toDouble()
            val lng = (dep.at(10, 1, 0, 0, 1, 3) as? Number)?.toDouble()
            val posTs = (dep.at(10, 1, 0, 0, 2) as? Number)?.toLong()
            val platform = (0 until row.length()).map { row.opt(it) }.filterIsInstance<String>()
                .firstOrNull { it.startsWith("Platform", ignoreCase = true) }
            out += LiveDeparture(
                trainNo = t.at(5, 0, 1, 0) as? String,
                trainName = name,
                estEpochSec = est,
                schedEpochSec = sched,
                realtime = flag != null,
                platform = platform,
                trainPos = if (lat != null && lng != null) LatLng(lat, lng) else null,
                trainPosEpochSec = posTs,
            )
        }
        return out
    }
}

/** Safe nested index into Google's positional JSON arrays. */
internal fun JSONArray.at(vararg path: Int): Any? {
    var node: Any? = this
    for (i in path) {
        val a = node as? JSONArray ?: return null
        if (i < 0 || i >= a.length() || a.isNull(i)) return null
        node = a.get(i)
    }
    return node
}
