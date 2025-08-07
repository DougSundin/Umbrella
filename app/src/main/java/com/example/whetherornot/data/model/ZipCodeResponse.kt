package com.example.whetherornot.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for OpenWeatherMap Geocoding API zip code response
 * Represents the JSON response from http://api.openweathermap.org/geo/1.0/zip
 */
data class ZipCodeResponse(
    @SerializedName("zip")
    val zip: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("lat")
    val lat: Double,

    @SerializedName("lon")
    val lon: Double,

    @SerializedName("country")
    val country: String
)
