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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.remote.TmdbSearchResult
import com.example.ui.theme.Amber500
import com.example.ui.theme.Red600
import com.example.ui.theme.Sky400
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc500
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc900

@Composable
fun TmdbSuggestionsDropdown(
    suggestions: List<TmdbSearchResult>,
    isLoading: Boolean,
    onSelectSuggestion: (TmdbSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty() && !isLoading) return

    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Zinc800, RoundedCornerShape(12.dp))
            .testTag("tmdb_suggestions_dropdown"),
        colors = CardDefaults.cardColors(containerColor = Zinc900),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isLoading && suggestions.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Red600
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Searching TMDb...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Zinc400
                    )
                }
            } else {
                suggestions.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSuggestion(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("suggestion_item_${item.title.lowercase().replace(" ", "_")}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Poster Thumbnail (or film icon fallback)
                        Box(
                            modifier = Modifier
                                .size(32.dp, 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Zinc800),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!item.posterUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.posterUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.title,
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Zinc500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title, Year, Badges
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (!item.releaseYear.isNullOrEmpty()) {
                                        "${item.title} (${item.releaseYear})"
                                    } else {
                                        item.title
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                // Media badge (MOVIE / TV)
                                val isTv = item.mediaType.equals("tv", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isTv) Sky400.copy(alpha = 0.15f) else Red600.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isTv) Sky400.copy(alpha = 0.4f) else Red600.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = if (isTv) "TV" else "MOVIE",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTv) Sky400 else Red600
                                    )
                                }
                            }

                            // Star rating badge & Overview snippet
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                if (item.voteAverage > 0.0) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format("%.1f", item.voteAverage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Amber500,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (!item.overview.isNullOrEmpty()) {
                                    Text(
                                        text = item.overview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Zinc500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    if (index < suggestions.size - 1) {
                        HorizontalDivider(color = Zinc800.copy(alpha = 0.6f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
