package com.example.whetherornot.data.database

import androidx.room.*
import com.example.whetherornot.data.model.ZipCodeResponse
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for saved location operations
 */
@Dao
interface LocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY searchedAt DESC")
    fun getAllLocations(): Flow<List<ZipCodeResponse>>

    @Query("SELECT * FROM saved_locations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteLocations(): Flow<List<ZipCodeResponse>>

    @Query("SELECT * FROM saved_locations ORDER BY searchedAt DESC LIMIT 10")
    fun getRecentLocations(): Flow<List<ZipCodeResponse>>

    @Query("SELECT * FROM saved_locations WHERE zip = :zipCode LIMIT 1")
    suspend fun getLocationByZip(zipCode: String): ZipCodeResponse?

    @Query("SELECT * FROM saved_locations WHERE name LIKE '%' || :name || '%' ORDER BY name ASC")
    fun searchLocationsByName(name: String): Flow<List<ZipCodeResponse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: ZipCodeResponse)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<ZipCodeResponse>)

    @Update
    suspend fun updateLocation(location: ZipCodeResponse)

    @Query("UPDATE saved_locations SET isFavorite = :isFavorite WHERE zip = :zipCode")
    suspend fun updateFavoriteStatus(zipCode: String, isFavorite: Boolean)

    @Query("UPDATE saved_locations SET searchedAt = :timestamp WHERE zip = :zipCode")
    suspend fun updateSearchTime(zipCode: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteLocation(location: ZipCodeResponse)

    @Query("DELETE FROM saved_locations WHERE zip = :zipCode")
    suspend fun deleteLocationByZip(zipCode: String)

    @Query("DELETE FROM saved_locations WHERE isFavorite = 0")
    suspend fun deleteNonFavorites()

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAllLocations()

    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun getLocationCount(): Int

    @Query("SELECT COUNT(*) FROM saved_locations WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int
}
