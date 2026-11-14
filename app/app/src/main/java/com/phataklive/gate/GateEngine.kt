package com.phataklive.gate

import com.phataklive.data.BoardRow
import com.phataklive.data.Gate
import com.phataklive.data.GatePass
import com.phataklive.data.GateState
import com.phataklive.data.GateStatus
import com.phataklive.data.Geo
import com.phataklive.data.LatLng
import com.phataklive.data.LiveDeparture
import com.phataklive.data.PhatakDb
import com.phataklive.data.Station
import com.phataklive.data.StopTime
import com.phataklive.data.TimeSource
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Works out when each rail gate closes and opens, following how gatemen work (sources in README "Gate timing rule").
 *
 *   - The gate CLOSES when the station master sends the train towards it. Before taking off the departure signal
 *     (or giving line clear) for a train, the station master gives the gateman the train's number and direction;
 *     the gateman closes and locks the gate and reports back with his private number. So the gate goes down about
 *     [CLOSE_AHEAD_MIN] min before the train leaves the station behind the gate, and stays down while the train
 *     runs to it: the lead time is the run from that station to the gate, not a fixed number. A train that does
 *     not stop at that station runs through on signals cleared while it approaches: [THROUGH_EXTRA_MIN] more.
 *   - Gates within a station's limits work with that station's signals, which are not cleared more than 10 min
 *     before a train is due: on the arrival side the lead is capped at [STATION_GATE_MAX_LEAD_MIN].
 *   - The lead is kept within [MIN_LEAD_MIN]..[MAX_LEAD_MIN] (long sections, halts that are not block stations).
 *   - The gate OPENS [OPEN_AFTER_MIN] min after the train has passed: tail board / tail lamp check, then the station
 *     master's permission, given only when no other train is in the section. Windows of trains that follow each
 *     other closely are merged: the gate stays down.
 *
 * When the train reaches the gate: the offline timetable has every station the train passes,
 * including ones where it does not stop, so the gate is placed on the track segment between two
 * consecutive stations and the time is interpolated by distance along that segment.
 * Live data (Google Maps station departures) shifts that time by the train's current delay.
 */
class GateEngine(private val db: PhatakDb) {

    companion object {
        /** private-number exchange + closing and locking the gate, before the train is sent off */
        const val CLOSE_AHEAD_MIN = 3
        /** a train that does not stop there runs through on signals cleared while it is still approaching */
        const val THROUGH_EXTRA_MIN = 3
        /** station signals are not taken off more than 10 min before the train is due (SR 3.36/2) */
        const val STATION_GATE_MAX_LEAD_MIN = 10
        const val MIN_LEAD_MIN = 4
        const val MAX_LEAD_MIN = 15
        /** lead used when the station the train comes from is unknown (live-only trains) */
        const val TYPICAL_LEAD_MIN = 8
        const val OPEN_AFTER_MIN = 3
        /** "Closing soon" in the last minutes before the gate goes down */
        const val SOON_MIN = 5
        const val LOOKAHEAD_MIN = 180
        /** a gap shorter than this is not worth opening for: the gate stays down between the two trains */
        private const val MERGE_GAP_SEC = 180
        /** reaching a gate less than a minute before it goes down counts as finding it closed: never race a gate */
        const val ARRIVE_MARGIN_SEC = 60
        private const val STATION_LIMIT_KM = 0.6
        private const val MAX_OFF_TRACK_KM = 1.2
        val IST: ZoneId = ZoneId.of("Asia/Kolkata")
        /** "7:23 PM" in IST, built by hand: Android's formatter may put a narrow no-break space before PM. */
        fun fmt(epochSec: Long): String {
            val t = java.time.Instant.ofEpochSecond(epochSec).atZone(IST)
            val h = t.hour % 12
            return "${if (h == 0) 12 else h}:${"%02d".format(java.util.Locale.ROOT, t.minute)} ${if (t.hour < 12) "AM" else "PM"}"
        }

        /**
         * What someone reaching the gate at [etaSec] finds. [waitSec] > 0: the gate is down then (or goes down within
         * [ARRIVE_MARGIN_SEC] of arriving, too close to get through safely) and opens at [opensAt]. [nextClose]: when it
         * goes down next after that. [trains]: the trains of the closing that holds them up.
         */
        data class Arrival(val waitSec: Long, val opensAt: Long?, val nextClose: Long?, val trains: List<GatePass>)

        fun arrival(passes: List<GatePass>, etaSec: Long): Arrival {
            data class W(val close: Long, var open: Long, val trains: MutableList<GatePass>)
            val windows = ArrayList<W>()
            for (p in passes.sortedBy { it.closeEpochSec }) {
                val last = windows.lastOrNull()
                if (last != null && p.closeEpochSec <= last.open + MERGE_GAP_SEC) { last.open = maxOf(last.open, p.openEpochSec); last.trains += p }
                else windows += W(p.closeEpochSec, p.openEpochSec, mutableListOf(p))
            }
            val hit = windows.firstOrNull { etaSec >= it.close - ARRIVE_MARGIN_SEC && etaSec < it.open }
                ?: return Arrival(0, null, windows.firstOrNull { it.close > etaSec }?.close, emptyList())
            return Arrival(hit.open - etaSec, hit.open, windows.firstOrNull { it.close > hit.open }?.close, hit.trains)
        }
    }

    /** Live departures fetched per station code, with the time they were fetched. */
    data class LiveBoard(val fetchedEpochSec: Long, val rows: List<LiveDeparture>)

    // ---------------------------------------------------------------- timetable -> real time

    /** Epoch seconds for minute [absMin] of a run that started on [start] (IST). */
    private fun epoch(start: LocalDate, absMin: Double): Long =
        start.atStartOfDay(IST).toEpochSecond() + (absMin * 60).roundToInt()

    private fun runsOn(runningDays: Int, start: LocalDate): Boolean {
        val bit = start.dayOfWeek.value - 1         // bit0 = Monday ... bit6 = Sunday
        return (runningDays shr bit) and 1 == 1
    }

    /** Run start dates whose call at minute [absMin] lands in [from, to]. */
    private fun runsInWindow(st: StopTime, absMin: Double, from: Long, to: Long): List<LocalDate> {
        val today = LocalDate.now(IST)
        return (-4..1).map { today.plusDays(it.toLong()) }.filter { d ->
            runsOn(st.runningDays, d) && epoch(d, absMin) in from..to
        }
    }

    private fun matchLive(board: LiveBoard?, trainNo: String, trainName: String, schedEpoch: Long): LiveDeparture? {
        if (board == null) return null
        return board.rows.filter { it.realtime }.filter { row ->
            row.trainNo == trainNo || (row.trainNo == null && row.trainName.equals(trainName, ignoreCase = true))
        }.minByOrNull { abs((it.schedEpochSec ?: it.estEpochSec) - schedEpoch) }
            ?.takeIf { abs((it.schedEpochSec ?: it.estEpochSec) - schedEpoch) < 6 * 3600 }
    }

    // ---------------------------------------------------------------- gate placement

    /** [leadMin]: how long before the train reaches the gate it goes down */
    private data class Placement(val station: Station, val stop: StopTime, val gateAbsMin: Double, val offTrackKm: Double, val leadMin: Double)

    private fun place(gate: LatLng, s: Station, self: StopTime, prev: StopTime?, next: StopTime?): Placement? {
        // gate at this station: best possible fit, so the train is timed (and live-matched) here
        val atStation = Geo.km(gate, s.pos) < STATION_LIMIT_KM
        var best: Placement? = null
        var bestAlong = Double.NEGATIVE_INFINITY
        fun consider(other: StopTime?, forward: Boolean) {
            other ?: return
            val o = db.station(other.stationCode) ?: return
            val (t, off) = Geo.project(gate, s.pos, o.pos)
            if (!atStation && (t < -0.05 || t > 1.05 || off > MAX_OFF_TRACK_KM)) return
            val tc = t.coerceIn(0.0, 1.0)
            val abs: Double
            var lead: Double
            if (forward) {
                // the train leaves this station, passes the gate, then reaches [other]
                var run = (other.arrAbs - self.depAbs).toDouble(); if (run < 0) run += 1440
                abs = self.depAbs + tc * run
                lead = tc * run + CLOSE_AHEAD_MIN + if (self.halt == 0 && prev != null) THROUGH_EXTRA_MIN else 0
                if (Geo.km(gate, o.pos) < STATION_LIMIT_KM) lead = min(lead, STATION_GATE_MAX_LEAD_MIN.toDouble())
            } else {
                // the train leaves [other], passes the gate, then reaches this station
                var run = (self.arrAbs - other.depAbs).toDouble(); if (run < 0) run += 1440
                abs = self.arrAbs - tc * run
                lead = (1 - tc) * run + CLOSE_AHEAD_MIN + if (other.halt == 0) THROUGH_EXTRA_MIN else 0
                if (atStation) lead = min(lead, STATION_GATE_MAX_LEAD_MIN.toDouble())
            }
            val p = Placement(s, self, abs, if (atStation) 0.0 else off, lead.coerceIn(MIN_LEAD_MIN.toDouble(), MAX_LEAD_MIN.toDouble()))
            // at a station, the side the gate lies on decides whether the train passes it arriving or leaving
            if (atStation) { if (t > bestAlong) { best = p; bestAlong = t } }
            else if (best == null || off < best!!.offTrackKm) best = p
        }
        consider(next, true)
        consider(prev, false)
        if (best == null && atStation) best = Placement(s, self, (self.arrAbs + self.depAbs) / 2.0, 0.0, TYPICAL_LEAD_MIN.toDouble())
        return best
    }

    // ---------------------------------------------------------------- public API

    fun gateStatus(gate: Gate, user: LatLng, live: Map<String, LiveBoard>, nowSec: Long): GateStatus {
        val stations = db.nearbyStations(gate.pos, 10.0, limit = 4, passengerOnly = false)
        val nearest = db.nearbyStations(gate.pos, 10.0, limit = 1).firstOrNull()
        val from = nowSec - (OPEN_AFTER_MIN + MAX_LEAD_MIN) * 60L
        val to = nowSec + LOOKAHEAD_MIN * 60L

        // best placement per train (a train is timed once per gate, from the station it fits best)
        val bestByTrain = HashMap<String, Placement>()
        for (s in stations) {
            for ((self, prev, next) in db.trainsAt(s.code)) {
                val p = place(gate.pos, s, self, prev, next) ?: continue
                val cur = bestByTrain[self.trainNo]
                if (cur == null || p.offTrackKm < cur.offTrackKm) bestByTrain[self.trainNo] = p
            }
        }

        val passes = ArrayList<GatePass>()
        for ((no, p) in bestByTrain) {
            for (start in runsInWindow(p.stop, p.gateAbsMin, from - 6 * 3600, to)) {
                val sched = epoch(start, p.gateAbsMin)
                val schedAtStation = epoch(start, p.stop.depAbs.toDouble())
                val board = live[p.station.code]
                val lv = matchLive(board, no, p.stop.trainName, schedAtStation)
                val delaySec = when {
                    lv == null -> 0L
                    lv.schedEpochSec != null -> lv.estEpochSec - lv.schedEpochSec
                    else -> 0L   // real-time and on time
                }
                val pass = sched + delaySec
                val close = pass - (p.leadMin * 60).roundToLong()
                val open = pass + OPEN_AFTER_MIN * 60
                if (open < from || close > to) continue
                passes += GatePass(
                    trainNo = no, trainName = p.stop.trainName, passEpochSec = pass,
                    closeEpochSec = close, openEpochSec = open,
                    delayMin = (delaySec / 60).toInt(),
                    timeSource = if (lv != null) TimeSource.LIVE else TimeSource.SCHEDULED,
                    liveCheckedEpochSec = if (lv != null) board?.fetchedEpochSec else null,
                    viaStation = p.station.name,
                )
            }
        }

        // live trains Google lists that the offline timetable does not have (specials, new trains)
        if (nearest != null && Geo.km(gate.pos, nearest.pos) < 3.0) {
            val board = live[nearest.code]
            val travelSec = (Geo.km(gate.pos, nearest.pos) / 40.0 * 3600).toLong()   // ~40 km/h near a station
            board?.rows?.filter { it.realtime && (it.trainNo == null || it.trainNo !in bestByTrain) }?.forEach { lv ->
                val close = lv.estEpochSec - travelSec - TYPICAL_LEAD_MIN * 60
                val open = lv.estEpochSec + travelSec + OPEN_AFTER_MIN * 60
                if (open < from || close > to) return@forEach
                passes += GatePass(lv.trainNo ?: "—", lv.trainName, lv.estEpochSec, close, open, lv.delayMin,
                    TimeSource.LIVE, board.fetchedEpochSec, nearest.name)
            }
        }

        passes.sortBy { it.closeEpochSec }
        return summarize(gate, Geo.km(user, gate.pos), nearest, passes, nowSec)
    }

    private fun summarize(gate: Gate, distKm: Double, nearest: Station?, passes: List<GatePass>, now: Long): GateStatus {
        // merge overlapping / back-to-back windows: the gate does not go up between them
        data class Window(var close: Long, var open: Long, val trains: MutableList<GatePass>)
        val windows = ArrayList<Window>()
        for (p in passes) {
            val last = windows.lastOrNull()
            if (last != null && p.closeEpochSec <= last.open + MERGE_GAP_SEC) {
                last.open = maxOf(last.open, p.openEpochSec); last.trains += p
            } else windows += Window(p.closeEpochSec, p.openEpochSec, mutableListOf(p))
        }
        windows.removeAll { it.open < now }

        fun describe(w: Window): String = w.trains.joinToString("\n") { t ->
            val src = if (t.timeSource == TimeSource.LIVE) {
                val late = when {
                    t.delayMin > 0 -> "${t.delayMin} min late"
                    t.delayMin < 0 -> "${-t.delayMin} min early"
                    else -> "on time"
                }
                "live: $late"
            } else "scheduled – not live-verified"
            "${t.trainNo} ${t.trainName} · passes ~${fmt(t.passEpochSec)} ($src)"
        }
        fun allLive(w: Window) = w.trains.all { it.timeSource == TimeSource.LIVE }

        val current = windows.firstOrNull { now in it.close..it.open }
        val upcoming = windows.firstOrNull { it.close > now }
        val (state, headline, detail, verified) = when {
            current != null -> Quad(GateState.CLOSED,
                "Gate CLOSED · opens ~${fmt(current.open)}", describe(current), allLive(current))
            upcoming != null && upcoming.close - now <= SOON_MIN * 60 -> Quad(GateState.CLOSING_SOON,
                "Closing at ~${fmt(upcoming.close)} (in ${((upcoming.close - now) / 60).coerceAtLeast(0)} min)",
                describe(upcoming), allLive(upcoming))
            upcoming != null -> Quad(GateState.OPEN,
                "Gate OPEN now · will close at ~${fmt(upcoming.close)}, reopen ~${fmt(upcoming.open)}",
                describe(upcoming), allLive(upcoming))
            else -> Quad(GateState.OPEN, "Gate OPEN · no train in the next ${LOOKAHEAD_MIN / 60} h",
                "No train in the timetable or live data passes this gate soon.", false)
        }
        val w = current ?: upcoming
        return GateStatus(gate, distKm, nearest, state, headline, detail, verified, passes,
            window = w?.trains ?: emptyList(), windowClose = w?.close, windowOpen = w?.open)
    }

    private data class Quad(val a: GateState, val b: String, val c: String, val d: Boolean)

    /** Next trains at a station: timetable rows, shifted and confirmed by live data where available. */
    fun board(station: Station, live: LiveBoard?, liveError: String?, nowSec: Long): com.phataklive.data.StationBoard {
        val to = nowSec + LOOKAHEAD_MIN * 60L
        val rows = ArrayList<BoardRow>()
        val seen = HashSet<String>()
        for ((self, _, _) in db.trainsAt(station.code)) {
            val absMin = self.depAbs.toDouble()
            for (start in runsInWindow(self, absMin, nowSec - 6 * 3600, to)) {
                val sched = epoch(start, absMin)
                val lv = matchLive(live, self.trainNo, self.trainName, sched)
                val delay = if (lv?.schedEpochSec != null) lv.estEpochSec - lv.schedEpochSec else 0L
                val t = sched + delay
                if (t < nowSec - 120 || t > to) continue
                if (self.halt == 0 && lv == null && t > nowSec + 90 * 60) continue   // far-off pass-throughs: less noise
                rows += BoardRow(self.trainNo, self.trainName, t, (delay / 60).toInt(),
                    if (lv != null) TimeSource.LIVE else TimeSource.SCHEDULED, lv?.platform, self.halt > 0)
                seen += self.trainNo
            }
        }
        live?.rows?.filter { it.realtime && (it.trainNo == null || it.trainNo !in seen) }?.forEach { lv ->
            if (lv.estEpochSec in (nowSec - 120)..to)
                rows += BoardRow(lv.trainNo ?: "—", lv.trainName, lv.estEpochSec, lv.delayMin, TimeSource.LIVE, lv.platform, true)
        }
        rows.sortBy { it.timeEpochSec }
        return com.phataklive.data.StationBoard(station, rows, live != null, liveError)
    }
}
