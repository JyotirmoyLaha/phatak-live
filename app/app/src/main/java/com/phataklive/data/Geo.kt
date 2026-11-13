package com.phataklive.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sqrt

object Geo {
    private const val R = 6371.0088

    fun km(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = Math.sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * Math.sin(dLng / 2).let { it * it }
        return 2 * R * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Projects [p] onto segment a->b in a local flat frame (fine at the few-km scale used here).
     * Returns (t, offTrackKm): t = 0 at a, 1 at b (not clamped), offTrackKm = distance from the line.
     */
    fun project(p: LatLng, a: LatLng, b: LatLng): Pair<Double, Double> {
        val kx = 111.320 * cos(Math.toRadians(a.lat))
        val ky = 110.574
        val bx = (b.lng - a.lng) * kx; val by = (b.lat - a.lat) * ky
        val px = (p.lng - a.lng) * kx; val py = (p.lat - a.lat) * ky
        val len2 = bx * bx + by * by
        if (len2 < 1e-9) return 0.0 to sqrt(px * px + py * py)
        val t = (px * bx + py * by) / len2
        val cx = t * bx; val cy = t * by
        return t to sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
    }
}
