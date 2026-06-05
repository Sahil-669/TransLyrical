package com.example.translyrical.network

import com.google.gson.annotations.SerializedName

data class TranslationResponse(
    @SerializedName("responseData")
    val responseData: TranslatedText
)

data class TranslatedText(
    @SerializedName("translatedText")
    val translatedText: String
)