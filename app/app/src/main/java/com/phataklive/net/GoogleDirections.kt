package com.phataklive.net

import android.net.Uri
import com.phataklive.data.LatLng
import okhttp3.Request
import org.json.JSONArray

/**
 * Google Maps directions without an API key: loads google.com/maps/dir/... and the directions JSON that page
 * preloads for the Maps web client. Unofficial and undocumented: the layout was verified on 2026-11-14 and can change.
 *
 * Each route has its length (m), time (s) and its road line: `[0][7][i]` holds the latitudes and longitudes in 1e-7
 * degrees, as a first value followed by differences. Train directions between two stations give the railway line.
 */
object GoogleDirections {

    /** [code]: Maps' travel mode in the dir URL (!3e..); [urlMode]: the same mode in a Maps navigation link. */
    enum class Mode(val code: Int, val urlMode: String) {
        TWO_WHEELER(9, "two-wheeler"), CAR(0, "driving"), WALK(2, "walking")
    }

    /** Public transport (trains): its route between two stations follows the railway. */
    private const val TRANSIT = 3

    data class Route(val distanceM: Int, val durationSec: Int, val summary: String?, val line: List<LatLng>)

    // the directions link is only in the desktop page
    private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/130.0 Safari/537.36"

    /** "23.49303,87.72876": Double.toString never uses a decimal comma, whatever the phone's language. */
    private fun ll(p: LatLng) = "${p.lat},${p.lng}"

    private fun dirUrl(points: List<LatLng>, code: Int) =
        "https://www.google.com/maps/dir/" + points.joinToString("/") { ll(it) } + "/data=!4m2!4m1!3e$code?hl=en&gl=in"

    /** The Google Maps web page for this trip (also shown inside the app). */
    fun webUrl(points: List<LatLng>, mode: Mode): String = dirUrl(points, mode.code)

    /**
     * Turn-by-turn navigation in the Google Maps app, through [stop] when given (a point just past the chosen
     * crossing). Without [origin] Maps starts from where the phone is, which is what navigation needs.
     */
    fun navigationUri(origin: LatLng?, stop: LatLng?, dest: LatLng, mode: Mode): Uri =
        Uri.parse("https://www.google.com/maps/dir/?api=1" +
            (origin?.let { "&origin=${ll(it)}" } ?: "") +
            "&destination=${ll(dest)}" + (stop?.let { "&waypoints=${ll(it)}" } ?: "") +
            "&travelmode=${mode.urlMode}&dir_action=navigate")

    private fun get(url: String): String {
        val req = Request.Builder().url(url)
            .header("User-Agent", DESKTOP_UA)
            .header("Accept-Language", "en-IN,en;q=0.9")
            .build()
        GoogleMaps.http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("Google Maps HTTP ${r.code}")
            return r.body!!.string()
        }
    }

    private fun fetch(points: List<LatLng>, code: Int): List<Route> {
        require(points.size >= 2)
        val page = get(dirUrl(points, code))
        val i = page.indexOf("<link href=\"/maps/preview/directions")
        if (i < 0) throw java.io.IOException("Google did not return directions")
        val start = i + "<link href=\"".length
        val end = page.indexOf('"', start)
        if (end < 0) throw java.io.IOException("Google did not return directions")
        return parse(get("https://www.google.com" + page.substring(start, end).replace("&amp;", "&")))
    }

    /** One retry after a short pause: Google sometimes answers a burst of requests with an error page. */
    private fun withRetry(points: List<LatLng>, code: Int): List<Route> =
        try { fetch(points, code) } catch (e: Exception) { Thread.sleep(1200); fetch(points, code) }

    /** Road routes Google offers through [points] in order (start, stops..., destination), its recommended one first. */
    fun routes(points: List<LatLng>, mode: Mode): List<Route> = withRetry(points, mode.code)

    /** Train routes between two stations: their lines follow the railway. */
    fun trainRoutes(a: LatLng, b: LatLng): List<Route> = withRetry(listOf(a, b), TRANSIT)

    /** Route rows `[0][1][i][0]` = [_, summary, [metres, "1.8 km"], [seconds, "7 min"], ...] with lines `[0][7][i]`. */
    internal fun parse(body: String): List<Route> {
        val json = JSONArray((if (body.startsWith(")]}'")) body.substring(4) else body).trim())
        val list = json.at(0, 1) as? JSONArray ?: return emptyList()
        val lines = json.at(0, 7) as? JSONArray
        return (0 until list.length()).mapNotNull { k ->
            val head = list.at(k, 0) as? JSONArray ?: return@mapNotNull null
            val m = (head.at(2, 0) as? Number)?.toInt() ?: return@mapNotNull null
            val s = (head.at(3, 0) as? Number)?.toInt() ?: return@mapNotNull null
            Route(m, s, head.at(1) as? String, decodeLine(lines?.optJSONArray(k)))
        }
    }

    /** [lat deltas, lng deltas, ...] in 1e-7 degrees -> points; empty when the layout is not as expected. */
    internal fun decodeLine(g: JSONArray?): List<LatLng> {
        val lat = g?.optJSONArray(0) ?: return emptyList()
        val lng = g.optJSONArray(1) ?: return emptyList()
        if (lat.length() != lng.length() || lat.length() < 2) return emptyList()
        val out = ArrayList<LatLng>(lat.length())
        var a = 0L; var b = 0L
        for (i in 0 until lat.length()) {
            if (lat.opt(i) !is Number || lng.opt(i) !is Number) return emptyList()
            a += lat.getLong(i); b += lng.getLong(i)
            out += LatLng(a / 1e7, b / 1e7)
        }
        return out
    }
}
