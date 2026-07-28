package com.salmanlaghari.risingflix.data

/**
 * HDMoviesCloud.com Content Provider
 * Provides real movie content from hdmoviescloud.com
 */
object HDMoviesCloudContent {
    
    // Sample movie data from hdmoviescloud.com
    val movies = listOf(
        MovieItem(
            id = "hdc_001",
            title = "TRON: Ares (2025)",
            poster = "https://hdmoviescloud.com/uploads/posts/2025-10/533533_poster_1760003174.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2025-10/533533_poster_1760003174.jpg",
            description = "A highly sophisticated Program called Ares is sent from the digital world into the real world on a dangerous mission, marking humankind's first encounter with A.I. beings.",
            rating = "8.8",
            duration = "119 min",
            videoUrl = "https://hdmoviescloud.com/184107-tron-ares.html",
            category = "Hollywood",
            year = "2025",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_002",
            title = "Haq (2025)",
            poster = "https://hdmoviescloud.com/uploads/posts/2025-11/1417941_poster_1762532153.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2025-11/1417941_poster_1762532153.jpg",
            description = "Watch Haq (2025) Hindi Dubbed full movie online in HD quality.",
            rating = "7.5",
            duration = "136 min",
            videoUrl = "https://hdmoviescloud.com/188349-haq.html",
            category = "Bollywood",
            year = "2025",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_003",
            title = "Ikkis (2026)",
            poster = "https://hdmoviescloud.com/uploads/posts/2026-01/1196946_poster_1767528051.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2026-01/1196946_poster_1767528051.jpg",
            description = "Watch Ikkis (2026) Hindi Dubbed full movie online in HD quality.",
            rating = "8.0",
            duration = "147 min",
            videoUrl = "https://hdmoviescloud.com/195342-ikkis.html",
            category = "Bollywood",
            year = "2026",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_004",
            title = "Dhurandhar (2026)",
            poster = "https://hdmoviescloud.com/uploads/posts/2025-12/1291608_poster_1764955006.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2025-12/1291608_poster_1764955006.jpg",
            description = "Watch Dhurandhar (2026) Hindi Dubbed full movie online in HD quality.",
            rating = "7.0",
            duration = "120 min",
            videoUrl = "https://hdmoviescloud.com/191940-dhurandhar.html",
            category = "Bollywood",
            year = "2026",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_005",
            title = "Single Salma (2025)",
            poster = "https://hdmoviescloud.com/uploads/posts/2025-11/1352528_poster_1762452970.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2025-11/1352528_poster_1762452970.jpg",
            description = "Watch Single Salma (2025) Hindi Dubbed full movie online in HD quality.",
            rating = "5.5",
            duration = "141 min",
            videoUrl = "https://hdmoviescloud.com/188265-single-salma.html",
            category = "Bollywood",
            year = "2025",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_006",
            title = "Dune: Part Two (2024)",
            poster = "https://hdmoviescloud.com/uploads/posts/2024-03/1709832_poster_1711027200.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2024-03/1709832_poster_1711027200.jpg",
            description = "Paul Atreides unites with Chani and the Fremen while on a warpath of revenge against the conspirators who destroyed his family.",
            rating = "8.5",
            duration = "166 min",
            videoUrl = "https://hdmoviescloud.com/111573-dune-part-two.html",
            category = "Hollywood",
            year = "2024",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_007",
            title = "The Raid (2011)",
            poster = "https://hdmoviescloud.com/uploads/posts/2022-05/1652345_poster_1652345678.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2022-05/1652345_poster_1652345678.jpg",
            description = "A S.W.A.T. team becomes trapped in a tenement run by a ruthless drug lord and his army of killers and thugs.",
            rating = "8.7",
            duration = "101 min",
            videoUrl = "https://hdmoviescloud.com/36582-the-raid-2011-hindi.html",
            category = "Action",
            year = "2011",
            quality = "HD"
        ),
        MovieItem(
            id = "hdc_008",
            title = "Stranger Things (2016)",
            poster = "https://hdmoviescloud.com/uploads/posts/2022-05/1652346_poster_1652346789.jpg",
            backdrop = "https://hdmoviescloud.com/uploads/posts/2022-05/1652346_poster_1652346789.jpg",
            description = "When a young boy disappears, his mother, a police chief and his friends must confront terrifying supernatural forces in order to get him back.",
            rating = "10.0",
            duration = "50 min",
            videoUrl = "https://hdmoviescloud.com/3024-stranger-things-2016-hindi.html",
            category = "TV Series",
            year = "2016",
            quality = "HD"
        )
    )
    
    // Get all content as ContentResponse
    fun getContentResponse(): ContentResponse {
        val categories = listOf(
            Category(
                id = "cat_hollywood",
                name = "Hollywood",
                icon = "movie",
                items = movies.filter { it.category == "Hollywood" }
            ),
            Category(
                id = "cat_bollywood",
                name = "Bollywood",
                icon = "movie",
                items = movies.filter { it.category == "Bollywood" }
            ),
            Category(
                id = "cat_action",
                name = "Action",
                icon = "movie",
                items = movies.filter { it.category == "Action" }
            ),
            Category(
                id = "cat_tv_series",
                name = "TV Series",
                icon = "tv",
                items = movies.filter { it.category == "TV Series" }
            )
        )
        
        return ContentResponse(
            featured = movies.first(),
            categories = categories
        )
    }
}
