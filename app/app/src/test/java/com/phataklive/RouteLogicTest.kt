package com.phataklive

import com.phataklive.data.GatePass
import com.phataklive.data.LatLng
import com.phataklive.data.TimeSource
import com.phataklive.gate.GateEngine
import com.phataklive.net.GoogleDirections
import com.phataklive.route.CrossingKind
import com.phataklive.route.CrossingPlanner
import com.phataklive.route.Lines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Best-crossing logic that needs no network or database: gate waits to the second, Google's route rows, bridge names. */
class RouteLogicTest {

    private fun pass(no: String, close: Long, open: Long) =
        GatePass(no, "Train $no", (close + open) / 2, close, open, 0, TimeSource.LIVE, null, "X")

    // two trains 100 s apart (less than the 3 min merge gap): the gate stays down 1000..2200; a third train 3000..3300
    private val passes = listOf(pass("B", 1700, 2200), pass("A", 1000, 1600), pass("C", 3000, 3300))

    @Test fun openWellBeforeClosing() {
        val a = GateEngine.arrival(passes, 900)
        assertEquals(0L, a.waitSec)
        assertNull(a.opensAt)
        assertEquals(1000L, a.nextClose)
    }

    @Test fun arrivingJustBeforeClosingCountsAsClosed() {
        // 50 s before it goes down: within the 60 s margin, so wait for the merged window to end
        val a = GateEngine.arrival(passes, 950)
        assertEquals(1250L, a.waitSec)
        assertEquals(2200L, a.opensAt)
        assertEquals(3000L, a.nextClose)
        assertEquals(listOf("A", "B"), a.trains.map { it.trainNo })
    }

    @Test fun exactlyOneMinuteBeforeClosingIsClosed() {
        assertEquals(2200L - 940, GateEngine.arrival(passes, 940).waitSec)
        assertEquals(0L, GateEngine.arrival(passes, 939).waitSec)
    }

    @Test fun insideTheGapBetweenMergedTrainsStillClosed() {
        assertEquals(550L, GateEngine.arrival(passes, 1650).waitSec)
    }

    @Test fun lastSecondOfWindowAndReopening() {
        assertEquals(1L, GateEngine.arrival(passes, 2199).waitSec)
        val open = GateEngine.arrival(passes, 2200)
        assertEquals(0L, open.waitSec)
        assertEquals(3000L, open.nextClose)
    }

    @Test fun afterLastTrainNothingAhead() {
        val a = GateEngine.arrival(passes, 4000)
        assertEquals(0L, a.waitSec)
        assertNull(a.nextClose)
        assertEquals(0L, GateEngine.arrival(emptyList(), 4000).waitSec)
    }

    @Test fun directionsRowsParsed() {
        val body = ")]}'\n[[null,[[[2,\"Guskara Station Rd\",[1800,\"1.8 km\",0],[420,\"7 min\"]]]," +
            "[[2,\"Municipality Rd\",[2100,\"2.1 km\",0],[480,\"8 min\"]]]]]]"
        val r = GoogleDirections.parse(body)
        assertEquals(2, r.size)
        assertEquals(1800, r[0].distanceM)
        assertEquals(420, r[0].durationSec)
        assertEquals("Guskara Station Rd", r[0].summary)
        assertEquals(480, r[1].durationSec)
        assertEquals(0, GoogleDirections.parse(")]}'\n[[null,null]]").size)
    }

    @Test fun roadLineDecoded() {
        // first value, then differences, in 1e-7 degrees (Google's [0][7][i] layout)
        val body = ")]}'\n[[null,[[[2,\"Rd\",[500,\"500 m\",0],[120,\"2 min\"]]]],null,null,null,null,null," +
            "[[[234932644,5,-64],[877320681,857,3038],null,null,[17,-1,-2]]]]]"
        val r = GoogleDirections.parse(body).single()
        assertEquals(3, r.line.size)
        assertEquals(23.4932644, r.line[0].lat, 1e-9)
        assertEquals(87.7320681, r.line[0].lng, 1e-9)
        assertEquals(23.4932585, r.line[2].lat, 1e-9)
        assertEquals(87.7324576, r.line[2].lng, 1e-9)
        // no line in the reply: the route still counts, with an empty line
        assertEquals(0, GoogleDirections.parse(")]}'\n[[null,[[[2,\"Rd\",[500,\"500 m\",0],[120,\"2 min\"]]]]]]").single().line.size)
    }

    @Test fun roadMeetsRailway() {
        // railway running north-south at lng 87.7336; a road going west across it, and one staying east
        val rail = listOf(LatLng(23.50, 87.7336), LatLng(23.48, 87.7336))
        val across = listOf(LatLng(23.4906, 87.7376), LatLng(23.4906, 87.7340), LatLng(23.4937, 87.7330), LatLng(23.4930, 87.7290))
        val hits = Lines.crossings(across, listOf(rail))
        assertEquals(1, hits.size)
        assertEquals(87.7336, hits[0].pos.lng, 1e-6)
        assertEquals(0, Lines.crossings(listOf(LatLng(23.49, 87.74), LatLng(23.48, 87.738)), listOf(rail)).size)
        // double track (lines ~4 m apart) counts once; across and back counts twice
        val double = listOf(rail, listOf(LatLng(23.50, 87.73364), LatLng(23.48, 87.73364)))
        assertEquals(1, Lines.crossings(across, double).size)
        val back = listOf(LatLng(23.49, 87.735), LatLng(23.49, 87.732), LatLng(23.485, 87.732), LatLng(23.485, 87.735))
        assertEquals(2, Lines.crossings(back, listOf(rail)).size)
    }

    @Test fun pointAlongLine() {
        val line = listOf(LatLng(23.0, 87.0), LatLng(23.0, 87.01))
        val len = Lines.length(line)
        val mid = Lines.pointAt(line, len / 2)
        assertEquals(87.005, mid.lng, 1e-6)
        assertEquals(87.01, Lines.pointAt(line, len + 5).lng, 1e-9)   // clamped to the end
    }

    @Test fun railLineMustFollowTheStations() {
        val a = LatLng(23.54544, 87.72072); val b = LatLng(23.49055, 87.73362)
        val train = GoogleDirections.Route(6300, 600, null, listOf(a, LatLng(23.52, 87.727), b))
        val road = GoogleDirections.Route(9000, 900, null, listOf(a, LatLng(23.52, 87.78), b))   // a long way round
        val wrongEnd = GoogleDirections.Route(6300, 600, null, listOf(a, LatLng(23.40, 87.70)))
        assertEquals(train.line, CrossingPlanner.pickRailLine(listOf(road, wrongEnd, train), a, b))
        assertNull(CrossingPlanner.pickRailLine(listOf(road, wrongEnd), a, b))
    }

    @Test fun probesSpreadAlongRailway() {
        val rail = listOf(LatLng(23.50, 87.7336), LatLng(23.40, 87.7336))   // ~11 km
        val all = CrossingPlanner.probePoints(listOf(rail)) { true }
        assertEquals(14, all.size)                                          // capped, spread end to end
        val few = CrossingPlanner.probePoints(listOf(rail)) { it.lat > 23.49 }
        assertEquals(4, few.size)                                           // ~1.1 km of corridor: at 0.15, 0.45, 0.75, 1.05 km
        // the same railway twice (two sections) adds no extra points
        assertEquals(few.size, CrossingPlanner.probePoints(listOf(rail, rail)) { it.lat > 23.49 }.size)
    }

    @Test fun bridgeNames() {
        fun k(name: String, cats: List<String> = emptyList(), nearStation: Boolean = false) = CrossingPlanner.bridgeKind(name, cats, nearStation)
        assertEquals(CrossingKind.OVERBRIDGE, k("Guskara Rail Over Bridge"))
        assertEquals(CrossingKind.OVERBRIDGE, k("ROB No. 12"))
        assertEquals(CrossingKind.OVERBRIDGE, k("Burdwan Overbridge"))
        assertEquals(CrossingKind.UNDERPASS, k("Railway Underpass"))
        assertEquals(CrossingKind.UNDERPASS, k("RUB 34"))
        assertEquals(CrossingKind.UNDERPASS, k("Subway", listOf("Tunnel"), nearStation = true))
        assertEquals(CrossingKind.FOOTBRIDGE, k("Foot Over Bridge, Platform 1"))
        assertEquals(CrossingKind.OVERBRIDGE, k("Railway Flyover"))
        // not railway crossings
        assertNull(k("Subway", listOf("Sandwich shop", "Fast food restaurant"), nearStation = true))
        assertNull(k("NH 19 Flyover"))
        assertNull(k("Over Bridge Hotel"))
        assertNull(k("Kunur River Bridge"))
        assertNull(k("Underpass", nearStation = false))
    }
}
