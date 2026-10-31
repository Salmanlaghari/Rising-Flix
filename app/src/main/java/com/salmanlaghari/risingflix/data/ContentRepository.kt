package com.salmanlaghari.risingflix.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext

interface ContentSource {
    val name: String
    suspend fun fetchContent(): ContentResponse?
}

class ContentRepository(private val apiService: ApiService, private val context: android.content.Context) {

    // This cache is isolated from preferences, downloads, and other user data.
    private val catalogCache = CatalogCache(File(context.filesDir, "content_catalog_cache.json"))
    private var cachedContentList: ContentResponse? = null
    private var cachedTrendingMovies: List<MovieItem>? = null
    private var cachedPopularDramas: List<MovieItem>? = null
    private val cachedVideoDetails = mutableMapOf<String, VideoDetails>()
    private var lastRefreshSucceeded = false

    // --- MAIN CONTENT LOADER (FULLY AUTOMATIC) ---

    suspend fun getContentList(forceRefresh: Boolean = false): ContentResponse = withContext(Dispatchers.IO) {
        val lastKnownCatalog = cachedContentList
            ?: catalogCache.load()?.let(CatalogResolver::removeDemoAndShortForm)
        if (!forceRefresh && lastKnownCatalog != null) {
            val bundled = TheMovieBoxContent.loadVerifiedCatalog(context)?.let(CatalogResolver::removeDemoAndShortForm)
            val localCatalog = if (bundled == null) lastKnownCatalog else
                CatalogResolver.removeDemoAndShortForm(mergeContent(lastKnownCatalog, bundled))
            cachedContentList = localCatalog
            return@withContext localCatalog
        }
        val bundledVerifiedCatalog = TheMovieBoxContent.loadVerifiedCatalog(context)
            ?.let(CatalogResolver::removeDemoAndShortForm)

        // Retrieve the maintained playable catalog immediately; the live title listing may
        // take a few seconds and is fetched concurrently rather than after a slow archive scan.
        val (githubContent, liveTheMovieBox) = coroutineScope {
            val github = async {
                try {
                    CatalogResolver.removeDemoAndShortForm(apiService.getContentList())
                } catch (e: Exception) {
                    null
                }
            }
            val live = async {
                try {
                    withTimeoutOrNull(5_000L) {
                        TheMovieBoxContent.fetchOnlineContent(context)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            github.await() to live.await()
        }

        val catalogCandidates = listOfNotNull(
            githubContent?.takeIf { it.hasItems() },
            lastKnownCatalog,
            bundledVerifiedCatalog
        )
        var finalMerged = catalogCandidates.reduceOrNull { primary, secondary ->
            mergeContent(primary, secondary)
        }
        if (liveTheMovieBox != null) {
            val enrichedLive = CatalogResolver.enrichDetailPagesWithDirectStreams(liveTheMovieBox, catalogCandidates)
            finalMerged = if (finalMerged == null) enrichedLive else mergeContent(finalMerged, enrichedLive)
        }
        finalMerged = CatalogResolver.removeDemoAndShortForm(finalMerged ?: getFallbackContentList())

        val hasOnlineCatalog = liveTheMovieBox.hasItems() || githubContent.hasItems()
        lastRefreshSucceeded = hasOnlineCatalog && finalMerged.categories.any { it.items.isNotEmpty() }

        if (lastRefreshSucceeded) {
            cachedContentList = finalMerged
            catalogCache.save(finalMerged)
            cachedVideoDetails.clear()
        } else {
            cachedContentList = lastKnownCatalog ?: finalMerged
        }
        cachedContentList!!
    }

    /** Returns the saved snapshot immediately, merged with the packaged verified feed. */
    suspend fun getOfflineContent(): ContentResponse = withContext(Dispatchers.IO) {
        val cached = cachedContentList
            ?: catalogCache.load()?.let(CatalogResolver::removeDemoAndShortForm)
        val bundled = TheMovieBoxContent.loadVerifiedCatalog(context)?.let(CatalogResolver::removeDemoAndShortForm)
        val local = when {
            cached != null && bundled != null -> CatalogResolver.removeDemoAndShortForm(mergeContent(cached, bundled))
            cached != null -> cached
            else -> bundled
        }
        if (local != null) {
            cachedContentList = local
            return@withContext local
        }
        getFallbackContentList()
    }

    /** Performs a network refresh and reports whether any online catalog produced usable items. */
    suspend fun syncContent(): Boolean = withContext(Dispatchers.IO) {
        getContentList(forceRefresh = true)
        lastRefreshSucceeded
    }

    private fun mergeContent(primary: ContentResponse, secondary: ContentResponse?): ContentResponse {
        if (secondary == null) return primary

        val allCategories = primary.categories.toMutableList()
        for (srcCat in secondary.categories) {
            val existingIdx = allCategories.indexOfFirst { it.name.equals(srcCat.name, ignoreCase = true) }
            if (existingIdx != -1) {
                val combined = mergeItems(allCategories[existingIdx].items, srcCat.items)
                allCategories[existingIdx] = allCategories[existingIdx].copy(items = combined)
            } else {
                allCategories.add(srcCat)
            }
        }
        return ContentResponse(
            featured = preferItem(primary.featured, secondary.featured),
            categories = allCategories
        )
    }

    private fun mergeItems(primary: List<MovieItem>, secondary: List<MovieItem>): List<MovieItem> {
        val merged = mutableListOf<MovieItem>()
        (primary + secondary).forEach { candidate ->
            val existingIndex = merged.indexOfFirst {
                it.id == candidate.id || CatalogResolver.sameCatalogTitle(it, candidate)
            }
            if (existingIndex < 0) {
                merged.add(candidate)
            } else {
                merged[existingIndex] = preferItem(merged[existingIndex], candidate) ?: candidate
            }
        }
        return merged
    }

    private fun preferItem(primary: MovieItem?, secondary: MovieItem?): MovieItem? {
        if (primary == null) return secondary
        if (secondary == null) return primary
        val primaryUrl = primary.videoUrl.trim()
        val secondaryUrl = secondary.videoUrl.trim()
        if (primaryUrl.isBlank()) return secondary
        if (secondaryUrl.isBlank()) return primary
        if (PlaybackUrlResolver.isEmbeddedPage(primaryUrl) && !PlaybackUrlResolver.isEmbeddedPage(secondaryUrl)) {
            return secondary
        }
        return primary
    }

    private fun ContentResponse?.hasItems(): Boolean =
        this?.categories?.any { it.items.isNotEmpty() } == true

    suspend fun getTrendingMovies(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedTrendingMovies != null) return@withContext cachedTrendingMovies!!
        try {
            val list = getContentList(forceRefresh)
            val trending = list.categories.firstOrNull {
                it.name.contains("Trending", ignoreCase = true) ||
                it.name.contains("Action", ignoreCase = true) ||
                it.name.contains("Movies", ignoreCase = true)
            }
            if (trending != null && trending.items.isNotEmpty()) {
                cachedTrendingMovies = trending.items
                return@withContext trending.items
            }
            val response = CatalogResolver.filterFeatureItems(
                try { apiService.getTrendingMovies() } catch (e: Exception) { emptyList() }
            )
            val localFeatures = CatalogResolver.filterFeatureItems(list.categories.flatMap { it.items })
            cachedTrendingMovies = response.ifEmpty { localFeatures.take(20) }
            cachedTrendingMovies!!
        } catch (e: Exception) {
            cachedTrendingMovies ?: emptyList()
        }
    }

    suspend fun getPopularDramas(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedPopularDramas != null) return@withContext cachedPopularDramas!!
        try {
            val list = getContentList(forceRefresh)
            val dramas = list.categories.firstOrNull {
                it.name.contains("Drama", ignoreCase = true) ||
                it.name.contains("Fantasy", ignoreCase = true)
            }
            if (dramas != null && dramas.items.isNotEmpty()) {
                cachedPopularDramas = dramas.items
                return@withContext dramas.items
            }
            val response = CatalogResolver.filterFeatureItems(
                try { apiService.getPopularDramas() } catch (e: Exception) { emptyList() }
            )
            cachedPopularDramas = response
            cachedPopularDramas!!
        } catch (e: Exception) {
            cachedPopularDramas ?: emptyList()
        }
    }

    suspend fun searchMovies(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            val allCached = (cachedContentList?.categories?.flatMap { it.items } ?: emptyList()) +
                    (cachedTrendingMovies ?: emptyList()) +
                    (cachedPopularDramas ?: emptyList())
            if (allCached.isNotEmpty()) {
                return@withContext allCached.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.safeDescription.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
                }.distinctBy { it.id }
            }
            CatalogResolver.filterEligibleItems(
                try { apiService.searchMovies(query).results } catch (e: Exception) { emptyList() }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVideoDetails(id: String, forceRefresh: Boolean = false): VideoDetails = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedVideoDetails.containsKey(id)) return@withContext cachedVideoDetails[id]!!
        try {
            val list = getContentList(forceRefresh)
            val allItems = list.categories.flatMap { it.items } + listOfNotNull(list.featured)
            val matchedItem = allItems.firstOrNull { it.id == id }

            if (matchedItem != null) {
                val details = VideoDetails(
                    id = matchedItem.id,
                    title = matchedItem.title,
                    poster = matchedItem.safePoster,
                    backdrop = matchedItem.safeBackdrop,
                    description = matchedItem.safeDescription,
                    rating = matchedItem.safeRating,
                    duration = matchedItem.safeDuration,
                    videoUrl = matchedItem.videoUrl,
                    releaseYear = matchedItem.safeReleaseYear,
                    genre = matchedItem.category,
                    language = matchedItem.safeQuality,
                    quality = matchedItem.safeQuality,
                    actualDurationSeconds = matchedItem.actualDurationSeconds,
                    contentType = matchedItem.contentType,
                    relatedItems = allItems.filter { it.id != matchedItem.id && it.category == matchedItem.category }.take(10)
                )
                cachedVideoDetails[id] = details
                return@withContext details
            }
            try { apiService.getVideoDetails(id) } catch (e: Exception) { getFallbackVideoDetails(id) }
        } catch (e: Exception) {
            cachedVideoDetails[id] ?: getFallbackVideoDetails(id)
        }
    }

    // --- FALLBACK ---

    private suspend fun getFallbackContentList(): ContentResponse {
        return TheMovieBoxContent.loadVerifiedCatalog(context)?.let(CatalogResolver::removeDemoAndShortForm)
            ?: TheMovieBoxContent.loadFallbackContent(context)?.let(CatalogResolver::removeDemoAndShortForm)
            ?: ContentResponse(featured = null, categories = emptyList())
    }

    private fun getFallbackVideoDetails(id: String): VideoDetails {
        val matched = cachedContentList?.categories?.flatMap { it.items }?.firstOrNull { it.id == id }
        if (matched == null) {
            return VideoDetails(
                id = id,
                title = "Content unavailable",
                poster = "",
                backdrop = "",
                description = "No catalog details or playable stream are available offline.",
                rating = "—",
                duration = "—",
                videoUrl = "",
                releaseYear = "—",
                genre = "",
                language = "",
                quality = "",
                relatedItems = emptyList()
            )
        }
        return VideoDetails(
            id = matched.id,
            title = matched.title,
            poster = matched.safePoster,
            backdrop = matched.safeBackdrop,
            description = matched.safeDescription,
            rating = matched.safeRating,
            duration = matched.safeDuration,
            videoUrl = matched.videoUrl,
            releaseYear = matched.safeReleaseYear,
            genre = matched.category,
            language = matched.safeQuality,
            quality = matched.safeQuality,
            actualDurationSeconds = matched.actualDurationSeconds,
            contentType = matched.contentType,
            relatedItems = cachedContentList?.categories?.flatMap { it.items }
                ?.filter { it.id != id }?.take(5).orEmpty()
        )
    }
}
