package com.salmanlaghari.risingflix.data

import java.text.Normalizer
import java.util.Locale

/** Catalog transformations that use measured durations when known without hiding catalog entries. */
internal object CatalogResolver {
    const val MIN_FEATURE_SECONDS = 45 * 60

    fun isVerifiedDirectStream(url: String, actualDurationSeconds: Int?): Boolean =
        PlaybackUrlResolver.isDirectStream(url) && actualDurationSeconds != null && actualDurationSeconds > 0

    fun isPlayableDirectStream(url: String): Boolean = PlaybackUrlResolver.isDirectStream(url)

    fun isVerifiedFeatureStream(url: String, actualDurationSeconds: Int?): Boolean =
        isVerifiedDirectStream(url, actualDurationSeconds) && actualDurationSeconds!! >= MIN_FEATURE_SECONDS

    fun isEligibleCatalogItem(item: MovieItem): Boolean = classify(item) != null

    /** Keep the complete catalog in its original categories and annotate stream certainty. */
    fun filterEligibleItems(items: List<MovieItem>): List<MovieItem> = items.mapNotNull(::classify)

    /** Optional strict feature-only view; normal catalog and browse feeds retain all items. */
    fun filterFeatureItems(items: List<MovieItem>): List<MovieItem> =
        items.mapNotNull(::classify).filter { it.contentType == "feature" }

    fun removeDemoAndShortForm(response: ContentResponse): ContentResponse {
        val categories = response.categories.mapNotNull { category ->
            val items = category.items.mapNotNull(::classify)
            items.takeIf { it.isNotEmpty() }?.let { category.copy(items = it) }
        }
        val requestedFeatured = response.featured?.let(::classify)
            ?.takeIf { it.contentType == "feature" }
        val featured = requestedFeatured
            ?: categories.firstOrNull { it.name.equals("Movies", ignoreCase = true) }
                ?.items?.firstOrNull { it.contentType == "feature" }
        return ContentResponse(featured = featured, categories = categories)
    }

    /**
     * A title page is not a stream. Reuse a direct URL only when its actual duration has
     * already been measured as feature-length and the title/year match unambiguously.
     */
    fun enrichDetailPagesWithDirectStreams(
        listing: ContentResponse,
        knownCatalogs: List<ContentResponse>
    ): ContentResponse {
        val directItems = knownCatalogs
            .flatMap { it.categories.flatMap(Category::items) + listOfNotNull(it.featured) }
            .filter { isVerifiedFeatureStream(it.videoUrl, it.actualDurationSeconds) }
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
                duration = direct.safeDuration,
                actualDurationSeconds = direct.actualDurationSeconds,
                contentType = "feature",
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

    fun formatDurationSeconds(seconds: Int): String {
        if (seconds <= 0) return "Runtime unverified"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return when {
            hours > 0 && minutes == 0 && remainingSeconds == 0 -> "$hours h"
            hours > 0 && minutes == 0 -> "$hours h $remainingSeconds sec"
            hours > 0 && remainingSeconds > 0 -> "$hours h $minutes min $remainingSeconds sec"
            hours > 0 -> "$hours h $minutes min"
            minutes > 0 && remainingSeconds > 0 -> "$minutes min $remainingSeconds sec"
            minutes > 0 -> "$minutes min"
            else -> "$remainingSeconds sec"
        }
    }

    private fun classify(item: MovieItem): MovieItem? {
        if (item.id.startsWith("auto_", ignoreCase = true)) return null
        if (PlaybackUrlResolver.isEmbeddedPage(item.videoUrl)) {
            return item.copy(contentType = "details_only")
        }
        if (!isPlayableDirectStream(item.videoUrl)) {
            return if (PlaybackUrlResolver.isValidHttpUrl(item.videoUrl)) {
                item.copy(contentType = "details_only")
            } else {
                null
            }
        }
        val measuredSeconds = item.actualDurationSeconds?.takeIf { it > 0 }
        return when {
            measuredSeconds == null -> item.copy(contentType = "runtime_unverified")
            measuredSeconds < MIN_FEATURE_SECONDS -> item.copy(contentType = "short_clip")
            else -> item.copy(contentType = "feature")
        }
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
