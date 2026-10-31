package com.salmanlaghari.risingflix.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads pre-existing, page-only links and the independently curated licensed feature feed. */
object TheMovieBoxContent {
    private val gson = Gson()

    internal suspend fun loadLicensedCatalog(context: Context): ContentResponse? =
        loadResponseAsset(context, "licensed_content.json")

    internal suspend fun loadMovieBoxLinkCatalog(context: Context): ContentResponse? =
        loadResponseAsset(context, "moviebox_links.json")
            ?.let(CatalogResolver::movieBoxPagesOnly)

    /** Kept as the offline fallback entry point; the asset contains page links, never streams. */
    internal suspend fun loadFallbackContent(context: Context): ContentResponse? =
        loadMovieBoxLinkCatalog(context)

    private suspend fun loadResponseAsset(context: Context, assetName: String): ContentResponse? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetName).bufferedReader().use { reader ->
                    gson.fromJson(reader, ContentResponse::class.java)
                }
            }.getOrNull()
        }

    /** Ignores any videoUrl in an old row; only a stored detailUrl/detailPath becomes a link. */
    internal fun resolveFallbackVideoUrl(item: Map<*, *>): String =
        MovieBoxLinkResolver.resolvePage(item["detailUrl"] as? String)
            ?: MovieBoxLinkResolver.resolvePage(item["sourcePageUrl"] as? String)
            ?: MovieBoxLinkResolver.resolvePage(item["detailPath"] as? String)
            .orEmpty()
}
