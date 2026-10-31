package com.salmanlaghari.risingflix.data

import java.text.Normalizer
import java.util.Locale

/** Small, deterministic catalog transformations shared by foreground and background sync. */
internal object CatalogResolver {
    private const val MIN_FEATURE_MINUTES = 45

    fun removeDemoAndShortForm(response: ContentResponse): ContentResponse {
        val categories = response.categories.mapNotNull { category ->
            val items = category.items.filterNot { item ->
                item.id.startsWith("auto_", ignoreCase = true) ||
                    durationMinutes(item.duration)?.let { it < MIN_FEATURE_MINUTES } == true
            }
            if (items.isEmpty()) null else category.copy(items = items)
        }
        val featured = response.featured?.takeUnless { item ->
            item.id.startsWith("auto_", ignoreCase = true) ||
                durationMinutes(item.duration)?.let { it < MIN_FEATURE_MINUTES } == true
        } ?: categories.firstOrNull { it.name.contains("movie", ignoreCase = true) }?.items?.firstOrNull()
            ?: categories.firstOrNull()?.items?.firstOrNull()
        return ContentResponse(featured = featured, categories = categories)
    }

    /**
     * A title page is not a stream. If the maintained catalog already contains a direct URL
     * for the same unambiguous title, reuse that URL while retaining the live listing metadata.
     */
    fun enrichDetailPagesWithDirectStreams(
        listing: ContentResponse,
        knownCatalogs: List<ContentResponse>
    ): ContentResponse {
        val directItems = knownCatalogs
            .flatMap { it.categories.flatMap(Category::items) + listOfNotNull(it.featured) }
            .filter { PlaybackUrlResolver.isDirectStream(it.videoUrl) }
            .distinctBy(MovieItem::id)
        val byTitle = directItems.groupBy { normalizeTitle(it.title) }.filterKeys { it.isNotEmpty() }

        fun enrich(item: MovieItem): MovieItem {
            if (!PlaybackUrlResolver.isEmbeddedPage(item.videoUrl)) return item
            val candidates = byTitle[normalizeTitle(item.title)].orEmpty()
            val direct = when {
                item.year.isNullOrBlank() -> candidates.singleOrNull()
                else -> candidates.firstOrNull { it.year.equals(item.year, ignoreCase = true) }
                    ?: candidates.singleOrNull { it.year.isNullOrBlank() }
            } ?: return item

            return item.copy(
                poster = item.poster.nonBlankOr(direct.poster),
                backdrop = item.backdrop.nonBlankOr(direct.backdrop),
                description = item.description.nonBlankOr(direct.description),
                rating = item.rating.nonBlankOr(direct.rating),
                duration = item.duration.nonBlankOr(direct.duration),
                videoUrl = direct.videoUrl,
                year = item.year.nonBlankOr(direct.year),
                quality = item.quality.nonBlankOr(direct.quality)
            )
        }

        return listing.copy(
            featured = listing.featured?.let(::enrich),
            categories = listing.categories.map { category ->
                category.copy(items = category.items.map(::enrich))
            }
        )
    }

    fun sameCatalogTitle(first: MovieItem, second: MovieItem): Boolean {
        if (normalizeTitle(first.title).isEmpty() || normalizeTitle(first.title) != normalizeTitle(second.title)) {
            return false
        }
        val firstYear = first.year.nonBlankOrNull()
        val secondYear = second.year.nonBlankOrNull()
        return firstYear == null || secondYear == null || firstYear == secondYear
    }

    fun durationMinutes(value: String?): Int? {
        val raw = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (raw.isEmpty()) return null

        val clock = Regex("^(\\d{1,2}):(\\d{2})(?::(\\d{2}))?$").matchEntire(raw)
        if (clock != null) {
            val parts = clock.groupValues.drop(1).filter(String::isNotEmpty).map(String::toInt)
            return if (parts.size == 3) parts[0] * 60 + parts[1] else parts[0]
        }

        val hours = Regex("(\\d+)\\s*(?:h|hr|hrs|hour|hours)\\b").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = Regex("(\\d+)\\s*(?:m|min|mins|minute|minutes)\\b").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val seconds = Regex("(\\d+)\\s*(?:s|sec|secs|second|seconds)\\b").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val secondsAsMinutes = if (seconds > 0) maxOf(1, (seconds + 59) / 60) else 0
        val total = hours * 60 + minutes + secondsAsMinutes
        return total.takeIf { it > 0 }
    }

    private fun normalizeTitle(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun String?.nonBlankOr(fallback: String?): String? = this?.takeIf(String::isNotBlank) ?: fallback
    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
