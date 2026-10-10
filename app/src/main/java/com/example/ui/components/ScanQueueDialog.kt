package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import com.example.ui.util.SafeTextToolbar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.TmdbSearchResult
import com.example.ui.ScanItem
import com.example.ui.ScanStatus
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCardBg
import com.example.ui.theme.CinemaCardStroke
import com.example.ui.theme.CinemaEmerald
import com.example.ui.theme.CinemaRuby
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun ScanQueueDialog(
    queue: List<ScanItem>,
    isProcessing: Boolean,
    onSelectNewScreenshots: (List<Uri>) -> Unit,
    onUpdateManualText: (String, String) -> Unit,
    onProcessManualFallback: (String, String) -> Unit,
    onRetryItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onSearchSuggestions: (suspend (String) -> List<TmdbSearchResult>)? = null,
    onSelectFallbackSuggestion: ((itemId: String, TmdbSearchResult) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val safeDismiss = remember(onDismiss) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    // Media picker for multiple screenshots
    val multiplePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onSelectNewScreenshots(uris)
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides SafeTextToolbar) {
        Dialog(
            onDismissRequest = safeDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CinemaCardStroke, RoundedCornerShape(24.dp))
                .testTag("scan_queue_dialog"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CinemaRuby.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = CinemaRuby,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Scan Screenshots",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (queue.isEmpty()) "Select movie screenshots to scan" else "${queue.size} item(s) in queue",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.testTag("close_scan_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CinemaCardStroke.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Pick Screenshots & Clear completed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            multiplePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_screenshots_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaRuby,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Screenshots", fontWeight = FontWeight.SemiBold)
                    }

                    if (queue.any { it.status == ScanStatus.SUCCESS }) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onClearCompleted() }
                                .testTag("clear_completed_button"),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaCardStroke),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear Done",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Queue Content
                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CinemaCardBg.copy(alpha = 0.5f))
                            .border(1.dp, CinemaCardStroke.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No screenshots in queue",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Take screenshots of movie recommendations from TikTok, Instagram, Twitter, or blogs and upload them here. CineTrack will extract titles via Gemini 3.8 Flash, fetch TMDb posters, and create your watchlist!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(queue, key = { it.id }) { item ->
                            ScanItemRow(
                                item = item,
                                onUpdateManualText = { text -> onUpdateManualText(item.id, text) },
                                onProcessManualFallback = { title -> onProcessManualFallback(item.id, title) },
                                onRetry = { onRetryItem(item.id) },
                                onRemove = { onRemoveItem(item.id) },
                                onSearchSuggestions = onSearchSuggestions,
                                onSelectFallbackSuggestion = if (onSelectFallbackSuggestion != null) {
                                    { suggestion -> onSelectFallbackSuggestion(item.id, suggestion) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun ScanItemRow(
    item: ScanItem,
    onUpdateManualText: (String) -> Unit,
    onProcessManualFallback: (String) -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onSearchSuggestions: (suspend (String) -> List<TmdbSearchResult>)? = null,
    onSelectFallbackSuggestion: ((TmdbSearchResult) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var suggestions by remember { mutableStateOf<List<TmdbSearchResult>>(emptyList()) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }

    // Live search with 300ms debounce on OCR fallback input
    LaunchedEffect(item.manualInputText) {
        val trimmed = item.manualInputText.trim()
        if (trimmed.length >= 2 && onSearchSuggestions != null && (item.status == ScanStatus.FAILED || item.status == ScanStatus.TIMED_OUT)) {
            delay(300) // 300ms debounce
            isSearchingSuggestions = true
            suggestions = try {
                onSearchSuggestions(trimmed)
            } catch (_: Exception) {
                emptyList()
            }
            isSearchingSuggestions = false
        } else {
            suggestions = emptyList()
            isSearchingSuggestions = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                when (item.status) {
                    ScanStatus.SUCCESS -> CinemaEmerald.copy(alpha = 0.4f)
                    ScanStatus.FAILED, ScanStatus.TIMED_OUT -> CinemaRuby.copy(alpha = 0.4f)
                    else -> CinemaCardStroke
                },
                RoundedCornerShape(14.dp)
            )
            .testTag("scan_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = CinemaCardBg
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.thumbnailBitmap != null) {
                        Image(
                            bitmap = item.thumbnailBitmap.asImageBitmap(),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info & Status
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Indicator Icon
                        when (item.status) {
                            ScanStatus.QUEUED -> {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "QUEUED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                            ScanStatus.COMPRESSING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = CinemaAmber
                                )
                                Text(
                                    text = "OPTIMIZING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaAmber
                                )
                            }
                            ScanStatus.OCR_ANALYZING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = CinemaRuby
                                )
                                Text(
                                    text = "GEMINI OCR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaRuby
                                )
                            }
                            ScanStatus.TMDB_SEARCHING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "TMDB SYNC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            ScanStatus.SUCCESS -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CinemaEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "SUCCESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaEmerald
                                )
                            }
                            ScanStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = CinemaRuby,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "FAILED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaRuby
                                )
                            }
                            ScanStatus.TIMED_OUT -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CinemaAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TIMED OUT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaAmber
                                )
                            }
                        }

                        if (item.byteSize > 0) {
                            Text(
                                text = "• ${(item.byteSize / 1024)} KB",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }

                // Delete item button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Extracted titles pill display
            if (item.extractedTitles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.extractedTitles.forEach { title ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CinemaEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CinemaEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = CinemaEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Non-blocking Fallback Input for Failed or Timed-out Items:
            // "If any image fails or times out, keep its thumbnail visible and provide an instant editable text input so the user can type or verify the title manually without losing progress."
            if (item.status == ScanStatus.FAILED || item.status == ScanStatus.TIMED_OUT) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CinemaCardStroke.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Manual Title Fallback:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CinemaAmber
                    )
                    Text(
                        text = "Auto-detected text",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.manualInputText,
                        onValueChange = onUpdateManualText,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_title_input_${item.id}"),
                        placeholder = { Text("Type movie title...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaAmber,
                            unfocusedBorderColor = CinemaCardStroke
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { onProcessManualFallback(item.manualInputText) },
                        enabled = item.manualInputText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaAmber,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("add_fallback_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("retry_scan_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry Scan",
                            tint = CinemaRuby,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Autocomplete dropdown for fallback input
                if (suggestions.isNotEmpty() || isSearchingSuggestions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TmdbSuggestionsDropdown(
                        suggestions = suggestions,
                        isLoading = isSearchingSuggestions,
                        onSelectSuggestion = { selectedItem ->
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            if (onSelectFallbackSuggestion != null) {
                                onSelectFallbackSuggestion(selectedItem)
                            } else {
                                onUpdateManualText(selectedItem.title)
                                onProcessManualFallback(selectedItem.title)
                            }
                            suggestions = emptyList()
                        }
                    )
                }
            }
        }
    }
}
