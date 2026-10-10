package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

enum class KeySource {
    SAVED_IN_APP,
    ENVIRONMENT,
    NOT_CONFIGURED
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cinetrack_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
    }

    fun getGeminiApiKey(): String {
        val saved = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim()
        if (!saved.isNullOrEmpty()) return saved

        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
            if (buildConfigKey.isNotEmpty() && !buildConfigKey.contains("MY_GEMINI_API_KEY")) {
                buildConfigKey
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun getGeminiKeySource(): KeySource {
        val saved = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim()
        if (!saved.isNullOrEmpty()) return KeySource.SAVED_IN_APP

        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
            if (buildConfigKey.isNotEmpty() && !buildConfigKey.contains("MY_GEMINI_API_KEY")) {
                KeySource.ENVIRONMENT
            } else KeySource.NOT_CONFIGURED
        } catch (_: Exception) {
            KeySource.NOT_CONFIGURED
        }
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun getTmdbApiKey(): String {
        val saved = prefs.getString(KEY_TMDB_API_KEY, null)?.trim()
        if (!saved.isNullOrEmpty()) return saved

        return try {
            val buildConfigKey = BuildConfig.TMDB_API_KEY.trim()
            if (buildConfigKey.isNotEmpty() && !buildConfigKey.contains("MY_TMDB_API_KEY")) {
                buildConfigKey
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun getTmdbKeySource(): KeySource {
        val saved = prefs.getString(KEY_TMDB_API_KEY, null)?.trim()
        if (!saved.isNullOrEmpty()) return KeySource.SAVED_IN_APP

        return try {
            val buildConfigKey = BuildConfig.TMDB_API_KEY.trim()
            if (buildConfigKey.isNotEmpty() && !buildConfigKey.contains("MY_TMDB_API_KEY")) {
                KeySource.ENVIRONMENT
            } else KeySource.NOT_CONFIGURED
        } catch (_: Exception) {
            KeySource.NOT_CONFIGURED
        }
    }

    fun setTmdbApiKey(key: String) {
        prefs.edit().putString(KEY_TMDB_API_KEY, key.trim()).apply()
    }

    fun hasKeys(): Boolean {
        return getGeminiApiKey().isNotEmpty() || getTmdbApiKey().isNotEmpty()
    }
}
