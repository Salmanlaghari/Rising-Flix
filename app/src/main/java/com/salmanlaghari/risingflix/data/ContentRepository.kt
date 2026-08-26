package com.salmanlaghari.risingflix.data

import android.util.Base64
import com.google.gson.JsonElement
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

interface ContentSource {
    val name: String
    suspend fun fetchContent(): ContentResponse?
}

class ContentRepository(private val apiService: ApiService) {

    // In-memory caching
    private var cachedContentList: ContentResponse? = null
    private var cachedTrendingMovies: List<MovieItem>? = null
    private var cachedPopularDramas: List<MovieItem>? = null
    private val cachedVideoDetails = mutableMapOf<String, VideoDetails>()

    private val USER_AGENTS = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    )

    private fun getRandomUserAgent() = USER_AGENTS.random()

    // --- MAIN CONTENT LOADER (FULLY AUTOMATIC) ---

    suspend fun getContentList(forceRefresh: Boolean = false): ContentResponse = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedContentList != null) {
            return@withContext cachedContentList!!
        }

        // AutoContentManager fetches everything automatically — no API key needed
        // Sources: Archive.org (60K+ music, movies), Blender Foundation, Google sample bucket
        val autoContent = try {
            AutoContentManager.getAllContent()
        } catch (e: Exception) {
            android.util.Log.e("ContentRepo", "Auto content fetch failed", e)
            getFallbackContentList()
        }

        // Also try static GitHub content as supplement
        val githubContent = try {
            apiService.getContentList()
        } catch (e: Exception) {
            null
        }

        // Merge auto + github + themoviebox content
        val themovieboxContent = try {
            TheMovieBoxContent.fetchContent()
        } catch (e: Exception) {
            null
        }

        val merged = mergeContent(autoContent, githubContent)
        val finalMerged = mergeContent(merged, themovieboxContent)
        cachedContentList = finalMerged
        finalMerged
    }

    private fun mergeContent(primary: ContentResponse, secondary: ContentResponse?): ContentResponse {
        if (secondary == null) return primary

        val allCategories = primary.categories.toMutableList()
        for (srcCat in secondary.categories) {
            val existingIdx = allCategories.indexOfFirst { it.name.equals(srcCat.name, ignoreCase = true) }
            if (existingIdx != -1) {
                val combined = (allCategories[existingIdx].items + srcCat.items).distinctBy { it.id }
                allCategories[existingIdx] = allCategories[existingIdx].copy(items = combined)
            } else {
                allCategories.add(srcCat)
            }
        }
        return ContentResponse(featured = primary.featured ?: secondary.featured, categories = allCategories)
    }

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
            val response = try { apiService.getTrendingMovies() } catch (e: Exception) { emptyList() }
            cachedTrendingMovies = response.ifEmpty { list.categories.flatMap { it.items }.take(20) }
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
            val response = try { apiService.getPopularDramas() } catch (e: Exception) { emptyList() }
            cachedPopularDramas = response.ifEmpty { list.categories.flatMap { it.items }.take(15) }
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
            try { apiService.searchMovies(query).results } catch (e: Exception) { emptyList() }
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

    private fun getFallbackContentList(): ContentResponse {
        return ContentResponse(
            featured = AutoContentManager.GUARANTEED_MOVIES.first().toMovieItem(),
            categories = listOf(
                Category(id = "cat_movies", name = "Movies", icon = "movie",
                    items = AutoContentManager.GUARANTEED_MOVIES.map { it.toMovieItem() }),
                Category(id = "cat_samples", name = "Entertainment", icon = "star",
                    items = AutoContentManager.SAMPLE_VIDEOS.map { it.toMovieItem() })
            )
        )
    }

    private fun getFallbackVideoDetails(id: String): VideoDetails {
        val all = AutoContentManager.GUARANTEED_MOVIES + AutoContentManager.SAMPLE_VIDEOS
        val matched = all.firstOrNull { it.id == id } ?: all.first()
        return VideoDetails(
            id = matched.id,
            title = matched.title,
            poster = matched.poster,
            backdrop = matched.poster,
            description = matched.description,
            rating = matched.rating,
            duration = matched.duration,
            videoUrl = matched.videoUrl,
            releaseYear = matched.year,
            genre = matched.category,
            language = "English",
            quality = matched.quality,
            relatedItems = all.filter { it.id != id }.take(5).map { it.toMovieItem() }
        )
    }
}
