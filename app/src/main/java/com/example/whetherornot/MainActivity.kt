package com.example.whetherornot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.whetherornot.ui.theme.WhetherOrNotTheme
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.whetherornot.data.repository.KotlinWeatherRepository
import com.example.whetherornot.data.repository.JavaWeatherRepository
import com.example.whetherornot.data.model.WeatherResponse
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhetherOrNotTheme {
                WeatherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherApp() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Kotlin Weather", "Java Weather")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "WhetherOrNot Weather",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content area for each tab
            when (selectedTabIndex) {
                0 -> KotlinWeatherContent()
                1 -> JavaWeatherContent()
            }
        }
    }
}

@Composable
fun KotlinWeatherContent() {
    var isLoading by remember { mutableStateOf(false) }
    var weatherJson by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentWeatherIcon by remember { mutableStateOf<String?>(null) }
    var currentWeatherDescription by remember { mutableStateOf<String?>(null) }
    var zipCodeInput by remember { mutableStateOf("") }
    var currentLocation by remember { mutableStateOf("Duluth, MN (46.8384°N, 92.1800°W)") }
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { KotlinWeatherRepository() }

    // Default coordinates: Duluth, MN
    var currentLatitude by remember { mutableStateOf(46.8384) }
    var currentLongitude by remember { mutableStateOf(-92.1800) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Kotlin Weather Implementation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Location: $currentLocation",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Zip Code Search Field
        OutlinedTextField(
            value = zipCodeInput,
            onValueChange = { zipCodeInput = it },
            label = { Text("Enter Zip Code") },
            placeholder = { Text("e.g., 90210") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = !isLoading
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    weatherJson = null
                    currentWeatherIcon = null
                    currentWeatherDescription = null

                    try {
                        val result = if (zipCodeInput.isNotBlank()) {
                            // Use zip code search
                            currentLocation = "Zip Code: ${zipCodeInput.trim()}"
                            repository.getWeatherDataByZip(zipCodeInput.trim())
                        } else {
                            // Use default coordinates
                            currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                            repository.getWeatherDataAsJson(currentLatitude, currentLongitude)
                        }

                        result.fold(
                            onSuccess = { json ->
                                weatherJson = json
                                isLoading = false
                                Log.d("KotlinWeather", "Weather JSON: $json")

                                // Parse JSON to extract icon information
                                try {
                                    val gson = Gson()
                                    val weatherResponse = gson.fromJson(json, WeatherResponse::class.java)
                                    weatherResponse.current?.weather?.firstOrNull()?.let { weather ->
                                        currentWeatherIcon = weather.icon
                                        currentWeatherDescription = weather.description
                                    }
                                } catch (e: Exception) {
                                    Log.e("KotlinWeather", "Error parsing weather data: ${e.message}")
                                }
                            },
                            onFailure = { exception ->
                                errorMessage = "Error: ${exception.message}"
                                isLoading = false
                            }
                        )
                    } catch (e: Exception) {
                        errorMessage = "Unexpected error: ${e.message}"
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Fetch Weather Data")
        }

        // Weather Icon Display
        currentWeatherIcon?.let { iconCode ->
            Card(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current day and date
                    val currentDate = java.util.Date()
                    val dayFormatter = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                    val dateFormatter = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = dayFormatter.format(currentDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatter.format(currentDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    AsyncImage(
                        model = "https://openweathermap.org/img/wn/${iconCode}@2x.png",
                        contentDescription = currentWeatherDescription ?: "Weather icon",
                        modifier = Modifier.size(80.dp)
                    )

                    currentWeatherDescription?.let { description ->
                        Text(
                            text = description.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    // Parse weather data for temperature and wind info
                    weatherJson?.let { json ->
                        val gson = Gson()
                        val weatherResponse = try {
                            gson.fromJson(json, WeatherResponse::class.java)
                        } catch (e: Exception) {
                            Log.e("KotlinWeather", "Error parsing weather display data: ${e.message}")
                            null
                        }

                        weatherResponse?.current?.let { current ->
                            // Temperature display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "${current.temp.toInt()}°F",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Wind speed and direction display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Wind: ${current.windSpeed.toInt()} mph ",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                // Wind direction arrow
                                Text(
                                    text = "↑",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = current.windDeg.toFloat()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Weather Forecast Row
        weatherJson?.let { json ->
            val gson = Gson()
            val weatherResponse = try {
                gson.fromJson(json, WeatherResponse::class.java)
            } catch (e: Exception) {
                Log.e("KotlinWeather", "Error parsing daily weather data: ${e.message}")
                null
            }

            weatherResponse?.daily?.let { dailyWeather ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "7-Day Forecast",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(dailyWeather) { day ->
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Day of week and date
                                    val dayFormatter = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                                    val dateFormatter = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                                    val date = java.util.Date(day.dt * 1000)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = dayFormatter.format(date),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dateFormatter.format(date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Weather icon
                                    day.weather.firstOrNull()?.let { weather ->
                                        AsyncImage(
                                            model = "https://openweathermap.org/img/wn/${weather.icon}@2x.png",
                                            contentDescription = weather.description,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .padding(bottom = 8.dp)
                                        )
                                    }

                                    // High/Low temperatures with labels
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = "High: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${day.temp.max.toInt()}°",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = "Low: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${day.temp.min.toInt()}°",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    // Wind info with label
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Wind: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${day.windSpeed.toInt()}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "↑",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.graphicsLayer {
                                                rotationZ = day.windDeg.toFloat()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }

        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        weatherJson?.let { json ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Weather Data (JSON):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = json,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun JavaWeatherContent() {
    var isLoading by remember { mutableStateOf(false) }
    var weatherJson by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentWeatherIcon by remember { mutableStateOf<String?>(null) }
    var currentWeatherDescription by remember { mutableStateOf<String?>(null) }
    var zipCodeInput by remember { mutableStateOf("") }
    var currentLocation by remember { mutableStateOf("Duluth, MN (46.8384°N, 92.1800°W)") }
    val repository = remember { JavaWeatherRepository() }

    // Default coordinates: Duluth, MN
    val latitude = 46.8384
    val longitude = -92.1800

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Java Weather Implementation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Location: $currentLocation",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Zip Code Search Field
        OutlinedTextField(
            value = zipCodeInput,
            onValueChange = { zipCodeInput = it },
            label = { Text("Enter Zip Code") },
            placeholder = { Text("e.g., 90210") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = !isLoading
        )

        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                weatherJson = null
                currentWeatherIcon = null
                currentWeatherDescription = null

                if (zipCodeInput.isNotBlank()) {
                    // Use zip code search
                    currentLocation = "Zip Code: ${zipCodeInput.trim()}"
                    repository.getWeatherDataByZip(zipCodeInput.trim(), object : JavaWeatherRepository.JsonDataCallback {
                        override fun onSuccess(jsonData: String) {
                            weatherJson = jsonData
                            isLoading = false
                            Log.d("JavaWeather", "Weather JSON (from zip): $jsonData")

                            // Parse JSON to extract icon information
                            try {
                                val gson = Gson()
                                val weatherResponse = gson.fromJson(jsonData, WeatherResponse::class.java)
                                weatherResponse.current?.weather?.firstOrNull()?.let { weather ->
                                    currentWeatherIcon = weather.icon
                                    currentWeatherDescription = weather.description
                                }
                            } catch (e: Exception) {
                                Log.e("JavaWeather", "Error parsing weather data: ${e.message}")
                            }
                        }

                        override fun onError(error: String) {
                            errorMessage = error
                            isLoading = false
                        }
                    })
                } else {
                    // Use default coordinates
                    currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                    repository.getWeatherDataAsJson(latitude, longitude, object : JavaWeatherRepository.JsonDataCallback {
                        override fun onSuccess(jsonData: String) {
                            weatherJson = jsonData
                            isLoading = false
                            Log.d("JavaWeather", "Weather JSON: $jsonData")

                            // Parse JSON to extract icon information
                            try {
                                val gson = Gson()
                                val weatherResponse = gson.fromJson(jsonData, WeatherResponse::class.java)
                                weatherResponse.current?.weather?.firstOrNull()?.let { weather ->
                                    currentWeatherIcon = weather.icon
                                    currentWeatherDescription = weather.description
                                }
                            } catch (e: Exception) {
                                Log.e("JavaWeather", "Error parsing weather data: ${e.message}")
                            }
                        }

                        override fun onError(error: String) {
                            errorMessage = error
                            isLoading = false
                        }
                    })
                }
            },
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Fetch Weather Data")
        }

        // Weather Icon Display
        currentWeatherIcon?.let { iconCode ->
            Card(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current day and date
                    val currentDate = java.util.Date()
                    val dayFormatter = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                    val dateFormatter = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = dayFormatter.format(currentDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatter.format(currentDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    AsyncImage(
                        model = "https://openweathermap.org/img/wn/${iconCode}@2x.png",
                        contentDescription = currentWeatherDescription ?: "Weather icon",
                        modifier = Modifier.size(80.dp)
                    )

                    currentWeatherDescription?.let { description ->
                        Text(
                            text = description.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    // Parse weather data for temperature and wind info
                    weatherJson?.let { json ->
                        val gson = Gson()
                        val weatherResponse = try {
                            gson.fromJson(json, WeatherResponse::class.java)
                        } catch (e: Exception) {
                            Log.e("JavaWeather", "Error parsing weather display data: ${e.message}")
                            null
                        }

                        weatherResponse?.current?.let { current ->
                            // Temperature display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "${current.temp.toInt()}°F",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Wind speed and direction display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Wind: ${current.windSpeed.toInt()} mph ",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                // Wind direction arrow
                                Text(
                                    text = "↑",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = current.windDeg.toFloat()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Weather Forecast Row
        weatherJson?.let { json ->
            val gson = Gson()
            val weatherResponse = try {
                gson.fromJson(json, WeatherResponse::class.java)
            } catch (e: Exception) {
                Log.e("JavaWeather", "Error parsing daily weather data: ${e.message}")
                null
            }

            weatherResponse?.daily?.let { dailyWeather ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "7-Day Forecast",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(dailyWeather) { day ->
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Day of week and date
                                    val dayFormatter = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                                    val dateFormatter = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                                    val date = java.util.Date(day.dt * 1000)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = dayFormatter.format(date),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dateFormatter.format(date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Weather icon
                                    day.weather.firstOrNull()?.let { weather ->
                                        AsyncImage(
                                            model = "https://openweathermap.org/img/wn/${weather.icon}@2x.png",
                                            contentDescription = weather.description,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .padding(bottom = 8.dp)
                                        )
                                    }

                                    // High/Low temperatures with labels
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = "High: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${day.temp.max.toInt()}°",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = "Low: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${day.temp.min.toInt()}°",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    // Wind info with label
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Wind: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${day.windSpeed.toInt()}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "↑",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.graphicsLayer {
                                                rotationZ = day.windDeg.toFloat()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }

        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        weatherJson?.let { json ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Weather Data (JSON):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = json,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherAppPreview() {
    WhetherOrNotTheme {
        WeatherApp()
    }
}