package com.salmanlaghari.risingflix

import com.google.gson.Gson
import com.salmanlaghari.risingflix.data.ContentResponse
import org.junit.Assert.*
import org.junit.Test

class ContentResponseParsingTest {

    @Test
    fun testContentResponse_deserializesCorrectly() {
        val json = """
            {
              "featured": {
                "id": "feat_01",
                "title": "Epic Space Odyssey",
                "poster": "https://example.com/image.jpg",
                "backdrop": "https://example.com/image_back.jpg",
                "description": "Featured test movie description",
                "rating": "9.8",
                "duration": "12 min",
                "videoUrl": "https://example.com/video.mp4",
                "releaseYear": "2026",
                "quality": "8K"
              },
              "categories": [
                {
                  "id": "cat_mov",
                  "name": "Movies",
                  "icon": "movie",
                  "items": [
                    {
                      "id": "mov_01",
                      "title": "Sintel",
                      "poster": "https://example.com/sintel.jpg",
                      "backdrop": "https://example.com/sintel_back.jpg",
                      "description": "Sintel test movie",
                      "rating": "9.2",
                      "duration": "2 h 14 m",
                      "actualDurationSeconds": 138,
                      "contentType": "short_clip",
                      "videoUrl": "https://example.com/sintel.mp4",
                      "releaseYear": "2026",
                      "quality": "8K"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val gson = Gson()
        val response = gson.fromJson(json, ContentResponse::class.java)

        assertNotNull("Response should be successfully deserialized", response)
        assertNotNull("Featured item should not be null", response.featured)
        assertEquals("Epic Space Odyssey", response.featured?.title)
        assertEquals("8K", response.featured?.quality)

        assertEquals(1, response.categories.size)
        val category = response.categories.first()
        assertEquals("Movies", category.name)
        assertEquals(1, category.items.size)

        val item = category.items.first()
        assertEquals("mov_01", item.id)
        assertEquals("Sintel", item.title)
        assertEquals("https://example.com/sintel.mp4", item.videoUrl)
        assertEquals("9.2", item.rating)
        assertEquals("2 h 14 m", item.duration)
        assertEquals(138, item.actualDurationSeconds ?: -1)
        assertEquals("2 min 18 sec", item.safeDuration)
        assertEquals("Sintel (short clip)", item.displayTitle)
        assertEquals("https://example.com/sintel.jpg", item.safePoster)
        assertEquals("2026", item.safeReleaseYear)
    }

    @Test
    fun posterAliasesAndBackdropFallbackAreSupported() {
        val gson = Gson()
        val posterAlias = gson.fromJson(
            """{"id":"poster","title":"Poster","poster":"https://example.org/poster.jpg","videoUrl":"https://example.org/poster.mp4"}""",
            com.salmanlaghari.risingflix.data.MovieItem::class.java
        )
        val coverAlias = gson.fromJson(
            """{"id":"cover","title":"Cover","coverUrl":"https://example.org/cover.jpg","backdropUrl":"https://example.org/backdrop.jpg","videoUrl":"https://example.org/cover.mp4","releaseYear":"1999"}""",
            com.salmanlaghari.risingflix.data.MovieItem::class.java
        )
        val backdropFallback = gson.fromJson(
            """{"id":"backdrop","title":"Backdrop","thumbnailUrl":"","backdrop":"https://example.org/backdrop-only.jpg","videoUrl":"https://example.org/backdrop.mp4"}""",
            com.salmanlaghari.risingflix.data.MovieItem::class.java
        )

        assertEquals("https://example.org/poster.jpg", posterAlias.safePoster)
        assertEquals("https://example.org/cover.jpg", coverAlias.safePoster)
        assertEquals("https://example.org/backdrop.jpg", coverAlias.safeBackdrop)
        assertEquals("1999", coverAlias.safeReleaseYear)
        assertEquals("https://example.org/backdrop-only.jpg", backdropFallback.safePoster)
    }
}
