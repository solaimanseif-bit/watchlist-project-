package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.SettingsManager
import com.example.model.SourceCategory
import com.example.model.StreamingSourcesProvider
import com.example.util.TextDetectorUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches CineTrack`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CineTrack", appName)
    }

    @Test
    fun `streaming sources provider returns official and web platforms`() {
        val sources = StreamingSourcesProvider.getSourcesForTitle("Inception")
        assertTrue(sources.isNotEmpty())

        val official = sources.filter { it.category == SourceCategory.OFFICIAL }
        val webFree = sources.filter { it.category == SourceCategory.WEB_FREE }

        // Official platform checks
        assertTrue(official.any { it.name == "Netflix" && it.url.contains("netflix.com") })
        assertTrue(official.any { it.name == "Shahid" && it.url.contains("shahid.mbc.net") })
        assertTrue(official.any { it.name == "WATCH IT" && it.url.contains("watchit.com") })
        assertTrue(official.any { it.name == "Disney+" && it.url.contains("disneyplus.com") })
        assertTrue(official.any { it.name == "Prime Video" && it.url.contains("primevideo.com") })
        assertTrue(official.any { it.name == "Hulu" && it.url.contains("hulu.com") })
        assertTrue(official.any { it.name == "Crunchyroll" && it.url.contains("crunchyroll.com") })

        // Web & Free sources checks
        assertTrue(webFree.any { it.name == "Ahwak TV" && it.url.contains("ahwak.com") })
        assertTrue(webFree.any { it.name == "MyCima" && it.url.contains("mycima.wecima.show") })
        assertTrue(webFree.any { it.name == "TopCinema" && it.url.contains("topcinema.cam") })
        assertTrue(webFree.any { it.name == "EgyDead" && it.url.contains("egydead.live") })
        assertTrue(webFree.any { it.name == "Google Search Fallback" && it.url.contains("google.com") })
    }

    @Test
    fun `settings manager stores and retrieves keys`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SettingsManager(context)

        manager.setGeminiApiKey("test_gemini_key_123")
        manager.setTmdbApiKey("test_tmdb_key_456")

        assertEquals("test_gemini_key_123", manager.getGeminiApiKey())
        assertEquals("test_tmdb_key_456", manager.getTmdbApiKey())
    }

    @Test
    fun `text detector cleans screenshot filenames into title candidates`() {
        assertEquals("Severance", TextDetectorUtil.cleanFileNameToTitle("Screenshot_20261010_Severance.png"))
        assertEquals("Dune Part Two", TextDetectorUtil.cleanFileNameToTitle("Dune_Part_Two_rec.jpg"))
        assertEquals("Inception", TextDetectorUtil.cleanFileNameToTitle("IMG_Inception_hd.webp"))
    }
}
