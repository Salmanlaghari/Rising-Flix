package com.salmanlaghari.risingflix.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import com.google.gson.JsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonElement

object TheMovieBoxContent {

    private const val TAG = "TheMovieBoxContent"
    private const val BASE_URL = "https://themoviebox.xyz"

    private val USER_AGENTS = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    )

    suspend fun fetchContent(context: android.content.Context): ContentResponse? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(BASE_URL)
                .userAgent(USER_AGENTS.random())
                .timeout(15000)
                .get()

            val nuxtScript = doc.selectFirst("script[id=__NUXT_DATA__]")
            val nuxtScriptContent = nuxtScript?.html()

            if (nuxtScriptContent == null || nuxtScriptContent.isEmpty()) {
                Log.w(TAG, "No NUXXT_DATA found on themoviebox.xyz")
                return@withContext loadFallbackContent(context)
            }

            val jsonArray = JsonParser.parseString(nuxtScriptContent).asJsonArray
            val allCategories = mutableListOf<Category>()
            val allItems = mutableListOf<MovieItem>()
            val processedSubjectIds = mutableSetOf<String>()

            fun scanForContent(obj: JsonElement) {
                if (obj.isJsonArray) {
                    val arr = obj.asJsonArray
                    if (arr.size() > 0 && arr[0].isJsonObject) {
                        val first = arr[0].asJsonObject
                        if (first.has("subjectId") && first.has("subject")) {
                            val categoryName = extractCategoryName(arr)
                            val itemsList = mutableListOf<MovieItem>()

                            for (i in 0 until arr.size()) {
                                val item = arr[i].asJsonObject
                                val subjectId = item["subjectId"]?.asString ?: continue
                                if (processedSubjectIds.contains(subjectId)) continue
                                processedSubjectIds.add(subjectId)

                                val subjectRef = item["subject"]
                                val subject = if (subjectRef != null && subjectRef.isJsonPrimitive && subjectRef.asJsonPrimitive.isNumber) {
                                    val idx = subjectRef.asInt
                                    if (idx >= 0 && idx < jsonArray.size()) jsonArray[idx].asJsonObject else null
                                } else null

                                if (subject != null) {
                                    val movieItem = createMovieItem(item, subject, jsonArray, categoryName)
                                    itemsList.add(movieItem)
                                    allItems.add(movieItem)
                                }
                            }

                            if (itemsList.isNotEmpty() && categoryName.isNotBlank()) {
                                allCategories.add(
                                    Category(
                                        id = "cat_tmb_${categoryName.lowercase().replace(" ", "_")}",
                                        name = categoryName,
                                        icon = "movie",
                                        items = itemsList
                                    )
                                )
                            }
                            return
                        }
                    }
                    for (i in 0 until arr.size()) {
                        scanForContent(arr[i])
                    }
                } else if (obj.isJsonObject) {
                    val jsonObj = obj.asJsonObject
                    for (key in jsonObj.keySet()) {
                        scanForContent(jsonObj[key])
                    }
                }
            }

            scanForContent(jsonArray)

            if (allCategories.isNotEmpty()) {
                Log.d(TAG, "Fetched ${allItems.size} items from themoviebox.xyz in ${allCategories.size} categories")
                return@withContext ContentResponse(
                    featured = allItems.firstOrNull(),
                    categories = allCategories
                )
            }

            Log.w(TAG, "No categories found in themoviebox.xyz response")
            loadFallbackContent(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch themoviebox.xyz content", e)
            loadFallbackContent(context)
        }
    }

    private suspend fun loadFallbackContent(context: android.content.Context): ContentResponse? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("themoviebox_content.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val items = com.google.gson.Gson().fromJson(json, Array::class.java)
                ?.mapNotNull { it as? Map<*, *> }
                ?.map { item ->
                    MovieItem(
                        id = "tmb_${item["id"]}",
                        title = item["title"] as? String ?: "",
                        poster = item["poster"] as? String ?: "",
                        backdrop = item["backdrop"] as? String ?: "",
                        description = item["description"] as? String ?: "",
                        rating = item["rating"] as? String ?: "",
                        duration = item["duration"] as? String ?: "",
                        videoUrl = item["videoUrl"] as? String ?: "",
                        category = item["category"] as? String ?: "Movies",
                        year = item["year"] as? String ?: "",
                        quality = item["quality"] as? String ?: "HD"
                    )
                } ?: emptyList()

            val categories = items.groupBy { it.category }.map { (name, items) ->
                Category(
                    id = "cat_tmb_${name.lowercase().replace(" ", "_")}",
                    name = name,
                    icon = "movie",
                    items = items
                )
            }

            ContentResponse(
                featured = items.firstOrNull(),
                categories = categories
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load fallback content", e)
            null
        }
    }

    private fun extractCategoryName(arr: JsonArray): String {
        for (i in 0 until arr.size()) {
            val item = arr[i]
            if (item.isJsonObject) {
                val obj = item.asJsonObject
                if (obj.has("title") && !obj.has("subjectId")) {
                    val title = obj["title"]?.asString
                    if (!title.isNullOrBlank() && title.length < 100) {
                        return title
                    }
                }
            }
        }
        return "Movies"
    }

    private fun createMovieItem(
        item: JsonObject,
        subject: JsonObject,
        jsonArray: JsonArray,
        categoryName: String
    ): MovieItem {
        val sTitle = resolveString(item["title"], jsonArray) ?: resolveString(subject["title"], jsonArray) ?: "Unknown"
        val sDetail = resolveString(subject["detailPath"], jsonArray) ?: ""
        val sDesc = resolveString(subject["description"], jsonArray) ?: ""
        val sGenre = resolveString(subject["genre"], jsonArray) ?: categoryName
        val sRating = resolveString(subject["imdbRatingValue"], jsonArray) ?: "9.0"
        val sDate = resolveString(subject["releaseDate"], jsonArray) ?: ""
        val sYear = if (sDate.length >= 4) sDate.substring(0, 4) else "2026"
        val sCorner = resolveString(subject["corner"], jsonArray) ?: ""
        val sCountry = resolveString(subject["countryName"], jsonArray) ?: ""
        val sDuration = resolveString(subject["duration"], jsonArray) ?: "120 min"

        var sCover = ""
        if (subject.has("cover")) {
            val coverRef = subject["cover"]
            val coverObj = if (coverRef.isJsonPrimitive && coverRef.asJsonPrimitive.isNumber) {
                val cIdx = coverRef.asInt
                if (cIdx >= 0 && cIdx < jsonArray.size()) jsonArray[cIdx].asJsonObject else null
            } else if (coverRef.isJsonObject) {
                coverRef.asJsonObject
            } else null

            if (coverObj != null && coverObj.has("url")) {
                val urlRef = coverObj["url"]
                sCover = if (urlRef.isJsonPrimitive && urlRef.asJsonPrimitive.isNumber) {
                    val urlIdx = urlRef.asInt
                    if (urlIdx >= 0 && urlIdx < jsonArray.size()) {
                        val urlElement = jsonArray[urlIdx]
                        if (urlElement.isJsonPrimitive) urlElement.asString else ""
                    } else ""
                } else {
                    urlRef?.asString ?: ""
                }
            }
        }

        val detailUrl = "$BASE_URL/detail/$sDetail"
        val subjectId = subject["subjectId"]?.asString ?: item["subjectId"]?.asString ?: ""

        return MovieItem(
            id = "tmb_$subjectId",
            title = sTitle,
            poster = sCover,
            backdrop = sCover,
            description = sDesc,
            rating = sRating,
            duration = sDuration,
            videoUrl = detailUrl,
            category = sGenre,
            year = sYear,
            quality = if (sCorner.isNotBlank()) sCorner else "HD"
        )
    }

    private fun resolveString(element: JsonElement?, jsonArray: JsonArray): String {
        if (element == null) return ""
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            val idx = element.asInt
            if (idx >= 0 && idx < jsonArray.size()) {
                val resolved = jsonArray[idx]
                if (resolved != null && resolved.isJsonPrimitive) resolved.asString else ""
            } else ""
        } else if (element.isJsonPrimitive) {
            element.asString
        } else ""
    }
}