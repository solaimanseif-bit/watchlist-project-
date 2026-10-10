package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.local.MovieEntity
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCardBg
import com.example.ui.theme.CinemaCardStroke
import com.example.ui.theme.CinemaEmerald
import com.example.ui.theme.CinemaRuby
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun WatchlistCard(
    movie: MovieEntity,
    onWatchSourcesClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (movie.isWatched) CinemaEmerald.copy(alpha = 0.35f) else CinemaCardStroke,
                RoundedCornerShape(16.dp)
            )
            .testTag("movie_card_${movie.id}"),
        colors = CardDefaults.cardColors(
            containerColor = CinemaCardBg
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Poster Image
                Box(
                    modifier = Modifier
                        .size(90.dp, 135.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, CinemaCardStroke.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!movie.posterUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(movie.posterUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = movie.title,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NO POSTER",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 8.sp
                            )
                        }
                    }

                    // Watched badge on poster
                    if (movie.isWatched) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            shape = CircleShape,
                            color = CinemaEmerald
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Watched",
                                tint = Color.Black,
                                modifier = Modifier
                                    .padding(3.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                }

                // Details Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 135.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Title
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!movie.originalTitle.isNullOrEmpty() && movie.originalTitle != movie.title) {
                            Text(
                                text = movie.originalTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Badges Row (Media type, Year, Rating)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Media Type badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CinemaRuby.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CinemaRuby.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = movie.mediaType.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CinemaRuby,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Release Year
                            if (!movie.releaseYear.isNullOrEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, CinemaCardStroke)
                                ) {
                                    Text(
                                        text = movie.releaseYear,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Rating Badge
                            if (movie.voteAverage > 0.0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CinemaAmber.copy(alpha = 0.15f))
                                        .border(0.5.dp, CinemaAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CinemaAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format("%.1f", movie.voteAverage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CinemaAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Overview snippet
                        if (!movie.overview.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = movie.overview,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = if (isExpanded) 10 else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { isExpanded = !isExpanded },
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: "Watch Sources", "Mark as Watched", "Delete"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action: "Watch Sources"
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onWatchSourcesClick() }
                        .testTag("watch_sources_button_${movie.id}"),
                    color = CinemaRuby,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Sources",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Toggle Watched Button
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleWatched() }
                        .testTag("toggle_watched_button_${movie.id}"),
                    color = if (movie.isWatched) CinemaEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (movie.isWatched) CinemaEmerald else CinemaCardStroke
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (movie.isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            tint = if (movie.isWatched) CinemaEmerald else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (movie.isWatched) "Watched" else "To Watch",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (movie.isWatched) CinemaEmerald else TextSecondary
                        )
                    }
                }

                // Delete Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDeleteClick() }
                        .testTag("delete_movie_button_${movie.id}"),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaCardStroke),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete from watchlist",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
