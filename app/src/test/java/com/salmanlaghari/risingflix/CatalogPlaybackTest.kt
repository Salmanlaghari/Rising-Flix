package com.salmanlaghari.risingflix

import com.salmanlaghari.risingflix.data.CatalogCache
import com.salmanlaghari.risingflix.data.CatalogResolver
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
    fun knownDetailPageIsNotMediaAndAdaptiveStreamsAreRecognized() {
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
    fun bundledMovieBoxDetailLinksRemainProviderPagesWithoutDirectStreams() {
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
    fun measuredShortStreamsAreLabeledAndSeparatedFromFeatureMovies() {
        val feature = movie("feature", "https://cdn.example.org/feature.mp4")
            .copy(duration = "2 h 14 m", actualDurationSeconds = 8_040)
        val metadataOnlyShort = movie("metadata-short", "https://cdn.example.org/metadata-short.mp4")
            .copy(title = "Metadata Short", duration = "2 h 14 m", actualDurationSeconds = 138)
        val unverified = movie("unverified", "https://cdn.example.org/unverified.mp4")
            .copy(duration = "2 h 14 m", actualDurationSeconds = null)
        val promo = movie("auto_promo", "https://cdn.example.org/promo.mp4")
            .copy(actualDurationSeconds = 15)
        val detailPage = movie("page", "https://themoviebox.xyz/detail/page")
            .copy(duration = "2 h 14 m", actualDurationSeconds = null)
        val result = CatalogResolver.removeDemoAndShortForm(
            ContentResponse(
                featured = metadataOnlyShort,
                categories = listOf(
                    Category("movies", "Movies", "movie", listOf(metadataOnlyShort, unverified, promo, feature, detailPage))
                )
            )
        )

        val movies = result.categories.first { it.name == "Movies" }.items
        val clips = result.categories.first { it.name == "Short Clips" }.items
        val providerListings = result.categories.first { it.name == "Provider Listings" }.items
        assertEquals(listOf("feature"), movies.map(MovieItem::id))
        assertEquals(listOf("metadata-short"), clips.map(MovieItem::id))
        assertEquals(listOf("page"), providerListings.map(MovieItem::id))
        assertEquals("feature", result.featured?.id)
        assertEquals("2 h 14 min", feature.safeDuration)
        assertEquals("2 min 18 sec", metadataOnlyShort.safeDuration)
        assertEquals("Metadata Short (short clip)", clips.single().displayTitle)
        assertEquals("short_clip", clips.single().contentType)
        assertEquals("page (details only)", providerListings.single().displayTitle)
        assertEquals("Runtime unverified", detailPage.safeDuration)
        assertFalse(CatalogResolver.isVerifiedFeatureStream(metadataOnlyShort.videoUrl, metadataOnlyShort.actualDurationSeconds))
        assertFalse(CatalogResolver.isVerifiedFeatureStream(unverified.videoUrl, unverified.actualDurationSeconds))
        assertTrue(CatalogResolver.isVerifiedDirectStream(metadataOnlyShort.videoUrl, metadataOnlyShort.actualDurationSeconds))
        assertFalse(CatalogResolver.isVerifiedDirectStream(unverified.videoUrl, unverified.actualDurationSeconds))
        assertTrue(CatalogResolver.isVerifiedFeatureStream(feature.videoUrl, feature.actualDurationSeconds))
    }

    @Test
    fun detailPageUsesOnlyAnExistingMatchingDirectStream() {
        val page = movie("tmb-1", "https://themoviebox.xyz/detail/example")
            .copy(title = "Example Film", year = "2020", duration = null)
        val direct = movie("catalog-1", "https://cdn.example.org/example-film.mp4")
            .copy(title = "Example Film", year = "2020", duration = "1 h 40 m", actualDurationSeconds = 6_000)
        val listing = ContentResponse(
            featured = page,
            categories = listOf(Category("movies", "Movies", "movie", listOf(page)))
        )
        val directCatalog = ContentResponse(
            featured = direct,
            categories = listOf(Category("movies", "Movies", "movie", listOf(direct)))
        )

        val enriched = CatalogResolver.enrichDetailPagesWithDirectStreams(listing, listOf(directCatalog))
        val result = enriched.categories.single().items.single()

        assertEquals("https://cdn.example.org/example-film.mp4", result.videoUrl)
        assertEquals("1 h 40 min", result.duration)
        assertEquals(6_000, result.actualDurationSeconds ?: -1)
    }

    @Test
    fun measuredShortClipCannotBeMatchedToAFullMovieProviderPage() {
        val page = movie("tmb-unverified", "https://themoviebox.xyz/detail/example")
            .copy(title = "Example Film", year = "2020")
        val shortClip = movie("catalog-short", "https://cdn.example.org/example-film.mp4")
            .copy(title = "Example Film", year = "2020", actualDurationSeconds = 138)
        val listing = ContentResponse(null, listOf(Category("movies", "Movies", "movie", listOf(page))))
        val catalog = ContentResponse(null, listOf(Category("movies", "Movies", "movie", listOf(shortClip))))

        val result = CatalogResolver.enrichDetailPagesWithDirectStreams(listing, listOf(catalog))

        assertEquals(page.videoUrl, result.categories.single().items.single().videoUrl)
    }

    @Test
    fun pageWithoutDirectStreamRemainsUnplayable() {
        val page = movie("tmb-2", "https://themoviebox.xyz/detail/unlisted")
        val listing = ContentResponse(null, listOf(Category("movies", "Movies", "movie", listOf(page))))

        val result = CatalogResolver.enrichDetailPagesWithDirectStreams(listing, emptyList())

        assertEquals(page.videoUrl, result.categories.single().items.single().videoUrl)
        assertFalse(PlaybackUrlResolver.isDirectStream(result.categories.single().items.single().videoUrl))
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
        videoUrl = url,
        actualDurationSeconds = if (PlaybackUrlResolver.isEmbeddedPage(url)) null else 7_200
    )
}
