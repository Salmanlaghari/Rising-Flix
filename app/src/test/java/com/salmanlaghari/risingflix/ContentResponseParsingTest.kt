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
                "category": "Movies",
                "videoUrl": "https://example.com/video.mp4",
                "thumbnailUrl": "https://example.com/image.jpg",
                "description": "Featured test movie description",
                "rating": "9.8",
                "quality": "8K",
                "year": "2026",
                "duration": "12 min"
              },
              "categories": [
                {
                  "name": "Movies",
                  "items": [
                    {
                      "id": "mov_01",
                      "title": "Sintel",
                      "category": "Movies",
                      "videoUrl": "https://example.com/sintel.mp4",
                      "thumbnailUrl": "https://example.com/sintel.jpg",
                      "description": "Sintel test movie",
                      "rating": "9.2",
                      "quality": "8K",
                      "year": "2026",
                      "duration": "14 min"
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
        assertEquals("Movies", response.featured?.category)
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
    }
}
