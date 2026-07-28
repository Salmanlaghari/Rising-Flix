package com.salmanlaghari.risingflix.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.data.VideoDetails
import com.salmanlaghari.risingflix.data.VideoPlayerManager
import com.salmanlaghari.risingflix.data.DownloadManager
import com.salmanlaghari.risingflix.data.DownloadStatus
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

    val isWebViewMode = videoDetails.videoUrl.contains("moviebox.pk") || videoDetails.videoUrl.contains("moviedetail")

    // Start playing current video safely
    LaunchedEffect(videoDetails.id) {
        if (!isWebViewMode && videoDetails.videoUrl.isNotEmpty()) {
            try {
                VideoPlayerManager.playVideo(context, videoDetails.id, videoDetails.videoUrl, videoDetails.subtitlesUrl)
            } catch (e: Exception) {
                Toast.makeText(context, "Playback Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else if (isWebViewMode) {
            VideoPlayerManager.pause()
        }
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

    // Quality & Subtitles states
    var selectedQuality by remember { mutableStateOf(videoDetails.quality) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var isSubtitlesEnabled by remember { mutableStateOf(false) }
    
    // Server/Link states
    var showServerDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf(0) }
    val serverLinks = listOf(
        videoDetails.videoUrl,
        videoDetails.videoUrl.replace("archive.org", "sample-videos.com").replace("BigBuckBunny_512kb.mp4", "video321/mp4/720/big_buck_bunny_720p_1mb.mp4"),
        videoDetails.videoUrl.replace("archive.org", "www.learningcontainer.com").replace("BigBuckBunny_512kb.mp4", "wp-content/uploads/2020/05/sample-mp4-file.mp4")
    )
    val serverNames = listOf("Server 1", "Server 2", "Server 3")

    // Download States
    var showDownloadDialog by remember { mutableStateOf(false) }
    val downloadsList by DownloadManager.downloads.collectAsState()
    val isDownloaded = downloadsList.firstOrNull { it.videoId == videoDetails.id }?.status == DownloadStatus.COMPLETED
    val isDownloading = downloadsList.firstOrNull { it.videoId == videoDetails.id }?.status == DownloadStatus.DOWNLOADING

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
            if (isWebViewMode) {
                var webViewLoadError by remember { mutableStateOf(false) }

                if (webViewLoadError) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "WebView could not be initialized on your device",
                                color = TextMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(videoDetails.videoUrl)
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = TrueBlack)
                            ) {
                                Text("Open in Browser")
                            }
                        }
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            try {
                                android.webkit.WebView(ctx).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                    }
                                    webViewClient = android.webkit.WebViewClient()
                                    webChromeClient = android.webkit.WebChromeClient()
                                    loadUrl(videoDetails.videoUrl)
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                                webViewLoadError = true
                                android.view.View(ctx) // Return a safe blank dummy view
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Render floating back button over WebView
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextMain
                        )
                    }
                }
            } else {
                // ExoPlayer View
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false // We draw custom overlay controls
                            // Force aspect ratio
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextMain
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(onClick = { isSubtitlesEnabled = !isSubtitlesEnabled }) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = "Subtitles",
                                        tint = if (isSubtitlesEnabled) AccentCyan else TextMain
                                    )
                                }
                                IconButton(onClick = { showQualityDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Quality",
                                        tint = TextMain
                                    )
                                }
                                IconButton(onClick = { showServerDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Server",
                                        tint = TextMain
                                    )
                                }
                                IconButton(onClick = {
                                    Toast.makeText(context, "Scanning for Chromecast devices...", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Cast,
                                        contentDescription = "Cast",
                                        tint = TextMain
                                    )
                                }
                            }
                        }

                        // Bottom Bar inside Player
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            // Rewind / Play-Pause / Forward
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { player.seekTo(player.currentPosition - 10000) }) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = TextMain
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        if (isPlaying) VideoPlayerManager.pause() else VideoPlayerManager.play()
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentCyan)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = TrueBlack
                                    )
                                }

                                IconButton(onClick = { player.seekTo(player.currentPosition + 10000) }) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = TextMain
                                    )
                                }
                            }

                            // Seek Timeline Bar
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(currentPosition),
                                        color = TextSub,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatTime(duration),
                                        color = TextSub,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                                val animatedProgress by animateFloatAsState(targetValue = progress)

                                Slider(
                                    value = animatedProgress,
                                    onValueChange = { newValue ->
                                        player.seekTo((newValue * duration).toLong())
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentCyan,
                                        activeTrackColor = AccentCyan,
                                        inactiveTrackColor = TextSub.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- MOVIE DETAILS SECTION ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Title & Meta Data
            Text(
                text = videoDetails.title,
                color = TextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gold Rating Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⭐",
                            color = GoldAccent,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = videoDetails.rating,
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = videoDetails.releaseYear,
                    color = TextSub,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = videoDetails.duration,
                    color = TextSub,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(12.dp))

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
                // PREMIUM OFFLINE DOWNLOAD BUTTON
                Button(
                    onClick = {
                        if (isDownloaded) {
                            Toast.makeText(context, "Already downloaded. Check your My Library tab!", Toast.LENGTH_SHORT).show()
                        } else if (isDownloading) {
                            Toast.makeText(context, "Download is already in progress!", Toast.LENGTH_SHORT).show()
                        } else {
                            showDownloadDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) Color.White.copy(alpha = 0.04f) else CardSurfaceDark,
                        contentColor = if (isDownloaded) AccentCyan else TextMain
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.CheckCircle else if (isDownloading) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                        contentDescription = "Download"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDownloaded) "Downloaded" else if (isDownloading) "Downloading..." else "Download",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Share link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
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

    // Server/Link Selector dialog
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text(text = "Select Server", color = TextMain) },
            containerColor = CardSurfaceDark,
            confirmButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text(text = "Cancel", color = AccentCyan)
                }
            },
            text = {
                Column {
                    serverNames.forEachIndexed { index, serverName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServer = index
                                    showServerDialog = false
                                    // Switch to selected server
                                    VideoPlayerManager.playVideo(context, videoDetails.id, serverLinks[index], videoDetails.subtitlesUrl)
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = serverName, color = TextMain)
                            if (selectedServer == index) {
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

    // Playback Quality Selector dialog
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

    // Phase 3: PRE-DOWNLOAD QUALITY SELECTION DIALOG
    if (showDownloadDialog) {
        var selectedDlQuality by remember { mutableStateOf("720p") }
        val dlQualities = listOf("480p", "720p", "1080p")

        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            containerColor = CardSurfaceDark,
            title = { Text(text = "Select Download Quality", color = TextMain) },
            text = {
                Column {
                    dlQualities.forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDlQuality = q }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (q == "1080p") "1080p (Full HD)" else if (q == "720p") "720p (HD)" else "480p (SD)", color = TextMain)
                            if (selectedDlQuality == q) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = AccentCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        DownloadManager.startDownload(
                            videoId = videoDetails.id,
                            title = videoDetails.title,
                            poster = videoDetails.poster,
                            videoUrl = videoDetails.videoUrl,
                            quality = selectedDlQuality
                        )
                        showDownloadDialog = false
                        Toast.makeText(context, "Download started! Track progress in My Library tab.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = "Download", color = AccentCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text(text = "Cancel", color = TextMain)
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
