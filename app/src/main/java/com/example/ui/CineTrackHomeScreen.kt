package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ManualAddDialog
import com.example.ui.components.ScanQueueDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WatchSourcesDialog
import com.example.ui.components.WatchlistCard
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber500_10
import com.example.ui.theme.Amber500_20
import com.example.ui.theme.HeaderGlassBg
import com.example.ui.theme.Red600
import com.example.ui.theme.UltraDarkBg
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc500
import com.example.ui.theme.Zinc700
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineTrackHomeScreen(
    viewModel: MainViewModel
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val movies by viewModel.filteredMovies.collectAsState()
    val allMovies by viewModel.allMovies.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val selectedMediaType by viewModel.selectedMediaType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val tmdbKey by viewModel.tmdbApiKey.collectAsState()

    val scanQueue by viewModel.scanQueue.collectAsState()
    val isProcessingQueue by viewModel.isProcessingQueue.collectAsState()

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showScanSheet by viewModel.showScanSheet.collectAsState()
    val showManualAddDialog by viewModel.showManualAddDialog.collectAsState()
    val activeWatchSourcesMovie by viewModel.activeWatchSourcesMovie.collectAsState()

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.enqueueScreenshots(uris)
        }
    }

    Scaffold(
        containerColor = UltraDarkBg,
        topBar = {
            // Sticky top header with glass background and zinc-900/zinc-800 border
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Red Clapperboard logo icon (bg-red-600)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Red600),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "CineTrack Clapperboard",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "CineTrack",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )

                            // Item counter badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Zinc800,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Zinc700)
                            ) {
                                Text(
                                    text = "${allMovies.size} titles",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Zinc400,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    actions = {
                        // Settings Gear Button
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.openSettingsDialog()
                            },
                            modifier = Modifier.testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (tmdbKey.isBlank() || geminiKey.isBlank()) Amber500 else Zinc400
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HeaderGlassBg
                    )
                )
                HorizontalDivider(color = Zinc900, thickness = 1.dp)
            }
        },
        bottomBar = {
            // Floating/Fixed Bottom Bar with Navigation and Primary Actions
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = HeaderGlassBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Zinc900)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Add Manual Action Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.openManualAddDialog()
                            }
                            .testTag("open_manual_add_button"),
                        color = Zinc900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Title",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Right: Primary Upload / Scan Screenshots Action (bg-red-600)
                    Button(
                        onClick = { viewModel.openScanSheet() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red600,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("open_scan_sheet_button")
                    ) {
                        if (scanQueue.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color.Black) {
                                        Text("${scanQueue.size}", color = Color.White)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProcessingQueue) "Scanning..." else "Scan Screenshot",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TMDb API Warning Banner (Prompt Specification)
            // Warning banner (bg-amber-500/10 border-amber-500/20) prompting the user to add key when missing
            if (tmdbKey.isBlank() || tmdbKey.contains("MY_TMDB_API_KEY")) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.openSettingsDialog()
                        }
                        .testTag("tmdb_api_banner"),
                    color = Amber500_10,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Amber500_20),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Amber500,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TMDb API Key Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Amber500
                            )
                            Text(
                                text = "Add your free API key in Settings to fetch HD movie poster artwork, release years & ratings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Zinc400,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Amber500,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "SETUP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Switcher: Pill tabs ("To Watch" / "Watched") inside zinc-900 container
            val toWatchCount = allMovies.count { !it.isWatched }
            val watchedCount = allMovies.count { it.isWatched }
            val allCount = allMovies.size

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Zinc-900 container
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Zinc900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BoltTabPill(
                            title = "To Watch",
                            count = toWatchCount,
                            isSelected = currentFilter == FilterTab.TO_WATCH,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setFilter(FilterTab.TO_WATCH) },
                            testTag = "filter_to_watch"
                        )

                        BoltTabPill(
                            title = "Watched",
                            count = watchedCount,
                            isSelected = currentFilter == FilterTab.WATCHED,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setFilter(FilterTab.WATCHED) },
                            testTag = "filter_watched"
                        )

                        BoltTabPill(
                            title = "All",
                            count = allCount,
                            isSelected = currentFilter == FilterTab.ALL,
                            modifier = Modifier.weight(0.7f),
                            onClick = { viewModel.setFilter(FilterTab.ALL) },
                            testTag = "filter_all"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Bar: Clean filter inputs for search query and media type
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_watchlist_input"),
                    placeholder = { Text("Search your titles...", color = Zinc500) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Zinc500,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.setSearchQuery("")
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Zinc400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Zinc700,
                        unfocusedBorderColor = Zinc800,
                        focusedContainerColor = Zinc900,
                        unfocusedContainerColor = Zinc900,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Media Type Filters (All, Movies, TV Shows)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaTypeChip(
                        label = "All Media",
                        isSelected = selectedMediaType == MediaTypeFilter.ALL,
                        onClick = { viewModel.setMediaTypeFilter(MediaTypeFilter.ALL) }
                    )
                    MediaTypeChip(
                        label = "Movies",
                        isSelected = selectedMediaType == MediaTypeFilter.MOVIE,
                        onClick = { viewModel.setMediaTypeFilter(MediaTypeFilter.MOVIE) }
                    )
                    MediaTypeChip(
                        label = "TV Shows",
                        isSelected = selectedMediaType == MediaTypeFilter.TV,
                        onClick = { viewModel.setMediaTypeFilter(MediaTypeFilter.TV) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Movie Cards List or Empty State
            if (movies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Zinc900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Red600,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches found" else "No titles in this list",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty())
                                "No movies or shows match \"$searchQuery\"."
                            else
                                "Scan screenshots of movie recommendations or add titles manually to view direct streaming sources.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Zinc400,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.85f),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    multiImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Red600,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("empty_state_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Screenshot", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.openManualAddDialog()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Zinc900,
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800),
                                modifier = Modifier.testTag("empty_state_add_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Title")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movies, key = { it.id }) { movie ->
                        WatchlistCard(
                            movie = movie,
                            onWatchSourcesClick = { viewModel.openWatchSources(movie) },
                            onToggleWatched = { viewModel.toggleWatched(movie) },
                            onDeleteClick = { viewModel.deleteMovie(movie) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSettingsDialog) {
        SettingsDialog(
            initialGeminiKey = geminiKey,
            initialTmdbKey = tmdbKey,
            geminiKeySource = viewModel.getGeminiKeySource(),
            tmdbKeySource = viewModel.getTmdbKeySource(),
            onVerifyGeminiKey = { viewModel.verifyGeminiKey(it) },
            onVerifyTmdbKey = { viewModel.verifyTmdbKey(it) },
            onSave = { gKey, tKey -> viewModel.saveApiKeys(gKey, tKey) },
            onDismiss = { viewModel.closeSettingsDialog() }
        )
    }

    if (showScanSheet) {
        ScanQueueDialog(
            queue = scanQueue,
            isProcessing = isProcessingQueue,
            onSelectNewScreenshots = { uris -> viewModel.enqueueScreenshots(uris) },
            onUpdateManualText = { id, text -> viewModel.updateManualInputText(id, text) },
            onProcessManualFallback = { id, title -> viewModel.processManualFallbackItem(id, title) },
            onRetryItem = { id -> viewModel.retryItem(id) },
            onRemoveItem = { id -> viewModel.removeScanItem(id) },
            onClearCompleted = { viewModel.clearCompletedScanQueue() },
            onSearchSuggestions = { query -> viewModel.searchTmdbSuggestions(query) },
            onSelectFallbackSuggestion = { itemId, suggestion ->
                viewModel.addSuggestionToFallbackItem(itemId, suggestion)
            },
            onDismiss = { viewModel.closeScanSheet() }
        )
    }

    if (showManualAddDialog) {
        ManualAddDialog(
            onAddTitle = { title, onDone ->
                viewModel.addManualTitleToWatchlist(title, onDone)
            },
            onSearchSuggestions = { query ->
                viewModel.searchTmdbSuggestions(query)
            },
            onSelectSuggestion = { suggestion ->
                viewModel.addTmdbResultToWatchlist(suggestion)
            },
            onDismiss = { viewModel.closeManualAddDialog() }
        )
    }

    activeWatchSourcesMovie?.let { movie ->
        WatchSourcesDialog(
            movie = movie,
            onFetchWatchProviders = { viewModel.getRealTimeWatchProviders(movie) },
            onDismiss = { viewModel.closeWatchSources() }
        )
    }
}

/**
 * Tab pill button with highlighted active background (bg-zinc-700) and count badge
 */
@Composable
fun BoltTabPill(
    title: String,
    count: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = if (isSelected) Zinc700 else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Zinc400
            )

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Zinc900 else Zinc800
            ) {
                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White else Zinc500,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MediaTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) Red600.copy(alpha = 0.15f) else Zinc900,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Red600 else Zinc800
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Red600 else Zinc400
        )
    }
}
