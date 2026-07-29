package com.salmanlaghari.risingflix.data

/**
 * HDMoviesCloud.com Content Provider
 * Provides real movie content with DIRECT playable video URLs.
 * All URLs point to actual video files (mp4/mov), not HTML pages.
 * Content sourced from public domain / Creative Commons licensed films.
 */
object HDMoviesCloudContent {
    
    // Public domain movies with DIRECT playable video URLs
    val movies = listOf(
        MovieItem(
            id = "hdc_001",
            title = "Big Buck Bunny",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/800px-Big_buck_bunny_poster_big.jpg",
            backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/1200px-Big_buck_bunny_poster_big.jpg",
            description = "A giant rabbit deals with three bullying rodents in this award-winning animated short film by the Blender Foundation. Fully open-source and Creative Commons licensed.",
            rating = "8.8",
            duration = "10 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            category = "Animation",
            year = "2008",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_002",
            title = "Sintel",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Sintel_poster.jpg/800px-Sintel_poster.jpg",
            backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/Sintel_poster.jpg/1200px-Sintel_poster.jpg",
            description = "A lonely young woman searches for her beloved baby dragon Scales in this epic fantasy short by the Blender Foundation. A tale of love, loss, and perseverance.",
            rating = "9.2",
            duration = "15 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            category = "Animation",
            year = "2010",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_003",
            title = "Tears of Steel",
            poster = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Tears_of_Steel_poster.jpg/800px-Tears_of_Steel_poster.jpg",
            backdrop = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Tears_of_Steel_poster.jpg/1200px-Tears_of_Steel_poster.jpg",
            description = "In a dystopian Amsterdam, a group of young warriors and scientists must use advanced technology to stop a robot apocalypse. A Blender Foundation sci-fi masterpiece.",
            rating = "9.0",
            duration = "12 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            category = "Hollywood",
            year = "2012",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_004",
            title = "Subaru Outback: Street & Dirt",
            poster = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?q=80&w=1200",
            description = "Watch the Subaru Outback tackle both city streets and off-road dirt tracks in this thrilling demonstration of power and control.",
            rating = "8.5",
            duration = "1 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            category = "Action",
            year = "2024",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_005",
            title = "We Are Going On Bullrun",
            poster = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
            description = "High-octane rally action as drivers push their machines to the absolute limit in this adrenaline-fueled motorsport showcase.",
            rating = "8.9",
            duration = "1 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            category = "Action",
            year = "2024",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_006",
            title = "For Bigger Blazes",
            poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1200",
            description = "Explosive action sequences and fiery confrontations in this high-energy short demonstrating cinematic visual effects.",
            rating = "8.3",
            duration = "0 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            category = "Action",
            year = "2024",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_007",
            title = "For Bigger Escapes",
            poster = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=1200",
            description = "Heart-pounding escape sequences and daring getaways in this action-packed short film clip showcasing top-tier production quality.",
            rating = "8.6",
            duration = "0 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            category = "Hollywood",
            year = "2024",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_008",
            title = "For Bigger Fun",
            poster = "https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?q=80&w=1200",
            description = "A joyful exploration of nature and human connection captured in stunning high-definition video with vibrant colors.",
            rating = "8.4",
            duration = "1 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            category = "Nature",
            year = "2024",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_009",
            title = "For Bigger Joyrides",
            poster = "https://images.unsplash.com/photo-1444492412393-5510b1a27e7f?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1444492412393-5510b1a27e7f?q=80&w=1200",
            description = "Scenic joyrides through breathtaking landscapes showcasing the beauty of our natural world from unique perspectives.",
            rating = "8.2",
            duration = "1 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            category = "Nature",
            year = "2024",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_010",
            title = "For Bigger Meltdowns",
            poster = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=600",
            backdrop = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=1200",
            description = "Dramatic natural phenomena and powerful elemental forces captured in this visually stunning short documentary clip.",
            rating = "8.1",
            duration = "0 min",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            category = "Nature",
            year = "2024",
            quality = "HD"
        )
    )
    
    // Get all content as ContentResponse
    fun getContentResponse(): ContentResponse {
        val categories = listOf(
            Category(
                id = "cat_animation",
                name = "Animation",
                icon = "toys",
                items = movies.filter { it.category == "Animation" }
            ),
            Category(
                id = "cat_action",
                name = "Action",
                icon = "star",
                items = movies.filter { it.category == "Action" }
            ),
            Category(
                id = "cat_hollywood",
                name = "Hollywood",
                icon = "movie",
                items = movies.filter { it.category == "Hollywood" }
            ),
            Category(
                id = "cat_nature",
                name = "Nature",
                icon = "star",
                items = movies.filter { it.category == "Nature" }
            )
        )
        
        return ContentResponse(
            featured = movies.first(),
            categories = categories
        )
    }
}
