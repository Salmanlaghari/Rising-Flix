package com.salmanlaghari.risingflix

import com.salmanlaghari.risingflix.data.CatalogCache
import com.salmanlaghari.risingflix.data.Category
import com.salmanlaghari.risingflix.data.ContentResponse
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.data.PlaybackUrlResolver
import com.salmanlaghari.risingflix.data.TheMovieBoxContent
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPlaybackTest {

    @Test
    fun knownDetailPageUsesWebViewAndAdaptiveStreamsUseNativePlayback() {
        assertTrue(PlaybackUrlResolver.isEmbeddedPage("https://themoviebox.xyz/detail/title-123"))
        assertTrue(PlaybackUrlResolver.isEmbeddedPage("https://sub.hdmoviescloud.com/watch/123"))
        assertFalse(PlaybackUrlResolver.isEmbeddedPage("https://cdn.example.org/video.m3u8?token=abc"))

        assertTrue(PlaybackUrlResolver.isDirectStream("https://cdn.example.org/live/master.m3u8?token=abc"))
        assertTrue(PlaybackUrlResolver.isDirectStream("https://cdn.example.org/movie.mpd"))
        assertFalse(PlaybackUrlResolver.isDirectStream("https://themoviebox.xyz/detail/title-123"))
    }

    @Test
    fun invalidOrNonHttpUrlsAreRejected() {
        assertFalse(PlaybackUrlResolver.isValidHttpUrl(""))
        assertFalse(PlaybackUrlResolver.isValidHttpUrl("file:///storage/movie.mp4"))
        assertTrue(PlaybackUrlResolver.isValidHttpUrl("https://cdn.example.org/video.mp4"))
    }

    @Test
    fun bundledMovieBoxDetailLinksBecomePlayablePageUrls() {
        assertEquals(
            "https://themoviebox.xyz/detail/example-title",
            TheMovieBoxContent.resolveFallbackVideoUrl(
                mapOf("detailUrl" to "https://themoviebox.xyz/detail/example-title")
            )
        )
        assertEquals(
            "https://themoviebox.xyz/detail/example-title",
            TheMovieBoxContent.resolveFallbackVideoUrl(mapOf("detailPath" to "example-title"))
        )
    }

    @Test
    fun catalogCacheRoundTripsCatalogWithoutChangingSavedEntries() {
        val directory = Files.createTempDirectory("risingflix-catalog").toFile()
        try {
            val file = File(directory, "content_catalog_cache.json")
            val cache = CatalogCache(file)
            val expected = ContentResponse(
                featured = movie("feature", "https://cdn.example.org/feature.mp4"),
                categories = listOf(
                    Category(
                        id = "movies",
                        name = "Movies",
                        icon = "movie",
                        items = listOf(movie("saved-id", "https://cdn.example.org/saved.mp4"))
                    )
                )
            )

            assertTrue(cache.save(expected))
            val restored = cache.load()

            assertNotNull(restored)
            assertEquals("saved-id", restored!!.categories.single().items.single().id)
            assertEquals("https://cdn.example.org/saved.mp4", restored.categories.single().items.single().videoUrl)
            assertEquals("feature", restored.featured?.id)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun missingOrMalformedCatalogCacheReturnsNull() {
        val directory = Files.createTempDirectory("risingflix-empty").toFile()
        try {
            val cacheFile = File(directory, "content_catalog_cache.json")
            val cache = CatalogCache(cacheFile)
            assertNull(cache.load())

            cacheFile.writeText("not json")
            assertNull(cache.load())
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun movie(id: String, url: String) = MovieItem(
        id = id,
        title = id,
        poster = "https://example.org/$id.jpg",
        videoUrl = url
    )
}
