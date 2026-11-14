package com.phataklive.route

import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import kotlin.math.abs
import kotlin.math.cos

/** Road and rail lines (lists of points): where they cross, and points along them. */
object Lines {

    /** Distance from the start of [line] to each of its points, in km. */
    fun along(line: List<LatLng>): DoubleArray {
        val acc = DoubleArray(line.size)
        for (i in 1 until line.size) acc[i] = acc[i - 1] + Geo.km(line[i - 1], line[i])
        return acc
    }

    fun length(line: List<LatLng>): Double = if (line.size < 2) 0.0 else along(line).last()

    /** The point [km] along [line] (clamped to its ends). */
    fun pointAt(line: List<LatLng>, km: Double): LatLng {
        if (line.isEmpty()) throw IllegalArgumentException("empty line")
        val acc = along(line)
        val s = km.coerceIn(0.0, acc.last())
        for (i in 0 until line.size - 1) {
            if (acc[i + 1] >= s) {
                val f = (s - acc[i]) / (acc[i + 1] - acc[i]).coerceAtLeast(1e-9)
                return LatLng(line[i].lat + f * (line[i + 1].lat - line[i].lat), line[i].lng + f * (line[i + 1].lng - line[i].lng))
            }
        }
        return line.last()
    }

    /** Shortest distance from [p] to any of [lines], in km (flat local frame: fine at these distances). */
    fun distanceKm(p: LatLng, lines: List<List<LatLng>>): Double {
        var best = Double.MAX_VALUE
        for (line in lines) {
            if (line.size == 1) best = minOf(best, Geo.km(p, line[0]))
            for (i in 0 until line.size - 1) {
                val (t, off) = Geo.project(p, line[i], line[i + 1])
                best = minOf(best, if (t in 0.0..1.0) off else minOf(Geo.km(p, line[i]), Geo.km(p, line[i + 1])))
            }
        }
        return best
    }

    /** How far [a] gets from [b] at most, in km (points of [a] against the whole of [b]). */
    fun deviationKm(a: List<LatLng>, b: List<LatLng>): Double = a.maxOfOrNull { distanceKm(it, listOf(b)) } ?: 0.0

    /** The point of [a] farthest from [b]: where one road leaves the other. */
    fun farthestFrom(a: List<LatLng>, b: List<LatLng>): LatLng = a.maxByOrNull { distanceKm(it, listOf(b)) } ?: a.first()

    /** Where a road meets the railway: the point, and how far along the road it is (km). */
    data class Hit(val pos: LatLng, val atKm: Double)

    /** Same place: closer than this is one crossing (double tracks, a road drawn as two lines). */
    const val SAME_KM = 0.06

    /** Every place [road] crosses any of [rails], in road order; crossings closer than [SAME_KM] count once. */
    fun crossings(road: List<LatLng>, rails: List<List<LatLng>>): List<Hit> {
        if (road.size < 2) return emptyList()
        val acc = along(road)
        val hits = ArrayList<Hit>()
        for (i in 0 until road.size - 1) {
            val a = road[i]; val b = road[i + 1]
            val k = cos(Math.toRadians(a.lat))
            for (rail in rails) for (j in 0 until rail.size - 1) {
                val c = rail[j]; val d = rail[j + 1]
                // quick reject by bounding boxes
                if (maxOf(a.lat, b.lat) < minOf(c.lat, d.lat) || minOf(a.lat, b.lat) > maxOf(c.lat, d.lat) ||
                    maxOf(a.lng, b.lng) < minOf(c.lng, d.lng) || minOf(a.lng, b.lng) > maxOf(c.lng, d.lng)) continue
                val t = segmentT(a, b, c, d, k) ?: continue
                val p = LatLng(a.lat + t * (b.lat - a.lat), a.lng + t * (b.lng - a.lng))
                if (hits.none { Geo.km(it.pos, p) < SAME_KM }) hits += Hit(p, acc[i] + t * (acc[i + 1] - acc[i]))
            }
        }
        return hits.sortedBy { it.atKm }
    }

    /** Position (0..1) along a->b where it crosses c->d, or null; [k] scales longitude to equal-distance units. */
    internal fun segmentT(a: LatLng, b: LatLng, c: LatLng, d: LatLng, k: Double): Double? {
        val x1 = a.lng * k; val y1 = a.lat; val x2 = b.lng * k; val y2 = b.lat
        val x3 = c.lng * k; val y3 = c.lat; val x4 = d.lng * k; val y4 = d.lat
        val den = (x2 - x1) * (y4 - y3) - (y2 - y1) * (x4 - x3)
        if (abs(den) < 1e-18) return null
        val t = ((x3 - x1) * (y4 - y3) - (y3 - y1) * (x4 - x3)) / den
        val u = ((x3 - x1) * (y2 - y1) - (y3 - y1) * (x2 - x1)) / den
        return if (t in 0.0..1.0 && u in 0.0..1.0) t else null
    }
}
