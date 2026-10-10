package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

object TextDetectorUtil {

    /**
     * Inspects image metadata, filename, and URI segments to extract
     * any recognizable movie or show title text.
     */
    fun detectTextFromUri(context: Context, uri: Uri): String {
        // 1. Check EXIF Image Description or User Comment if available
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = android.media.ExifInterface(stream)
                val description = exif.getAttribute(android.media.ExifInterface.TAG_IMAGE_DESCRIPTION)
                if (!description.isNullOrBlank()) {
                    val cleaned = cleanRawText(description)
                    if (cleaned.isNotBlank()) return cleaned
                }
            }
        } catch (_: Exception) {
            // Ignore exif read issues
        }

        // 2. Query Display Name via ContentResolver
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val fileName = it.getString(nameIndex)
                        if (!fileName.isNullOrBlank()) {
                            val candidate = cleanFileNameToTitle(fileName)
                            if (candidate.isNotBlank()) return candidate
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore cursor issues
        }

        // 3. Fallback to URI last path segment
        try {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                val candidate = cleanFileNameToTitle(lastSegment)
                if (candidate.isNotBlank()) return candidate
            }
        } catch (_: Exception) {
            // Ignore uri issues
        }

        return "Movie Title"
    }

    /**
     * Cleans raw filenames into capitalized movie/show title candidates.
     * e.g. "Screenshot_2026_Severance_Season2.png" -> "Severance Season 2"
     * e.g. "Dune_Part_Two.jpg" -> "Dune Part Two"
     * e.g. "arcane_netflix_rec.webp" -> "Arcane"
     */
    fun cleanFileNameToTitle(fileName: String): String {
        // Strip extension
        var name = fileName.substringBeforeLast(".")

        // Remove typical screenshot/camera prefixes
        name = name.replace(
            Regex("(?i)^(screenshot|screen_shot|screen|img|image|photo|pxl|pic|wp|video|rec|media)[_\\-\\s]*"),
            ""
        )

        // Remove numeric date/timestamp stamps e.g. 2026-10-10, 20261010, 103022
        name = name.replace(Regex("\\b(19|20)\\d{2}[-_]?\\d{2}[-_]?\\d{2}\\b"), "")
        name = name.replace(Regex("\\b\\d{6,14}\\b"), "")

        // Replace separators with spaces
        name = name.replace('_', ' ').replace('-', ' ').replace('.', ' ')

        // Remove common non-title trailing words like "recommendation", "poster", "review"
        name = name.replace(Regex("(?i)\\b(recommendation|rec|review|poster|hd|4k|1080p|720p)\\b"), "")

        // Normalize whitespaces
        name = name.trim().replace(Regex("\\s+"), " ")

        // If string contains meaningful characters
        val words = name.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.all { char -> char.isDigit() } }

        if (words.isNotEmpty()) {
            val titleCandidate = words.joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                }
            }
            if (titleCandidate.isNotBlank()) {
                return titleCandidate
            }
        }

        return if (name.isNotBlank()) name else "Movie Title"
    }

    private fun cleanRawText(raw: String): String {
        return raw.trim()
            .removePrefix("\"").removeSuffix("\"")
            .removePrefix("'").removeSuffix("'")
            .trim()
    }
}
