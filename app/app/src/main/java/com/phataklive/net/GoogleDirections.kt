package com.phataklive.net

import android.net.Uri
import com.phataklive.data.LatLng
import okhttp3.Request
import org.json.JSONArray

/**
 * Google Maps road directions without an API key: loads google.com/maps/dir/... and the directions JSON that page
 * preloads for the Maps web client. Unofficial and undocumented: the layout was verified on 2026-11-14 and can change.
 *
 * The reply has each route's length (m) and time (s) but no full road line, so a route through a given crossing is
 * asked for by passing the crossing as a stop: start -> crossing -> destination.
 */
object GoogleDirections {

    /** [code]: Maps' travel mode in the dir URL (!3e..); [urlMode]: the same mode in a Maps navigation link. */
    enum class Mode(val code: Int, val urlMode: String) {
        TWO_WHEELER(9, "two-wheeler"), CAR(0, "driving"), WALK(2, "walking")
    }

    data class Route(val distanceM: Int, val durationSec: Int, val summary: String?)

    // the directions link is only in the desktop page
    private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/130.0 Safari/537.36"

    /** "23.49303,87.72876": Double.toString never uses a decimal comma, whatever the phone's language. */
    private fun ll(p: LatLng) = "${p.lat},${p.lng}"

    /** The Google Maps web page for this trip (also shown inside the app). */
    fun webUrl(points: List<LatLng>, mode: Mode): String =
        "https://www.google.com/maps/dir/" + points.joinToString("/") { ll(it) } + "/data=!4m2!4m1!3e${mode.code}?hl=en&gl=in"

    /**
     * Turn-by-turn navigation in the Google Maps app through [via]. Without [origin] Maps starts from where the phone
     * is, which is what navigation needs; a typed start place is passed on.
     */
    fun navigationUri(origin: LatLng?, via: LatLng, dest: LatLng, mode: Mode): Uri =
        Uri.parse("https://www.google.com/maps/dir/?api=1" +
            (origin?.let { "&origin=${ll(it)}" } ?: "") +
            "&destination=${ll(dest)}&waypoints=${ll(via)}&travelmode=${mode.urlMode}&dir_action=navigate")

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

    /** Routes Google offers through [points] in order (start, stops..., destination), its recommended one first. */
    fun routes(points: List<LatLng>, mode: Mode): List<Route> {
        require(points.size >= 2)
        val page = get(webUrl(points, mode))
        val i = page.indexOf("<link href=\"/maps/preview/directions")
        if (i < 0) throw java.io.IOException("Google did not return directions")
        val start = i + "<link href=\"".length
        val end = page.indexOf('"', start)
        if (end < 0) throw java.io.IOException("Google did not return directions")
        val link = page.substring(start, end).replace("&amp;", "&")
        return parse(get("https://www.google.com$link"))
    }

    /** Route rows of the directions JSON: [_, summary, [metres, "1.8 km"], [seconds, "7 min"], ...]. */
    internal fun parse(body: String): List<Route> {
        val json = JSONArray((if (body.startsWith(")]}'")) body.substring(4) else body).trim())
        val list = json.at(0, 1) as? JSONArray ?: return emptyList()
        return (0 until list.length()).mapNotNull { k ->
            val head = list.at(k, 0) as? JSONArray ?: return@mapNotNull null
            val m = (head.at(2, 0) as? Number)?.toInt() ?: return@mapNotNull null
            val s = (head.at(3, 0) as? Number)?.toInt() ?: return@mapNotNull null
            Route(m, s, head.at(1) as? String)
        }
    }
}
