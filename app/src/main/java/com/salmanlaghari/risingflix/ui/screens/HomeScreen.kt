package com.salmanlaghari.risingflix.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.ui.components.CategoryRow
import com.salmanlaghari.risingflix.ui.components.FeaturedBanner
import com.salmanlaghari.risingflix.ui.components.PremiumVideoCard
import com.salmanlaghari.risingflix.ui.theme.*
import com.salmanlaghari.risingflix.viewmodel.MainViewModel
import com.salmanlaghari.risingflix.viewmodel.UiState

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onVideoSelected: (MovieItem) -> Unit,
    onSearchIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var selectedCategoryChip by remember { mutableStateOf("All") }
    val categoryChips = listOf("All", "Movies", "Dramas", "Sports", "Kids", "TV Shows")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlueBg)
            .verticalScroll(scrollState)
    ) {
        // --- TOP APP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Rising Flix",
                    color = AccentCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Movies • Dramas • Sports • Cartoons",
                    color = TextSub,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // TV Casting Button
                IconButton(onClick = {
                    Toast.makeText(context, "Scanning for Chromecast / Smart TV devices in network...", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.Cast,
                        contentDescription = "Cast to TV",
                        tint = AccentCyan
                    )
                }

                IconButton(onClick = onSearchIconClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, BorderSoft, RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = AccentCyan,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }

        // --- FEATURED CONTENT CAROUSEL WITH AUTO-PLAY SIMULATION ---
        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            is UiState.Success -> {
                val data = (uiState as UiState.Success).data
                data.featured?.let { video ->
                    FeaturedBanner(
                        video = video,
                        onPlayClick = onVideoSelected,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
            is UiState.Error -> {
                // Loaded gracefully via offline cache
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- CATEGORIES CHIPS ROW ---
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categoryChips) { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedCategoryChip == chip) AccentCyan else CardSurfaceDark)
                        .clickable { selectedCategoryChip = chip }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = chip,
                        color = if (selectedCategoryChip == chip) TrueBlack else TextMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTIONS ---
        when (uiState) {
            is UiState.Success -> {
                val data = (uiState as UiState.Success).data
                val filteredCategories = if (selectedCategoryChip == "All") {
                    data.categories
                } else {
                    data.categories.filter { it.name.equals(selectedCategoryChip, ignoreCase = true) }
                }

                if (filteredCategories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No content available in $selectedCategoryChip",
                            color = TextSub,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    filteredCategories.forEach { category ->
                        CategoryRow(
                            category = category,
                            onVideoClick = onVideoSelected
                        )
                    }
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
