package com.example.whetherornot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.whetherornot.data.model.ZipCodeResponse
import com.example.whetherornot.data.repository.LocationRepository
import com.example.whetherornot.ui.theme.WhetherOrNotTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SavedLocationsActivity : ComponentActivity() {

    private lateinit var locationRepository: LocationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationRepository = LocationRepository(this)

        setContent {
            WhetherOrNotTheme {
                SavedLocationsScreen(
                    locationRepository = locationRepository,
                    onBackClick = { finish() },
                    onLocationClick = { location ->
                        // Return the selected location to MainActivity
                        val resultIntent = android.content.Intent().apply {
                            putExtra("selected_zip", location.zip)
                            putExtra("selected_name", location.name)
                            putExtra("selected_lat", location.lat)
                            putExtra("selected_lon", location.lon)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(
    locationRepository: LocationRepository,
    onBackClick: () -> Unit,
    onLocationClick: (ZipCodeResponse) -> Unit
) {
    var locations by remember { mutableStateOf<List<ZipCodeResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    // Load locations when the screen opens
    LaunchedEffect(showFavoritesOnly) {
        try {
            if (showFavoritesOnly) {
                locationRepository.getFavoriteLocations().collect { favoriteLocations ->
                    locations = favoriteLocations
                    isLoading = false
                }
            } else {
                locationRepository.getAllLocations().collect { allLocations ->
                    locations = allLocations
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error loading locations: ${e.message}"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Saved Locations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        // Filter Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = { showFavoritesOnly = false },
                label = { Text("All Locations") },
                selected = !showFavoritesOnly
            )
            FilterChip(
                onClick = { showFavoritesOnly = true },
                label = { Text("Favorites") },
                selected = showFavoritesOnly
            )
        }

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            locations.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (showFavoritesOnly) "No favorite locations saved" else "No locations saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showFavoritesOnly) "Mark locations as favorites to see them here" else "Search for weather data to save locations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locations) { location ->
                        LocationCard(
                            location = location,
                            onLocationClick = onLocationClick,
                            onFavoriteToggle = { loc, isFavorite ->
                                // Update favorite status in database
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        locationRepository.toggleFavorite(loc.zip, isFavorite)
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            },
                            onDelete = { loc ->
                                // Delete location from database
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        locationRepository.deleteLocation(loc)
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationCard(
    location: ZipCodeResponse,
    onLocationClick: (ZipCodeResponse) -> Unit,
    onFavoriteToggle: (ZipCodeResponse, Boolean) -> Unit,
    onDelete: (ZipCodeResponse) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onLocationClick(location) },
        colors = CardDefaults.cardColors(
            containerColor = if (location.isFavorite) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ZIP: ${location.zip}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${location.country} • ${String.format("%.4f°N, %.4f°W", location.lat, Math.abs(location.lon))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Last searched time
                val searchedDate = Date(location.searchedAt)
                val dateFormatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                Text(
                    text = "Last searched: ${dateFormatter.format(searchedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onFavoriteToggle(location, !location.isFavorite) }
                ) {
                    Icon(
                        imageVector = if (location.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (location.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (location.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onDelete(location) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete location",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
