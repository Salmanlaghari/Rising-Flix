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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onVideoSelected: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabState by remember { mutableStateOf(0) } // 0: Downloads, 1: Favorites, 2: History
    val tabTitles = listOf("Downloads", "Favorites", "Watch History")

    // Mock Downloads data
    val initialDownloads = remember {
        mutableStateListOf(
            MovieItem(
                id = "mov_01",
                title = "Sintel: Rise of the Guardian",
                poster = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop",
                backdrop = "",
                description = "",
                rating = "9.2",
                duration = "14 min",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                category = "Movies"
            )
        )
    }

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

    var showDeleteConfirmDialog by remember { mutableStateOf<Pair<Int, MovieItem>?>(null) } // Tab index, Item

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
            val currentList = when (selectedTabState) {
                0 -> initialDownloads
                1 -> initialFavorites
                else -> initialHistory
            }

            if (currentList.isEmpty()) {
                // Empty state view
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (selectedTabState) {
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
                        text = "Your ${tabTitles[selectedTabState]} is empty",
                        color = TextSub,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentList, key = { it.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onVideoSelected(item) },
                                    onLongClick = { showDeleteConfirmDialog = Pair(selectedTabState, item) }
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
                                    model = item.poster,
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
                                        text = "${item.duration} • Rating: ${item.rating}",
                                        color = TextSub,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    // If tab is Downloads (0), show custom download progress indicator simulation
                                    if (selectedTabState == 0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            LinearProgressIndicator(
                                                progress = { 1.0f },
                                                color = AccentCyan,
                                                trackColor = Color.White.copy(alpha = 0.08f),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Completed",
                                                color = AccentCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { showDeleteConfirmDialog = Pair(selectedTabState, item) }) {
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
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmDialog?.let { pair ->
        val tabIndex = pair.first
        val item = pair.second
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            containerColor = CardSurfaceDark,
            title = { Text(text = "Delete Item?", color = TextMain) },
            text = { Text(text = "Are you sure you want to remove \"${item.title}\" from your ${tabTitles[tabIndex]}?", color = TextSub) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (tabIndex) {
                            0 -> initialDownloads.remove(item)
                            1 -> initialFavorites.remove(item)
                            else -> initialHistory.remove(item)
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
