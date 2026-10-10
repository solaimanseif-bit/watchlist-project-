package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TmdbWatchProvider(
    val id: Int,
    val name: String,
    val logoPath: String?,
    val logoUrl: String?,
    val providerType: String, // "flatrate" or "free"
    val watchLink: String? = null
)

data class TmdbWatchProvidersResult(
    val providers: List<TmdbWatchProvider> = emptyList(),
    val tmdbLink: String? = null,
    val region: String = "EG"
)

data class TmdbSearchResult(
    val tmdbId: Int? = null,
    val title: String,
    val originalTitle: String? = null,
    val mediaType: String = "movie",
    val posterPath: String? = null,
    val posterUrl: String? = null,
    val releaseYear: String? = null,
    val voteAverage: Double = 0.0,
    val overview: String? = null
)

class TmdbService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_SEARCH_URL = "https://api.themoviedb.org/3/search/multi"
        private const val POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500"
    }

    suspend fun verifyApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.contains("MY_TMDB_API_KEY")) {
            return@withContext Result.failure(IllegalArgumentException("TMDb key is empty or placeholder."))
        }

        try {
            val url = "https://api.themoviedb.org/3/configuration?api_key=$cleanKey"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("TMDb key verified! (HTTP 200)")
                } else {
                    Result.failure(Exception("TMDb rejected key (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun searchMultiSuggestions(apiKey: String, query: String, limit: Int = 5): List<TmdbSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || apiKey.isBlank() || apiKey.contains("MY_TMDB_API_KEY")) {
            return@withContext emptyList()
        }

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = "$BASE_SEARCH_URL?api_key=$apiKey&query=$encodedQuery&include_adult=false&language=en-US"

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()

                val responseBody = response.body?.string().orEmpty()
                val root = JSONObject(responseBody)
                val results = root.optJSONArray("results") ?: return@withContext emptyList()

                val list = mutableListOf<TmdbSearchResult>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val mediaType = item.optString("media_type")
                    if (mediaType != "movie" && mediaType != "tv") continue

                    val isMovie = mediaType == "movie"
                    val resolvedTitle = if (isMovie) {
                        item.optString("title").ifEmpty { item.optString("name") }
                    } else {
                        item.optString("name").ifEmpty { item.optString("title") }
                    }
                    if (resolvedTitle.isBlank()) continue

                    val originalTitle = if (isMovie) {
                        item.optString("original_title").ifEmpty { null }
                    } else {
                        item.optString("original_name").ifEmpty { null }
                    }

                    val posterPath = item.optString("poster_path").ifEmpty { null }
                    val posterUrl = if (!posterPath.isNullOrEmpty()) "$POSTER_BASE_URL$posterPath" else null

                    val dateStr = if (isMovie) item.optString("release_date") else item.optString("first_air_date")
                    val releaseYear = if (dateStr.length >= 4) dateStr.substring(0, 4) else null

                    val voteAverage = item.optDouble("vote_average", 0.0)
                    val overview = item.optString("overview").ifEmpty { null }

                    val tmdbId = item.optInt("id", 0).let { if (it > 0) it else null }

                    list.add(
                        TmdbSearchResult(
                            tmdbId = tmdbId,
                            title = resolvedTitle,
                            originalTitle = originalTitle,
                            mediaType = if (mediaType == "tv") "tv" else "movie",
                            posterPath = posterPath,
                            posterUrl = posterUrl,
                            releaseYear = releaseYear,
                            voteAverage = voteAverage,
                            overview = overview
                        )
                    )

                    if (list.size >= limit) break
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchMulti(apiKey: String, query: String): TmdbSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (apiKey.isBlank()) {
            return@withContext TmdbSearchResult(
                title = cleanQuery,
                originalTitle = null,
                mediaType = "movie",
                posterPath = null,
                posterUrl = null,
                releaseYear = null,
                voteAverage = 0.0,
                overview = "Directly added title. Add TMDb API Key in Settings to automatically fetch posters, synopsis, and ratings."
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = "$BASE_SEARCH_URL?api_key=$apiKey&query=$encodedQuery&include_adult=false&language=en-US"

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext TmdbSearchResult(
                        title = cleanQuery,
                        overview = "TMDb search returned HTTP ${response.code}."
                    )
                }

                val responseBody = response.body?.string().orEmpty()
                val root = JSONObject(responseBody)
                val results = root.optJSONArray("results")

                if (results == null || results.length() == 0) {
                    return@withContext TmdbSearchResult(
                        title = cleanQuery,
                        overview = "No match found on TMDb for \"$cleanQuery\"."
                    )
                }

                // Find first item that is 'movie' or 'tv'
                var bestMatch: JSONObject? = null
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val mediaType = item.optString("media_type")
                    if (mediaType == "movie" || mediaType == "tv") {
                        bestMatch = item
                        break
                    }
                }

                if (bestMatch == null) {
                    bestMatch = results.getJSONObject(0)
                }

                val mediaType = bestMatch.optString("media_type", "movie")
                val isMovie = mediaType == "movie"

                val resolvedTitle = if (isMovie) {
                    bestMatch.optString("title").ifEmpty { bestMatch.optString("name", cleanQuery) }
                } else {
                    bestMatch.optString("name").ifEmpty { bestMatch.optString("title", cleanQuery) }
                }

                val originalTitle = if (isMovie) {
                    bestMatch.optString("original_title").ifEmpty { null }
                } else {
                    bestMatch.optString("original_name").ifEmpty { null }
                }

                val posterPath = bestMatch.optString("poster_path").ifEmpty { null }
                val posterUrl = if (posterPath != null && posterPath.isNotEmpty()) {
                    "$POSTER_BASE_URL$posterPath"
                } else null

                val dateStr = if (isMovie) {
                    bestMatch.optString("release_date")
                } else {
                    bestMatch.optString("first_air_date")
                }
                val releaseYear = if (dateStr.length >= 4) {
                    dateStr.substring(0, 4)
                } else null

                val voteAverage = bestMatch.optDouble("vote_average", 0.0)
                val overview = bestMatch.optString("overview").ifEmpty { null }
                val tmdbId = bestMatch.optInt("id", 0).let { if (it > 0) it else null }

                TmdbSearchResult(
                    tmdbId = tmdbId,
                    title = resolvedTitle.ifEmpty { cleanQuery },
                    originalTitle = originalTitle,
                    mediaType = if (mediaType == "tv") "tv" else "movie",
                    posterPath = posterPath,
                    posterUrl = posterUrl,
                    releaseYear = releaseYear,
                    voteAverage = voteAverage,
                    overview = overview
                )
            }
        } catch (e: Exception) {
            TmdbSearchResult(
                title = cleanQuery,
                overview = "Error fetching TMDb metadata: ${e.message}"
            )
        }
    }

    /**
     * Fetches real-time watch providers for a given title/ID using TMDb Watch Providers API (powered by JustWatch).
     * Endpoint: https://api.themoviedb.org/3/{media_type}/{tmdb_id}/watch/providers?api_key={apiKey}
     * Regions checked in priority order: EG (Egypt) -> US -> AE
     */
    suspend fun getWatchProviders(
        apiKey: String,
        mediaType: String,
        tmdbId: Int
    ): TmdbWatchProvidersResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.contains("MY_TMDB_API_KEY") || tmdbId <= 0) {
            return@withContext TmdbWatchProvidersResult()
        }

        val type = if (mediaType.equals("tv", ignoreCase = true)) "tv" else "movie"
        val url = "https://api.themoviedb.org/3/$type/$tmdbId/watch/providers?api_key=$cleanKey"

        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext TmdbWatchProvidersResult()
                }

                val body = response.body?.string().orEmpty()
                val root = JSONObject(body)
                val resultsObj = root.optJSONObject("results") ?: return@withContext TmdbWatchProvidersResult()

                // Check region in priority order: EG (Egypt) -> US -> AE
                val priorityRegions = listOf("EG", "US", "AE")
                var selectedRegion = "EG"
                var regionObj: JSONObject? = null

                for (region in priorityRegions) {
                    val cand = resultsObj.optJSONObject(region)
                    if (cand != null) {
                        val flatrate = cand.optJSONArray("flatrate")
                        val free = cand.optJSONArray("free")
                        if ((flatrate != null && flatrate.length() > 0) || (free != null && free.length() > 0)) {
                            regionObj = cand
                            selectedRegion = region
                            break
                        }
                    }
                }

                // If none of EG, US, AE had providers, try the first available region in results
                if (regionObj == null) {
                    val keys = resultsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val cand = resultsObj.optJSONObject(key)
                        if (cand != null) {
                            val flatrate = cand.optJSONArray("flatrate")
                            val free = cand.optJSONArray("free")
                            if ((flatrate != null && flatrate.length() > 0) || (free != null && free.length() > 0)) {
                                regionObj = cand
                                selectedRegion = key
                                break
                            }
                        }
                    }
                }

                if (regionObj == null) {
                    return@withContext TmdbWatchProvidersResult()
                }

                val tmdbLink = regionObj.optString("link").ifEmpty { null }
                val providersList = mutableListOf<TmdbWatchProvider>()

                // Extract 'flatrate' (subscription) providers
                val flatrateArray = regionObj.optJSONArray("flatrate")
                if (flatrateArray != null) {
                    for (i in 0 until flatrateArray.length()) {
                        val p = flatrateArray.getJSONObject(i)
                        val providerId = p.optInt("provider_id", i)
                        val providerName = p.optString("provider_name")
                        val logoPath = p.optString("logo_path").ifEmpty { null }
                        val logoUrl = if (!logoPath.isNullOrEmpty()) "https://image.tmdb.org/t/p/w92$logoPath" else null
                        if (providerName.isNotBlank()) {
                            providersList.add(
                                TmdbWatchProvider(
                                    id = providerId,
                                    name = providerName,
                                    logoPath = logoPath,
                                    logoUrl = logoUrl,
                                    providerType = "flatrate",
                                    watchLink = tmdbLink
                                )
                            )
                        }
                    }
                }

                // Extract 'free' providers
                val freeArray = regionObj.optJSONArray("free")
                if (freeArray != null) {
                    for (i in 0 until freeArray.length()) {
                        val p = freeArray.getJSONObject(i)
                        val providerId = p.optInt("provider_id", 1000 + i)
                        val providerName = p.optString("provider_name")
                        val logoPath = p.optString("logo_path").ifEmpty { null }
                        val logoUrl = if (!logoPath.isNullOrEmpty()) "https://image.tmdb.org/t/p/w92$logoPath" else null
                        if (providerName.isNotBlank() && providersList.none { it.id == providerId || it.name == providerName }) {
                            providersList.add(
                                TmdbWatchProvider(
                                    id = providerId,
                                    name = providerName,
                                    logoPath = logoPath,
                                    logoUrl = logoUrl,
                                    providerType = "free",
                                    watchLink = tmdbLink
                                )
                            )
                        }
                    }
                }

                TmdbWatchProvidersResult(
                    providers = providersList,
                    tmdbLink = tmdbLink,
                    region = selectedRegion
                )
            }
        } catch (_: Exception) {
            TmdbWatchProvidersResult()
        }
    }
}
