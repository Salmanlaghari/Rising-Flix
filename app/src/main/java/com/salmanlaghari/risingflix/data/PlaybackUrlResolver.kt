package com.salmanlaghari.risingflix.data

import java.net.URI

/** Only the reviewed licensed feature is recognized as playable media. */
internal object PlaybackUrlResolver {
    fun isEmbeddedPage(url: String): Boolean = MovieBoxLinkResolver.isMovieBoxPage(url)

    fun isDirectStream(url: String): Boolean =
        LicensedMediaPolicy.isAuthorizedMediaUrl(LicensedMediaPolicy.VALKAAMA_ID, url)

    fun isValidHttpUrl(url: String): Boolean = parseHttpUri(url) != null

    private fun parseHttpUri(value: String): URI? = runCatching {
        URI(value.trim()).takeIf {
            (it.scheme.equals("http", true) || it.scheme.equals("https", true)) && !it.host.isNullOrBlank()
        }
    }.getOrNull()
}
