package com.salmanlaghari.risingflix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

class RisingFlixApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this@RisingFlixApplication)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(this@RisingFlixApplication.cacheDir, "risingflix_posters"))
                .maxSizeBytes(128L * 1024 * 1024)
                .build()
        }
        .crossfade(false)
        .build()
}
