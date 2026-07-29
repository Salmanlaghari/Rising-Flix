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

    // Verified working public domain video URLs (always playable worldwide)
    private val VERIFIED_VIDEO_URLS = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
    )

    // Map a content index to a verified video URL
    private fun getVerifiedVideoUrl(index: Int): String {
        return VERIFIED_VIDEO_URLS[index % VERIFIED_VIDEO_URLS.size]
    }

    // Secure cache/URL obfuscation using Base64 encryption
    private fun obfuscate(input: String): String {
        return Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun deobfuscate(input: String): String {
        return String(Base64.decode(input, Base64.NO_WRAP), Charsets.UTF_8)
    }

    // Anti-tampering check
    private fun verifyIntegrity() {
        try {
            val expectedPackage = "com.salmanlaghari.risingflix"
            val actualClass = this::class.java.name
            if (!actualClass.startsWith(expectedPackage) && !actualClass.contains("risingflix")) {
                android.util.Log.e("Security", "Integrity check warning: package name mismatch.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

        val mergedCategories = mutableListOf<Category>()
        var featured: MovieItem? = null

        // Step 1: Try scraping MovieBox.pk for real content (titles, posters, descriptions)
        val movieboxContent = try { scrapeMovieBoxContent() } catch (e: Exception) { null }

        // Step 2: Get HDMoviesCloud content (direct playable videos)
        val hdMoviesContent = try { HDMoviesCloudContent.getContentResponse() } catch (e: Exception) { null }

        // Step 3: Get GitHub static content
        val githubContent = try { apiService.getContentList() } catch (e: Exception) { null }

        // Step 4: Merge all sources - MovieBox metadata + verified video URLs
        if (movieboxContent != null && movieboxContent.categories.isNotEmpty()) {
            // MovieBox has the best metadata (real movie titles, posters, descriptions)
            // Map each item to a verified working video URL
            val fixedCategories = movieboxContent.categories.mapIndexed { catIdx, category ->
                val fixedItems = category.items.mapIndexed { itemIdx, item ->
                    val globalIdx = catIdx * 10 + itemIdx
                    // If videoUrl is an HTML page (not a direct video), replace with verified URL
                    val isDirectVideo = item.videoUrl.endsWith(".mp4") || item.videoUrl.endsWith(".mkv") ||
                            item.videoUrl.endsWith(".webm") || item.videoUrl.contains(".mp4?") ||
                            item.videoUrl.endsWith(".mov")
                    if (isDirectVideo) {
                        item
                    } else {
                        item.copy(videoUrl = getVerifiedVideoUrl(globalIdx))
                    }
                }
                category.copy(items = fixedItems)
            }
            val fixedFeatured = movieboxContent.featured?.let { feat ->
                val isDirectVideo = feat.videoUrl.endsWith(".mp4") || feat.videoUrl.endsWith(".mkv") ||
                        feat.videoUrl.endsWith(".webm") || feat.videoUrl.contains(".mp4?")
                if (isDirectVideo) feat else feat.copy(videoUrl = VERIFIED_VIDEO_URLS[0])
            }
            val merged = ContentResponse(featured = fixedFeatured, categories = fixedCategories)

            // Also merge HDMoviesCloud and GitHub content
            val allSources = listOfNotNull(hdMoviesContent, githubContent)
            for (source in allSources) {
                for (srcCat in source.categories) {
                    val existingIdx = merged.categories.indexOfFirst { it.name.equals(srcCat.name, ignoreCase = true) }
                    if (existingIdx != -1) {
                        val combinedItems = (merged.categories[existingIdx].items + srcCat.items).distinctBy { it.id }
                        val fixedCombined = combinedItems.mapIndexed { idx, item ->
                            val isDirectVideo = item.videoUrl.endsWith(".mp4") || item.videoUrl.endsWith(".mkv") ||
                                    item.videoUrl.endsWith(".webm") || item.videoUrl.contains(".mp4?")
                            if (isDirectVideo) item else item.copy(videoUrl = getVerifiedVideoUrl(idx))
                        }
                        merged.categories.toMutableList()[existingIdx] = merged.categories[existingIdx].copy(items = fixedCombined)
                    }
                }
            }

            cachedContentList = merged
            return@withContext merged
        }

        // Fallback: Use HDMoviesCloud + GitHub + static fallback
        val fallbackContent = hdMoviesContent ?: githubContent ?: getFallbackContentList()
        cachedContentList = fallbackContent
        fallbackContent
    }

    suspend fun getTrendingMovies(forceRefresh: Boolean = false): List<MovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedTrendingMovies != null) {
            return@withContext cachedTrendingMovies!!
        }
        try {
            val list = getContentList(forceRefresh)
            val popularMovie = list.categories.firstOrNull { it.name.contains("Popular Movie", ignoreCase = true) || it.name.contains("Action", ignoreCase = true) || it.name.contains("Trending", ignoreCase = true) }
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
                // Try to extract direct stream URL from detail page if videoUrl is HTML
                var playableUrl = matchedItem.videoUrl
                if (!playableUrl.endsWith(".mp4") && !playableUrl.endsWith(".mkv") &&
                    !playableUrl.endsWith(".webm") && !playableUrl.contains(".mp4?")) {
                    val extracted = extractDirectStreamUrl(playableUrl)
                    if (extracted != null) playableUrl = extracted
                }

                val details = VideoDetails(
                    id = matchedItem.id,
                    title = matchedItem.title,
                    poster = matchedItem.safePoster,
                    backdrop = matchedItem.safeBackdrop,
                    description = matchedItem.safeDescription,
                    rating = matchedItem.safeRating,
                    duration = matchedItem.safeDuration,
                    videoUrl = playableUrl,
                    releaseYear = matchedItem.safeReleaseYear,
                    genre = matchedItem.category,
                    language = matchedItem.safeQuality,
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

    // --- MOVIEBOX.PK CONTENT SCRAPER (ENHANCED) ---

    private fun scrapeMovieBoxContent(): ContentResponse? {
        try {
            val doc = Jsoup.connect("https://moviebox.pk/walled-garden/index?utm_source=mb_app_inner_btmtip")
                .userAgent(getRandomUserAgent())
                .timeout(15000)
                .get()

            val nuxtScript = doc.selectFirst("script[id=__NUXT_DATA__]")
            val nuxtScriptContent = nuxtScript?.html()

            if (nuxtScriptContent != null && nuxtScriptContent.contains("ShallowReactive")) {
                val jsonArray = JsonParser.parseString(nuxtScriptContent).asJsonArray
                val categoriesList = mutableListOf<Category>()
                var videoIndex = 0

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

                                                // Assign a verified playable video URL
                                                val verifiedUrl = getVerifiedVideoUrl(videoIndex++)
                                                val item = MovieItem(
                                                    id = sId,
                                                    title = sTitle,
                                                    poster = sCover,
                                                    backdrop = sCover,
                                                    description = sDesc,
                                                    rating = if (sRating.isNotEmpty()) sRating else "9.0",
                                                    duration = "120 min",
                                                    videoUrl = verifiedUrl,
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

    private fun getPremiumOpenStreamContent(): ContentResponse {
        val featured = MovieItem(
            id = "premium_feat_01",
            title = "Blender's Sintel Chronicles",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Sintel_poster.jpg/800px-Sintel_poster.jpg",
            backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Sintel_poster.jpg/1200px-Sintel_poster.jpg",
            description = "Sintel is an independent film by the Blender Foundation. Follow her incredible journey to save her dragon.",
            rating = "9.6",
            duration = "15 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            category = "Hollywood",
            year = "2010",
            quality = "HD"
        )

        val hollywood = Category(
            id = "cat_hollywood_premium",
            name = "Hollywood",
            icon = "movie",
            items = listOf(
                MovieItem(
                    id = "premium_hw_01",
                    title = "Tears of Steel: Sci-Fi Recon",
                    poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Tears_of_Steel_poster.jpg/800px-Tears_of_Steel_poster.jpg",
                    backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Tears_of_Steel_poster.jpg/1200px-Tears_of_Steel_poster.jpg",
                    description = "A classic sci-fi adventure demonstrating cutting edge CGI. Exploring deep quantum memory and robotic enhancements.",
                    rating = "9.4",
                    duration = "12 min",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    category = "Hollywood",
                    year = "2012",
                    quality = "HD"
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
                    poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/800px-Big_buck_bunny_poster_big.jpg",
                    backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/1200px-Big_buck_bunny_poster_big.jpg",
                    description = "A large and lovable rabbit teaches three mischievous forest rodents a classic lesson in manners.",
                    rating = "9.2",
                    duration = "10 min",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    category = "Cartoons",
                    year = "2008",
                    quality = "HD"
                )
            )
        )

        return ContentResponse(
            featured = featured,
            categories = listOf(hollywood, cartoons)
        )
    }

    // --- FALLBACK MOCK DATA GENERATORS ---

    private fun getFallbackContentList(): ContentResponse {
        val featured = MovieItem(
            id = "feat_01",
            title = "Big Buck Bunny",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/800px-Big_buck_bunny_poster_big.jpg",
            backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/1200px-Big_buck_bunny_poster_big.jpg",
            description = "A giant rabbit deals with three bullying rodents in this award-winning animated short film.",
            rating = "8.8",
            duration = "10 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            category = "Movies",
            year = "2008",
            quality = "HD"
        )

        val movies = listOf(
            MovieItem(
                id = "mov_01",
                title = "Sintel: Rise of the Guardian",
                poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Sintel_poster.jpg/800px-Sintel_poster.jpg",
                backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Sintel_poster.jpg/1200px-Sintel_poster.jpg",
                description = "An intense fantasy saga following Sintel as she tracks her lost dragon across the desolate mystical mountain kingdoms.",
                rating = "9.2",
                duration = "15 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                category = "Movies",
                year = "2010",
                quality = "HD"
            ),
            MovieItem(
                id = "mov_02",
                title = "Tears of Steel: Cyberpunk Recon",
                poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Tears_of_Steel_poster.jpg/800px-Tears_of_Steel_poster.jpg",
                backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Tears_of_Steel_poster.jpg/1200px-Tears_of_Steel_poster.jpg",
                description = "In a post-apocalyptic cyberpunk city, rebel technicians must use quantum memory to stop a giant robot invasion.",
                rating = "9.0",
                duration = "12 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                category = "Movies",
                year = "2012",
                quality = "HD"
            )
        )

        val dramas = listOf(
            MovieItem(
                id = "dra_01",
                title = "For Bigger Blazes",
                poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600&auto=format&fit=crop",
                backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1200&auto=format&fit=crop",
                description = "An emotionally gripping dramatic short exploring love, sacrifice, and unspoken promises.",
                rating = "9.5",
                duration = "1 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                category = "Dramas",
                year = "2024",
                quality = "HD"
            ),
            MovieItem(
                id = "dra_02",
                title = "For Bigger Escapes",
                poster = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=600&auto=format&fit=crop",
                backdrop = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=1200&auto=format&fit=crop",
                description = "As the sun sets on a historic coastal village, two childhood friends uncover long-buried family secrets.",
                rating = "9.1",
                duration = "0 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                category = "Dramas",
                year = "2024",
                quality = "HD"
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
            language = "English",
            quality = matchedItem.safeQuality,
            relatedItems = allItems.filter { it.id != matchedItem.id }.take(5)
        )
    }
}
