package com.salmanlaghari.risingflix.data

import java.net.URI
import java.util.Locale

/** Routes known title/detail pages to WebView and media URLs to Media3. */
internal object PlaybackUrlResolver {
    private val embeddedPageHosts = setOf(
        "themoviebox.xyz",
        "moviebox.pk",
        "hdmoviescloud.com"
    )

    private val streamExtensions = setOf(
        "aac", "m3u8", "mpd", "mp3", "m4a", "m4v", "mkv", "mov", "mp4", "ogg", "ogv", "ts", "wav", "webm"
    )

    fun isEmbeddedPage(url: String): Boolean {
        val uri = parseHttpUri(url) ?: return false
        val host = uri.host.lowercase(Locale.ROOT).removePrefix("www.")
        return embeddedPageHosts.any { host == it || host.endsWith(".$it") }
    }

    fun isDirectStream(url: String): Boolean {
        val uri = parseHttpUri(url) ?: return false
        val extension = uri.path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return extension in streamExtensions && !isEmbeddedPage(url)
    }

    fun isValidHttpUrl(url: String): Boolean = parseHttpUri(url) != null

    private fun parseHttpUri(value: String): URI? = runCatching {
        URI(value.trim()).takeIf {
            (it.scheme.equals("http", true) || it.scheme.equals("https", true)) && !it.host.isNullOrBlank()
        }
    }.getOrNull()
}
