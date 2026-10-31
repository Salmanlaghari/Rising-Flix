package com.salmanlaghari.risingflix.data

import java.net.URI
import java.util.Locale

/** Converts only existing MovieBox detail paths into external links; it never resolves streams. */
internal object MovieBoxLinkResolver {
    private const val CURRENT_HOST = "themoviebox.xyz"
    private const val CURRENT_BASE = "https://themoviebox.xyz"
    private val acceptedHosts = setOf("themoviebox.xyz", "www.themoviebox.xyz", "moviebox.pk", "www.moviebox.pk")
    private val safeSlug = Regex("^[A-Za-z0-9._~-]+$")

    fun resolvePage(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null

        val parsed = runCatching { URI(raw) }.getOrNull()
        if (parsed == null || parsed.scheme.isNullOrBlank()) {
            val path = raw.trimStart('/')
            val slug = when {
                path.startsWith("detail/", ignoreCase = true) -> path.substringAfter('/')
                path.startsWith("moviedetail/", ignoreCase = true) -> path.substringAfter('/')
                else -> path
            }.trimEnd('/')
            return pageForSlug(slug)
        }

        if (!(parsed.scheme.equals("http", true) || parsed.scheme.equals("https", true))) return null
        val host = parsed.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return null
        if (host !in acceptedHosts.map { it.removePrefix("www.") }.toSet()) return null

        val path = parsed.path.orEmpty().trimEnd('/')
        val slug = when {
            path.startsWith("/detail/", ignoreCase = true) -> path.removePrefixIgnoreCase("/detail/")
            path.startsWith("/moviedetail/", ignoreCase = true) -> path.removePrefixIgnoreCase("/moviedetail/")
            else -> return null
        }
        return pageForSlug(slug)
    }

    fun isMovieBoxPage(value: String?): Boolean = resolvePage(value) != null

    private fun pageForSlug(rawSlug: String): String? {
        val slug = rawSlug.trim().trim('/')
        if (slug.isBlank() || !safeSlug.matches(slug)) return null
        return "$CURRENT_BASE/detail/$slug"
    }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this
}
