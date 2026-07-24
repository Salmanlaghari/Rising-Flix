package com.salmanlaghari.risingflix.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.data.VideoDetails
import com.salmanlaghari.risingflix.data.VideoPlayerManager
import com.salmanlaghari.risingflix.ui.components.PremiumVideoCard
import com.salmanlaghari.risingflix.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("UnrememberedMutableState")
@Composable
fun PlayerScreen(
    videoDetails: VideoDetails,
    onBackClick: () -> Unit,
    onRelatedVideoClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Retrieve singleton player
    val player = remember { VideoPlayerManager.getPlayer(context) }

    // Start playing current video
    LaunchedEffect(videoDetails.id) {
        VideoPlayerManager.playVideo(context, videoDetails.id, videoDetails.videoUrl, videoDetails.subtitlesUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            VideoPlayerManager.savePlaybackState()
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var selectedQuality by remember { mutableStateOf(videoDetails.quality) }
    var showQualityDialog by remember { mutableStateOf(false) }

    // ExoPlayer Listener for progress and states
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = player.duration
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    // Controls progress polling
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition
            delay(500)
        }
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlueBg)
            .verticalScroll(scrollState)
    ) {
        // --- CINEMATIC FULLSCREEN EXOPLAYER SECTION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlaid controls
            if (showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(16.dp)
                ) {
                    // Top Bar inside Player
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextMain
                            )
                        }

                        Row {
                            IconButton(onClick = { showQualityDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Quality",
                                    tint = TextMain
                                )
                            }
                        }
                    }

                    // Centered controller triggers
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        IconButton(onClick = { player.seekTo((player.currentPosition - 10000).coerceAtLeast(0L)) }) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind",
                                tint = TextMain,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AccentCyan, RoundedCornerShape(50))
                                .clickable {
                                    if (player.isPlaying) {
                                        VideoPlayerManager.pause()
                                    } else {
                                        VideoPlayerManager.play()
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "PlayPause",
                                tint = TrueBlack,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }

                        IconButton(onClick = { player.seekTo((player.currentPosition + 10000).coerceAtMost(duration)) }) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward",
                                tint = TextMain,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Progress Timeline row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = TextSub,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatTime(duration),
                                color = TextSub,
                                fontSize = 11.sp
                            )
                        }

                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                val target = (fraction * duration).toLong()
                                player.seekTo(target)
                                currentPosition = target
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = AccentCyan,
                                thumbColor = AccentCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // --- MOVIE DETAILS & ACTIONS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = videoDetails.title,
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-info row (Rating, Year, Duration, Quality)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = GoldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = videoDetails.rating,
                        color = TextMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(text = videoDetails.releaseYear, color = TextSub, fontSize = 13.sp)
                Text(text = videoDetails.duration, color = TextSub, fontSize = 13.sp)

                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = selectedQuality,
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download & Share Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { /* Handle Local Storage download simulation */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardSurfaceDark,
                        contentColor = TextMain
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Download")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { /* Handle share intent simulation */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardSurfaceDark,
                        contentColor = TextMain
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            Text(
                text = videoDetails.description,
                color = TextSub,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- RELATED CONTENT LIST ---
            if (videoDetails.relatedItems.isNotEmpty()) {
                Text(
                    text = "More Like This",
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(videoDetails.relatedItems) { video ->
                        PremiumVideoCard(
                            video = video,
                            onClick = { onRelatedVideoClick(video) }
                        )
                    }
                }
            }
        }
    }

    // Quality Selector dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(text = "Select Playback Quality", color = TextMain) },
            containerColor = CardSurfaceDark,
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text(text = "Cancel", color = AccentCyan)
                }
            },
            text = {
                Column {
                    listOf("1080p (Full HD)", "720p (HD)", "480p (SD)", "Auto").forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedQuality = quality
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = quality, color = TextMain)
                            if (selectedQuality == quality) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AccentCyan
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
