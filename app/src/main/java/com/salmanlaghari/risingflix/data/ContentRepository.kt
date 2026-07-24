package com.salmanlaghari.risingflix.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRepository(private val apiService: ApiService) {

    // In-memory caching variables for faster loading & reducing network requests
    private var cachedContentList: ContentResponse? = null
    private var cachedTrendingMovies: List<MovieItem>? = null
    private var cachedPopularDramas: List<MovieItem>? = null
    private val cachedVideoDetails = mutableMapOf<String, VideoDetails>()

    suspend fun getContentList(forceRefresh: Boolean = false): ContentResponse = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedContentList != null) {
            return@withContext cachedContentList!!
        }
        try {
            val response = apiService.getContentList()
            cachedContentList = response
            response
        } catch (e: Exception) {
            // If cache exists, fall back to it
            cachedContentList ?: getFallbackContentList()
        }
    }

    suspend fun getTrendingMovies(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedTrendingMovies != null) {
            return@withContext cachedTrendingMovies!!
        }
        try {
            val response = apiService.getTrendingMovies()
            cachedTrendingMovies = response
            response
        } catch (e: Exception) {
            cachedTrendingMovies ?: getFallbackTrendingMovies()
        }
    }

    suspend fun getPopularDramas(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedPopularDramas != null) {
            return@withContext cachedPopularDramas!!
        }
        try {
            val response = apiService.getPopularDramas()
            cachedPopularDramas = response
            response
        } catch (e: Exception) {
            cachedPopularDramas ?: getFallbackPopularDramas()
        }
    }

    suspend fun searchMovies(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchMovies(query)
            response.results
        } catch (e: Exception) {
            // Local search across the cache as fallback
            val allCached = (cachedContentList?.categories?.flatMap { it.items } ?: emptyList()) +
                    (cachedTrendingMovies ?: emptyList()) +
                    (cachedPopularDramas ?: emptyList())

            allCached.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.safeDescription.contains(query, ignoreCase = true)
            }.distinctBy { it.id }
        }
    }

    suspend fun getVideoDetails(id: String, forceRefresh: Boolean = false): VideoDetails = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedVideoDetails.containsKey(id)) {
            return@withContext cachedVideoDetails[id]!!
        }
        try {
            val response = apiService.getVideoDetails(id)
            cachedVideoDetails[id] = response
            response
        } catch (e: Exception) {
            cachedVideoDetails[id] ?: getFallbackVideoDetails(id)
        }
    }

    // --- FALLBACK MOCK DATA GENERATORS (Ensures robust playback/UI reviews) ---

    private fun getFallbackContentList(): ContentResponse {
        val featured = MovieItem(
            id = "feat_01",
            title = "Epic Space Odyssey: Beyond Horizon",
            poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=600&auto=format&fit=crop",
            backdrop = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1200&auto=format&fit=crop",
            description = "A breathtaking visual masterpiece exploring the uncharted outer edge of the galaxy and the secrets of time-space travel.",
            rating = "9.8",
            duration = "12 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            category = "Movies",
            year = "2026",
            quality = "8K"
        )

        val movies = listOf(
            MovieItem(
                id = "mov_01",
                title = "Sintel: Rise of the Guardian",
                poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop",
                backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=1200&auto=format&fit=crop",
                description = "An intense fantasy saga following Sintel as she tracks her lost dragon across the desolate mystical mountain kingdoms.",
                rating = "9.2",
                duration = "14 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                category = "Movies",
                year = "2026",
                quality = "8K"
            ),
            MovieItem(
                id = "mov_02",
                title = "Tears of Steel: Cyberpunk Recon",
                poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600&auto=format&fit=crop",
                backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1200&auto=format&fit=crop",
                description = "In a post-apocalyptic cyberpunk city, a group of rebel technicians must use advanced quantum memory to stop a giant robot invasion.",
                rating = "9.0",
                duration = "12 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                category = "Movies",
                year = "2025",
                quality = "4K"
            )
        )

        val dramas = listOf(
            MovieItem(
                id = "dra_01",
                title = "Echoes of the Heart: Silent Tears",
                poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600&auto=format&fit=crop",
                backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1200&auto=format&fit=crop",
                description = "An emotionally gripping romantic drama exploring love, sacrifice, and the unspoken promises that bridge two separate worlds.",
                rating = "9.5",
                duration = "5 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                category = "Dramas",
                year = "2026",
                quality = "8K"
            ),
            MovieItem(
                id = "dra_02",
                title = "Whispers of the Golden Hour",
                poster = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=600&auto=format&fit=crop",
                backdrop = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=1200&auto=format&fit=crop",
                description = "As the sun sets on a historic coastal village, two childhood friends uncover long-buried family secrets that will change their destiny.",
                rating = "9.1",
                duration = "3 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                category = "Dramas",
                year = "2025",
                quality = "4K"
            )
        )

        return ContentResponse(
            featured = featured,
            categories = listOf(
                Category(id = "cat_mov", name = "Movies", icon = "movie", items = movies),
                Category(id = "cat_dra", name = "Dramas", icon = "face", items = dramas)
            )
        )
    }

    private fun getFallbackTrendingMovies(): List<MovieItem> {
        return getFallbackContentList().categories.firstOrNull { it.name == "Movies" }?.items ?: emptyList()
    }

    private fun getFallbackPopularDramas(): List<MovieItem> {
        return getFallbackContentList().categories.firstOrNull { it.name == "Dramas" }?.items ?: emptyList()
    }

    private fun getFallbackVideoDetails(id: String): VideoDetails {
        val list = getFallbackContentList()
        val allItems = list.categories.flatMap { it.items } + listOfNotNull(list.featured)
        val matchedItem = allItems.firstOrNull { it.id == id } ?: list.featured!!

        return VideoDetails(
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
            language = "English / Urdu",
            quality = matchedItem.safeQuality,
            relatedItems = allItems.filter { it.id != matchedItem.id }
        )
    }
}
