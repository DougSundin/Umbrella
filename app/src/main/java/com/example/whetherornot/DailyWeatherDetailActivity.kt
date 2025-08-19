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
import com.example.whetherornot.data.model.DailyWeather
import com.example.whetherornot.ui.theme.UmbrellaTheme
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class DailyWeatherDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dailyWeatherJson = intent.getStringExtra("daily_weather_json")
        val location = intent.getStringExtra("location") ?: "Unknown Location"

        setContent {
            UmbrellaTheme {
                DailyWeatherDetailScreen(
                    dailyWeatherJson = dailyWeatherJson,
                    location = location,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyWeatherDetailScreen(
    dailyWeatherJson: String?,
    location: String,
    onBackClick: () -> Unit
) {
    val dailyWeather = remember(dailyWeatherJson) {
        dailyWeatherJson?.let {
            try {
                Gson().fromJson(it, DailyWeather::class.java)
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
                text = "Daily Weather Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        dailyWeather?.let { weather ->
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
                    // Location and date
                    Text(
                        text = location,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val forecastDate = Date(weather.dt * 1000)
                    val dayFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                    Text(
                        text = dayFormatter.format(forecastDate),
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
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Summary if available
                    weather.summary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Temperature range
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "High",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${weather.temp.max.toInt()}°F",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Low",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${weather.temp.min.toInt()}°F",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Detailed temperature breakdown
                    Text(
                        text = "Temperature Throughout Day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherDetailRow("Morning", "${weather.temp.morn.toInt()}°F")
                        WeatherDetailRow("Day", "${weather.temp.day.toInt()}°F")
                        WeatherDetailRow("Evening", "${weather.temp.eve.toInt()}°F")
                        WeatherDetailRow("Night", "${weather.temp.night.toInt()}°F")
                    }

                    // Feels like temperatures
                    Text(
                        text = "Feels Like Temperatures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherDetailRow("Morning", "${weather.feelsLike.morn.toInt()}°F")
                        WeatherDetailRow("Day", "${weather.feelsLike.day.toInt()}°F")
                        WeatherDetailRow("Evening", "${weather.feelsLike.eve.toInt()}°F")
                        WeatherDetailRow("Night", "${weather.feelsLike.night.toInt()}°F")
                    }

                    // Weather details
                    Text(
                        text = "Weather Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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

                        WeatherDetailRow(
                            label = "Chance of Rain",
                            value = "${(weather.pop * 100).toInt()}%"
                        )

                        WeatherDetailRow(
                            label = "Moon Phase",
                            value = "${(weather.moonPhase * 100).toInt()}%"
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

                        WeatherDetailRow(
                            label = "Moonrise",
                            value = timeFormatter.format(Date(weather.moonrise * 1000))
                        )

                        WeatherDetailRow(
                            label = "Moonset",
                            value = timeFormatter.format(Date(weather.moonset * 1000))
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
