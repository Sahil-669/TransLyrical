package com.example.translyrical.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

data class GeminiRequest(val contents: List<GeminiContent>)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiPart(val text: String)

data class GeminiResponse(val candidates: List<GeminiCandidate>?)
data class GeminiCandidate(val content: GeminiContent?)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.6-flash:generateContent")
    suspend fun translateLyrics(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ) : GeminiResponse
}