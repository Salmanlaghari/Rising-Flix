package com.salmanlaghari.risingflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.risingflix.data.Category
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.ui.components.FeaturedBanner
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
    val uiState by viewModel.uiState.collectAsState()
    val exploreCategory by viewModel.exploreCategoryFilter.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlueBg)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setNavSection(0) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = TextMain
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (exploreCategory != null) "Explore: $exploreCategory" else "Explore All",
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        PremiumSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Error loading content", color = TextSub)
                }
            }
            is UiState.Success -> {
                val data = (uiState as UiState.Success).data

                // Filter items based on exploreCategory
                val itemsToShow = if (exploreCategory != null) {
                    data.categories
                        .filter { cat ->
                            cat.name.equals(exploreCategory, ignoreCase = true) ||
                            (exploreCategory == "Movies" && cat.name.equals("Cinema", ignoreCase = true)) ||
                            (exploreCategory == "Kids" && cat.name.equals("Cartoons", ignoreCase = true))
                        }
                        .flatMap { it.items }
                } else {
                    data.categories.flatMap { it.items }
                }

                val searchedItems = if (searchQuery.isNotBlank()) {
                    itemsToShow.filter { item ->
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.safeDescription.contains(searchQuery, ignoreCase = true) ||
                        item.category.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    itemsToShow
                }

                if (searchedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No results for \"$searchQuery\"" else "No items found in this category",
                            color = TextSub,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Grid Layout
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), // 2 columns per row
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchedItems, key = { it.id }) { video ->
                            PremiumVideoCard(
                                video = video,
                                onClick = { onVideoSelected(video) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
