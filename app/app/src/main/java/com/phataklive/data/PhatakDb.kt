package com.phataklive.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlin.math.cos

/**
 * Offline India railway data shipped in assets/phatak.db (built by data-pipeline/build_asset.py):
 * 10k stations with coordinates and every train's stops, including stations it passes without stopping.
 */
class PhatakDb private constructor(private val db: SQLiteDatabase) {

    /**
     * Stations within [radiusKm]. [passengerOnly] hides block cabins, loco sheds, yards etc.: they stay in the
     * timetable (useful for timing a gate between two points) but are not stations a user can go to.
     */
    fun nearbyStations(at: LatLng, radiusKm: Double, limit: Int = 30, passengerOnly: Boolean = true): List<Station> {
        val dLat = radiusKm / 110.574
        val dLng = radiusKm / (111.320 * cos(Math.toRadians(at.lat)).coerceAtLeast(0.1))
        val out = ArrayList<Station>()
        db.rawQuery(
            "select code, name, lat, lng from station where lat between ? and ? and lng between ? and ?" +
                if (passengerOnly) PASSENGER_ONLY else "",
            arrayOf("${at.lat - dLat}", "${at.lat + dLat}", "${at.lng - dLng}", "${at.lng + dLng}"),
        ).use { c ->
            while (c.moveToNext()) {
                val pos = LatLng(c.getDouble(2), c.getDouble(3))
                val d = Geo.km(at, pos)
                if (d <= radiusKm) out += Station(c.getString(0), c.getString(1), pos, d)
            }
        }
        return out.sortedBy { it.distanceKm }.take(limit)
    }

    fun station(code: String): Station? =
        db.rawQuery("select code, name, lat, lng from station where code = ?", arrayOf(code)).use { c ->
            if (c.moveToFirst()) Station(c.getString(0), c.getString(1), LatLng(c.getDouble(2), c.getDouble(3))) else null
        }

    /** Every train that calls at or passes [stationCode], with its neighbouring stops for interpolation. */
    fun trainsAt(stationCode: String): List<Triple<StopTime, StopTime?, StopTime?>> {
        val rows = ArrayList<Triple<StopTime, StopTime?, StopTime?>>()
        db.rawQuery(
            """
            select s.train_no, t.name, t.running_days,
                   s.seq, s.arr, s.dep, s.day, s.halt,
                   p.seq, p.station, p.arr, p.dep, p.day, p.halt,
                   n.seq, n.station, n.arr, n.dep, n.day, n.halt
            from stop s
            join train t on t.no = s.train_no
            left join stop p on p.train_no = s.train_no and p.seq = s.seq - 1
            left join stop n on n.train_no = s.train_no and n.seq = s.seq + 1
            where s.station = ?
            """.trimIndent(),
            arrayOf(stationCode),
        ).use { c ->
            while (c.moveToNext()) {
                val no = c.getString(0); val name = c.getString(1); val days = c.getInt(2)
                fun stopAt(i: Int, code: String): StopTime {
                    val day = c.getInt(i + 3)
                    val arr = c.getInt(i + 1); val dep = c.getInt(i + 2)
                    val arrAbs = (day - 1) * 1440 + arr
                    var depAbs = (day - 1) * 1440 + dep
                    if (depAbs < arrAbs) depAbs += 1440
                    return StopTime(no, name, c.getInt(i), code, arrAbs, depAbs, c.getInt(i + 4), day, days)
                }
                val self = stopAt(3, stationCode)
                val prev = if (c.isNull(8)) null else StopTime(no, name, c.getInt(8), c.getString(9),
                    (c.getInt(12) - 1) * 1440 + c.getInt(10), (c.getInt(12) - 1) * 1440 + c.getInt(11),
                    c.getInt(13), c.getInt(12), days)
                val next = if (c.isNull(14)) null else StopTime(no, name, c.getInt(14), c.getString(15),
                    (c.getInt(18) - 1) * 1440 + c.getInt(16), (c.getInt(18) - 1) * 1440 + c.getInt(17),
                    c.getInt(19), c.getInt(18), days)
                rows += Triple(self, prev, next)
            }
        }
        return rows
    }

    data class TrainMatch(val no: String, val name: String, val exact: Boolean, val alternatives: List<Pair<String, String>>)

    /**
     * Train by number ("13017", also Hindi/Bengali digits) or name ("ganadevta"). An exact number or name substring is
     * `exact`; otherwise the closest names by spelling are returned with `exact = false` so the UI can ask
     * "Did you mean…?" instead of silently picking a sibling train.
     */
    fun findTrain(number: String?, name: String?): TrainMatch? {
        number?.filter { it.isDigit() }?.map { Character.digit(it, 10) }?.joinToString("")?.takeIf { it.length == 5 }?.let { no ->
            db.rawQuery("select no, name from train where no = ?", arrayOf(no)).use {
                if (it.moveToFirst()) return TrainMatch(it.getString(0), it.getString(1), true, emptyList())
            }
        }
        val n = name?.trim()?.takeIf { it.length >= 3 } ?: return null
        db.rawQuery("select no, name from train where name like ? order by length(name) limit 4", arrayOf("%$n%")).use { c ->
            val rows = ArrayList<Pair<String, String>>()
            while (c.moveToNext()) rows += c.getString(0) to c.getString(1)
            if (rows.isNotEmpty()) return TrainMatch(rows[0].first, rows[0].second, rows.size == 1, rows.drop(1))
        }
        val ranked = fuzzyTrains(n)
        val best = ranked.firstOrNull() ?: return null
        return TrainMatch(best.first, best.second, false, ranked.drop(1))
    }

    /**
     * Passenger stations whose name matches [query]: names starting with it first, then containing it, then the
     * closest spellings ("guskra" -> Guskara, "bandle" -> Bandel), best first.
     */
    fun findStations(query: String, limit: Int = 3): List<Station> {
        val q = query.trim().lowercase()
        if (q.length < 3) return emptyList()
        val all = passengerStations
        // names starting with (then containing) what was typed, shortest first: the user may not have finished typing
        val typed = all.filter { it.code.equals(q, true) || it.name.lowercase().startsWith(q) }.sortedBy { it.name.length } +
            all.filter { !it.name.lowercase().startsWith(q) && it.name.lowercase().contains(q) }.sortedBy { it.name.length }
        val typedCodes = typed.map { it.code }.toSet()
        // closest spellings, for mistakes ("guskra" -> Guskara, "bandle" -> Bandel, "bali" -> Bally)
        val fuzzy = all.asSequence().filter { it.code !in typedCodes }
            .map { it to Fuzzy.nameScore(q, it.name) }.filter { it.second >= 0.6 }
            .sortedByDescending { it.second }.map { it.first }.take(limit).toList()
        // alternate the two lists, so a likely misspelling is not pushed out by longer names that merely start alike
        val out = ArrayList<Station>()
        val a = typed.iterator(); val b = fuzzy.iterator()
        while (out.size < limit && (a.hasNext() || b.hasNext())) {
            if (a.hasNext()) out += a.next()
            if (out.size < limit && b.hasNext()) out += b.next()
        }
        return out
    }

    /** All passenger stations, read once (~10k rows) for spelling-tolerant name search. */
    private val passengerStations: List<Station> by lazy {
        db.rawQuery("select code, name, lat, lng from station where 1 = 1$PASSENGER_ONLY", null).use { c ->
            buildList { while (c.moveToNext()) add(Station(c.getString(0), c.getString(1), LatLng(c.getDouble(2), c.getDouble(3)))) }
        }
    }

    /** Closest train names by character-trigram similarity; only clear matches (score >= 0.45), best first. */
    private fun fuzzyTrains(query: String, top: Int = 3): List<Pair<String, String>> {
        val q = Fuzzy.grams(query)
        if (q.isEmpty()) return emptyList()
        val scored = ArrayList<Triple<Double, String, String>>()
        db.rawQuery("select no, name from train", null).use { c ->
            while (c.moveToNext()) {
                val score = Fuzzy.similarity(q, Fuzzy.grams(c.getString(1)))
                if (score >= 0.45) scored += Triple(score, c.getString(0), c.getString(1))
            }
        }
        return scored.sortedByDescending { it.first }.take(top).map { it.second to it.third }
    }

    data class RouteStop(val code: String, val name: String, val pos: LatLng, val arrAbs: Int, val depAbs: Int, val halt: Int)

    fun route(trainNo: String): Pair<Int, List<RouteStop>> {
        val days = db.rawQuery("select running_days from train where no = ?", arrayOf(trainNo)).use { if (it.moveToFirst()) it.getInt(0) else 127 }
        val out = ArrayList<RouteStop>()
        db.rawQuery(
            "select s.station, st.name, st.lat, st.lng, s.arr, s.dep, s.day, s.halt from stop s join station st on st.code = s.station " +
                "where s.train_no = ? order by s.seq", arrayOf(trainNo),
        ).use { c ->
            while (c.moveToNext()) {
                val day = c.getInt(6)
                val arr = (day - 1) * 1440 + c.getInt(4)
                var dep = (day - 1) * 1440 + c.getInt(5)
                if (dep < arr) dep += 1440
                out += RouteStop(c.getString(0), c.getString(1), LatLng(c.getDouble(2), c.getDouble(3)), arr, dep, c.getInt(7))
            }
        }
        return days to out
    }

    fun trainName(no: String): String? =
        db.rawQuery("select name from train where no = ?", arrayOf(no)).use { if (it.moveToFirst()) it.getString(0) else null }

    companion object {
        @Volatile private var instance: PhatakDb? = null
        private const val ASSET = "phatak.db"
        private const val PASSENGER_ONLY = " and code not like 'XX-%' and name not like '%Cabin%' and name not like '%Loco Shed%'" +
            " and name not like '%Yard%' and name not like '%Goods%' and name not like '% Outer%'"
        private const val VERSION = 1

        fun get(context: Context): PhatakDb = instance ?: synchronized(this) {
            instance ?: open(context.applicationContext).also { instance = it }
        }

        private fun open(context: Context): PhatakDb {
            val file = File(context.filesDir, "phatak-v$VERSION.db")
            if (!file.exists()) {
                val tmp = File(context.filesDir, "phatak.tmp")
                context.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
                tmp.renameTo(file)
            }
            return PhatakDb(SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY))
        }
    }
}
