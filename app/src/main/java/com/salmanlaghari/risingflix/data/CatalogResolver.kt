package com.salmanlaghari.risingflix.data

import java.text.Normalizer
import java.util.Locale

/** Catalog policy: MovieBox rows are external links; only explicitly licensed verified features play in-app. */
internal object CatalogResolver {
    const val MIN_FEATURE_SECONDS = LicensedMediaPolicy.MIN_FEATURE_SECONDS

    fun isVerifiedDirectStream(url: String, actualDurationSeconds: Int?): Boolean =
        LicensedMediaPolicy.isAuthorizedMediaUrl(LicensedMediaPolicy.VALKAAMA_ID, url) &&
            actualDurationSeconds == LicensedMediaPolicy.VALKAAMA_DURATION_SECONDS

    fun isPlayableDirectStream(url: String): Boolean =
        LicensedMediaPolicy.isAuthorizedMediaUrl(LicensedMediaPolicy.VALKAAMA_ID, url)

    fun isVerifiedFeatureStream(url: String, actualDurationSeconds: Int?): Boolean =
        actualDurationSeconds != null &&
            isVerifiedDirectStream(url, actualDurationSeconds) &&
            actualDurationSeconds >= MIN_FEATURE_SECONDS

    fun isPlayableFeature(item: MovieItem): Boolean = LicensedMediaPolicy.isVerifiedFeature(item)

    fun isEligibleCatalogItem(item: MovieItem): Boolean =
        LicensedMediaPolicy.isVerifiedFeature(item) ||
            MovieBoxLinkResolver.isMovieBoxPage(item.sourcePageUrl) ||
            MovieBoxLinkResolver.isMovieBoxPage(item.videoUrl)

    fun filterEligibleItems(items: List<MovieItem>): List<MovieItem> = items.mapNotNull { item ->
        when {
            LicensedMediaPolicy.isVerifiedFeature(item) -> item.copy(contentType = "feature")
            else -> movieBoxPageOnly(item)
        }
    }

    fun filterFeatureItems(items: List<MovieItem>): List<MovieItem> =
        items.filter { item -> LicensedMediaPolicy.isVerifiedFeature(item) }
            .map { it.copy(contentType = "feature") }

    fun removeDemoAndShortForm(response: ContentResponse): ContentResponse =
        normalizeCatalog(listOf(response))

    /** Combines trusted licensed records with existing MovieBox detail links, never stream URLs. */
    fun normalizeCatalog(
        sources: List<ContentResponse>,
        movieBoxFallback: ContentResponse? = null
    ): ContentResponse {
        val fallbackPages = movieBoxFallback?.let(::movieBoxPageItems).orEmpty()
        val fallbackByTitle = fallbackPages.groupBy { normalizeTitle(it.title) }
        val candidates = sources.flatMap(::allItems) + movieBoxFallback?.let(::allItems).orEmpty()

        val licensed = candidates
            .filter(LicensedMediaPolicy::isVerifiedFeature)
            .distinctBy(MovieItem::id)
            .map { it.copy(contentType = "feature") }

        val pageOnly = candidates.mapNotNull { item ->
            movieBoxPageOnly(item) ?: legacyPageForCachedItem(item, fallbackByTitle)
        }.plus(fallbackPages)

        val categories = mutableListOf<Category>()
        if (licensed.isNotEmpty()) {
            categories += Category(
                id = "open_licensed_features",
                name = "Open-Licensed Features",
                icon = "movie",
                items = licensed
            )
        }
        pageOnly.groupBy { it.category.ifBlank { "Movies" } }
            .toSortedMap()
            .forEach { (originalCategory, items) ->
                val uniqueInCategory = items.distinctBy { it.sourcePageUrl ?: it.videoUrl }
                categories += Category(
                    id = "moviebox_${slug(originalCategory)}",
                    name = "MovieBox · $originalCategory",
                    icon = "open_in_new",
                    items = uniqueInCategory
                )
            }

        return ContentResponse(featured = licensed.firstOrNull(), categories = categories)
    }

    /** Force every MovieBox record into an external detail-page state, even if its old row has an MP4. */
    fun movieBoxPagesOnly(response: ContentResponse): ContentResponse =
        normalizeCatalog(listOf(response))

    fun movieBoxPageItems(response: ContentResponse): List<MovieItem> =
        allItems(response).mapNotNull(::movieBoxPageOnly)
            .distinctBy { it.sourcePageUrl ?: it.videoUrl }

    fun toMovieBoxPageOnly(item: MovieItem): MovieItem? = movieBoxPageOnly(item)

    fun sameCatalogTitle(first: MovieItem, second: MovieItem): Boolean {
        val firstTitle = normalizeTitle(first.title)
        if (firstTitle.isEmpty() || firstTitle != normalizeTitle(second.title)) return false
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

    private fun movieBoxPageOnly(item: MovieItem): MovieItem? {
        if (LicensedMediaPolicy.isVerifiedFeature(item)) return null
        val page = MovieBoxLinkResolver.resolvePage(item.sourcePageUrl)
            ?: MovieBoxLinkResolver.resolvePage(item.videoUrl)
            ?: return null
        return item.copy(
            videoUrl = page,
            sourcePageUrl = page,
            sourceName = "MovieBox",
            duration = "Runtime unverified",
            actualDurationSeconds = null,
            contentType = "details_only",
            licenseName = null,
            licenseUrl = null,
            attribution = null
        )
    }

    private fun legacyPageForCachedItem(
        item: MovieItem,
        fallbackByTitle: Map<String, List<MovieItem>>
    ): MovieItem? {
        if (LicensedMediaPolicy.isVerifiedFeature(item) || !isLegacyMovieBoxId(item.id)) return null
        val matches = fallbackByTitle[normalizeTitle(item.title)].orEmpty()
        val link = when {
            item.year.isNullOrBlank() -> matches.singleOrNull()
            else -> matches.firstOrNull { it.year.equals(item.year, ignoreCase = true) }
                ?: matches.singleOrNull { it.year.isNullOrBlank() }
        } ?: return null
        return link.copy(
            id = item.id,
            poster = item.poster.nonBlankOr(link.poster),
            backdrop = item.backdrop.nonBlankOr(link.backdrop),
            description = item.description.nonBlankOr(link.description),
            rating = item.rating.nonBlankOr(link.rating),
            category = item.category.ifBlank { link.category }
        )
    }

    private fun isLegacyMovieBoxId(id: String): Boolean =
        listOf("mov_", "act_", "scifi_", "hor_", "rc_", "dr_", "tv_", "an_", "feat_", "tmb_")
            .any { id.startsWith(it, ignoreCase = true) }

    private fun allItems(response: ContentResponse): List<MovieItem> =
        response.categories.flatMap { category ->
            val inheritedCategory = category.name.removePrefix("MovieBox ·").trim()
            category.items.map { item ->
                if (category.id != "open_licensed_features" && inheritedCategory.isNotBlank() &&
                    (item.category.isBlank() || item.category.equals("Movies", ignoreCase = true)) &&
                    !inheritedCategory.equals("Movies", ignoreCase = true)
                ) item.copy(category = inheritedCategory) else item
            }
        } + listOfNotNull(response.featured)

    private fun normalizeTitle(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun slug(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    private fun String?.nonBlankOr(fallback: String?): String? = this?.takeIf(String::isNotBlank) ?: fallback
    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
