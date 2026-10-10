package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MovieEntity
import com.example.data.preferences.SettingsManager
import com.example.data.remote.GeminiOcrResult
import com.example.data.remote.GeminiVisionService
import com.example.data.remote.TmdbSearchResult
import com.example.data.remote.TmdbService
import com.example.data.remote.TmdbWatchProvider
import com.example.data.remote.TmdbWatchProvidersResult
import com.example.util.ImageOptimizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class FilterTab {
    ALL,
    TO_WATCH,
    WATCHED
}

enum class ScanStatus {
    QUEUED,
    COMPRESSING,
    OCR_ANALYZING,
    TMDB_SEARCHING,
    SUCCESS,
    FAILED,
    TIMED_OUT
}

data class ScanItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val thumbnailBitmap: Bitmap? = null,
    val status: ScanStatus = ScanStatus.QUEUED,
    val statusMessage: String = "Waiting in queue...",
    val byteSize: Int = 0,
    val extractedTitles: List<String> = emptyList(),
    val manualInputText: String = "",
    val isAddedToWatchlist: Boolean = false,
    val addedMovieCount: Int = 0
)

enum class MediaTypeFilter {
    ALL,
    MOVIE,
    TV
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val movieDao = database.movieDao()
    val settingsManager = SettingsManager(application)
    private val geminiService = GeminiVisionService()
    private val tmdbService = TmdbService()

    // API Keys state
    private val _geminiApiKey = MutableStateFlow(settingsManager.getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _tmdbApiKey = MutableStateFlow(settingsManager.getTmdbApiKey())
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey.asStateFlow()

    // Filter and search
    private val _currentFilter = MutableStateFlow(FilterTab.TO_WATCH)
    val currentFilter: StateFlow<FilterTab> = _currentFilter.asStateFlow()

    private val _selectedMediaType = MutableStateFlow(MediaTypeFilter.ALL)
    val selectedMediaType: StateFlow<MediaTypeFilter> = _selectedMediaType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setMediaTypeFilter(filter: MediaTypeFilter) {
        _selectedMediaType.value = filter
    }

    // All movies from Room
    val allMovies = movieDao.getAllMovies().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered movies
    val filteredMovies: StateFlow<List<MovieEntity>> = combine(
        allMovies,
        _currentFilter,
        _selectedMediaType,
        _searchQuery
    ) { list, filter, mediaType, query ->
        list.filter { movie ->
            val matchesFilter = when (filter) {
                FilterTab.ALL -> true
                FilterTab.TO_WATCH -> !movie.isWatched
                FilterTab.WATCHED -> movie.isWatched
            }
            val matchesMedia = when (mediaType) {
                MediaTypeFilter.ALL -> true
                MediaTypeFilter.MOVIE -> movie.mediaType.equals("movie", ignoreCase = true)
                MediaTypeFilter.TV -> movie.mediaType.equals("tv", ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    movie.title.contains(query, ignoreCase = true) ||
                    (movie.originalTitle?.contains(query, ignoreCase = true) == true) ||
                    (movie.overview?.contains(query, ignoreCase = true) == true)
            matchesFilter && matchesMedia && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Scan queue
    private val _scanQueue = MutableStateFlow<List<ScanItem>>(emptyList())
    val scanQueue: StateFlow<List<ScanItem>> = _scanQueue.asStateFlow()

    private val _isProcessingQueue = MutableStateFlow(false)
    val isProcessingQueue: StateFlow<Boolean> = _isProcessingQueue.asStateFlow()

    private var queueJob: Job? = null

    // UI Dialog states
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showScanSheet = MutableStateFlow(false)
    val showScanSheet: StateFlow<Boolean> = _showScanSheet.asStateFlow()

    private val _showManualAddDialog = MutableStateFlow(false)
    val showManualAddDialog: StateFlow<Boolean> = _showManualAddDialog.asStateFlow()

    private val _activeWatchSourcesMovie = MutableStateFlow<MovieEntity?>(null)
    val activeWatchSourcesMovie: StateFlow<MovieEntity?> = _activeWatchSourcesMovie.asStateFlow()

    fun setFilter(tab: FilterTab) {
        _currentFilter.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun openScanSheet() {
        _showScanSheet.value = true
    }

    fun closeScanSheet() {
        _showScanSheet.value = false
    }

    fun openManualAddDialog() {
        _showManualAddDialog.value = true
    }

    fun closeManualAddDialog() {
        _showManualAddDialog.value = false
    }

    fun openWatchSources(movie: MovieEntity) {
        _activeWatchSourcesMovie.value = movie
    }

    fun closeWatchSources() {
        _activeWatchSourcesMovie.value = null
    }

    fun saveApiKeys(geminiKey: String, tmdbKey: String) {
        settingsManager.setGeminiApiKey(geminiKey)
        settingsManager.setTmdbApiKey(tmdbKey)
        _geminiApiKey.value = geminiKey.trim()
        _tmdbApiKey.value = tmdbKey.trim()
    }

    fun toggleWatched(movie: MovieEntity) {
        viewModelScope.launch {
            movieDao.updateWatchedStatus(movie.id, !movie.isWatched)
        }
    }

    fun deleteMovie(movie: MovieEntity) {
        viewModelScope.launch {
            movieDao.deleteMovie(movie)
        }
    }

    suspend fun verifyGeminiKey(key: String): Result<String> {
        return geminiService.verifyApiKey(key)
    }

    suspend fun verifyTmdbKey(key: String): Result<String> {
        return tmdbService.verifyApiKey(key)
    }

    fun getGeminiKeySource() = settingsManager.getGeminiKeySource()
    fun getTmdbKeySource() = settingsManager.getTmdbKeySource()

    // Add screenshots to batch queue and start processing sequentially
    fun enqueueScreenshots(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()
        val newItems = uris.map { uri ->
            val detected = com.example.util.TextDetectorUtil.detectTextFromUri(context, uri)
            ScanItem(
                uri = uri,
                status = ScanStatus.QUEUED,
                statusMessage = "Queued for processing",
                manualInputText = detected
            )
        }
        _scanQueue.value = _scanQueue.value + newItems
        _showScanSheet.value = true
        startQueueProcessing()
    }

    fun updateManualInputText(itemId: String, text: String) {
        _scanQueue.value = _scanQueue.value.map { item ->
            if (item.id == itemId) item.copy(manualInputText = text) else item
        }
    }

    fun clearCompletedScanQueue() {
        _scanQueue.value = _scanQueue.value.filter {
            it.status != ScanStatus.SUCCESS
        }
    }

    fun removeScanItem(itemId: String) {
        _scanQueue.value = _scanQueue.value.filter { it.id != itemId }
    }

    suspend fun searchSuggestions(query: String): List<com.example.data.remote.TmdbSearchResult> {
        val tmdbKey = _tmdbApiKey.value
        return tmdbService.searchMultiSuggestions(tmdbKey, query, 5)
    }

    fun addMovieDirectly(result: com.example.data.remote.TmdbSearchResult, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val entity = MovieEntity(
                title = result.title,
                originalTitle = result.originalTitle,
                mediaType = result.mediaType,
                posterPath = result.posterPath,
                posterUrl = result.posterUrl,
                releaseYear = result.releaseYear,
                voteAverage = result.voteAverage,
                overview = result.overview
            )
            movieDao.insertMovie(entity)
            onDone?.invoke()
        }
    }

    fun processManualFallbackWithResult(itemId: String, result: com.example.data.remote.TmdbSearchResult) {
        viewModelScope.launch {
            val entity = MovieEntity(
                title = result.title,
                originalTitle = result.originalTitle,
                mediaType = result.mediaType,
                posterPath = result.posterPath,
                posterUrl = result.posterUrl,
                releaseYear = result.releaseYear,
                voteAverage = result.voteAverage,
                overview = result.overview
            )
            movieDao.insertMovie(entity)
            _scanQueue.value = _scanQueue.value.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        status = ScanStatus.SUCCESS,
                        statusMessage = "Added \"${result.title}\" to Watchlist!",
                        extractedTitles = listOf(result.title),
                        isAddedToWatchlist = true,
                        addedMovieCount = 1
                    )
                } else item
            }
        }
    }

    suspend fun getRealTimeWatchProviders(movie: MovieEntity): TmdbWatchProvidersResult {
        val apiKey = _tmdbApiKey.value.trim()
        if (apiKey.isBlank() || apiKey.contains("MY_TMDB_API_KEY")) {
            return TmdbWatchProvidersResult()
        }
        // First look up tmdb_id if needed
        val searchResult = tmdbService.searchMulti(apiKey, movie.title)
        val tmdbId = searchResult.tmdbId ?: return TmdbWatchProvidersResult()
        val mediaType = if (movie.mediaType.equals("tv", ignoreCase = true)) "tv" else searchResult.mediaType
        return tmdbService.getWatchProviders(apiKey, mediaType, tmdbId)
    }

    suspend fun searchTmdbSuggestions(query: String): List<TmdbSearchResult> {
        val apiKey = _tmdbApiKey.value.trim()
        return tmdbService.searchMultiSuggestions(apiKey, query, limit = 5)
    }

    fun addTmdbResultToWatchlist(result: TmdbSearchResult, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val entity = MovieEntity(
                title = result.title,
                originalTitle = result.originalTitle,
                mediaType = result.mediaType,
                posterPath = result.posterPath,
                posterUrl = result.posterUrl,
                releaseYear = result.releaseYear,
                voteAverage = result.voteAverage,
                overview = result.overview
            )
            movieDao.insertMovie(entity)
            onDone?.invoke()
        }
    }

    fun addSuggestionToFallbackItem(itemId: String, result: TmdbSearchResult) {
        viewModelScope.launch {
            val entity = MovieEntity(
                title = result.title,
                originalTitle = result.originalTitle,
                mediaType = result.mediaType,
                posterPath = result.posterPath,
                posterUrl = result.posterUrl,
                releaseYear = result.releaseYear,
                voteAverage = result.voteAverage,
                overview = result.overview
            )
            movieDao.insertMovie(entity)
            _scanQueue.value = _scanQueue.value.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        status = ScanStatus.SUCCESS,
                        statusMessage = "Added \"${result.title}\" to Watchlist!",
                        extractedTitles = listOf(result.title),
                        isAddedToWatchlist = true,
                        addedMovieCount = 1
                    )
                } else item
            }
        }
    }

    fun addManualTitleToWatchlist(title: String, onDone: (() -> Unit)? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val tmdbKey = _tmdbApiKey.value
            val searchResult = tmdbService.searchMulti(tmdbKey, title)
            val entity = MovieEntity(
                title = searchResult.title,
                originalTitle = searchResult.originalTitle,
                mediaType = searchResult.mediaType,
                posterPath = searchResult.posterPath,
                posterUrl = searchResult.posterUrl,
                releaseYear = searchResult.releaseYear,
                voteAverage = searchResult.voteAverage,
                overview = searchResult.overview
            )
            movieDao.insertMovie(entity)
            onDone?.invoke()
        }
    }

    fun processManualFallbackItem(itemId: String, customTitle: String) {
        if (customTitle.isBlank()) return
        viewModelScope.launch {
            updateItemStatus(itemId, ScanStatus.TMDB_SEARCHING, "Searching TMDb for \"$customTitle\"...")
            val tmdbKey = _tmdbApiKey.value
            val searchResult = tmdbService.searchMulti(tmdbKey, customTitle)
            val entity = MovieEntity(
                title = searchResult.title,
                originalTitle = searchResult.originalTitle,
                mediaType = searchResult.mediaType,
                posterPath = searchResult.posterPath,
                posterUrl = searchResult.posterUrl,
                releaseYear = searchResult.releaseYear,
                voteAverage = searchResult.voteAverage,
                overview = searchResult.overview
            )
            movieDao.insertMovie(entity)
            _scanQueue.value = _scanQueue.value.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        status = ScanStatus.SUCCESS,
                        statusMessage = "Added \"${searchResult.title}\" to Watchlist!",
                        extractedTitles = listOf(searchResult.title),
                        isAddedToWatchlist = true,
                        addedMovieCount = 1
                    )
                } else item
            }
        }
    }

    fun retryItem(itemId: String) {
        val target = _scanQueue.value.find { it.id == itemId } ?: return
        _scanQueue.value = _scanQueue.value.map {
            if (it.id == itemId) it.copy(status = ScanStatus.QUEUED, statusMessage = "Re-queued for processing") else it
        }
        startQueueProcessing()
    }

    private fun startQueueProcessing() {
        if (_isProcessingQueue.value) return
        queueJob = viewModelScope.launch {
            _isProcessingQueue.value = true
            val context = getApplication<Application>()

            while (true) {
                // Find next queued item
                val currentQueue = _scanQueue.value
                val nextItem = currentQueue.firstOrNull { it.status == ScanStatus.QUEUED }
                if (nextItem == null) {
                    break
                }

                processSingleItem(nextItem, context)

                // Mandatory 1.5s delay between sequential requests to protect free tier quotas
                delay(1500)
            }

            _isProcessingQueue.value = false
        }
    }

    private suspend fun processSingleItem(item: ScanItem, context: Application) {
        val itemId = item.id
        try {
            // Step 1: Compress and Optimize Image (max 800px, 60% JPEG quality)
            updateItemStatus(itemId, ScanStatus.COMPRESSING, "Optimizing & compressing (<100KB)...")
            val optimizedResult = try {
                ImageOptimizer.optimize(context, item.uri)
            } catch (e: Exception) {
                updateItemFailure(itemId, "Image compression failed: ${e.message}", null)
                return
            }

            // Update with thumbnail and byte size
            _scanQueue.value = _scanQueue.value.map {
                if (it.id == itemId) {
                    it.copy(
                        thumbnailBitmap = optimizedResult.thumbnailBitmap,
                        byteSize = optimizedResult.byteSize
                    )
                } else it
            }

            // Step 2: Gemini OCR Title Extraction
            val apiKey = _geminiApiKey.value.trim()
            val context = getApplication<Application>()
            val detectedFallback = com.example.util.TextDetectorUtil.detectTextFromUri(context, item.uri)

            if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY")) {
                updateItemFailure(
                    itemId = itemId,
                    errorMsg = "Gemini API Key is not configured. Please set it in Settings to scan screenshots.",
                    thumb = optimizedResult.thumbnailBitmap
                )
                return
            }

            updateItemStatus(itemId, ScanStatus.OCR_ANALYZING, "Extracting titles via Gemini...")

            val ocrResult = geminiService.extractTitles(
                apiKey = apiKey,
                base64JpegData = optimizedResult.base64Data,
                onProgressUpdate = { msg ->
                    updateItemStatus(itemId, ScanStatus.OCR_ANALYZING, msg)
                }
            )

            when (ocrResult) {
                is GeminiOcrResult.Error -> {
                    val status = if (ocrResult.isTimeout) ScanStatus.TIMED_OUT else ScanStatus.FAILED
                    _scanQueue.value = _scanQueue.value.map {
                        if (it.id == itemId) {
                            val candidateText = it.manualInputText.ifBlank { detectedFallback }
                            it.copy(
                                status = status,
                                statusMessage = ocrResult.message,
                                manualInputText = candidateText
                            )
                        } else it
                    }
                    return
                }
                is GeminiOcrResult.Success -> {
                    val titles = ocrResult.titles
                    if (titles.isEmpty()) {
                        updateItemFailure(itemId, "No movie or show titles recognized.", optimizedResult.thumbnailBitmap)
                        return
                    }

                    // Step 3: TMDb Search & Add for each extracted title
                    updateItemStatus(itemId, ScanStatus.TMDB_SEARCHING, "Fetching TMDb metadata for ${titles.size} title(s)...")

                    val tmdbKey = _tmdbApiKey.value
                    var addedCount = 0
                    for (title in titles) {
                        val tmdbInfo = tmdbService.searchMulti(tmdbKey, title)
                        val entity = MovieEntity(
                            title = tmdbInfo.title,
                            originalTitle = tmdbInfo.originalTitle,
                            mediaType = tmdbInfo.mediaType,
                            posterPath = tmdbInfo.posterPath,
                            posterUrl = tmdbInfo.posterUrl,
                            releaseYear = tmdbInfo.releaseYear,
                            voteAverage = tmdbInfo.voteAverage,
                            overview = tmdbInfo.overview
                        )
                        movieDao.insertMovie(entity)
                        addedCount++
                    }

                    _scanQueue.value = _scanQueue.value.map {
                        if (it.id == itemId) {
                            it.copy(
                                status = ScanStatus.SUCCESS,
                                statusMessage = "Extracted: ${titles.joinToString(", ")} ($addedCount added)",
                                extractedTitles = titles,
                                isAddedToWatchlist = true,
                                addedMovieCount = addedCount
                            )
                        } else it
                    }
                }
            }
        } catch (e: Exception) {
            updateItemFailure(itemId, "Error: ${e.message}", null)
        }
    }

    private fun updateItemStatus(itemId: String, status: ScanStatus, message: String) {
        _scanQueue.value = _scanQueue.value.map {
            if (it.id == itemId) it.copy(status = status, statusMessage = message) else it
        }
    }

    private fun updateItemFailure(itemId: String, errorMsg: String, thumb: Bitmap?) {
        val context = getApplication<Application>()
        _scanQueue.value = _scanQueue.value.map {
            if (it.id == itemId) {
                val candidateText = it.manualInputText.ifBlank {
                    com.example.util.TextDetectorUtil.detectTextFromUri(context, it.uri)
                }
                it.copy(
                    status = ScanStatus.FAILED,
                    statusMessage = errorMsg,
                    thumbnailBitmap = thumb ?: it.thumbnailBitmap,
                    manualInputText = candidateText
                )
            } else it
        }
    }
}
