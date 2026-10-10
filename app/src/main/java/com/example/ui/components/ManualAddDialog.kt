package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import com.example.ui.util.SafeTextToolbar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.TmdbSearchResult
import com.example.ui.theme.Red600
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc500
import com.example.ui.theme.Zinc700
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc900
import kotlinx.coroutines.delay

@Composable
fun ManualAddDialog(
    onAddTitle: (title: String, onDone: () -> Unit) -> Unit,
    onSearchSuggestions: (suspend (String) -> List<TmdbSearchResult>)? = null,
    onSelectSuggestion: ((TmdbSearchResult) -> Unit)? = null,
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

    var title by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var suggestions by remember { mutableStateOf<List<TmdbSearchResult>>(emptyList()) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }

    // Live search with 300ms debounce
    LaunchedEffect(title) {
        val trimmed = title.trim()
        if (trimmed.length >= 2 && onSearchSuggestions != null) {
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

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides SafeTextToolbar) {
        Dialog(
            onDismissRequest = safeDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Zinc800, RoundedCornerShape(20.dp))
                    .testTag("manual_add_dialog"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            color = Red600.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Red600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add Movie or TV Show",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.testTag("close_manual_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Zinc400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Zinc800, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Title or Search Query",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_add_title_input"),
                    placeholder = { Text("Start typing e.g. Breaking Bad, Dune, Arcane...", color = Zinc500) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Zinc500,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSearchingSuggestions) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Red600
                            )
                        } else if (title.isNotEmpty()) {
                            IconButton(onClick = {
                                title = ""
                                suggestions = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Zinc400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Red600,
                        unfocusedBorderColor = Zinc800,
                        focusedContainerColor = Zinc900,
                        unfocusedContainerColor = Zinc900,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Live Autocomplete Suggestions Dropdown
                if (suggestions.isNotEmpty() || isSearchingSuggestions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TmdbSuggestionsDropdown(
                        suggestions = suggestions,
                        isLoading = isSearchingSuggestions,
                        onSelectSuggestion = { selectedItem ->
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            if (onSelectSuggestion != null) {
                                onSelectSuggestion(selectedItem)
                                safeDismiss()
                            } else {
                                title = selectedItem.title
                                suggestions = emptyList()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = safeDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Zinc900,
                            contentColor = Zinc400
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                isLoading = true
                                onAddTitle(title.trim()) {
                                    isLoading = false
                                    safeDismiss()
                                }
                            }
                        },
                        enabled = title.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_manual_add_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red600,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
}
