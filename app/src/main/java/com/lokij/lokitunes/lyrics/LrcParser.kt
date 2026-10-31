package com.lokij.lokitunes.lyrics

/** No network usage: all lyrics come from a user-selected local text document. */
data class LyricLine(val startMs: Long?, val text: String)
data class LyricDocument(val lines: List<LyricLine>, val synchronized: Boolean)

object LrcParser {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""^\[offset:([+-]?\d+)]$""", RegexOption.IGNORE_CASE)
    private val metadataTag = Regex("""^\[(ar|al|ti|by|re|ve|length):.*]$""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): LyricDocument {
        val lines = raw.removePrefix("\uFEFF").lines()
        val offset = lines.firstNotNullOfOrNull { line ->
            offsetTag.matchEntire(line.trim())?.groupValues?.get(1)?.toLongOrNull()
        }?.coerceIn(-60_000L, 60_000L) ?: 0L
        val timed = mutableListOf<LyricLine>()
        val plain = mutableListOf<LyricLine>()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || metadataTag.matches(line) || offsetTag.matches(line)) continue
            val matches = timestamp.findAll(line).toList()
            if (matches.isEmpty()) {
                if (!line.startsWith("[")) plain.add(LyricLine(null, line))
                continue
            }
            val text = timestamp.replace(line, "").trim()
            // A blank timestamp represents an instrumental section; show a subtle break.
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                if (seconds !in 0..59) return@forEach
                val fraction = match.groupValues[3]
                val milliseconds = when (fraction.length) {
                    1 -> fraction.toLong() * 100L
                    2 -> fraction.toLong() * 10L
                    3 -> fraction.toLong()
                    else -> 0L
                }
                val ms = (minutes * 60_000L + seconds * 1_000L + milliseconds + offset)
                    .coerceAtLeast(0L)
                timed.add(LyricLine(ms, text.ifEmpty { "♪" }))
            }
        }
        return if (timed.isNotEmpty()) {
            LyricDocument(timed.sortedBy { it.startMs }, synchronized = true)
        } else {
            LyricDocument(plain, synchronized = false)
        }
    }

    /** -1 means the first timestamp has not been reached yet. */
    fun activeIndex(document: LyricDocument, positionMs: Long): Int {
        if (!document.synchronized || document.lines.isEmpty()) return -1
        var low = 0
        var high = document.lines.lastIndex
        var result = -1
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if ((document.lines[middle].startMs ?: Long.MAX_VALUE) <= positionMs) {
                result = middle
                low = middle + 1
            } else high = middle - 1
        }
        return result
    }
}
