package com.example.model

import androidx.compose.ui.graphics.Color
import java.net.URLEncoder

data class WatchSource(
    val name: String,
    val category: SourceCategory,
    val url: String,
    val brandColor: Color,
    val subtitle: String
)

enum class SourceCategory {
    OFFICIAL,
    WEB_FREE
}

object StreamingSourcesProvider {
    fun getSourcesForTitle(title: String): List<WatchSource> {
        val encodedTitle = try {
            URLEncoder.encode(title.trim(), "UTF-8")
        } catch (_: Exception) {
            title.trim().replace(" ", "%20")
        }

        val arabicSearchEncoded = try {
            URLEncoder.encode("${title.trim()} مشاهدة مترجم", "UTF-8")
        } catch (_: Exception) {
            "${encodedTitle}+%D9%85%D8%B4%D8%A7%D9%87%D8%AF%D8%A9"
        }

        return listOf(
            // Official Platforms
            WatchSource(
                name = "Netflix",
                category = SourceCategory.OFFICIAL,
                url = "https://www.netflix.com/search?q=$encodedTitle",
                brandColor = Color(0xFFE50914),
                subtitle = "Official Subscription"
            ),
            WatchSource(
                name = "Shahid",
                category = SourceCategory.OFFICIAL,
                url = "https://shahid.mbc.net/ar/search?q=$encodedTitle",
                brandColor = Color(0xFF00B0F0),
                subtitle = "VIP & Free Catalog"
            ),
            WatchSource(
                name = "WATCH IT",
                category = SourceCategory.OFFICIAL,
                url = "https://www.watchit.com/#/search?q=$encodedTitle",
                brandColor = Color(0xFFFFCC00),
                subtitle = "Regional Hits & Exclusives"
            ),
            WatchSource(
                name = "OSN+",
                category = SourceCategory.OFFICIAL,
                url = "https://osnplus.com/search?q=$encodedTitle",
                brandColor = Color(0xFFE60000),
                subtitle = "HBO & Warner Catalog"
            ),
            WatchSource(
                name = "Disney+",
                category = SourceCategory.OFFICIAL,
                url = "https://www.disneyplus.com/search?q=$encodedTitle",
                brandColor = Color(0xFF113CCF),
                subtitle = "Disney, Marvel, Pixar"
            ),
            WatchSource(
                name = "Hulu",
                category = SourceCategory.OFFICIAL,
                url = "https://www.hulu.com/search?q=$encodedTitle",
                brandColor = Color(0xFF1CE783),
                subtitle = "TV Shows & FX Exclusives"
            ),
            WatchSource(
                name = "Crunchyroll",
                category = SourceCategory.OFFICIAL,
                url = "https://www.crunchyroll.com/search?q=$encodedTitle",
                brandColor = Color(0xFFF47521),
                subtitle = "Anime & Manga Streaming"
            ),
            WatchSource(
                name = "Prime Video",
                category = SourceCategory.OFFICIAL,
                url = "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=$encodedTitle",
                brandColor = Color(0xFF00A8E1),
                subtitle = "Amazon Prime Video"
            ),
            WatchSource(
                name = "Hulu",
                category = SourceCategory.OFFICIAL,
                url = "https://www.hulu.com/search?q=$encodedTitle",
                brandColor = Color(0xFF1CE783),
                subtitle = "TV Shows & FX Exclusives"
            ),
            WatchSource(
                name = "Crunchyroll",
                category = SourceCategory.OFFICIAL,
                url = "https://www.crunchyroll.com/search?q=$encodedTitle",
                brandColor = Color(0xFFF47521),
                subtitle = "Anime & Manga Streaming"
            ),

            // Web & Free Sources
            WatchSource(
                name = "Ahwak TV",
                category = SourceCategory.WEB_FREE,
                url = "https://tv.ahwak.com/search?q=$encodedTitle",
                brandColor = Color(0xFF8B5CF6),
                subtitle = "Free Arabic Streaming"
            ),
            WatchSource(
                name = "MyCima",
                category = SourceCategory.WEB_FREE,
                url = "https://mycima.wecima.show/search/$encodedTitle",
                brandColor = Color(0xFFEC4899),
                subtitle = "Online Cinema Library"
            ),
            WatchSource(
                name = "TopCinema",
                category = SourceCategory.WEB_FREE,
                url = "https://topcinema.cam/search?q=$encodedTitle",
                brandColor = Color(0xFF10B981),
                subtitle = "HD Cinema Streams"
            ),
            WatchSource(
                name = "EgyDead",
                category = SourceCategory.WEB_FREE,
                url = "https://egydead.live/?s=$encodedTitle",
                brandColor = Color(0xFFF97316),
                subtitle = "Subtitled & Dubbed Hub"
            ),
            WatchSource(
                name = "Google Search Fallback",
                category = SourceCategory.WEB_FREE,
                url = "https://www.google.com/search?q=watch+$encodedTitle+online",
                brandColor = Color(0xFF4285F4),
                subtitle = "Search for Online Streams"
            )
        )
    }
}
