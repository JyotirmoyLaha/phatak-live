package com.phataklive

import com.phataklive.data.GatePass
import com.phataklive.data.TimeSource
import com.phataklive.gate.GateEngine
import com.phataklive.net.GoogleDirections
import com.phataklive.route.CrossingKind
import com.phataklive.route.CrossingPlanner
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
