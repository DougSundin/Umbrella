package com.example.whetherornot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import com.example.whetherornot.data.repository.LocationRepository
import com.example.whetherornot.data.model.WeatherResponse
import com.example.whetherornot.data.model.ZipCodeResponse
import com.example.whetherornot.utils.LocationManager
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
                        text = "Whether Or Not",
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
    var currentLocation by remember { mutableStateOf("Loading location...") }
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { KotlinWeatherRepository() }
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val locationRepository = remember { LocationRepository(context) }

    // States for dropdown functionality
    var savedLocations by remember { mutableStateOf<List<ZipCodeResponse>>(emptyList()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var isUserTyping by remember { mutableStateOf(false) }

    // Load saved locations from database
    LaunchedEffect(Unit) {
        locationRepository.getAllLocations().collect { locations ->
            savedLocations = locations
        }
    }

    // Default coordinates: Duluth, MN
    var currentLatitude by remember { mutableStateOf(46.8384) }
    var currentLongitude by remember { mutableStateOf(-92.1800) }
    var hasTriedLocation by remember { mutableStateOf(false) }

    // Function to fetch weather data (moved before savedLocationsLauncher)
    suspend fun fetchWeatherData(lat: Double, lon: Double, locationName: String) {
        isLoading = true
        errorMessage = null
        weatherJson = null
        currentWeatherIcon = null
        currentWeatherDescription = null
        currentLocation = locationName

        try {
            val result = repository.getWeatherDataAsJson(lat, lon)
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

                    // Save location to database if it was a zip code search
                    if (zipCodeInput.isNotBlank()) {
                        try {
                            coroutineScope.launch {
                                // First get the location data from the zip code
                                val zipResult = repository.getLocationDataByZip(zipCodeInput.trim())
                                zipResult.fold(
                                    onSuccess = { zipCodeResponse ->
                                        // Save the location to database
                                        locationRepository.saveLocation(zipCodeResponse)
                                        Log.d("KotlinWeather", "Location saved: ${zipCodeResponse.name}")
                                    },
                                    onFailure = { e ->
                                        Log.e("KotlinWeather", "Failed to save location: ${e.message}")
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("KotlinWeather", "Error saving location: ${e.message}")
                        }
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

    // Activity result launcher for saved locations
    val savedLocationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val selectedZip = data.getStringExtra("selected_zip")
                val selectedName = data.getStringExtra("selected_name")
                val selectedLat = data.getDoubleExtra("selected_lat", 0.0)
                val selectedLon = data.getDoubleExtra("selected_lon", 0.0)

                if (selectedZip != null && selectedName != null) {
                    zipCodeInput = selectedZip
                    currentLocation = selectedName
                    currentLatitude = selectedLat
                    currentLongitude = selectedLon

                    // Fetch weather data for selected location
                    coroutineScope.launch {
                        fetchWeatherData(selectedLat, selectedLon, selectedName)
                    }
                }
            }
        }
    }

    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        coroutineScope.launch {
            if (fineLocationGranted || coarseLocationGranted) {
                // Permission granted, get location and fetch weather
                try {
                    locationManager.getCurrentLocation()?.let { locationData ->
                        currentLatitude = locationData.latitude
                        currentLongitude = locationData.longitude
                        fetchWeatherData(locationData.latitude, locationData.longitude, locationData.locationName)
                    } ?: run {
                        // Location unavailable, use default coordinates
                        currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                        fetchWeatherData(currentLatitude, currentLongitude, currentLocation)
                    }
                } catch (e: Exception) {
                    Log.e("KotlinWeather", "Error getting location: ${e.message}")
                    // Fallback to default coordinates
                    currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                    fetchWeatherData(currentLatitude, currentLongitude, currentLocation)
                }
            } else {
                // Permission denied, use default coordinates
                currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                fetchWeatherData(currentLatitude, currentLongitude, currentLocation)
            }
            hasTriedLocation = true
        }
    }

    // Auto-fetch location and weather data on first load
    LaunchedEffect(Unit) {
        if (!hasTriedLocation) {
            if (locationManager.hasLocationPermission()) {
                // Permission already granted, get location
                try {
                    locationManager.getCurrentLocation()?.let { locationData ->
                        currentLatitude = locationData.latitude
                        currentLongitude = locationData.longitude
                        fetchWeatherData(locationData.latitude, locationData.longitude, locationData.locationName)
                    } ?: run {
                        // Location unavailable, use default coordinates
                        currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                        fetchWeatherData(currentLatitude, currentLongitude, currentLocation)
                    }
                } catch (e: Exception) {
                    Log.e("KotlinWeather", "Error getting location: ${e.message}")
                    // Fallback to default coordinates
                    currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                    fetchWeatherData(currentLatitude, currentLongitude, currentLocation)
                }
            } else {
                // Request location permission
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            hasTriedLocation = true
        }
    }

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

        // Zip Code Search Field with Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = zipCodeInput,
                onValueChange = {
                    val previousValue = zipCodeInput
                    zipCodeInput = it
                    isUserTyping = true

                    // Show dropdown when user types and there are saved locations
                    if (it.isNotEmpty() && savedLocations.isNotEmpty()) {
                        isDropdownExpanded = true
                    } else if (it.isEmpty() && savedLocations.isNotEmpty()) {
                        // If user clears the field, show all locations
                        isDropdownExpanded = true
                    } else {
                        isDropdownExpanded = false
                    }
                },
                label = { Text("Enter Zip Code") },
                placeholder = { Text("e.g., 90210") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (savedLocations.isNotEmpty()) {
                            isUserTyping = false // User clicked, not typing
                            isDropdownExpanded = !isDropdownExpanded
                        }
                    },
                enabled = !isLoading,
                trailingIcon = {
                    if (savedLocations.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                isUserTyping = false // User clicked icon, not typing
                                isDropdownExpanded = !isDropdownExpanded
                            }
                        ) {
                            Icon(
                                imageVector = if (isDropdownExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isDropdownExpanded) "Collapse dropdown" else "Expand dropdown",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            // Dropdown Menu using stable Material3 API
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 200.dp) // Limit height and allow scrolling
            ) {
                // Filter saved locations based on user interaction
                val filteredLocations = if (!isUserTyping || zipCodeInput.isEmpty()) {
                    // Show all locations when user clicked to open dropdown or field is empty
                    savedLocations.take(8) // Show first 8 when no filter
                } else {
                    // Filter only when user is actively typing
                    savedLocations.filter { location ->
                        location.zip.contains(zipCodeInput, ignoreCase = true) ||
                        location.name.contains(zipCodeInput, ignoreCase = true)
                    }.take(5) // Limit to 5 most recent matches
                }

                if (filteredLocations.isEmpty()) {
                    // Show "No results" item
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (zipCodeInput.isEmpty()) "No saved locations" else "No matching saved locations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        onClick = { },
                        enabled = false
                    )
                } else {
                    // Show filtered saved locations
                    filteredLocations.forEach { location ->
                        DropdownMenuItem(
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = location.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Zip: ${location.zip}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            },
                            onClick = {
                                // Set the zip code and fetch weather data
                                zipCodeInput = location.zip
                                currentLocation = location.name
                                isDropdownExpanded = false

                                // Fetch weather data for selected location using stored coordinates
                                coroutineScope.launch {
                                    fetchWeatherData(location.lat, location.lon, location.name)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Show divider and "View all" option if there are many saved locations
                    if (savedLocations.size > 8) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "View all saved locations...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                isDropdownExpanded = false
                                val intent = Intent(context, SavedLocationsActivity::class.java)
                                savedLocationsLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Button Row - Fetch Weather Data and View Saved Locations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

                                    // Save location to database if it was a zip code search
                                    if (zipCodeInput.isNotBlank()) {
                                        try {
                                            coroutineScope.launch {
                                                // First get the location data from the zip code
                                                val zipResult = repository.getLocationDataByZip(zipCodeInput.trim())
                                                zipResult.fold(
                                                    onSuccess = { zipCodeResponse ->
                                                        // Save the location to database
                                                        locationRepository.saveLocation(zipCodeResponse)
                                                        Log.d("KotlinWeather", "Location saved: ${zipCodeResponse.name}")
                                                    },
                                                    onFailure = { e ->
                                                        Log.e("KotlinWeather", "Failed to save location: ${e.message}")
                                                    }
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Log.e("KotlinWeather", "Error saving location: ${e.message}")
                                        }
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
                modifier = Modifier.weight(1f)
            ) {
                Text("Fetch Weather Data")
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(context, SavedLocationsActivity::class.java)
                    savedLocationsLauncher.launch(intent)
                },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("View Saved")
            }
        }

        // Weather Icon Display
        currentWeatherIcon?.let { iconCode ->
            Card(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        weatherJson?.let { json ->
                            val gson = Gson()
                            val weatherResponse = try {
                                gson.fromJson(json, WeatherResponse::class.java)
                            } catch (e: Exception) {
                                null
                            }

                            weatherResponse?.current?.let { current ->
                                val intent = Intent(context, CurrentWeatherDetailActivity::class.java).apply {
                                    putExtra("current_weather_json", gson.toJson(current))
                                    putExtra("location", currentLocation)
                                }
                                context.startActivity(intent)
                            }
                        }
                    },
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val gson = Gson()
                                        val intent = Intent(context, DailyWeatherDetailActivity::class.java).apply {
                                            putExtra("daily_weather_json", gson.toJson(day))
                                            putExtra("location", currentLocation)
                                        }
                                        context.startActivity(intent)
                                    },
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
    var currentLocation by remember { mutableStateOf("Loading location...") }
    val repository = remember { JavaWeatherRepository() }
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val locationRepository = remember { LocationRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    // States for dropdown functionality
    var savedLocations by remember { mutableStateOf<List<ZipCodeResponse>>(emptyList()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isUserTyping by remember { mutableStateOf(false) }

    // Load saved locations from database
    LaunchedEffect(Unit) {
        locationRepository.getAllLocations().collect { locations ->
            savedLocations = locations
        }
    }

    // Default coordinates: Duluth, MN
    var currentLatitude by remember { mutableStateOf(46.8384) }
    var currentLongitude by remember { mutableStateOf(-92.1800) }
    var hasTriedLocation by remember { mutableStateOf(false) }

    // Function to fetch weather data (moved before savedLocationsLauncher)
    fun fetchWeatherData(lat: Double, lon: Double, locationName: String) {
        isLoading = true
        errorMessage = null
        weatherJson = null
        currentWeatherIcon = null
        currentWeatherDescription = null
        currentLocation = locationName

        repository.getWeatherDataAsJson(lat, lon, object : JavaWeatherRepository.JsonDataCallback {
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
                errorMessage = "Error: $error"
                isLoading = false
            }
        })
    }

    // Activity result launcher for saved locations
    val savedLocationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val selectedZip = data.getStringExtra("selected_zip")
                val selectedName = data.getStringExtra("selected_name")
                val selectedLat = data.getDoubleExtra("selected_lat", 0.0)
                val selectedLon = data.getDoubleExtra("selected_lon", 0.0)

                if (selectedZip != null && selectedName != null) {
                    zipCodeInput = selectedZip
                    currentLocation = selectedName
                    currentLatitude = selectedLat
                    currentLongitude = selectedLon

                    // Fetch weather data for selected location
                    isLoading = true
                    errorMessage = null
                    weatherJson = null
                    currentWeatherIcon = null
                    currentWeatherDescription = null

                    repository.getWeatherDataAsJson(selectedLat, selectedLon, object : JavaWeatherRepository.JsonDataCallback {
                        override fun onSuccess(jsonData: String) {
                            weatherJson = jsonData
                            isLoading = false
                            Log.d("JavaWeather", "Weather JSON (from saved location): $jsonData")

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
            }
        }
    }

    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, get location and fetch weather
            isLoading = true
            errorMessage = null
            weatherJson = null
            currentWeatherIcon = null
            currentWeatherDescription = null

            repository.getCurrentLocationAndWeather(object : JavaWeatherRepository.LocationWeatherCallback {
                override fun onLocationReceived(latitude: Double, longitude: Double, locationName: String) {
                    currentLatitude = latitude
                    currentLongitude = longitude
                    currentLocation = locationName
                }

                override fun onWeatherSuccess(jsonData: String) {
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
                    // Fallback to default coordinates on error
                    currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                    repository.getWeatherDataAsJson(currentLatitude, currentLongitude, object : JavaWeatherRepository.JsonDataCallback {
                        override fun onSuccess(jsonData: String) {
                            weatherJson = jsonData
                            isLoading = false
                            Log.d("JavaWeather", "Weather JSON (fallback): $jsonData")

                            try {
                                val gson = Gson()
                                val weatherResponse = gson.fromJson(jsonData, WeatherResponse::class.java)
                                weatherResponse.current?.weather?.firstOrNull()?.let { weather ->
                                    currentWeatherIcon = weather.icon
                                    currentWeatherDescription = weather.description
                                }
                            } catch (e: Exception) {
                                Log.e("JavaWeather", "Error parsing fallback weather data: ${e.message}")
                            }
                        }

                        override fun onError(fallbackError: String) {
                            errorMessage = "Location error: $error, Weather error: $fallbackError"
                            isLoading = false
                        }
                    })
                }
            }, context)
        } else {
            // Permission denied, use default coordinates
            currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
            isLoading = true
            repository.getWeatherDataAsJson(currentLatitude, currentLongitude, object : JavaWeatherRepository.JsonDataCallback {
                override fun onSuccess(jsonData: String) {
                    weatherJson = jsonData
                    isLoading = false
                    Log.d("JavaWeather", "Weather JSON (default): $jsonData")

                    try {
                        val gson = Gson()
                        val weatherResponse = gson.fromJson(jsonData, WeatherResponse::class.java)
                        weatherResponse.current?.weather?.firstOrNull()?.let { weather ->
                            currentWeatherIcon = weather.icon
                            currentWeatherDescription = weather.description
                        }
                    } catch (e: Exception) {
                        Log.e("JavaWeather", "Error parsing default weather data: ${e.message}")
                    }
                }

                override fun onError(defaultError: String) {
                    errorMessage = defaultError
                    isLoading = false
                }
            })
        }
        hasTriedLocation = true
    }

    // Auto-fetch location and weather data on first load
    LaunchedEffect(Unit) {
        if (!hasTriedLocation) {
            if (locationManager.hasLocationPermission()) {
                // Permission already granted, get location
                isLoading = true
                errorMessage = null
                weatherJson = null
                currentWeatherIcon = null
                currentWeatherDescription = null

                repository.getCurrentLocationAndWeather(object : JavaWeatherRepository.LocationWeatherCallback {
                    override fun onLocationReceived(latitude: Double, longitude: Double, locationName: String) {
                        currentLatitude = latitude
                        currentLongitude = longitude
                        currentLocation = locationName
                    }

                    override fun onWeatherSuccess(jsonData: String) {
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
                        // Fallback to default coordinates on error
                        currentLocation = "Duluth, MN (46.8384°N, 92.1800°W)"
                        repository.getWeatherDataAsJson(currentLatitude, currentLongitude, object : JavaWeatherRepository.JsonDataCallback {
                            override fun onSuccess(jsonData: String) {
                                weatherJson = jsonData
                                isLoading = false
                                Log.d("JavaWeather", "Weather JSON (fallback): $jsonData")

                                try {
                                    val gson = Gson()
                                    val weatherResponse = gson.fromJson(jsonData, WeatherResponse::class.java)
                                    weatherResponse.current?.weather?.firstOrNull()?.let { weather ->
                                        currentWeatherIcon = weather.icon
                                        currentWeatherDescription = weather.description
                                    }
                                } catch (e: Exception) {
                                    Log.e("JavaWeather", "Error parsing fallback weather data: ${e.message}")
                                }
                            }

                            override fun onError(fallbackError: String) {
                                errorMessage = "Location error: $error, Weather error: $fallbackError"
                                isLoading = false
                            }
                        })
                    }
                }, context)
            } else {
                // Request location permission
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            hasTriedLocation = true
        }
    }

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

        // Zip Code Search Field with Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = zipCodeInput,
                onValueChange = {
                    zipCodeInput = it
                    isUserTyping = true

                    // Show dropdown when user types and there are saved locations
                    if (it.isNotEmpty() && savedLocations.isNotEmpty()) {
                        isDropdownExpanded = true
                    } else if (it.isEmpty() && savedLocations.isNotEmpty()) {
                        // If user clears the field, show all locations
                        isDropdownExpanded = true
                    } else {
                        isDropdownExpanded = false
                    }
                },
                label = { Text("Enter Zip Code") },
                placeholder = { Text("e.g., 90210") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (savedLocations.isNotEmpty()) {
                            isUserTyping = false // User clicked, not typing
                            isDropdownExpanded = !isDropdownExpanded
                        }
                    },
                enabled = !isLoading,
                trailingIcon = {
                    if (savedLocations.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                isUserTyping = false // User clicked icon, not typing
                                isDropdownExpanded = !isDropdownExpanded
                            }
                        ) {
                            Icon(
                                imageVector = if (isDropdownExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (isDropdownExpanded) "Collapse dropdown" else "Expand dropdown",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            // Dropdown Menu using stable Material3 API
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 200.dp) // Limit height and allow scrolling
            ) {
                // Filter saved locations based on user interaction
                val filteredLocations = if (!isUserTyping || zipCodeInput.isEmpty()) {
                    // Show all locations when user clicked to open dropdown or field is empty
                    savedLocations.take(8) // Show first 8 when no filter
                } else {
                    // Filter only when user is actively typing
                    savedLocations.filter { location ->
                        location.zip.contains(zipCodeInput, ignoreCase = true) ||
                        location.name.contains(zipCodeInput, ignoreCase = true)
                    }.take(5) // Limit to 5 most recent matches
                }

                if (filteredLocations.isEmpty()) {
                    // Show "No results" item
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (zipCodeInput.isEmpty()) "No saved locations" else "No matching saved locations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        onClick = { },
                        enabled = false
                    )
                } else {
                    // Show filtered saved locations
                    filteredLocations.forEach { location ->
                        DropdownMenuItem(
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = location.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Zip: ${location.zip}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            },
                            onClick = {
                                // Set the zip code and fetch weather data
                                zipCodeInput = location.zip
                                currentLocation = location.name
                                isDropdownExpanded = false

                                // Fetch weather data for selected location using stored coordinates
                                coroutineScope.launch {
                                    fetchWeatherData(location.lat, location.lon, location.name)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Show divider and "View all" option if there are many saved locations
                    if (savedLocations.size > 8) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "View all saved locations...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                isDropdownExpanded = false
                                val intent = Intent(context, SavedLocationsActivity::class.java)
                                savedLocationsLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Button Row - Fetch Weather Data and View Saved Locations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        repository.getWeatherDataAsJson(currentLatitude, currentLongitude, object : JavaWeatherRepository.JsonDataCallback {
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
                modifier = Modifier.weight(1f)
            ) {
                Text("Fetch Weather Data")
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(context, SavedLocationsActivity::class.java)
                    savedLocationsLauncher.launch(intent)
                },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("View Saved")
            }
        }

        // Weather Icon Display
        currentWeatherIcon?.let { iconCode ->
            Card(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        weatherJson?.let { json ->
                            val gson = Gson()
                            val weatherResponse = try {
                                gson.fromJson(json, WeatherResponse::class.java)
                            } catch (e: Exception) {
                                null
                            }

                            weatherResponse?.current?.let { current ->
                                val intent = Intent(context, CurrentWeatherDetailActivity::class.java).apply {
                                    putExtra("current_weather_json", gson.toJson(current))
                                    putExtra("location", currentLocation)
                                }
                                context.startActivity(intent)
                            }
                        }
                    },
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val gson = Gson()
                                        val intent = Intent(context, DailyWeatherDetailActivity::class.java).apply {
                                            putExtra("daily_weather_json", gson.toJson(day))
                                            putExtra("location", currentLocation)
                                        }
                                        context.startActivity(intent)
                                    },
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
