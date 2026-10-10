package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import com.example.ui.util.SafeTextToolbar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.KeySource
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCardStroke
import com.example.ui.theme.CinemaEmerald
import com.example.ui.theme.CinemaRuby
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    initialGeminiKey: String,
    initialTmdbKey: String,
    geminiKeySource: KeySource = KeySource.NOT_CONFIGURED,
    tmdbKeySource: KeySource = KeySource.NOT_CONFIGURED,
    onVerifyGeminiKey: (suspend (String) -> Result<String>)? = null,
    onVerifyTmdbKey: (suspend (String) -> Result<String>)? = null,
    onSave: (geminiKey: String, tmdbKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val safeDismiss = remember(onDismiss) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onDismiss()
        }
    }

    var geminiKey by remember { mutableStateOf(initialGeminiKey) }
    var tmdbKey by remember { mutableStateOf(initialTmdbKey) }

    var showGeminiKeyText by remember { mutableStateOf(false) }
    var showTmdbKeyText by remember { mutableStateOf(false) }

    var isVerifyingGemini by remember { mutableStateOf(false) }
    var geminiVerifyMessage by remember { mutableStateOf<String?>(null) }
    var isGeminiValid by remember { mutableStateOf<Boolean?>(null) }

    var isVerifyingTmdb by remember { mutableStateOf(false) }
    var tmdbVerifyMessage by remember { mutableStateOf<String?>(null) }
    var isTmdbValid by remember { mutableStateOf<Boolean?>(null) }

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
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CinemaCardStroke, RoundedCornerShape(24.dp))
                .testTag("settings_dialog"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title and Close
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
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = CinemaRuby,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "API Key Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CinemaCardStroke.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Information banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaCardStroke.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CinemaAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configure your API keys for automated screenshot title extraction and movie posters metadata. Keys are saved locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Gemini API Key Input Header & Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gemini API Key (Vision / OCR)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    KeySourceBadge(geminiKey, geminiKeySource)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = {
                        geminiKey = it
                        geminiVerifyMessage = null
                        isGeminiValid = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_input"),
                    placeholder = { Text("Enter Gemini API Key (e.g. AIzaSy...)", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = if (showGeminiKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showGeminiKeyText) "HIDE" else "SHOW",
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CinemaCardStroke.copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clickable { showGeminiKeyText = !showGeminiKeyText },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CinemaRuby,
                        unfocusedBorderColor = CinemaCardStroke
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Gemini Verification Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used by model gemini-3.8-flash for rapid OCR.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = geminiKey.isNotBlank() && !isVerifyingGemini) {
                                if (onVerifyGeminiKey != null) {
                                    isVerifyingGemini = true
                                    geminiVerifyMessage = null
                                    coroutineScope.launch {
                                        val result = onVerifyGeminiKey(geminiKey)
                                        isVerifyingGemini = false
                                        if (result.isSuccess) {
                                            isGeminiValid = true
                                            geminiVerifyMessage = "Key verified successfully!"
                                        } else {
                                            isGeminiValid = false
                                            geminiVerifyMessage = result.exceptionOrNull()?.message ?: "Verification failed"
                                        }
                                    }
                                }
                            }
                            .testTag("verify_gemini_key_button"),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, CinemaCardStroke)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVerifyingGemini) {
                                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = CinemaRuby)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isVerifyingGemini) "Testing..." else "Test Key",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CinemaRuby
                            )
                        }
                    }
                }

                geminiVerifyMessage?.let { msg ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGeminiValid == true) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isGeminiValid == true) CinemaEmerald else CinemaRuby,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGeminiValid == true) CinemaEmerald else CinemaRuby
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // TMDb API Key Input Header & Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TMDb API Key (Metadata & Posters)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    KeySourceBadge(tmdbKey, tmdbKeySource)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = tmdbKey,
                    onValueChange = {
                        tmdbKey = it
                        tmdbVerifyMessage = null
                        isTmdbValid = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tmdb_api_key_input"),
                    placeholder = { Text("Enter TMDb v3 API Key (32 hex characters)", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = if (showTmdbKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showTmdbKeyText) "HIDE" else "SHOW",
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CinemaCardStroke.copy(alpha = 0.3f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clickable { showTmdbKeyText = !showTmdbKeyText },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = CinemaCardStroke
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // TMDb Verification Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fetches official HD posters and ratings.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = tmdbKey.isNotBlank() && !isVerifyingTmdb) {
                                if (onVerifyTmdbKey != null) {
                                    isVerifyingTmdb = true
                                    tmdbVerifyMessage = null
                                    coroutineScope.launch {
                                        val result = onVerifyTmdbKey(tmdbKey)
                                        isVerifyingTmdb = false
                                        if (result.isSuccess) {
                                            isTmdbValid = true
                                            tmdbVerifyMessage = "Key verified successfully!"
                                        } else {
                                            isTmdbValid = false
                                            tmdbVerifyMessage = result.exceptionOrNull()?.message ?: "Verification failed"
                                        }
                                    }
                                }
                            }
                            .testTag("verify_tmdb_key_button"),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, CinemaCardStroke)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVerifyingTmdb) {
                                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isVerifyingTmdb) "Testing..." else "Test Key",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                tmdbVerifyMessage?.let { msg ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTmdbValid == true) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isTmdbValid == true) CinemaEmerald else CinemaRuby,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTmdbValid == true) CinemaEmerald else CinemaRuby
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = safeDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onSave(geminiKey.trim(), tmdbKey.trim())
                            safeDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CinemaRuby,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Keys", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

@Composable
fun KeySourceBadge(
    currentKeyVal: String,
    source: KeySource
) {
    val isSet = currentKeyVal.isNotBlank() && !currentKeyVal.contains("MY_")
    val (label, color) = when {
        !isSet -> Pair("Not Set", CinemaAmber)
        source == KeySource.ENVIRONMENT -> Pair("Loaded from .env", Color(0xFF38BDF8))
        else -> Pair("Loaded from App", CinemaEmerald)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 10.sp
        )
    }
}
