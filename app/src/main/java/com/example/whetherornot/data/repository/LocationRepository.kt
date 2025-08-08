package com.example.whetherornot.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.whetherornot.data.database.WeatherDatabase
import com.example.whetherornot.data.database.LocationDao
import com.example.whetherornot.data.model.ZipCodeResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing location data with local database storage
 */
class LocationRepository(context: Context) {

    private val database = WeatherDatabase.getDatabase(context)
    private val locationDao: LocationDao = database.locationDao()

    // Flow-based operations for reactive UI updates
    fun getAllLocations(): Flow<List<ZipCodeResponse>> = locationDao.getAllLocations()

    fun getFavoriteLocations(): Flow<List<ZipCodeResponse>> = locationDao.getFavoriteLocations()

    fun getRecentLocations(): Flow<List<ZipCodeResponse>> = locationDao.getRecentLocations()

    fun searchLocationsByName(name: String): Flow<List<ZipCodeResponse>> =
        locationDao.searchLocationsByName(name)

    // LiveData versions for compatibility with existing code
    fun getAllLocationsLiveData(): LiveData<List<ZipCodeResponse>> =
        getAllLocations().asLiveData()

    fun getFavoriteLocationsLiveData(): LiveData<List<ZipCodeResponse>> =
        getFavoriteLocations().asLiveData()

    fun getRecentLocationsLiveData(): LiveData<List<ZipCodeResponse>> =
        getRecentLocations().asLiveData()

    // Suspend functions for database operations
    suspend fun getLocationByZip(zipCode: String): ZipCodeResponse? =
        withContext(Dispatchers.IO) {
            locationDao.getLocationByZip(zipCode)
        }

    suspend fun saveLocation(location: ZipCodeResponse) =
        withContext(Dispatchers.IO) {
            locationDao.insertLocation(location)
        }

    suspend fun saveLocations(locations: List<ZipCodeResponse>) =
        withContext(Dispatchers.IO) {
            locationDao.insertLocations(locations)
        }

    suspend fun updateLocation(location: ZipCodeResponse) =
        withContext(Dispatchers.IO) {
            locationDao.updateLocation(location)
        }

    suspend fun toggleFavorite(zipCode: String, isFavorite: Boolean) =
        withContext(Dispatchers.IO) {
            locationDao.updateFavoriteStatus(zipCode, isFavorite)
        }

    suspend fun updateSearchTime(zipCode: String) =
        withContext(Dispatchers.IO) {
            locationDao.updateSearchTime(zipCode)
        }

    suspend fun deleteLocation(location: ZipCodeResponse) =
        withContext(Dispatchers.IO) {
            locationDao.deleteLocation(location)
        }

    suspend fun deleteLocationByZip(zipCode: String) =
        withContext(Dispatchers.IO) {
            locationDao.deleteLocationByZip(zipCode)
        }

    suspend fun clearNonFavorites() =
        withContext(Dispatchers.IO) {
            locationDao.deleteNonFavorites()
        }

    suspend fun clearAllLocations() =
        withContext(Dispatchers.IO) {
            locationDao.deleteAllLocations()
        }

    suspend fun getLocationCount(): Int =
        withContext(Dispatchers.IO) {
            locationDao.getLocationCount()
        }

    suspend fun getFavoriteCount(): Int =
        withContext(Dispatchers.IO) {
            locationDao.getFavoriteCount()
        }

    /**
     * Save a location from API response and update search time if it already exists
     */
    suspend fun saveOrUpdateLocation(location: ZipCodeResponse) =
        withContext(Dispatchers.IO) {
            val existingLocation = locationDao.getLocationByZip(location.zip)
            if (existingLocation != null) {
                // Update search time and preserve favorite status
                val updatedLocation = location.copy(
                    searchedAt = System.currentTimeMillis(),
                    isFavorite = existingLocation.isFavorite
                )
                locationDao.updateLocation(updatedLocation)
            } else {
                locationDao.insertLocation(location)
            }
        }

    /**
     * Check if location exists in database
     */
    suspend fun locationExists(zipCode: String): Boolean =
        withContext(Dispatchers.IO) {
            locationDao.getLocationByZip(zipCode) != null
        }
}
