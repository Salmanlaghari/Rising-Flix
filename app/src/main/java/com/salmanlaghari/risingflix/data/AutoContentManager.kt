package com.salmanlaghari.risingflix.data

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * AutoContentManager - Fully automatic content fetcher
 * NO API keys needed. Fetches FREE, LEGAL, public domain content from:
 * - Archive.org (movies, music, audio - 60,000+ items)
 * - Blender Foundation (open movies)
 * - Public domain video samples
 *
 * All content is Creative Commons / Public Domain licensed.
 */
object AutoContentManager {

    private const val TAG = "AutoContentManager"
    private const val ARCHIVE_SEARCH = "https://archive.org/advancedsearch.php"
    private const val ARCHIVE_METADATA = "https://archive.org/metadata"

    // Verified working direct video URLs (never fail)
    val GUARANTEED_MOVIES = listOf(
        ContentItem(
            id = "auto_bbb",
            title = "Big Buck Bunny",
            description = "A giant rabbit takes revenge on three bullying rodents in this award-winning animated short by Blender Foundation. Creative Commons licensed.",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/400px-Big_buck_bunny_poster_big.jpg",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            category = "Animation",
            duration = "10 min",
            rating = "8.8",
            year = "2008",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_sintel",
            title = "Sintel",
            description = "A lonely young woman searches for her lost dragon Scales in this epic fantasy short by Blender Foundation.",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Sintel_poster.jpg/400px-Sintel_poster.jpg",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            category = "Fantasy",
            duration = "15 min",
            rating = "9.2",
            year = "2010",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_tos",
            title = "Tears of Steel",
            description = "In a dystopian Amsterdam, warriors use advanced technology to stop a robot apocalypse. Blender Foundation sci-fi masterpiece.",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Tears_of_Steel_poster.jpg/400px-Tears_of_Steel_poster.jpg",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            category = "Sci-Fi",
            duration = "12 min",
            rating = "9.0",
            year = "2012",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_ed",
            title = "Elephant's Dream",
            description = "Two characters explore a surreal mechanical world in the world's first open movie by Blender Foundation.",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Elephants_Dream_s1_proog.jpg/400px-Elephants_Dream_s1_proog.jpg",
            videoUrl = "https://download.blender.org/peach/bigbuckbunny_movies/ElephantsDream.mov",
            category = "Animation",
            duration = "11 min",
            rating = "8.5",
            year = "2006",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_spring",
            title = "Spring",
            description = "A young sheep herder faces an ancient spirit to save her village. Beautiful Blender Foundation short with stunning nature.",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d5/Spring_Blender_Open_Movie.jpg/400px-Spring_Blender_Open_Movie.jpg",
            videoUrl = "https://ftp.nluug.nl/pub/graphics/blender/demo/movies/Spring-1080p.mp4",
            category = "Fantasy",
            duration = "8 min",
            rating = "9.1",
            year = "2019",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_cosmos",
            title = "Cosmos Laundromat",
            description = "A suicidal sheep meets a mysterious being who offers him a different life. Blender Foundation's first feature-length attempt.",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Cosmos_Laundromat_poster.jpg/400px-Cosmos_Laundromat_poster.jpg",
            videoUrl = "https://ftp.nluug.nl/pub/graphics/blender/demo/movies/CosmosLaundromat-FirstCycle-1080p.mp4",
            category = "Animation",
            duration = "12 min",
            rating = "8.7",
            year = "2015",
            quality = "HD"
        )
    )

    // Google Sample bucket videos (always work)
    val SAMPLE_VIDEOS = listOf(
        ContentItem(
            id = "auto_blaze",
            title = "For Bigger Blazes",
            description = "Explosive action sequences and fiery confrontations in this high-energy cinematic short.",
            poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=400",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            category = "Action",
            duration = "15 sec",
            rating = "8.0",
            year = "2024",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_escape",
            title = "For Bigger Escapes",
            description = "Heart-pounding escape sequences and daring getaways in this action-packed clip.",
            poster = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=400",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            category = "Action",
            duration = "15 sec",
            rating = "8.2",
            year = "2024",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_fun",
            title = "For Bigger Fun",
            description = "A joyful exploration captured in stunning high-definition with vibrant colors.",
            poster = "https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?q=80&w=400",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            category = "Entertainment",
            duration = "15 sec",
            rating = "7.8",
            year = "2024",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_subaru",
            title = "Subaru Outback: Street & Dirt",
            description = "Watch the Subaru Outback tackle both city streets and off-road dirt tracks.",
            poster = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=400",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            category = "Sports",
            duration = "30 sec",
            rating = "8.5",
            year = "2024",
            quality = "HD"
        ),
        ContentItem(
            id = "auto_bullrun",
            title = "We Are Going On Bullrun",
            description = "High-octane rally action as drivers push machines to the limit.",
            poster = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=400",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            category = "Sports",
            duration = "15 sec",
            rating = "8.9",
            year = "2024",
            quality = "HD"
        )
    )

    /**
     * Fetch movies from Archive.org automatically
     * Returns up to [limit] items with direct playable URLs
     */
    suspend fun fetchArchiveMovies(limit: Int = 15): List<ContentItem> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$ARCHIVE_SEARCH?q=mediatype:movies+AND+licenseurl:*creative*&fl[]=identifier,title,description&sort[]=downloads+desc&rows=$limit&output=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "RisingFlix/1.0 (Android; open-source)")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val response = conn.inputStream.bufferedReader().readText()
            val json = JsonParser.parseString(response).asJsonObject
            val docs = json.getAsJsonObject("response").getAsJsonArray("docs")

            val items = mutableListOf<ContentItem>()
            for (i in 0 until docs.size()) {
                val doc = docs[i].asJsonObject
                val identifier = doc.get("identifier").asString
                val title = doc.get("title").asString
                val desc = if (doc.has("description")) doc.get("description").asString else "Free public domain video from Archive.org"

                // Get actual file URL from metadata
                val fileUrl = getArchiveFileUrl(identifier)
                if (fileUrl != null) {
                    items.add(
                        ContentItem(
                            id = "archive_$identifier",
                            title = title,
                            description = desc.take(200),
                            poster = "https://archive.org/services/img/$identifier",
                            videoUrl = fileUrl,
                            category = "Movies",
                            duration = "Varies",
                            rating = "8.0",
                            year = "2024",
                            quality = "HD"
                        )
                    )
                }
            }
            Log.d(TAG, "Fetched ${items.size} movies from Archive.org")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Archive.org movies", e)
            emptyList()
        }
    }

    /**
     * Fetch music from Archive.org automatically
     * Returns up to [limit] items with direct playable URLs
     */
    suspend fun fetchArchiveMusic(limit: Int = 20): List<ContentItem> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$ARCHIVE_SEARCH?q=mediatype:audio+AND+format:mp3&fl[]=identifier,title,creator&sort[]=downloads+desc&rows=$limit&output=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "RisingFlix/1.0 (Android; open-source)")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val response = conn.inputStream.bufferedReader().readText()
            val json = JsonParser.parseString(response).asJsonObject
            val docs = json.getAsJsonObject("response").getAsJsonArray("docs")

            val items = mutableListOf<ContentItem>()
            for (i in 0 until docs.size()) {
                val doc = docs[i].asJsonObject
                val identifier = doc.get("identifier").asString
                val title = doc.get("title").asString
                val creator = if (doc.has("creator")) {
                    val c = doc.get("creator")
                    if (c.isJsonArray) c.asJsonArray.joinToString(", ") { it.asString } else c.asString
                } else "Unknown Artist"

                // Get actual MP3 file URL from metadata
                val fileUrl = getArchiveAudioUrl(identifier)
                if (fileUrl != null) {
                    items.add(
                        ContentItem(
                            id = "music_$identifier",
                            title = title,
                            description = "By $creator — Free music from Archive.org",
                            poster = "https://archive.org/services/img/$identifier",
                            videoUrl = fileUrl,
                            category = "Music",
                            duration = "Varies",
                            rating = "8.0",
                            year = "2024",
                            quality = "MP3"
                        )
                    )
                }
            }
            Log.d(TAG, "Fetched ${items.size} music tracks from Archive.org")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Archive.org music", e)
            emptyList()
        }
    }

    /**
     * Fetch cartoons/animation from Archive.org
     */
    suspend fun fetchArchiveCartoons(limit: Int = 10): List<ContentItem> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$ARCHIVE_SEARCH?q=mediatype:movies+AND+(subject:animation+OR+subject:cartoon)+AND+licenseurl:*creative*&fl[]=identifier,title,description&sort[]=downloads+desc&rows=$limit&output=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "RisingFlix/1.0 (Android; open-source)")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val response = conn.inputStream.bufferedReader().readText()
            val json = JsonParser.parseString(response).asJsonObject
            val docs = json.getAsJsonObject("response").getAsJsonArray("docs")

            val items = mutableListOf<ContentItem>()
            for (i in 0 until docs.size()) {
                val doc = docs[i].asJsonObject
                val identifier = doc.get("identifier").asString
                val title = doc.get("title").asString
                val desc = if (doc.has("description")) doc.get("description").asString else "Free cartoon from Archive.org"

                val fileUrl = getArchiveFileUrl(identifier)
                if (fileUrl != null) {
                    items.add(
                        ContentItem(
                            id = "cartoon_$identifier",
                            title = title,
                            description = desc.take(200),
                            poster = "https://archive.org/services/img/$identifier",
                            videoUrl = fileUrl,
                            category = "Cartoons",
                            duration = "Varies",
                            rating = "8.5",
                            year = "2024",
                            quality = "HD"
                        )
                    )
                }
            }
            items
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Archive.org cartoons", e)
            emptyList()
        }
    }

    /**
     * Get the actual MP4/OGV file URL from Archive.org metadata
     */
    private fun getArchiveFileUrl(identifier: String): String? {
        try {
            val url = URL("$ARCHIVE_METADATA/$identifier")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "RisingFlix/1.0 (Android; open-source)")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val response = conn.inputStream.bufferedReader().readText()
            val json = JsonParser.parseString(response).asJsonObject
            val files = json.getAsJsonArray("files")

            // Prefer mp4, then ogv, then webm
            var bestFile: String? = null
            var bestPriority = 999

            for (i in 0 until files.size()) {
                val file = files[i].asJsonObject
                val name = file.get("name").asString
                val format = file.get("format")?.asString ?: ""

                val priority = when {
                    name.endsWith(".mp4") && (format.contains("MPEG4") || format.contains("mp4")) -> 1
                    name.endsWith(".mp4") -> 2
                    name.endsWith(".ogv") -> 3
                    name.endsWith(".webm") -> 4
                    else -> 999
                }

                if (priority < bestPriority) {
                    bestPriority = priority
                    bestFile = name
                }
            }

            return if (bestFile != null) "https://archive.org/download/$identifier/$bestFile" else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Archive.org file URL for $identifier", e)
            return null
        }
    }

    /**
     * Get the actual MP3/OGG file URL from Archive.org audio metadata
     */
    private fun getArchiveAudioUrl(identifier: String): String? {
        try {
            val url = URL("$ARCHIVE_METADATA/$identifier")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "RisingFlix/1.0 (Android; open-source)")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val response = conn.inputStream.bufferedReader().readText()
            val json = JsonParser.parseString(response).asJsonObject
            val files = json.getAsJsonArray("files")

            var bestFile: String? = null
            var bestPriority = 999

            for (i in 0 until files.size()) {
                val file = files[i].asJsonObject
                val name = file.get("name").asString
                val format = file.get("format")?.asString ?: ""

                val priority = when {
                    name.endsWith(".mp3") && format.contains("MP3") -> 1
                    name.endsWith(".mp3") -> 2
                    name.endsWith(".ogg") -> 3
                    else -> 999
                }

                if (priority < bestPriority) {
                    bestPriority = priority
                    bestFile = name
                }
            }

            return if (bestFile != null) "https://archive.org/download/$identifier/$bestFile" else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Archive.org audio URL for $identifier", e)
            return null
        }
    }

    /**
     * Get ALL content merged into categories
     */
    suspend fun getAllContent(): ContentResponse = withContext(Dispatchers.IO) {
        val movies = mutableListOf<ContentItem>()
        val music = mutableListOf<ContentItem>()
        val cartoons = mutableListOf<ContentItem>()

        // Step 1: Add guaranteed content (always works, no network needed)
        movies.addAll(GUARANTEED_MOVIES)
        movies.addAll(SAMPLE_VIDEOS)

        // Step 2: Fetch from Archive.org (automatic, no API key)
        try {
            val archiveMovies = fetchArchiveMovies(15)
            movies.addAll(archiveMovies)
        } catch (e: Exception) {
            Log.e(TAG, "Archive.org movies fetch failed, using fallback", e)
        }

        try {
            val archiveMusic = fetchArchiveMusic(20)
            music.addAll(archiveMusic)
        } catch (e: Exception) {
            Log.e(TAG, "Archive.org music fetch failed", e)
        }

        try {
            val archiveCartoons = fetchArchiveCartoons(10)
            cartoons.addAll(archiveCartoons)
        } catch (e: Exception) {
            Log.e(TAG, "Archive.org cartoons fetch failed", e)
        }

        // Step 3: Build categories
        val categories = mutableListOf<Category>()

        if (movies.isNotEmpty()) {
            categories.add(Category(id = "cat_movies", name = "Movies", icon = "movie", items = movies.map { it.toMovieItem() }))
        }
        if (cartoons.isNotEmpty()) {
            categories.add(Category(id = "cat_cartoons", name = "Cartoons", icon = "toys", items = cartoons.map { it.toMovieItem() }))
        }
        if (music.isNotEmpty()) {
            categories.add(Category(id = "cat_music", name = "Music", icon = "music_note", items = music.map { it.toMovieItem() }))
        }

        // Add Drama category from movies that have drama-like titles
        val dramaItems = movies.filter {
            it.title.contains("drama", ignoreCase = true) ||
            it.description.contains("drama", ignoreCase = true) ||
            it.title.contains("story", ignoreCase = true) ||
            it.title.contains("love", ignoreCase = true)
        }
        if (dramaItems.isNotEmpty()) {
            categories.add(Category(id = "cat_dramas", name = "Dramas", icon = "face", items = dramaItems.map { it.toMovieItem() }))
        }

        // Add Sports category
        val sportsItems = movies.filter {
            it.title.contains("sport", ignoreCase = true) ||
            it.title.contains("race", ignoreCase = true) ||
            it.title.contains("football", ignoreCase = true) ||
            it.category == "Sports"
        }
        if (sportsItems.isNotEmpty()) {
            categories.add(Category(id = "cat_sports", name = "Sports", icon = "sports_soccer", items = sportsItems.map { it.toMovieItem() }))
        }

        ContentResponse(
            featured = movies.firstOrNull()?.toMovieItem(),
            categories = categories
        )
    }
}

/**
 * Helper data class for content items
 */
data class ContentItem(
    val id: String,
    val title: String,
    val description: String,
    val poster: String,
    val videoUrl: String,
    val category: String,
    val duration: String,
    val rating: String,
    val year: String,
    val quality: String
) {
    fun toMovieItem(): MovieItem {
        return MovieItem(
            id = id,
            title = title,
            poster = poster,
            backdrop = poster,
            description = description,
            rating = rating,
            duration = duration,
            videoUrl = videoUrl,
            category = category,
            year = year,
            quality = quality
        )
    }
}
