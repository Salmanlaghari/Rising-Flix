package com.salmanlaghari.risingflix.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class DownloadStatus {
    DOWNLOADING, PAUSED, COMPLETED, FAILED
}

data class DownloadItem(
    val videoId: String,
    val title: String,
    val poster: String,
    val videoUrl: String,
    val quality: String,
    val progress: Float, // 0.0 to 1.0
    val status: DownloadStatus,
    val sizeLabel: String,
    val downloadSpeed: String = "0 KB/s"
)

/**
 * Singleton Offline Download Manager for Rising Flix.
 * Simulates background downloads, handles pause/resume/cancel, quality and storage rules.
 */
object DownloadManager {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeDownloadJobs = mutableMapOf<String, Job>()

    var isWifiOnly: Boolean = true
    var storageLocation: String = "Internal Storage" // "Internal Storage" or "SD Card"

    init {
        // Pre-populate with one completed download for high-quality library presentation
        _downloads.value = listOf(
            DownloadItem(
                videoId = "mov_01",
                title = "Sintel: Rise of the Guardian",
                poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                quality = "720p",
                progress = 1.0f,
                status = DownloadStatus.COMPLETED,
                sizeLabel = "42.5 MB"
            )
        )
    }

    fun startDownload(videoId: String, title: String, poster: String, videoUrl: String, quality: String) {
        // Check if already downloading or completed
        val existing = _downloads.value.firstOrNull { it.videoId == videoId }
        if (existing != null && (existing.status == DownloadStatus.DOWNLOADING || existing.status == DownloadStatus.COMPLETED)) {
            return
        }

        val size = when (quality) {
            "480p" -> "15.4 MB"
            "720p" -> "42.5 MB"
            else -> "112.8 MB"
        }

        val newItem = DownloadItem(
            videoId = videoId,
            title = title,
            poster = poster,
            videoUrl = videoUrl,
            quality = quality,
            progress = 0.0f,
            status = DownloadStatus.DOWNLOADING,
            sizeLabel = size,
            downloadSpeed = "1.5 MB/s"
        )

        updateItemInList(newItem)
        runDownloadSimulation(videoId)
    }

    fun pauseDownload(videoId: String) {
        val item = _downloads.value.firstOrNull { it.videoId == videoId } ?: return
        activeDownloadJobs[videoId]?.cancel()
        activeDownloadJobs.remove(videoId)

        val updated = item.copy(status = DownloadStatus.PAUSED, downloadSpeed = "0 KB/s")
        updateItemInList(updated)
    }

    fun resumeDownload(videoId: String) {
        val item = _downloads.value.firstOrNull { it.videoId == videoId } ?: return
        if (item.status == DownloadStatus.PAUSED) {
            val updated = item.copy(status = DownloadStatus.DOWNLOADING, downloadSpeed = "1.8 MB/s")
            updateItemInList(updated)
            runDownloadSimulation(videoId)
        }
    }

    fun cancelDownload(videoId: String) {
        activeDownloadJobs[videoId]?.cancel()
        activeDownloadJobs.remove(videoId)
        _downloads.value = _downloads.value.filter { it.videoId != videoId }
    }

    fun deleteDownloadedFile(videoId: String) {
        cancelDownload(videoId)
    }

    private fun updateItemInList(item: DownloadItem) {
        val list = _downloads.value.toMutableList()
        val index = list.indexOfFirst { it.videoId == item.videoId }
        if (index != -1) {
            list[index] = item
        } else {
            list.add(item)
        }
        _downloads.value = list
    }

    private fun runDownloadSimulation(videoId: String) {
        activeDownloadJobs[videoId]?.cancel()
        val job = coroutineScope.launch {
            while (isActive) {
                delay(1000) // update every second
                val item = _downloads.value.firstOrNull { it.videoId == videoId } ?: break
                if (item.status != DownloadStatus.DOWNLOADING) break

                val newProgress = (item.progress + 0.1f).coerceAtMost(1.0f)
                val isCompleted = newProgress >= 1.0f
                val newStatus = if (isCompleted) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
                val speed = if (isCompleted) "0 KB/s" else "${(1.2 + Math.random() * 0.8).format(1)} MB/s"

                val updated = item.copy(
                    progress = newProgress,
                    status = newStatus,
                    downloadSpeed = speed
                )
                updateItemInList(updated)

                if (isCompleted) {
                    activeDownloadJobs.remove(videoId)
                    break
                }
            }
        }
        activeDownloadJobs[videoId] = job
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
