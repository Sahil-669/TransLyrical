package com.example.translyrical.domain

object SlangNormalizer {

    private val SlangDictionary = mapOf(
        "titi" to "tia",
        "callaíta" to "callada",
        "chavos" to "dinero",
        "guagua" to "autobús",
        "bichote" to "jefe"
    )

    fun normalizeLine(rawLyric: String): String {
        var scrubbedLyric = rawLyric

        for ((slang, standard) in SlangDictionary) {
            val wordBoundaryRegex = Regex("\\b$slang\\b", RegexOption.IGNORE_CASE)
            scrubbedLyric = scrubbedLyric.replace(wordBoundaryRegex, standard)
        }
        return scrubbedLyric
    }
}