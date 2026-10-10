package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.MovieEntity
import com.example.model.SourceCategory
import com.example.model.StreamingSourcesProvider
import com.example.model.WatchSource
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCardStroke
import com.example.ui.theme.CinemaRuby
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.remote.TmdbWatchProvider
import com.example.data.remote.TmdbWatchProvidersResult

@Composable
fun WatchSourcesDialog(
    movie: MovieEntity,
    onFetchWatchProviders: (suspend () -> TmdbWatchProvidersResult)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allSources = remember(movie.title) {
        StreamingSourcesProvider.getSourcesForTitle(movie.title)
    }

    val officialSources = remember(allSources) {
        allSources.filter { it.category == SourceCategory.OFFICIAL }
    }

    val webFreeSources = remember(allSources) {
        allSources.filter { it.category == SourceCategory.WEB_FREE }
    }

    var isLoadingProviders by remember { mutableStateOf(onFetchWatchProviders != null) }
    var realTimeResult by remember { mutableStateOf<TmdbWatchProvidersResult?>(null) }

    LaunchedEffect(movie.id) {
        if (onFetchWatchProviders != null) {
            isLoadingProviders = true
            try {
                realTimeResult = onFetchWatchProviders()
            } catch (_: Exception) {
                realTimeResult = null
            } finally {
                isLoadingProviders = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CinemaCardStroke, RoundedCornerShape(24.dp))
                .testTag("watch_sources_dialog"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Movie Details and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Poster thumbnail
                    if (!movie.posterUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(movie.posterUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = movie.title,
                            modifier = Modifier
                                .size(50.dp, 75.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!movie.releaseYear.isNullOrEmpty()) {
                                Text(
                                    text = movie.releaseYear,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (movie.voteAverage > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = CinemaAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format("%.1f", movie.voteAverage),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CinemaAmber
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CinemaRuby.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = movie.mediaType.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CinemaRuby,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sources_button")
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

                // Scrollable Sources List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. TOP PRIORITY: Streaming Now On (Real-Time) powered by TMDb Watch Providers (JustWatch)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STREAMING NOW ON (REAL-TIME)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    letterSpacing = 1.2.sp
                                )
                            }

                            realTimeResult?.let { res ->
                                if (res.providers.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = res.region,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (isLoadingProviders) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Checking real-time streaming availability...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            val providers = realTimeResult?.providers.orEmpty()
                            if (providers.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    providers.forEach { provider ->
                                        RealTimeProviderRow(
                                            provider = provider,
                                            context = context,
                                            fallbackLink = realTimeResult?.tmdbLink
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, CinemaCardStroke.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "No direct subscription or free stream detected in EG/US. Check the official & alternative platforms below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Official Platforms
                    Text(
                        text = "OFFICIAL PLATFORMS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CinemaRuby,
                        letterSpacing = 1.2.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        officialSources.forEach { source ->
                            SourceItemRow(source = source, context = context)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Alternative & Web Sources
                    Text(
                        text = "ALTERNATIVE & WEB SOURCES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.2.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        webFreeSources.forEach { source ->
                            SourceItemRow(source = source, context = context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealTimeProviderRow(
    provider: TmdbWatchProvider,
    context: Context,
    fallbackLink: String?
) {
    val destinationUrl = provider.watchLink ?: fallbackLink ?: "https://www.themoviedb.org"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10B981).copy(alpha = 0.1f))
            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable {
                launchUrl(context, destinationUrl)
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("realtime_provider_${provider.name.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Provider Logo
            if (!provider.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(provider.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = provider.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (provider.providerType == "free") "Stream for Free" else "Included with Subscription",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF10B981).copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Watch",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Watch now",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SourceItemRow(
    source: WatchSource,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, CinemaCardStroke.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable {
                launchUrl(context, source.url)
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("source_${source.name.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Brand Icon Circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(source.brandColor.copy(alpha = 0.15f))
                    .border(1.dp, source.brandColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = source.brandColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = source.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open in browser",
            tint = source.brandColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun launchUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser for source", Toast.LENGTH_SHORT).show()
    }
}
