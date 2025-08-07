package com.example.whetherornot.data.api

import com.example.whetherornot.data.model.WeatherResponse
import com.example.whetherornot.data.model.ZipCodeResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for OpenWeatherMap API calls
 * Supports both Java (Call) and Kotlin (suspend) implementations
 */
interface WeatherApiService {

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
        const val API_KEY = "150cc45f78958ce4fb9d708c85bfdc1c"
    }

    /**
     * Get weather data using One Call API 3.0 (Kotlin coroutines version)
     * @param lat Latitude coordinate
     * @param lon Longitude coordinate
     * @param exclude Parts of the weather data to exclude (minutely,alerts)
     * @param appid API key
     * @param units Units of measurement (metric, imperial, standard)
     */
    @GET("data/3.0/onecall")
    suspend fun getWeatherData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("exclude") exclude: String = "minutely,alerts",
        @Query("appid") appid: String = API_KEY,
        @Query("units") units: String = "imperial"
    ): Response<WeatherResponse>

    /**
     * Get weather data using One Call API 3.0 (Java Call version)
     * @param lat Latitude coordinate
     * @param lon Longitude coordinate
     * @param exclude Parts of the weather data to exclude (minutely,alerts)
     * @param appid API key
     * @param units Units of measurement (metric, imperial, standard)
     */
    @GET("data/3.0/onecall")
    fun getWeatherDataCall(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("exclude") exclude: String,
        @Query("appid") appid: String,
        @Query("units") units: String
    ): Call<WeatherResponse>

    /**
     * Get coordinates from zip code using Geocoding API
     * @param zip Zip code with country code (e.g., "90210,US")
     * @param appid API key
     */
    @GET("geo/1.0/zip")
    suspend fun getCoordinatesFromZip(
        @Query("zip") zip: String,
        @Query("appid") appid: String = API_KEY
    ): Response<ZipCodeResponse>

    /**
     * Get coordinates from zip code using Geocoding API (Java Call version)
     * @param zip Zip code with country code (e.g., "90210,US")
     * @param appid API key
     */
    @GET("geo/1.0/zip")
    fun getCoordinatesFromZipCall(
        @Query("zip") zip: String,
        @Query("appid") appid: String
    ): Call<ZipCodeResponse>
}
