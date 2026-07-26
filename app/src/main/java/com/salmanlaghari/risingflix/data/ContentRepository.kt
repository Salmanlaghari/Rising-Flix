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
            val merged = mergeMovieBoxContent(response)
            cachedContentList = merged
            merged
        } catch (e: Exception) {
            // If cache exists, fall back to it
            val mergedFallback = mergeMovieBoxContent(getFallbackContentList())
            cachedContentList ?: mergedFallback
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
                    ),
                    MovieItem(
                        id = "mb_trend_02",
                        title = "A Shop for Killers",
                        poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1200",
                        description = "After her uncle's sudden death, a college student inherits a mysterious shopping mall frequented by deadly killers.",
                        rating = "9.2",
                        duration = "50 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "Trending",
                        year = "2024",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_trend_03",
                        title = "Elite Force [English]",
                        poster = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                        description = "An elite special forces unit must prevent a global catastrophe when a tactical satellite falls into rogue hands.",
                        rating = "8.7",
                        duration = "112 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "Trending",
                        year = "2024",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_trend_04",
                        title = "Agent Kim Reactivated",
                        poster = "https://images.unsplash.com/photo-1543857778-c4a1a3e0b2eb?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1543857778-c4a1a3e0b2eb?q=80&w=1200",
                        description = "After years off the grid, Agent Kim is reactivated to neutralize a threat from her past that endangers the agency.",
                        rating = "9.0",
                        duration = "124 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "Trending",
                        year = "2026",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_trend_05",
                        title = "Ride or Die",
                        poster = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=1200",
                        description = "Two drift racers with contrasting backgrounds team up to win a tournament while being hunted by corrupt syndicates.",
                        rating = "9.1",
                        duration = "118 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_10MB.mp4",
                        category = "Trending",
                        year = "2026",
                        quality = "4K"
                    )
                )
            ),
            Category(
                id = "cat_cinema_mb",
                name = "Cinema",
                icon = "movie",
                items = listOf(
                    MovieItem(
                        id = "mb_cinema_01",
                        title = "Colony",
                        poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1200",
                        description = "In a near-future dystopian Los Angeles, a family struggles to survive and bring liberty back to the people.",
                        rating = "9.6",
                        duration = "48 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "Cinema",
                        year = "2026",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_cinema_02",
                        title = "72 Hours",
                        poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1200",
                        description = "An action-packed race against time where a detective has exactly 72 hours to dismantle an international syndicate.",
                        rating = "8.9",
                        duration = "118 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "Cinema",
                        year = "2026",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_cinema_03",
                        title = "The Death of Robin Hood",
                        poster = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=1200",
                        description = "An aging Robin Hood grapples with his past and battles a ruthless new lord to secure his legacy.",
                        rating = "9.3",
                        duration = "125 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "Cinema",
                        year = "2026",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_cinema_04",
                        title = "Star Wars: The Mandalorian and Grogu",
                        poster = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=1200",
                        description = "The legendary bounty hunter and his powerful ward Grogu set out on a new cosmic journey through the outer rim.",
                        rating = "9.7",
                        duration = "132 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "Cinema",
                        year = "2026",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_cinema_05",
                        title = "The Furious",
                        poster = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=1200",
                        description = "High-octane action thriller highlighting a street racer who enters a dangerous underworld after a family tragedy.",
                        rating = "9.5",
                        duration = "110 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_10MB.mp4",
                        category = "Cinema",
                        year = "2026",
                        quality = "8K"
                    )
                )
            ),
            Category(
                id = "cat_hindi_mb",
                name = "Hindi",
                icon = "face",
                items = listOf(
                    MovieItem(
                        id = "mb_hindi_01",
                        title = "Wednesday [Hindi]",
                        poster = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=1200",
                        description = "The highly anticipated Hindi dubbed version of Wednesday Addams' dark, mystery-filled adventure.",
                        rating = "9.4",
                        duration = "52 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "Hindi",
                        year = "2025",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_hindi_02",
                        title = "Animal [Hindi]",
                        poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1200",
                        description = "An intense high-drama family saga of power, loyalty, and retribution in the underworld in Hindi.",
                        rating = "9.1",
                        duration = "165 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "Hindi",
                        year = "2024",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_hindi_03",
                        title = "Kalki 2898 AD [Hindi]",
                        poster = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?q=80&w=1200",
                        description = "A futuristic sci-fi epic inspired by ancient mythology, depicting the arrival of a divine avatar in Hindi.",
                        rating = "9.3",
                        duration = "172 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "Hindi",
                        year = "2024",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_hindi_04",
                        title = "Jawan [Hindi]",
                        poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=1200",
                        description = "A high-octane emotional thriller detailing a man's struggle to correct the wrongs in society with a team of skilled women.",
                        rating = "9.2",
                        duration = "168 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "Hindi",
                        year = "2023",
                        quality = "4K"
                    )
                )
            ),
            Category(
                id = "cat_hollywood_mb",
                name = "Hollywood",
                icon = "movie",
                items = listOf(
                    MovieItem(
                        id = "mb_holly_01",
                        title = "Enola Holmes 3",
                        poster = "https://images.unsplash.com/photo-1563089145-599997674d42?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1563089145-599997674d42?q=80&w=1200",
                        description = "Enola takes on her most complex case yet, involving a network of elite conspiracies in Victorian London.",
                        rating = "9.0",
                        duration = "115 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "Hollywood",
                        year = "2025",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_holly_02",
                        title = "Dune: Prophecy",
                        poster = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=1200",
                        description = "Set 10,000 years before the rise of Paul Atreides, tracing the origins of the legendary Bene Gesserit sisterhood.",
                        rating = "9.5",
                        duration = "60 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "Hollywood",
                        year = "2025",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_holly_03",
                        title = "Peaky Blinders",
                        poster = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?q=80&w=1200",
                        description = "A gangster family epic set in Birmingham, England in 1919, centered on a gang led by Tommy Shelby.",
                        rating = "9.4",
                        duration = "60 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "Hollywood",
                        year = "2013",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_holly_04",
                        title = "Tulsa King",
                        poster = "https://images.unsplash.com/photo-1531315630201-bb15abeb1653?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1531315630201-bb15abeb1653?q=80&w=1200",
                        description = "A mafia capo is exiled to Tulsa, Oklahoma, where he builds a new criminal empire with unlikely allies.",
                        rating = "9.1",
                        duration = "45 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "Hollywood",
                        year = "2022",
                        quality = "4K"
                    )
                )
            ),
            Category(
                id = "cat_south_indian_mb",
                name = "South Indian",
                icon = "star",
                items = listOf(
                    MovieItem(
                        id = "mb_south_01",
                        title = "Pushpa 2: The Rule",
                        poster = "https://images.unsplash.com/photo-1444492412393-5510b1a27e7f?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1444492412393-5510b1a27e7f?q=80&w=1200",
                        description = "The epic conclusion to Pushpa's rise through the red sandalwood smuggling empire, facing fierce opposition.",
                        rating = "9.6",
                        duration = "168 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "South Indian",
                        year = "2025",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_south_02",
                        title = "Devara: Part 1",
                        poster = "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?q=80&w=1200",
                        description = "An intense action chronicle depicting coastal lands where an iron-willed protector fights to protect his people.",
                        rating = "9.0",
                        duration = "158 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "South Indian",
                        year = "2024",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_south_03",
                        title = "Salaar: Ceasefire",
                        poster = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=1200",
                        description = "In the lawless city of Khansaar, a commander goes to extreme lengths to protect his childhood friend.",
                        rating = "9.1",
                        duration = "175 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "South Indian",
                        year = "2024",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_south_04",
                        title = "Leo: Born to Rule",
                        poster = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1200",
                        description = "A mild-mannered cafe owner becomes a local hero, but his actions trigger ghosts from a dark criminal past.",
                        rating = "9.2",
                        duration = "164 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "South Indian",
                        year = "2023",
                        quality = "4K"
                    )
                )
            ),
            Category(
                id = "cat_asian_mb",
                name = "Asian",
                icon = "language",
                items = listOf(
                    MovieItem(
                        id = "mb_asian_01",
                        title = "The East Palace",
                        poster = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=1200",
                        description = "A classic Chinese historical drama outlining intrigue, forbidden romance, and the rise of a new general.",
                        rating = "9.2",
                        duration = "45 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "Asian",
                        year = "2026",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_asian_02",
                        title = "My Idol, My Debut",
                        poster = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=1200",
                        description = "Follow five young music trainees through trials and triumphs in their quest to become the next global K-Pop group.",
                        rating = "8.9",
                        duration = "40 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "Asian",
                        year = "2025",
                        quality = "HD+"
                    ),
                    MovieItem(
                        id = "mb_asian_03",
                        title = "Love Has Fireworks",
                        poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1200",
                        description = "A heartwarming Asian romance mapping two strong-willed professionals who clash in business but fall in love.",
                        rating = "9.0",
                        duration = "45 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "Asian",
                        year = "2025",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_asian_04",
                        title = "Reborn Rookie",
                        poster = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                        description = "An aging executive gets a second chance at life when he is reborn as the rookie employee in his old firm.",
                        rating = "9.1",
                        duration = "50 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "Asian",
                        year = "2025",
                        quality = "4K"
                    )
                )
            ),
            Category(
                id = "cat_sports_mb",
                name = "Sports",
                icon = "sports_soccer",
                items = listOf(
                    MovieItem(
                        id = "mb_sports_01",
                        title = "WWE Night of Champions 2026",
                        poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1200",
                        description = "Relive the absolute best matchups, incredible title contests, and legendary high-flying wrestling action.",
                        rating = "9.5",
                        duration = "180 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "Sports",
                        year = "2026",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_sports_02",
                        title = "All American: Season 8",
                        poster = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                        description = "A rising high school football player from South LA is recruited to play for Beverly Hills High, bringing cultural crashes.",
                        rating = "9.2",
                        duration = "45 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "Sports",
                        year = "2018",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_sports_03",
                        title = "World Football Championship Highlights",
                        poster = "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?q=80&w=1200",
                        description = "The absolute best goals, spectacular saves, and legendary moments from the world's biggest football matches.",
                        rating = "9.7",
                        duration = "5 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "Sports",
                        year = "2026",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_sports_04",
                        title = "Extreme Mountain Biking",
                        poster = "https://images.unsplash.com/photo-1444492412393-5510b1a27e7f?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1444492412393-5510b1a27e7f?q=80&w=1200",
                        description = "Adrenaline-fueled adventure capturing riders as they tackle vertical cliffs and drop-offs at breakneck speeds.",
                        rating = "9.4",
                        duration = "10 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "Sports",
                        year = "2025",
                        quality = "4K"
                    )
                )
            ),
            Category(
                id = "cat_cartoons_mb",
                name = "Cartoons",
                icon = "toys",
                items = listOf(
                    MovieItem(
                        id = "mb_cart_01",
                        title = "X-Men '97",
                        poster = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=1200",
                        description = "The legendary band of mutants returns to protect a world that hates and fears them in this nostalgic animated sequel.",
                        rating = "9.6",
                        duration = "30 min",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        category = "Cartoons",
                        year = "2024",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_cart_02",
                        title = "Rick and Morty",
                        poster = "https://images.unsplash.com/photo-1563089145-599997674d42?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1563089145-599997674d42?q=80&w=1200",
                        description = "An eccentric, super-genius scientist drags his timid grandson on wild, dangerous, multi-dimensional space adventures.",
                        rating = "9.4",
                        duration = "22 min",
                        videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4",
                        category = "Cartoons",
                        year = "2024",
                        quality = "4K"
                    ),
                    MovieItem(
                        id = "mb_cart_03",
                        title = "Mushoku Tensei: Jobless Reincarnation",
                        poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1200",
                        description = "An unaccomplished man is reborn into a magical medieval fantasy world, retaining his memories and seeking a fresh start.",
                        rating = "9.1",
                        duration = "24 min",
                        videoUrl = "https://www.w3schools.com/html/movie.mp4",
                        category = "Cartoons",
                        year = "2022",
                        quality = "8K"
                    ),
                    MovieItem(
                        id = "mb_cart_04",
                        title = "Big Buck Bunny",
                        poster = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=600",
                        backdrop = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=1200",
                        description = "A classic animated adventure featuring a giant, gentle rabbit who decides to teach three mischievous forest rodents a lesson.",
                        rating = "8.8",
                        duration = "10 min",
                        videoUrl = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_10MB.mp4",
                        category = "Cartoons",
                        year = "2024",
                        quality = "HD+"
                    )
                )
            )
        )

        // Combine categories, avoiding duplicate category names
        val originalCategories = originalResponse.categories.toMutableList()
        movieboxCategories.forEach { movieboxCat ->
            val index = originalCategories.indexOfFirst { it.name.equals(movieboxCat.name, ignoreCase = true) }
            if (index != -1) {
                // If category already exists, merge the items
                val combinedItems = (originalCategories[index].items + movieboxCat.items).distinctBy { it.id }
                originalCategories[index] = originalCategories[index].copy(items = combinedItems)
            } else {
                // Otherwise, add the new moviebox category
                originalCategories.add(movieboxCat)
            }
        }

        return originalResponse.copy(categories = originalCategories)
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
            ),
            MovieItem(
                id = "dra_02",
                title = "Whispers of the Golden Hour",
                poster = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=600&auto=format&fit=crop",
                backdrop = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=1200&auto=format&fit=crop",
                description = "As the sun sets on a historic coastal village, two childhood friends uncover long-buried family secrets that will change their destiny.",
                rating = "9.1",
                duration = "3 min",
                videoUrl = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_10MB.mp4",
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
