package com.salmanlaghari.risingflix.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.salmanlaghari.risingflix.data.*
import com.salmanlaghari.risingflix.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onVideoSelected: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabState by remember { mutableStateOf(0) } // 0: Downloads, 1: Favorites, 2: History
    val tabTitles = listOf("Downloads", "Favorites", "Watch History")

    // Collect downloads list from DownloadManager StateFlow!
    val downloadList by DownloadManager.downloads.collectAsState()

    // Mock Favorites
    val initialFavorites = remember {
        mutableStateListOf(
            MovieItem(
                id = "dra_01",
                title = "Echoes of the Heart: Silent Tears",
                poster = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600&auto=format&fit=crop",
                backdrop = "",
                description = "",
                rating = "9.5",
                duration = "5 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                category = "Dramas"
            )
        )
    }

    // Mock History
    val initialHistory = remember {
        mutableStateListOf(
            MovieItem(
                id = "feat_01",
                title = "Epic Space Odyssey: Beyond Horizon",
                poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=600&auto=format&fit=crop",
                backdrop = "",
                description = "",
                rating = "9.8",
                duration = "12 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                category = "Movies"
            )
        )
    }

    var showDeleteConfirmDialog by remember { mutableStateOf<Pair<Int, Any>?>(null) } // Tab index, Item (DownloadItem or MovieItem)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlueBg)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "My Library",
            color = TextMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTabState,
            containerColor = CardSurfaceDark,
            contentColor = AccentCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                    color = AccentCyan
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabState == index,
                    onClick = { selectedTabState = index },
                    text = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    selectedContentColor = AccentCyan,
                    unselectedContentColor = TextSub
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabState) {
                0 -> { // DOWNLOADS TAB
                    if (downloadList.isEmpty()) {
                        EmptyStateView(tabIndex = 0, tabTitles = tabTitles)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(downloadList, key = { it.videoId }) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (item.status == DownloadStatus.COMPLETED) {
                                                    // Wrap and play completed offline file directly
                                                    onVideoSelected(
                                                        MovieItem(
                                                            id = item.videoId,
                                                            title = item.title,
                                                            poster = item.poster,
                                                            backdrop = "",
                                                            description = "Offline playback",
                                                            rating = "9.0",
                                                            duration = "",
                                                            videoUrl = item.videoUrl
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = { showDeleteConfirmDialog = Pair(0, item) }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.poster,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(60.dp, 80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = TextMain,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.quality} • ${item.sizeLabel}",
                                                color = TextSub,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Real-time progress bar
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                LinearProgressIndicator(
                                                    progress = item.progress,
                                                    color = AccentCyan,
                                                    trackColor = Color.White.copy(alpha = 0.08f),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = when (item.status) {
                                                        DownloadStatus.DOWNLOADING -> "Downloading (${item.downloadSpeed})"
                                                        DownloadStatus.PAUSED -> "Paused"
                                                        DownloadStatus.COMPLETED -> "Completed"
                                                        DownloadStatus.FAILED -> "Failed"
                                                    },
                                                    color = if (item.status == DownloadStatus.COMPLETED) AccentCyan else TextSub,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Action controls (Pause / Play / Delete)
                                        Row {
                                            if (item.status == DownloadStatus.DOWNLOADING) {
                                                IconButton(onClick = { DownloadManager.pauseDownload(item.videoId) }) {
                                                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = AccentCyan, modifier = Modifier.size(18.dp))
                                                }
                                            } else if (item.status == DownloadStatus.PAUSED) {
                                                IconButton(onClick = { DownloadManager.resumeDownload(item.videoId) }) {
                                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume", tint = AccentCyan, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            IconButton(onClick = { showDeleteConfirmDialog = Pair(0, item) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // FAVORITES TAB
                    LibraryMovieListView(list = initialFavorites, onVideoSelected = onVideoSelected, onLongClick = { showDeleteConfirmDialog = Pair(1, it) })
                }
                2 -> { // HISTORY TAB
                    LibraryMovieListView(list = initialHistory, onVideoSelected = onVideoSelected, onLongClick = { showDeleteConfirmDialog = Pair(2, it) })
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmDialog?.let { pair ->
        val tabIndex = pair.first
        val rawItem = pair.second
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            containerColor = CardSurfaceDark,
            title = { Text(text = "Delete Item?", color = TextMain) },
            text = {
                val title = if (rawItem is DownloadItem) rawItem.title else (rawItem as MovieItem).title
                Text(text = "Are you sure you want to remove \"$title\" from your ${tabTitles[tabIndex]}?", color = TextSub)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tabIndex == 0) {
                            val dl = rawItem as DownloadItem
                            DownloadManager.deleteDownloadedFile(dl.videoId)
                        } else if (tabIndex == 1) {
                            initialFavorites.remove(rawItem as MovieItem)
                        } else {
                            initialHistory.remove(rawItem as MovieItem)
                        }
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text(text = "Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(text = "Cancel", color = TextMain)
                }
            }
        )
    }
}

@Composable
fun EmptyStateView(tabIndex: Int, tabTitles: List<String>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when (tabIndex) {
                0 -> Icons.Default.FolderZip
                1 -> Icons.Default.Star
                else -> Icons.Default.History
            },
            contentDescription = "Empty",
            tint = TextSub.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your ${tabTitles[tabIndex]} is empty",
            color = TextSub,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMovieListView(
    list: List<MovieItem>,
    onVideoSelected: (MovieItem) -> Unit,
    onLongClick: (MovieItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(list, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onVideoSelected(item) },
                        onLongClick = { onLongClick(item) }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail Poster
                    AsyncImage(
                        model = item.safePoster,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp, 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Text details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = TextMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${item.safeDuration} • Rating: ${item.safeRating}",
                            color = TextSub,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(onClick = { onLongClick(item) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
