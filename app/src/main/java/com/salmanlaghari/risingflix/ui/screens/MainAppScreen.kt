package com.salmanlaghari.risingflix.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.salmanlaghari.risingflix.ui.components.PremiumBottomBar
import com.salmanlaghari.risingflix.ui.theme.DeepBlueBg
import com.salmanlaghari.risingflix.viewmodel.MainViewModel

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var isSplashFinished by remember { mutableStateOf(false) }
    val selectedVideoDetails by viewModel.selectedVideoDetails.collectAsState()

    if (!isSplashFinished) {
        SplashScreen(
            onSplashFinished = { isSplashFinished = true },
            modifier = modifier.fillMaxSize()
        )
    } else {
        HomeScreenContainer(
            viewModel = viewModel,
            modifier = modifier.fillMaxSize()
        )

        // Overlay Player Details & ExoPlayer Screen if a video is actively selected!
        selectedVideoDetails?.let { details ->
            PlayerScreen(
                videoDetails = details,
                onBackClick = { viewModel.selectVideo(null) },
                onRelatedVideoClick = { related ->
                    viewModel.selectVideo(related)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun HomeScreenContainer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentNavSection by viewModel.currentNavSection.collectAsState()

    Scaffold(
        bottomBar = {
            PremiumBottomBar(
                currentSection = currentNavSection,
                onSectionSelected = { viewModel.setNavSection(it) }
            )
        },
        containerColor = DeepBlueBg,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentNavSection) {
                0 -> { // HOME
                    HomeScreen(
                        viewModel = viewModel,
                        onVideoSelected = { viewModel.selectVideo(it) },
                        onSearchIconClick = { viewModel.setNavSection(1) }
                    )
                }
                1 -> { // EXPLORE
                    ExploreScreen(
                        viewModel = viewModel,
                        onVideoSelected = { viewModel.selectVideo(it) }
                    )
                }
                2 -> { // LIBRARY
                    LibraryScreen(
                        onVideoSelected = { viewModel.selectVideo(it) }
                    )
                }
                3 -> { // PROFILE
                    ProfileScreen()
                }
            }
        }
    }
}
