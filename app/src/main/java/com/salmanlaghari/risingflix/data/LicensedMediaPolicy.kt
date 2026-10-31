package com.salmanlaghari.risingflix.data

/** Fail-closed playback allowlist for creator-authorized, measured feature films. */
internal object LicensedMediaPolicy {
    const val MIN_FEATURE_SECONDS = 45 * 60

    const val VALKAAMA_ID = "valkaama_cc_by_sa"
    const val VALKAAMA_MEDIA_URL = "https://www.valkaama.com/media/movie/Valkaama_720p.mkv"
    const val VALKAAMA_SOURCE_URL = "https://www.valkaama.com/index.php?page=valkaama&l=en"
    const val VALKAAMA_LICENSE_NAME = "CC BY-SA 3.0"
    const val VALKAAMA_LICENSE_URL = "https://creativecommons.org/licenses/by-sa/3.0/"
    const val VALKAAMA_SOURCE_NAME = "Valkaama official site"
    const val VALKAAMA_DURATION_SECONDS = 5_586
    const val VALKAAMA_ATTRIBUTION =
        "Valkaama, directed and produced by Tim Baumann; story by Hendrik Behnisch and Lynn Gray. " +
            "Film licensed CC BY-SA 3.0. Thumbnail is a resized still from the film."

    fun isAuthorizedMediaUrl(id: String, url: String): Boolean =
        id == VALKAAMA_ID && url.trim() == VALKAAMA_MEDIA_URL

    fun isVerifiedFeature(item: MovieItem): Boolean =
        isAuthorizedMediaUrl(item.id, item.videoUrl) &&
            item.actualDurationSeconds == VALKAAMA_DURATION_SECONDS &&
            item.actualDurationSeconds >= MIN_FEATURE_SECONDS &&
            item.sourcePageUrl == VALKAAMA_SOURCE_URL &&
            item.sourceName == VALKAAMA_SOURCE_NAME &&
            item.licenseName == VALKAAMA_LICENSE_NAME &&
            item.licenseUrl == VALKAAMA_LICENSE_URL &&
            item.attribution == VALKAAMA_ATTRIBUTION

    fun isVerifiedFeature(details: VideoDetails): Boolean =
        isAuthorizedMediaUrl(details.id, details.videoUrl) &&
            details.actualDurationSeconds == VALKAAMA_DURATION_SECONDS &&
            details.actualDurationSeconds >= MIN_FEATURE_SECONDS &&
            details.sourcePageUrl == VALKAAMA_SOURCE_URL &&
            details.sourceName == VALKAAMA_SOURCE_NAME &&
            details.licenseName == VALKAAMA_LICENSE_NAME &&
            details.licenseUrl == VALKAAMA_LICENSE_URL &&
            details.attribution == VALKAAMA_ATTRIBUTION
}
