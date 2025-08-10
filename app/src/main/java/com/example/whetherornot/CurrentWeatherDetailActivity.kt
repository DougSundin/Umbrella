package com.example.whetherornot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.whetherornot.data.model.CurrentWeather
import com.example.whetherornot.ui.theme.WhetherOrNotTheme
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class CurrentWeatherDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val currentWeatherJson = intent.getStringExtra("current_weather_json")
        val location = intent.getStringExtra("location") ?: "Unknown Location"

        setContent {
            WhetherOrNotTheme {
                CurrentWeatherDetailScreen(
                    currentWeatherJson = currentWeatherJson,
                    location = location,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherDetailScreen(
    currentWeatherJson: String?,
    location: String,
    onBackClick: () -> Unit
) {
    val currentWeather = remember(currentWeatherJson) {
        currentWeatherJson?.let {
            try {
                Gson().fromJson(it, CurrentWeather::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Current Weather Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        currentWeather?.let { weather ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Location and current day
                    Text(
                        text = location,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val currentDate = Date(weather.dt * 1000)
                    val dayFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                    Text(
                        text = dayFormatter.format(currentDate),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Weather icon and description
                    weather.weather.firstOrNull()?.let { weatherInfo ->
                        AsyncImage(
                            model = "https://openweathermap.org/img/wn/${weatherInfo.icon}@4x.png",
                            contentDescription = weatherInfo.description,
                            modifier = Modifier
                                .size(120.dp)
                                .padding(bottom = 8.dp)
                        )
                        Text(
                            text = weatherInfo.description.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Temperature
                    Text(
                        text = "${weather.temp.toInt()}°F",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Feels like ${weather.feelsLike.toInt()}°F",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Weather details grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Wind information
                        WeatherDetailRow(
                            label = "Wind",
                            value = "${weather.windSpeed.toInt()} mph",
                            extra = {
                                Text(
                                    text = "↑",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = weather.windDeg.toFloat()
                                    }
                                )
                            }
                        )

                        weather.windGust?.let { gust ->
                            WeatherDetailRow(
                                label = "Wind Gust",
                                value = "${gust.toInt()} mph"
                            )
                        }

                        WeatherDetailRow(
                            label = "Humidity",
                            value = "${weather.humidity}%"
                        )

                        WeatherDetailRow(
                            label = "Pressure",
                            value = "${weather.pressure} hPa"
                        )

                        WeatherDetailRow(
                            label = "Visibility",
                            value = "${(weather.visibility / 1609.344).toInt()} miles"
                        )

                        WeatherDetailRow(
                            label = "UV Index",
                            value = "${weather.uvi}"
                        )

                        WeatherDetailRow(
                            label = "Dew Point",
                            value = "${weather.dewPoint.toInt()}°F"
                        )

                        WeatherDetailRow(
                            label = "Cloud Cover",
                            value = "${weather.clouds}%"
                        )

                        // Sunrise and Sunset
                        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                        WeatherDetailRow(
                            label = "Sunrise",
                            value = timeFormatter.format(Date(weather.sunrise * 1000))
                        )

                        WeatherDetailRow(
                            label = "Sunset",
                            value = timeFormatter.format(Date(weather.sunset * 1000))
                        )
                    }
                }
            }
        } ?: run {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Unable to load weather details",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun WeatherDetailRow(
    label: String,
    value: String,
    extra: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
            extra?.invoke()
        }
    }
}
