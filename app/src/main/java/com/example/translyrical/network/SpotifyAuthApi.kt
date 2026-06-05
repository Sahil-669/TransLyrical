package com.example.translyrical.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface SpotifyAuthApi {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getAccessToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ) : SpotifyTokenResponse
}

data class SpotifyTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)