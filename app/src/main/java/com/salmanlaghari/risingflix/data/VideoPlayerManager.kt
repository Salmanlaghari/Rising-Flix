package com.salmanlaghari.risingflix.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Singleton ExoPlayer wrapper with a fail-closed authorized-media gate. */
object VideoPlayerManager {
    private var playerInstance: ExoPlayer? = null
    private var lastPlayedMediaId: String? = null
    private var lastPlaybackPosition: Long = 0L
    private var lastPlayWhenReady: Boolean = true

    fun getPlayer(context: Context): ExoPlayer {
        if (playerInstance == null) {
            playerInstance = ExoPlayer.Builder(context.applicationContext).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
        }
        return playerInstance!!
    }

    fun playVideo(context: Context, videoId: String, videoUrl: String, subtitleUrl: String? = null) {
        require(LicensedMediaPolicy.isAuthorizedMediaUrl(videoId, videoUrl)) {
            "This media source is not in the reviewed licensed catalog."
        }
        val player = getPlayer(context)
        val mediaBuilder = MediaItem.Builder().setUri(Uri.parse(videoUrl))
        if (!subtitleUrl.isNullOrBlank()) {
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                .setMimeType("text/vtt")
                .setLanguage("en")
                .build()
            mediaBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }
        val mediaItem = mediaBuilder.build()
        if (videoId == lastPlayedMediaId) {
            player.setMediaItem(mediaItem, lastPlaybackPosition)
            player.playWhenReady = lastPlayWhenReady
        } else {
            player.setMediaItem(mediaItem)
            lastPlaybackPosition = 0L
            player.playWhenReady = true
        }
        player.prepare()
        lastPlayedMediaId = videoId
    }

    fun pause() { playerInstance?.pause() }
    fun play() { playerInstance?.play() }
    fun seekTo(positionMs: Long) { playerInstance?.seekTo(positionMs) }

    fun changeQuality(context: Context, newUrl: String) {
        val videoId = lastPlayedMediaId ?: return
        if (!LicensedMediaPolicy.isAuthorizedMediaUrl(videoId, newUrl)) return
        val player = playerInstance ?: return
        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying
        player.setMediaItem(MediaItem.fromUri(newUrl), currentPosition)
        player.prepare()
        player.playWhenReady = wasPlaying
    }

    fun savePlaybackState() {
        playerInstance?.let { player ->
            lastPlaybackPosition = player.currentPosition
            lastPlayWhenReady = player.playWhenReady
        }
    }

    fun release() {
        savePlaybackState()
        playerInstance?.release()
        playerInstance = null
    }

    fun enableBackgroundAudio(enabled: Boolean) {
        // Media3 session/background-audio configuration remains application-controlled.
    }
}
