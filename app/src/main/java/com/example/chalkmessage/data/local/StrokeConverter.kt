package com.example.chalkmessage.data.local

import androidx.room.TypeConverter
import com.example.chalkmessage.data.model.Stroke
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StrokeConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStrokesList(strokes: List<Stroke>): String {
        return json.encodeToString(strokes)
    }

    @TypeConverter
    fun toStrokesList(jsonString: String): List<Stroke> {
        return json.decodeFromString(jsonString)
    }
}
