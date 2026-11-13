package com.phataklive.data

data class LatLng(val lat: Double, val lng: Double)

data class Station(val code: String, val name: String, val pos: LatLng, val distanceKm: Double = 0.0)

/** One train's call (or pass-through) at a station, from the offline timetable. */
data class StopTime(
    val trainNo: String,
    val trainName: String,
    val seq: Int,
    val stationCode: String,
    /** minutes since midnight of the day the train left its source */
    val arrAbs: Int,
    val depAbs: Int,
    val halt: Int,
    val day: Int,
    val runningDays: Int,
)

/** Live departure row read from Google Maps for one station. */
data class LiveDeparture(
    val trainNo: String?,
    val trainName: String,
    val estEpochSec: Long,
    val schedEpochSec: Long?,
    /** true when Google marks the row as real-time (on time or delayed) */
    val realtime: Boolean,
    val platform: String?,
    val trainPos: LatLng?,
    val trainPosEpochSec: Long?,
) {
    val delayMin: Int get() = if (schedEpochSec == null) 0 else ((estEpochSec - schedEpochSec) / 60).toInt()
}

data class Gate(
    val name: String,
    val pos: LatLng,
    val placeId: String?,
    /** where the gate came from ("maps": Google Maps search) */
    val source: String,
)

enum class TimeSource { LIVE, SCHEDULED }

/** A train expected at a gate, with the closing window the app derives from it. */
data class GatePass(
    val trainNo: String,
    val trainName: String,
    val passEpochSec: Long,
    val closeEpochSec: Long,
    val openEpochSec: Long,
    val delayMin: Int,
    val timeSource: TimeSource,
    val liveCheckedEpochSec: Long?,
    val viaStation: String,
)

enum class GateState { CLOSED, CLOSING_SOON, OPEN, UNKNOWN }

data class GateStatus(
    val gate: Gate,
    val distanceKm: Double,
    val nearestStation: Station?,
    val state: GateState,
    val headline: String,
    val detail: String,
    val verified: Boolean,
    val passes: List<GatePass>,
    /** trains of the current (or next) closing window, and its close/open times */
    val window: List<GatePass> = emptyList(),
    val windowClose: Long? = null,
    val windowOpen: Long? = null,
)

data class StationBoard(
    val station: Station,
    val trains: List<BoardRow>,
    val liveAvailable: Boolean,
    val liveError: String?,
)

data class BoardRow(
    val trainNo: String,
    val trainName: String,
    val timeEpochSec: Long,
    val delayMin: Int,
    val source: TimeSource,
    val platform: String?,
    val halts: Boolean,
)
