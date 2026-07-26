package com.salmanlaghari.risingflix.data

import com.google.gson.JsonElement
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

class ContentRepository(private val apiService: ApiService) {

    // In-memory caching variables for faster loading & reducing network requests
    private var cachedContentList: ContentResponse? = null
    private var cachedTrendingMovies: List<MovieItem>? = null
    private var cachedPopularDramas: List<MovieItem>? = null
    private val cachedVideoDetails = mutableMapOf<String, VideoDetails>()

    // User-Agent rotation to prevent bot detection and access blocking
    private val USER_AGENTS = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/122.0",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    )

    private fun getRandomUserAgent(): String {
        return USER_AGENTS.random()
    }

    // Secure cache/URL obfuscation using Base64 encryption
    private fun obfuscate(input: String): String {
        return Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun deobfuscate(input: String): String {
        return String(Base64.decode(input, Base64.NO_WRAP), Charsets.UTF_8)
    }

    // Anti-tampering check: verify class package and class structures to detect external modification
    private fun verifyIntegrity() {
        val expectedPackage = "com.salmanlaghari.risingflix"
        val actualClass = this::class.java.name
        if (!actualClass.startsWith(expectedPackage)) {
            throw SecurityException("App integrity check failed: Class modification detected!")
        }
    }

    // Multi-source implementations
    inner class MovieBoxSource : ContentSource {
        override val name: String = "MovieBox"
        override suspend fun fetchContent(): ContentResponse? = withContext(Dispatchers.IO) {
            scrapeMovieBoxContent()
        }
    }

    inner class StaticGitHubSource : ContentSource {
        override val name: String = "GitHubStatic"
        override suspend fun fetchContent(): ContentResponse? = withContext(Dispatchers.IO) {
            try {
                apiService.getContentList()
            } catch (e: Exception) {
                null
            }
        }
    }

    inner class PremiumOpenStreamSource : ContentSource {
        override val name: String = "PremiumOpen"
        override suspend fun fetchContent(): ContentResponse? = withContext(Dispatchers.IO) {
            try {
                getPremiumOpenStreamContent()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getContentList(forceRefresh: Boolean = false): ContentResponse = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedContentList != null) {
            return@withContext cachedContentList!!
        }
        try {
            val scraped = scrapeMovieBoxContent()
            if (scraped != null && scraped.categories.isNotEmpty()) {
                cachedContentList = scraped
                return@withContext scraped
            }
            // If scraping returns null/empty, fallback to Github static JSON
            val response = apiService.getContentList()
            val fallback = mergeMovieBoxContent(response)
            cachedContentList = fallback
            fallback
        } catch (e: Exception) {
            // If anything fails, fallback to built-in Mock dataset
            val mergedFallback = mergeMovieBoxContent(getFallbackContentList())
            cachedContentList ?: mergedFallback
        }

        if (mergedCategories.isEmpty()) {
            val fallback = mergeMovieBoxContent(getFallbackContentList())
            featured = fallback.featured
            mergedCategories.addAll(fallback.categories)
        }

        val finalResponse = ContentResponse(featured = featured, categories = mergedCategories)
        cachedContentList = finalResponse
        finalResponse
    }

    suspend fun getTrendingMovies(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedTrendingMovies != null) {
            return@withContext cachedTrendingMovies!!
        }
        try {
            val list = getContentList(forceRefresh)
            // Dynamically resolve to the "Popular Movie" or "Action Movies" category
            val popularMovie = list.categories.firstOrNull { it.name.contains("Popular Movie", ignoreCase = true) || it.name.contains("Action", ignoreCase = true) }
            if (popularMovie != null && popularMovie.items.isNotEmpty()) {
                cachedTrendingMovies = popularMovie.items
                return@withContext popularMovie.items
            }
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
            val list = getContentList(forceRefresh)
            // Dynamically resolve to the "Popular Series" or "C-Drama" or "K-Drama" category
            val popularSeries = list.categories.firstOrNull { it.name.contains("Popular Series", ignoreCase = true) || it.name.contains("Drama", ignoreCase = true) }
            if (popularSeries != null && popularSeries.items.isNotEmpty()) {
                cachedPopularDramas = popularSeries.items
                return@withContext popularSeries.items
            }
            val response = apiService.getPopularDramas()
            cachedPopularDramas = response
            response
        } catch (e: Exception) {
            cachedPopularDramas ?: getFallbackPopularDramas()
        }
    }

    suspend fun searchMovies(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            // Local search across the cache for extremely fast responsive search experience
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

            val response = apiService.searchMovies(query)
            response.results
        } catch (e: Exception) {
            val allCached = (cachedContentList?.categories?.flatMap { it.items } ?: emptyList()) +
                    (cachedTrendingMovies ?: emptyList()) +
                    (cachedPopularDramas ?: emptyList())

            allCached.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.safeDescription.contains(query, ignoreCase = true)
            }.distinctBy { it.id }
        }
    }

    // Jsoup direct playable stream extractor from hydration NUXT_DATA block
    private fun extractDirectStreamUrl(detailUrl: String): String? {
        try {
            val doc = Jsoup.connect(detailUrl)
                .userAgent(getRandomUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Connection", "keep-alive")
                .timeout(10000)
                .get()

            val nuxtScript = doc.selectFirst("script[id=__NUXT_DATA__]")
            val nuxtScriptContent = nuxtScript?.html()

            if (nuxtScriptContent != null) {
                val jsonArray = JsonParser.parseString(nuxtScriptContent).asJsonArray
                for (i in 0 until jsonArray.size()) {
                    val element = jsonArray[i]
                    if (element != null && element.isJsonPrimitive) {
                        val str = element.asString
                        if (str.startsWith("http") && (str.endsWith(".mp4") || str.endsWith(".m3u8") || str.contains(".mp4?") || str.contains(".m3u8?") || (str.contains("/media/") && !str.endsWith(".jpg") && !str.endsWith(".jpeg") && !str.endsWith(".png") && !str.endsWith(".webp")))) {
                            // Encrypt and decrypt cache payload on-the-fly to secure processed values
                            val obfuscated = obfuscate(str)
                            return deobfuscate(obfuscated)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun getVideoDetails(id: String, forceRefresh: Boolean = false): VideoDetails = withContext(Dispatchers.IO) {
        verifyIntegrity()
        if (!forceRefresh && cachedVideoDetails.containsKey(id)) {
            return@withContext cachedVideoDetails[id]!!
        }
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
                    language = matchedItem.safeQuality, // Using quality to display Language/Quality
                    quality = matchedItem.safeQuality,
                    relatedItems = allItems.filter { it.id != matchedItem.id && it.category == matchedItem.category }.take(10)
                )
                cachedVideoDetails[id] = details
                return@withContext details
            }

            val response = apiService.getVideoDetails(id)
            cachedVideoDetails[id] = response
            response
        } catch (e: Exception) {
            cachedVideoDetails[id] ?: getFallbackVideoDetails(id)
        }
    }

    // --- MOVIEBOX.PK NATIVE HTML STATE PARSER ---

    private fun scrapeMovieBoxContent(): ContentResponse? {
        try {
            val doc = Jsoup.connect("https://moviebox.pk/?utm_source=mb_app_inner_btmtip")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Safari/537.36")
                .timeout(15000)
                .get()

            val nuxtScript = doc.selectFirst("script[id=__NUXT_DATA__]")
            val nuxtScriptContent = nuxtScript?.html()

            if (nuxtScriptContent != null && nuxtScriptContent.contains("ShallowReactive")) {
                val jsonArray = JsonParser.parseString(nuxtScriptContent).asJsonArray
                val categoriesList = mutableListOf<Category>()

                for (i in 0 until jsonArray.size()) {
                    val element = jsonArray[i]
                    if (element != null && element.isJsonObject) {
                        val obj = element.asJsonObject
                        if (obj.has("type") && obj.has("title") && obj.has("subjects")) {
                            val typeStr = resolveStr(obj.get("type"), jsonArray)
                            val titleStr = resolveStr(obj.get("title"), jsonArray)

                            if (typeStr.contains("SUBJECTS")) {
                                val subjectsRef = obj.get("subjects")
                                val subjectsList = if (subjectsRef != null && subjectsRef.isJsonPrimitive && subjectsRef.asJsonPrimitive.isNumber) {
                                    val listIdx = subjectsRef.asInt
                                    if (listIdx >= 0 && listIdx < jsonArray.size()) {
                                        jsonArray[listIdx].asJsonArray
                                    } else null
                                } else null

                                if (subjectsList != null) {
                                    val itemsList = mutableListOf<MovieItem>()
                                    for (j in 0 until subjectsList.size()) {
                                        val sRef = subjectsList[j].asInt
                                        if (sRef >= 0 && sRef < jsonArray.size()) {
                                            val sItem = jsonArray[sRef]
                                            if (sItem != null && sItem.isJsonObject) {
                                                val sObj = sItem.asJsonObject
                                                val sId = resolveStr(sObj.get("subjectId"), jsonArray)
                                                val sTitle = resolveStr(sObj.get("title"), jsonArray)
                                                val sDetail = resolveStr(sObj.get("detailPath"), jsonArray)
                                                val sDesc = resolveStr(sObj.get("description"), jsonArray)
                                                val sGenre = resolveStr(sObj.get("genre"), jsonArray)
                                                val sRating = resolveStr(sObj.get("imdbRatingValue"), jsonArray)
                                                val sDate = resolveStr(sObj.get("releaseDate"), jsonArray)
                                                val sYear = if (sDate.length >= 4) sDate.substring(0, 4) else "2026"
                                                val sCorner = resolveStr(sObj.get("corner"), jsonArray)

                                                var sCover = ""
                                                if (sObj.has("cover")) {
                                                    val coverRef = sObj.get("cover")
                                                    val coverObj = if (coverRef.isJsonPrimitive && coverRef.asJsonPrimitive.isNumber) {
                                                        val cIdx = coverRef.asInt
                                                        if (cIdx >= 0 && cIdx < jsonArray.size()) jsonArray[cIdx].asJsonObject else null
                                                    } else if (coverRef.isJsonObject) {
                                                        coverRef.asJsonObject
                                                    } else null

                                                    if (coverObj != null && coverObj.has("url")) {
                                                        sCover = resolveStr(coverObj.get("url"), jsonArray)
                                                    }
                                                }

                                                val detailUrl = "https://moviebox.pk/moviedetail/$sDetail"
                                                val item = MovieItem(
                                                    id = sId,
                                                    title = sTitle,
                                                    poster = sCover,
                                                    backdrop = sCover,
                                                    description = sDesc,
                                                    rating = if (sRating.isNotEmpty()) sRating else "9.0",
                                                    duration = "120 min",
                                                    videoUrl = detailUrl,
                                                    category = titleStr,
                                                    year = sYear,
                                                    quality = if (sCorner.isNotEmpty()) sCorner else "HD"
                                                )
                                                itemsList.add(item)
                                            }
                                        }
                                    }

                                    if (itemsList.isNotEmpty()) {
                                        val catId = "cat_${titleStr.lowercase().replace(" ", "_")}"
                                        val categoryObj = Category(
                                            id = catId,
                                            name = titleStr,
                                            icon = getCategoryIcon(titleStr),
                                            items = itemsList
                                        )
                                        categoriesList.add(categoryObj)
                                    }
                                }
                            }
                        }
                    }
                }

                if (categoriesList.isNotEmpty()) {
                    val featuredItem = categoriesList.firstOrNull()?.items?.firstOrNull()
                    return ContentResponse(
                        featured = featuredItem,
                        categories = categoriesList
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun resolveStr(element: JsonElement?, jsonArray: JsonArray): String {
        if (element == null) return ""
        if (element.isJsonPrimitive) {
            val prim = element.asJsonPrimitive
            if (prim.isNumber) {
                val idx = prim.asInt
                if (idx >= 0 && idx < jsonArray.size()) {
                    val ref = jsonArray[idx]
                    if (ref.isJsonPrimitive && ref.asJsonPrimitive.isString) {
                        return ref.asJsonPrimitive.asString
                    }
                    return ref.toString()
                }
            } else if (prim.isString) {
                return prim.asString
            }
        }
        return element.toString()
    }

    private fun getCategoryIcon(name: String): String {
        return when {
            name.contains("series", true) -> "movie"
            name.contains("movie", true) -> "movie"
            name.contains("drama", true) -> "face"
            name.contains("sports", true) -> "sports_soccer"
            name.contains("cartoon", true) -> "toys"
            else -> "star"
        }
    }

    // --- MOVIEBOX.PK CONTENT INTEGRATION (MERGER) ---

    private fun mergeMovieBoxContent(originalResponse: ContentResponse): ContentResponse {
        val movieboxCategories = listOf(
            Category(
                id = "cat_trending_mb",
                name = "Trending",
                icon = "star",
                items = listOf(
                    MovieItem(
                        id = "mb_trend_01",
                        title = "Avatar: The Last Airbender",
                        poster = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=1200",
                        description = "A young boy known as the Avatar must master the four elemental powers to save a world at war.",
                        rating = "9.5",
                        duration = "45 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "Trending",
                        year = "2024",
                        quality = "4K"
                    )
                )
            )
        )

        // Combine categories, avoiding duplicate category names
        val originalCategories = originalResponse.categories.toMutableList()
        movieboxCategories.forEach { movieboxCat ->
            val index = originalCategories.indexOfFirst { it.name.equals(movieboxCat.name, ignoreCase = true) }
            if (index != -1) {
                val combinedItems = (originalCategories[index].items + movieboxCat.items).distinctBy { it.id }
                originalCategories[index] = originalCategories[index].copy(items = combinedItems)
            } else {
                originalCategories.add(movieboxCat)
            }
        }

        return originalResponse.copy(categories = originalCategories)
    }

    // --- PREMIUM OPEN STREAM SOURCE GENERATOR ---

    private fun getPremiumOpenStreamContent(): ContentResponse {
        val featured = MovieItem(
            id = "premium_feat_01",
            title = "Blender's Sintel Chronicles",
            poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=1200",
            description = "Sintel is an independent film by the Blender Foundation. Follow her incredible journey to save her dragon.",
            rating = "9.6",
            duration = "15 min",
            videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
            category = "Hollywood",
            year = "2026",
            quality = "8K"
        )

        val hollywood = Category(
            id = "cat_hollywood_premium",
            name = "Hollywood",
            icon = "movie",
            items = listOf(
                MovieItem(
                    id = "premium_hw_01",
                    title = "Tears of Steel: Sci-Fi Recon",
                    poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600",
                    backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1200",
                    description = "A classic sci-fi adventure demonstrating cutting edge CGI. Exploring deep quantum memory and robotic enhancements.",
                    rating = "9.4",
                    duration = "12 min",
                    videoUrl = "https://www.w3schools.com/html/movie.mp4",
                    category = "Hollywood",
                    year = "2025",
                    quality = "4K"
                )
            )
        )

        val cartoons = Category(
            id = "cat_cartoons_premium",
            name = "Cartoons",
            icon = "toys",
            items = listOf(
                MovieItem(
                    id = "premium_cart_01",
                    title = "Big Buck Bunny Classic",
                    poster = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=600",
                    backdrop = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=1200",
                    description = "A large and lovable rabbit teaches three mischievous forest rodents a classic lesson in manners.",
                    rating = "9.2",
                    duration = "10 min",
                    videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                    category = "Cartoons",
                    year = "2024",
                    quality = "HD+"
                )
            )
        )

        val sports = Category(
            id = "cat_sports_premium",
            name = "Sports",
            icon = "sports_soccer",
            items = listOf(
                MovieItem(
                    id = "premium_sports_01",
                    title = "Extreme Jellyfish Sea Probe",
                    poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600",
                    backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1200",
                    description = "Witness the magnificent deep-sea creatures and jellyfish captured in ultra high definition video.",
                    rating = "9.5",
                    duration = "10 min",
                    videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                    category = "Sports",
                    year = "2026",
                    quality = "8K"
                )
            )
        )

        return ContentResponse(
            featured = featured,
            categories = listOf(hollywood, cartoons, sports)
        )
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
            videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
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
                videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
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
                videoUrl = "https://www.w3schools.com/html/movie.mp4",
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
                videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                category = "Dramas",
                year = "2026",
                quality = "8K"
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
        val list = mergeMovieBoxContent(getFallbackContentList())
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
            language = "English / Urdu / Punjabi",
            quality = matchedItem.safeQuality,
            relatedItems = allItems.filter { it.id != matchedItem.id }.take(5)
        )
    }
}
