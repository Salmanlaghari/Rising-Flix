package com.salmanlaghari.risingflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.ui.components.PremiumSearchBar
import com.salmanlaghari.risingflix.ui.components.PremiumVideoCard
import com.salmanlaghari.risingflix.ui.theme.*
import com.salmanlaghari.risingflix.viewmodel.MainViewModel
import com.salmanlaghari.risingflix.viewmodel.UiState

@Composable
fun ExploreScreen(
    viewModel: MainViewModel,
    onVideoSelected: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val trendingSearches = listOf(
        "Cricket Live", "Action Saga 2026", "Golden Horizon Drama", "Cartoons 8K", "Sintel Adventure"
    )

    // Filter states
    var selectedGenre by remember { mutableStateOf("All") }
    var selectedQuality by remember { mutableStateOf("All") }
    val genres = listOf("All", "Movies", "Dramas", "Sports", "Cartoons")
    val qualities = listOf("All", "8K", "4K", "HD+")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlueBg)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Cinematic Search",
            color = TextMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = "Explore endless streaming catalogs dynamically",
            color = TextSub,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar at Top
        PremiumSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- FILTER CHIPS ROW ---
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(text = "Filters", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genres) { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedGenre == genre) AccentCyan else CardSurfaceDark)
                            .clickable { selectedGenre = genre }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            color = if (selectedGenre == genre) TrueBlack else TextMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(qualities) { quality ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedQuality == quality) AccentCyan else CardSurfaceDark)
                            .clickable { selectedQuality = quality }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = quality,
                            color = if (selectedQuality == quality) TrueBlack else TextMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List contents
        Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isNotEmpty()) {
                // Search result cards
                when (uiState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is UiState.Success -> {
                        val response = (uiState as UiState.Success).data
                        val allItems = response.categories.flatMap { it.items }.filter { item ->
                            val matchGenre = selectedGenre == "All" || item.category.equals(selectedGenre, ignoreCase = true)
                            val matchQuality = selectedQuality == "All" || item.quality.equals(selectedQuality, ignoreCase = true)
                            matchGenre && matchQuality
                        }

                        if (allItems.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "None", tint = TextSub, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "No filtered matches found", color = TextSub, fontSize = 14.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(allItems, key = { it.id }) { video ->
                                    PremiumVideoCard(
                                        video = video,
                                        onClick = { onVideoSelected(video) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            } else {
                // Show grid of categories + trending searches when search query is empty
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Trending Searches",
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    trendingSearches.forEach { search ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSearchQueryChanged(search) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Trending",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = search, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Explore Categories",
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2 Column Category list cards
                    val categoryGridItems = listOf(
                        Pair("Movies", Icons.Default.PlayArrow),
                        Pair("Dramas", Icons.Default.Face),
                        Pair("Sports", Icons.Default.Star),
                        Pair("Cartoons", Icons.Default.Face),
                        Pair("TV Shows", Icons.Default.Menu),
                        Pair("News & Updates", Icons.Default.Info)
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        categoryGridItems.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(CardSurfaceDark)
                                            .border(1.dp, BorderSoft, RoundedCornerShape(14.dp))
                                            .clickable { viewModel.onSearchQueryChanged(item.first) }
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = item.second,
                                                contentDescription = item.first,
                                                tint = AccentCyan,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = item.first,
                                                color = TextMain,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
