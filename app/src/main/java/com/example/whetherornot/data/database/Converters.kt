package com.example.whetherornot.data.database

import androidx.room.TypeConverter

/**
 * Type converters for Room database
 * Currently minimal as ZipCodeResponse uses simple types
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Long {
        return value ?: 0L
    }

    @TypeConverter
    fun dateToTimestamp(timestamp: Long): Long {
        return timestamp
    }
}
