package com.example.whetherornot.data.repository

import com.example.whetherornot.data.api.WeatherApiService
import com.example.whetherornot.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log

/**
 * Repository class for handling weather data operations in Kotlin
 * Follows Repository pattern for data abstraction
 */
class KotlinWeatherRepository {

    private val apiService: WeatherApiService

    init {
        // Setup HTTP logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Configure OkHttp client
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        // Setup Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(WeatherApiService.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(WeatherApiService::class.java)
    }

    /**
     * Fetch weather data from API
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Result containing weather data or error
     */
    suspend fun getWeatherData(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWeatherData(
                    lat = latitude,
                    lon = longitude,
                    exclude = "minutely,alerts",
                    appid = WeatherApiService.API_KEY,
                    units = "imperial"
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("API call failed: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get raw JSON response as string for debugging purposes
     */
    suspend fun getWeatherDataAsJson(latitude: Double, longitude: Double): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWeatherData(
                    lat = latitude,
                    lon = longitude,
                    exclude = "minutely,alerts",
                    appid = WeatherApiService.API_KEY,
                    units = "imperial"
                )
                if (response.isSuccessful && response.body() != null) {
                    // Convert response to JSON string
                    val gson = com.google.gson.Gson()
                    val jsonString = gson.toJson(response.body())
                    Result.success(jsonString)
                } else {
                    Result.failure(Exception("API call failed: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get coordinates from zip code using Geocoding API
     * @param zipCode Zip code (e.g., "90210")
     * @param countryCode Country code (default: "US")
     * @return Result containing coordinates or error
     */
    suspend fun getCoordinatesFromZip(zipCode: String, countryCode: String = "US"): Result<Pair<Double, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                val zipQuery = "$zipCode,$countryCode"
                val response = apiService.getCoordinatesFromZip(
                    zip = zipQuery,
                    appid = WeatherApiService.API_KEY
                )
                if (response.isSuccessful && response.body() != null) {
                    val zipCodeResponse = response.body()!!
                    Result.success(Pair(zipCodeResponse.lat, zipCodeResponse.lon))
                } else {
                    Result.failure(Exception("Geocoding API call failed: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get weather data by zip code (combines geocoding and weather calls)
     * @param zipCode Zip code (e.g., "90210")
     * @param countryCode Country code (default: "US")
     * @return Result containing weather data JSON or error
     */
    suspend fun getWeatherDataByZip(zipCode: String, countryCode: String = "US"): Result<String> {
        return try {
            // First get coordinates from zip code
            Log.d("KotlinWeather", "Getting coordinates for zip code: $zipCode")
            val coordinatesResult = getCoordinatesFromZip(zipCode, countryCode)
            coordinatesResult.fold(
                onSuccess = { (lat, lon) ->
                    Log.d("KotlinWeather", "Got coordinates from zip $zipCode: lat=$lat, lon=$lon")
                    // Then get weather data using those coordinates
                    Log.d("KotlinWeather", "Calling weather API with coordinates: lat=$lat, lon=$lon")
                    getWeatherDataAsJson(lat, lon)
                },
                onFailure = { exception ->
                    Log.e("KotlinWeather", "Failed to get coordinates for zip $zipCode: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("KotlinWeather", "Exception in getWeatherDataByZip: ${e.message}")
            Result.failure(e)
        }
    }
}
