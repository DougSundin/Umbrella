package com.example.whetherornot.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data models for OpenWeatherMap One Call API 3.0 response
 */
data class WeatherResponse(
    @SerializedName("lat")
    val lat: Double,

    @SerializedName("lon")
    val lon: Double,

    @SerializedName("timezone")
    val timezone: String,

    @SerializedName("timezone_offset")
    val timezoneOffset: Int,

    @SerializedName("current")
    val current: CurrentWeather?,

    @SerializedName("hourly")
    val hourly: List<HourlyWeather>?,

    @SerializedName("daily")
    val daily: List<DailyWeather>?
)

data class CurrentWeather(
    @SerializedName("dt")
    val dt: Long,

    @SerializedName("sunrise")
    val sunrise: Long,

    @SerializedName("sunset")
    val sunset: Long,

    @SerializedName("temp")
    val temp: Double,

    @SerializedName("feels_like")
    val feelsLike: Double,

    @SerializedName("pressure")
    val pressure: Int,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("dew_point")
    val dewPoint: Double,

    @SerializedName("uvi")
    val uvi: Double,

    @SerializedName("clouds")
    val clouds: Int,

    @SerializedName("visibility")
    val visibility: Int,

    @SerializedName("wind_speed")
    val windSpeed: Double,

    @SerializedName("wind_deg")
    val windDeg: Double,

    @SerializedName("wind_gust")
    val windGust: Double?,

    @SerializedName("weather")
    val weather: List<Weather>
)

data class HourlyWeather(
    @SerializedName("dt")
    val dt: Long,

    @SerializedName("temp")
    val temp: Double,

    @SerializedName("feels_like")
    val feelsLike: Double,

    @SerializedName("pressure")
    val pressure: Int,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("dew_point")
    val dewPoint: Double,

    @SerializedName("uvi")
    val uvi: Double,

    @SerializedName("clouds")
    val clouds: Int,

    @SerializedName("visibility")
    val visibility: Int,

    @SerializedName("wind_speed")
    val windSpeed: Double,

    @SerializedName("wind_deg")
    val windDeg: Double,

    @SerializedName("wind_gust")
    val windGust: Double?,

    @SerializedName("weather")
    val weather: List<Weather>,

    @SerializedName("pop")
    val pop: Double
)

data class DailyWeather(
    @SerializedName("dt")
    val dt: Long,

    @SerializedName("sunrise")
    val sunrise: Long,

    @SerializedName("sunset")
    val sunset: Long,

    @SerializedName("moonrise")
    val moonrise: Long,

    @SerializedName("moonset")
    val moonset: Long,

    @SerializedName("moon_phase")
    val moonPhase: Double,

    @SerializedName("summary")
    val summary: String?,

    @SerializedName("temp")
    val temp: DailyTemperature,

    @SerializedName("feels_like")
    val feelsLike: DailyFeelsLike,

    @SerializedName("pressure")
    val pressure: Int,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("dew_point")
    val dewPoint: Double,

    @SerializedName("wind_speed")
    val windSpeed: Double,

    @SerializedName("wind_deg")
    val windDeg: Double,

    @SerializedName("wind_gust")
    val windGust: Double?,

    @SerializedName("weather")
    val weather: List<Weather>,

    @SerializedName("clouds")
    val clouds: Int,

    @SerializedName("pop")
    val pop: Double,

    @SerializedName("uvi")
    val uvi: Double
)

data class DailyTemperature(
    @SerializedName("day")
    val day: Double,

    @SerializedName("min")
    val min: Double,

    @SerializedName("max")
    val max: Double,

    @SerializedName("night")
    val night: Double,

    @SerializedName("eve")
    val eve: Double,

    @SerializedName("morn")
    val morn: Double
)

data class DailyFeelsLike(
    @SerializedName("day")
    val day: Double,

    @SerializedName("night")
    val night: Double,

    @SerializedName("eve")
    val eve: Double,

    @SerializedName("morn")
    val morn: Double
)

data class Weather(
    @SerializedName("id")
    val id: Int,

    @SerializedName("main")
    val main: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("icon")
    val icon: String
)
