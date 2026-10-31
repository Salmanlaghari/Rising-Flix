package com.salmanlaghari.risingflix.data

import java.text.Normalizer
import java.util.Locale

/** Catalog transformations that classify media from measured stream duration, never title metadata. */
internal object CatalogResolver {
    const val MIN_FEATURE_SECONDS = 45 * 60

    fun isVerifiedDirectStream(url: String, actualDurationSeconds: Int?): Boolean =
        PlaybackUrlResolver.isDirectStream(url) && actualDurationSeconds != null && actualDurationSeconds > 0

    fun isVerifiedFeatureStream(url: String, actualDurationSeconds: Int?): Boolean =
        isVerifiedDirectStream(url, actualDurationSeconds) && actualDurationSeconds!! >= MIN_FEATURE_SECONDS

    fun isEligibleCatalogItem(item: MovieItem): Boolean = classify(item) != null

    /** Retain measured short media and page-only records, but explicitly classify them. */
    fun filterEligibleItems(items: List<MovieItem>): List<MovieItem> = items.mapNotNull(::classify)

    /** Used by feeds that must contain only feature-length movies, not clips or detail pages. */
    fun filterFeatureItems(items: List<MovieItem>): List<MovieItem> =
        items.mapNotNull(::classify).filter { it.contentType == "feature" }

    fun removeDemoAndShortForm(response: ContentResponse): ContentResponse {
        val featureCategories = mutableListOf<Category>()
        val shortClips = linkedMapOf<String, MovieItem>()
        val providerListings = linkedMapOf<String, MovieItem>()

        for (category in response.categories) {
            val features = mutableListOf<MovieItem>()
            for (item in category.items) {
                val normalized = classify(item) ?: continue
                when (normalized.contentType) {
                    "feature" -> features += normalized
                    "short_clip" -> shortClips.putIfAbsent(normalized.videoUrl, normalized)
                    "details_only" -> providerListings.putIfAbsent(
                        normalized.videoUrl.ifBlank { normalized.id }, normalized
                    )
                }
            }
            if (features.isNotEmpty() && !category.name.equals("Short Clips", ignoreCase = true) &&
                !category.name.equals("Provider Listings", ignoreCase = true)
            ) {
                featureCategories += category.copy(items = features)
            }
        }

        if (shortClips.isNotEmpty()) {
            featureCategories += Category(
                id = "cat_short_clips",
                name = "Short Clips",
                icon = "video",
                items = shortClips.values.toList()
            )
        }
        if (providerListings.isNotEmpty()) {
            featureCategories += Category(
                id = "cat_provider_listings",
                name = "Provider Listings",
                icon = "info",
                items = providerListings.values.toList()
            )
        }

        val requestedFeatured = response.featured?.let(::classify)
            ?.takeIf { it.contentType == "feature" }
        val featured = requestedFeatured
            ?: featureCategories.firstOrNull { it.name.equals("Movies", ignoreCase = true) }
                ?.items?.firstOrNull { it.contentType == "feature" }
        return ContentResponse(featured = featured, categories = featureCategories)
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
            return item.copy(contentType = "details_only", category = "Provider Listings")
        }
        if (!isVerifiedDirectStream(item.videoUrl, item.actualDurationSeconds)) return null
        return if (isVerifiedFeatureStream(item.videoUrl, item.actualDurationSeconds)) {
            item.copy(contentType = "feature")
        } else {
            item.copy(contentType = "short_clip", category = "Short Clips")
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
