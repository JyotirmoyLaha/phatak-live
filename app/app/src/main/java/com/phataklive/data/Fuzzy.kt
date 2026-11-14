package com.phataklive.data

/** Spelling-tolerant text matching (character trigrams), used for station, train and gate names. */
object Fuzzy {
    fun grams(s: String): Set<String> {
        val t = " " + s.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.replace(Regex("\\s+"), " ").trim() + " "
        return (0 until (t.length - 2).coerceAtLeast(0)).map { t.substring(it, it + 3) }.toSet()
    }

    /** Dice similarity of two trigram sets: 1.0 = same spelling, 0 = nothing in common. */
    fun similarity(a: Set<String>, b: Set<String>): Double =
        if (a.isEmpty() || b.isEmpty()) 0.0 else 2.0 * a.count { it in b } / (a.size + b.size)

    fun similarity(a: String, b: String): Double = similarity(grams(a), grams(b))

    // words that describe a gate rather than name a place, incl. common misspellings and Hindi / Bengali
    private val gateWords = setOf(
        "rail", "railway", "raliway", "railwey", "rly", "gate", "gat", "get", "level", "lavel", "label", "crossing",
        "crosing", "crossin", "lc", "l.c", "l.c.", "phatak", "fatak", "phatak", "no", "no.", "number", "near",
        "रेल", "रेलवे", "फाटक", "गेट", "क्रॉसिंग", "রেল", "গেট", "ফটক", "ক্রসিং",
    )

    /**
     * The place part of a gate query: "guskra level crosing no 12" -> "guskra". A half-typed gate word at the end
     * goes too ("bally rai" -> "bally"), so suggestions work before the word is finished.
     */
    fun placePart(query: String): String {
        val words = query.split(Regex("\\s+")).map { it.lowercase().trim(',', '-', '.') }.filter { it.isNotEmpty() }
        val kept = words.filterIndexed { i, l ->
            val halfGateWord = i == words.lastIndex && i > 0 && l.length >= 2 && gateWords.any { it.length > l.length && it.startsWith(l) }
            l !in gateWords && !halfGateWord && !l.all { it.isDigit() || it == '/' || it == '-' }
        }
        return kept.joinToString(" ")
    }

    /** Edit distance where swapping two neighbouring letters counts as one mistake ("bandle" / "bandel"). */
    fun editDistance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
        }
        return d[a.length][b.length]
    }

    /**
     * How well what was typed matches a place name, 0..1, also while typing: the name starts with it ("gonn" ->
     * "Gonna Kalitala") = 1, a word of it does = 0.9, otherwise spelling closeness of the whole name or its first words.
     */
    fun matchScore(typed: String, name: String): Double {
        val q = typed.lowercase().trim()
        val n = name.lowercase().trim()
        if (q.isEmpty() || n.isEmpty()) return 0.0
        if (n.startsWith(q)) return 1.0
        if (n.split(' ', ',', '-', '(').any { it.isNotEmpty() && it.startsWith(q) }) return 0.9
        return maxOf(similarity(q, n), nameScore(q, n))
    }

    /** How well a typed place matches a name, 0..1: the whole name, or its first words ("bandel" vs "Bandel Jn"). */
    fun nameScore(typed: String, name: String): Double {
        val q = typed.lowercase().trim()
        val n = name.lowercase().trim()
        if (q.isEmpty() || n.isEmpty()) return 0.0
        val firstWords = n.split(' ').take(q.split(' ').size).joinToString(" ")
        return listOf(n, firstWords).maxOf { 1.0 - editDistance(q, it).toDouble() / maxOf(q.length, it.length) }
    }
}
