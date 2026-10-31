package com.salmanlaghari.risingflix

import com.salmanlaghari.risingflix.data.CatalogCache
import com.salmanlaghari.risingflix.data.CatalogResolver
import com.salmanlaghari.risingflix.data.Category
import com.salmanlaghari.risingflix.data.ContentResponse
import com.salmanlaghari.risingflix.data.LicensedMediaPolicy
import com.salmanlaghari.risingflix.data.MovieBoxLinkResolver
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.data.PlaybackUrlResolver
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
    fun onlyExactLicensedFeatureIsRecognizedAsPlayable() {
        assertTrue(PlaybackUrlResolver.isDirectStream(LicensedMediaPolicy.VALKAAMA_MEDIA_URL))
        assertFalse(PlaybackUrlResolver.isDirectStream("https://cdn.example.org/claimed-feature.mp4"))
        assertFalse(PlaybackUrlResolver.isDirectStream("https://macdn.aoneroom.com/clip.mp4"))
        assertFalse(PlaybackUrlResolver.isDirectStream("https://themoviebox.xyz/detail/title-123"))
        assertTrue(PlaybackUrlResolver.isEmbeddedPage("https://themoviebox.xyz/detail/title-123"))
        assertFalse(PlaybackUrlResolver.isEmbeddedPage("https://cdn.example.org/movie.mp4"))
    }

    @Test
    fun onlyExactLicenseAndMeasuredRuntimePassTheFeaturePolicy() {
        val approved = licensedFilm()
        assertTrue(CatalogResolver.isPlayableFeature(approved))
        assertTrue(LicensedMediaPolicy.isVerifiedFeature(approved))
        assertTrue(CatalogResolver.filterFeatureItems(listOf(approved, movie("other", "https://cdn.example.org/movie.mp4"))).single() == approved)

        assertFalse(LicensedMediaPolicy.isVerifiedFeature(approved.copy(actualDurationSeconds = 138)))
        assertFalse(LicensedMediaPolicy.isVerifiedFeature(approved.copy(licenseName = "Unknown")))
        assertFalse(LicensedMediaPolicy.isVerifiedFeature(approved.copy(sourcePageUrl = "https://example.org/")))
        assertFalse(LicensedMediaPolicy.isVerifiedFeature(approved.copy(videoUrl = "https://cdn.example.org/full.mp4")))
    }

    @Test
    fun oldCachedMovieBoxDirectStreamMigratesToExistingOriginalPage() {
        val legacy = movie("mov_black-panther", "https://macdn.aoneroom.com/old-extracted.mp4")
            .copy(title = "Black Panther", duration = "2 h 14 m", actualDurationSeconds = 138)
        val page = movie("page-black-panther", "https://themoviebox.xyz/detail/black-panther-abc")
            .copy(title = "Black Panther", year = "2018", sourcePageUrl = "https://themoviebox.xyz/detail/black-panther-abc", sourceName = "MovieBox", contentType = "details_only")
        val result = CatalogResolver.normalizeCatalog(
            sources = listOf(ContentResponse(null, listOf(Category("movies", "Movies", "movie", listOf(legacy))))),
            movieBoxFallback = ContentResponse(null, listOf(Category("movies", "Movies", "movie", listOf(page))))
        )

        val migrated = result.categories.flatMap(Category::items).single()
        assertEquals("https://themoviebox.xyz/detail/black-panther-abc", migrated.videoUrl)
        assertEquals(migrated.videoUrl, migrated.sourcePageUrl)
        assertEquals("details_only", migrated.contentType)
        assertEquals("Runtime unverified", migrated.safeDuration)
        assertTrue(migrated.isMovieBoxPage)
        assertFalse(PlaybackUrlResolver.isDirectStream(migrated.videoUrl))
    }

    @Test
    fun unlicensedDirectRecordsAreNotIncludedInThePlayableCatalog() {
        val unlicensed = movie("legacy", "https://cdn.example.org/feature.mp4")
            .copy(duration = "2 h 14 m", actualDurationSeconds = 8_040)
        assertTrue(CatalogResolver.filterEligibleItems(listOf(unlicensed)).isEmpty())
        assertTrue(CatalogResolver.filterFeatureItems(listOf(unlicensed)).isEmpty())
    }

    @Test
    fun movieBoxResolverOnlyAcceptsARecognizedDetailPath() {
        assertEquals(
            "https://themoviebox.xyz/detail/example-123",
            MovieBoxLinkResolver.resolvePage("https://moviebox.pk/moviedetail/example-123")
        )
        assertNull(MovieBoxLinkResolver.resolvePage("https://cdn.example.org/video.mp4"))
        assertNull(MovieBoxLinkResolver.resolvePage("https://evil.example/detail/example-123"))
    }

    @Test
    fun catalogKeepsPageRowsInEachOriginalCategory() {
        val page = "https://themoviebox.xyz/detail/shared-title"
        val row = movie("moviebox-shared", page).copy(
            title = "Shared title",
            sourcePageUrl = page,
            sourceName = "MovieBox",
            contentType = "details_only"
        )
        val result = CatalogResolver.normalizeCatalog(
            listOf(ContentResponse(null, listOf(
                Category("action", "Action & Thriller", "movie", listOf(row)),
                Category("drama", "Drama", "movie", listOf(row.copy(id = "moviebox-shared-drama")))
            )))
        )

        val categories = result.categories.associateBy(Category::name)
        assertEquals(1, categories.getValue("MovieBox · Action & Thriller").items.size)
        assertEquals(1, categories.getValue("MovieBox · Drama").items.size)
        assertEquals("Action & Thriller", categories.getValue("MovieBox · Action & Thriller").items.single().category)
        assertEquals("Drama", categories.getValue("MovieBox · Drama").items.single().category)
    }

    @Test
    fun bundledFallbackUsesOnlyStoredDetailPageFields() {
        assertEquals(
            "https://themoviebox.xyz/detail/example-title",
            com.salmanlaghari.risingflix.data.TheMovieBoxContent.resolveFallbackVideoUrl(
                mapOf("detailUrl" to "https://themoviebox.xyz/detail/example-title", "videoUrl" to "https://cdn.example.org/clip.mp4")
            )
        )
        assertEquals(
            "https://themoviebox.xyz/detail/example-title",
            com.salmanlaghari.risingflix.data.TheMovieBoxContent.resolveFallbackVideoUrl(mapOf("detailPath" to "example-title"))
        )
        assertEquals(
            "",
            com.salmanlaghari.risingflix.data.TheMovieBoxContent.resolveFallbackVideoUrl(mapOf("videoUrl" to "https://cdn.example.org/clip.mp4"))
        )
    }

    @Test
    fun catalogCacheRoundTripsAppContentWithoutLosingMetadata() {
        val directory = Files.createTempDirectory("risingflix-catalog").toFile()
        try {
            val cache = CatalogCache(File(directory, "content_catalog_cache.json"))
            val expected = ContentResponse(
                featured = licensedFilm(),
                categories = listOf(
                    Category("open_licensed_features", "Open-Licensed Features", "movie", listOf(licensedFilm()))
                )
            )
            assertTrue(cache.save(expected))
            val restored = cache.load()
            assertNotNull(restored)
            assertEquals(LicensedMediaPolicy.VALKAAMA_ID, restored!!.featured?.id)
            assertEquals(LicensedMediaPolicy.VALKAAMA_MEDIA_URL, restored.featured?.videoUrl)
            assertEquals(LicensedMediaPolicy.VALKAAMA_LICENSE_URL, restored.featured?.licenseUrl)
            assertEquals(LicensedMediaPolicy.VALKAAMA_DURATION_SECONDS, restored.featured?.actualDurationSeconds ?: -1)
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

    private fun licensedFilm() = MovieItem(
        id = LicensedMediaPolicy.VALKAAMA_ID,
        title = "Valkaama",
        poster = "android.resource://com.salmanlaghari.risingflix/drawable/valkaama_poster",
        backdrop = "android.resource://com.salmanlaghari.risingflix/drawable/valkaama_poster",
        description = "Creator-authorized feature film.",
        rating = "—",
        duration = "1 h 33 min 6 sec",
        actualDurationSeconds = LicensedMediaPolicy.VALKAAMA_DURATION_SECONDS,
        contentType = "feature",
        videoUrl = LicensedMediaPolicy.VALKAAMA_MEDIA_URL,
        category = "Open-Licensed Features",
        year = "",
        quality = "720p",
        sourcePageUrl = LicensedMediaPolicy.VALKAAMA_SOURCE_URL,
        sourceName = LicensedMediaPolicy.VALKAAMA_SOURCE_NAME,
        licenseName = LicensedMediaPolicy.VALKAAMA_LICENSE_NAME,
        licenseUrl = LicensedMediaPolicy.VALKAAMA_LICENSE_URL,
        attribution = LicensedMediaPolicy.VALKAAMA_ATTRIBUTION
    )

    private fun movie(id: String, url: String) = MovieItem(
        id = id,
        title = id,
        poster = "https://example.org/$id.jpg",
        videoUrl = url
    )
}
