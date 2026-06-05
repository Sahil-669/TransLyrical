package com.example.translyrical.parser

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {

    private val lrcPattern = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})]\s*(.*)""")

    fun parse(lrcContent: String): List<LyricLine> {
        val parsedLines = mutableListOf<LyricLine>()
        val lines = lrcContent.lines()

        for (line in lines) {
            val matchResult = lrcPattern.matchEntire(line.trim())

            if (matchResult != null) {
                val (minuteStr, secondStr, milliStr, text) = matchResult.destructured
                val minutes = minuteStr.toLong()
                val seconds = secondStr.toLong()

                val millis = if (milliStr.length == 2) {
                    milliStr.toLong() * 10
                } else {
                    milliStr.toLong()
                }

                val totalTimeMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                parsedLines.add(LyricLine(totalTimeMs, text))
            }
        }
        return parsedLines
    }
}