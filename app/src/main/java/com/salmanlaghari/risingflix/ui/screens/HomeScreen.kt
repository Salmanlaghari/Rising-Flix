package com.salmanlaghari.risingflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.risingflix.data.Category
import com.salmanlaghari.risingflix.data.VideoItem
import com.salmanlaghari.risingflix.ui.components.*
import com.salmanlaghari.risingflix.ui.theme.*
import com.salmanlaghari.risingflix.viewmodel.MainViewModel
import com.salmanlaghari.risingflix.viewmodel.UiState

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onVideoSelected: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentNavSection by viewModel.currentNavSection.collectAsState()

    Scaffold(
        bottomBar = {
            PremiumBottomBar(
                currentSection = currentNavSection,
                onSectionSelected = { viewModel.setNavSection(it) }
            )
        },
        containerColor = DeepBlack,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentNavSection) {
                0 -> { // HOME
                    HomeDashboardContent(
                        uiState = uiState,
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onVideoSelected = onVideoSelected,
                        onRetryClick = { viewModel.fetchContent() }
                    )
                }
                1 -> { // SEARCH (DEDICATED FULL SCREEN SEARCH VIEW)
                    SearchTabContent(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        uiState = uiState,
                        onVideoSelected = onVideoSelected
                    )
                }
                2 -> { // PREMIUM BENEFITS & CHANNELS
                    PremiumTabContent()
                }
                3 -> { // PROFILE & SETTINGS
                    ProfileTabContent()
                }
            }
        }
    }
}

@Composable
fun HomeDashboardContent(
    uiState: UiState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onVideoSelected: (VideoItem) -> Unit,
    onRetryClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // App header title
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
                    color = PremiumGreen,
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
                    tint = PremiumGreen,
                    modifier = Modifier.size(20.dp).align(Alignment.Center)
                )
            }
        }

        // Floating glassmorphic search bar
        PremiumSearchBar(
            query = searchQuery,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    CircularProgressIndicator(
                        color = PremiumGreen,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            is UiState.Success -> {
                val data = uiState.data
                if (data.categories.isEmpty() && searchQuery.isNotEmpty()) {
                    // Search Empty State
                    NoResultsFoundView(query = searchQuery)
                } else {
                    // Featured Banner (Only show if not searching or if featured exists)
                    if (searchQuery.isEmpty() && data.featured != null) {
                        FeaturedBanner(
                            video = data.featured,
                            onPlayClick = onVideoSelected,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Categorized Content Lists
                    data.categories.forEach { category ->
                        CategoryRow(
                            category = category,
                            onVideoClick = onVideoSelected
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = TextSub,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to fetch content list",
                        color = TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.message,
                        color = TextSub,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumGreen,
                            contentColor = TrueBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Try Again", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTabContent(
    query: String,
    onQueryChange: (String) -> Unit,
    uiState: UiState,
    onVideoSelected: (VideoItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Cinematic Search",
            color = TextMain,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = "Instantly explore all premium content directories",
            color = TextSub,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        PremiumSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        color = PremiumGreen,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            is UiState.Success -> {
                val data = uiState.data
                val allItems = data.categories.flatMap { it.items }

                if (allItems.isEmpty()) {
                    NoResultsFoundView(query = query)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        data.categories.forEach { category ->
                            CategoryRow(
                                category = category,
                                onVideoClick = onVideoSelected
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                NoResultsFoundView(query = query)
            }
        }
    }
}

@Composable
fun PremiumTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium benefits",
            tint = GoldAccent,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Rising Flix Premium",
            color = TextMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Unlock Ultra HD 8K Streaming Experience",
            color = PremiumGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        PremiumBenefitRow(icon = Icons.Default.Star, title = "8K & 4K Ultra HD Streaming")
        PremiumBenefitRow(icon = Icons.Default.PlayArrow, title = "Zero Buffering & Instant Load with ExoPlayer")
        PremiumBenefitRow(icon = Icons.Default.Favorite, title = "All-in-One: Movies, Dramas, Sports, Cartoons")
        PremiumBenefitRow(icon = Icons.Default.ArrowDownward, title = "Unlimited Content Directory Access")

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumGreen,
                contentColor = TrueBlack
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                text = "GET PREMIUM ACCESS",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PremiumBenefitRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, BorderSoft, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PremiumGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProfileTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User profile card
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.04f))
                .border(2.dp, PremiumGreen, RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = PremiumGreen,
                modifier = Modifier.size(50.dp).align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Salman Laghari",
            color = TextMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Pro Member • salman@risingflix.com",
            color = TextSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile options list
        ProfileOptionItem(icon = Icons.Default.Settings, title = "App Settings")
        ProfileOptionItem(icon = Icons.Default.Refresh, title = "Watch History")
        ProfileOptionItem(icon = Icons.Default.ArrowDownward, title = "Offline Downloads")
        ProfileOptionItem(icon = Icons.Default.Info, title = "About App & Licensing")

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Rising Flix v1.0.0 (Premium Platform)",
            color = TextSub.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ProfileOptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(14.dp))
            .border(1.dp, BorderSoft.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PremiumGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSub.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun NoResultsFoundView(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Empty",
            tint = TextSub.copy(alpha = 0.6f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results found for",
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "\"$query\"",
            color = PremiumGreen,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "Please check the spelling or search for alternative movies/dramas categories.",
            color = TextSub,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
