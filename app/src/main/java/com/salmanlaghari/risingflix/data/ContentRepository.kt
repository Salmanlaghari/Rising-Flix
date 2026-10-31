package com.salmanlaghari.risingflix.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    // Isolated app-private catalog snapshot; preferences/downloads and other user data are untouched.
    private val catalogCache = CatalogCache(File(context.filesDir, "content_catalog_cache.json"))
    private var cachedContentList: ContentResponse? = null
    private var cachedTrendingMovies: List<MovieItem>? = null
    private var cachedPopularDramas: List<MovieItem>? = null
    private val cachedVideoDetails = mutableMapOf<String, VideoDetails>()
    private var lastRefreshSucceeded = false

    suspend fun getContentList(forceRefresh: Boolean = false): ContentResponse = withContext(Dispatchers.IO) {
        val movieBoxLinks = loadMovieBoxLinks()
        val cached = cachedContentList ?: catalogCache.load()
        val bundledLicensed = TheMovieBoxContent.loadLicensedCatalog(context)
        val bundledLinks = TheMovieBoxContent.loadMovieBoxLinkCatalog(context)

        if (!forceRefresh) {
            val local = normalize(listOfNotNull(cached, bundledLicensed, bundledLinks), movieBoxLinks)
            cachedContentList = local
            if (local.hasItems()) {
                // This also rewrites an old direct-stream cache as safe page links on first launch.
                catalogCache.save(local)
                return@withContext local
            }
        }

        val remote = runCatching { apiService.getPullRequestLicensedCatalog() }
            .recoverCatching { apiService.getMainLicensedCatalog() }
            .getOrNull()
        val remoteContainsAuthorizedFeature = remote
            ?.let { CatalogResolver.filterFeatureItems(allItems(it)).isNotEmpty() }
            ?: false
        val combined = normalize(listOfNotNull(remote, cached, bundledLicensed, bundledLinks), movieBoxLinks)

        lastRefreshSucceeded = remoteContainsAuthorizedFeature
        cachedContentList = combined
        if (combined.hasItems()) catalogCache.save(combined)
        cachedTrendingMovies = null
        cachedPopularDramas = null
        cachedVideoDetails.clear()
        combined
    }

    /** Loads the last good snapshot plus bundled licensed media and page-only links, without network access. */
    suspend fun getOfflineContent(): ContentResponse = withContext(Dispatchers.IO) {
        val movieBoxLinks = loadMovieBoxLinks()
        val cached = cachedContentList ?: catalogCache.load()
        val bundledLicensed = TheMovieBoxContent.loadLicensedCatalog(context)
        val bundledLinks = TheMovieBoxContent.loadMovieBoxLinkCatalog(context)
        val local = normalize(listOfNotNull(cached, bundledLicensed, bundledLinks), movieBoxLinks)
        cachedContentList = local
        if (local.hasItems()) catalogCache.save(local)
        local
    }

    /** Called by the unique periodic WorkManager job; false means retain cache and retry later. */
    suspend fun syncContent(): Boolean = withContext(Dispatchers.IO) {
        getContentList(forceRefresh = true)
        lastRefreshSucceeded
    }

    suspend fun getTrendingMovies(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedTrendingMovies != null) return@withContext cachedTrendingMovies.orEmpty()
        val content = getContentList(forceRefresh)
        cachedTrendingMovies = CatalogResolver.filterFeatureItems(allItems(content)).distinctBy(MovieItem::id)
        cachedTrendingMovies.orEmpty()
    }

    suspend fun getPopularDramas(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedPopularDramas != null) return@withContext cachedPopularDramas.orEmpty()
        val content = getContentList(forceRefresh)
        cachedPopularDramas = allItems(content)
            .filter { item ->
                item.category.contains("Drama", ignoreCase = true) ||
                    item.category.contains("Fantasy", ignoreCase = true) ||
                    item.category.contains("TV", ignoreCase = true)
            }
            .mapNotNull(CatalogResolver::toMovieBoxPageOnly)
            .distinctBy { it.sourcePageUrl ?: it.videoUrl }
        cachedPopularDramas.orEmpty()
    }

    suspend fun searchMovies(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val items = allItems(getContentList())
        items.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                item.safeDescription.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
        }.distinctBy { item -> item.sourcePageUrl ?: item.id }
    }

    suspend fun getVideoDetails(id: String, forceRefresh: Boolean = false): VideoDetails = withContext(Dispatchers.IO) {
        if (!forceRefresh) cachedVideoDetails[id]?.let { return@withContext it }
        val content = getContentList(forceRefresh)
        val matched = allItems(content).firstOrNull { it.id == id }
            ?: return@withContext fallbackVideoDetails(id)
        if (!CatalogResolver.isEligibleCatalogItem(matched)) return@withContext fallbackVideoDetails(id)

        val details = matched.toVideoDetails(
            relatedItems = allItems(content).filter { it.id != matched.id && it.category == matched.category }.take(10)
        )
        cachedVideoDetails[id] = details
        details
    }

    private suspend fun loadMovieBoxLinks(): ContentResponse? =
        TheMovieBoxContent.loadMovieBoxLinkCatalog(context)

    private fun normalize(sources: List<ContentResponse>, movieBoxLinks: ContentResponse?): ContentResponse =
        CatalogResolver.normalizeCatalog(sources, movieBoxLinks)

    private fun fallbackVideoDetails(id: String): VideoDetails {
        val matched = cachedContentList?.let { allItems(it) }.orEmpty().firstOrNull { it.id == id }
        return matched?.toVideoDetails(relatedItems = emptyList()) ?: VideoDetails(
            id = id,
            title = "Content unavailable",
            poster = "",
            backdrop = "",
            description = "No licensed stream or source-page listing is available in the offline catalog.",
            rating = "—",
            duration = "Runtime unverified",
            videoUrl = "",
            releaseYear = "—",
            genre = "",
            language = "",
            quality = "",
            relatedItems = emptyList()
        )
    }

    private fun ContentResponse.hasItems(): Boolean =
        featured != null || categories.any { category -> category.items.isNotEmpty() }

    private fun allItems(response: ContentResponse): List<MovieItem> =
        response.categories.flatMap(Category::items) + listOfNotNull(response.featured)

    private fun MovieItem.toVideoDetails(relatedItems: List<MovieItem>) = VideoDetails(
        id = id,
        title = title,
        poster = safePoster,
        backdrop = safeBackdrop,
        description = safeDescription,
        rating = safeRating,
        duration = safeDuration,
        videoUrl = videoUrl,
        releaseYear = safeReleaseYear,
        genre = category,
        language = if (id == LicensedMediaPolicy.VALKAAMA_ID) "Multiple audio tracks" else "",
        quality = safeQuality,
        actualDurationSeconds = actualDurationSeconds,
        contentType = contentType,
        relatedItems = relatedItems,
        sourcePageUrl = sourcePageUrl,
        sourceName = sourceName,
        licenseName = licenseName,
        licenseUrl = licenseUrl,
        attribution = attribution
    )
}
