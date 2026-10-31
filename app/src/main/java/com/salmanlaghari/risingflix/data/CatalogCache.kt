package com.salmanlaghari.risingflix.data

import com.google.gson.Gson
import java.io.File

/** Stores only the last known content catalog in the app's private files directory. */
internal class CatalogCache(private val cacheFile: File) {
    private val gson = Gson()

    fun load(): ContentResponse? = runCatching {
        if (!cacheFile.isFile || cacheFile.length() == 0L) return null
        gson.fromJson(cacheFile.readText(Charsets.UTF_8), ContentResponse::class.java)
            ?.takeIf { it.categories.isNotEmpty() }
    }.getOrNull()

    fun save(content: ContentResponse): Boolean = runCatching {
        val parent = cacheFile.parentFile ?: return false
        if (!parent.exists() && !parent.mkdirs()) return false

        val temporaryFile = File(parent, "${cacheFile.name}.tmp")
        temporaryFile.writeText(gson.toJson(content), Charsets.UTF_8)
        if (temporaryFile.renameTo(cacheFile)) {
            true
        } else {
            temporaryFile.copyTo(cacheFile, overwrite = true)
            temporaryFile.delete()
            true
        }
    }.getOrDefault(false)
}
