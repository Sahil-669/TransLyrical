package com.example.translyrical.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parser correctly extracts milliseconds and ignores metadata`() {

        val sampleLrc = """
            [ar: BAD BUNNY]
            [00:15.22] Me porto bonito
            [00:18.450] Tú no eres bebecita
        """.trimIndent()

        val result = LrcParser.parse(sampleLrc)

        assertEquals(2, result.size)
        assertEquals(15220L, result[0].startTimeMs)
        assertEquals("Me porto bonito", result[0].text)
        assertEquals(18450L, result[1].startTimeMs)
        assertEquals("Tú no eres bebecita", result[1].text)
    }
}