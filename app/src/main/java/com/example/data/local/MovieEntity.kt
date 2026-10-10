package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalTitle: String? = null,
    val mediaType: String = "movie", // "movie" or "tv"
    val posterPath: String? = null,
    val posterUrl: String? = null,
    val releaseYear: String? = null,
    val voteAverage: Double = 0.0,
    val overview: String? = null,
    val isWatched: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
