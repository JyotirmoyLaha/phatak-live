package com.phataklive.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Railway lines between neighbouring stations, as Google's train directions draw them (filled in by the Route
 * planner, kept for the app's lifetime: track doesn't move). The gate engine uses them to check that a train's
 * section really passes a gate, which matters at junctions where several lines meet.
 */
object RailLines {
    private val lines = ConcurrentHashMap<String, List<LatLng>>()

    private fun key(a: String, b: String) = if (a < b) "$a|$b" else "$b|$a"

    fun get(a: String, b: String): List<LatLng>? = lines[key(a, b)]

    fun put(a: String, b: String, line: List<LatLng>) { lines[key(a, b)] = line }
}
