package com.salmanlaghari.risingflix.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.salmanlaghari.risingflix.viewmodel.MainViewModel

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var isSplashFinished by remember { mutableStateOf(false) }
    val selectedVideo by viewModel.selectedVideo.collectAsState()

    if (!isSplashFinished) {
        SplashScreen(
            onSplashFinished = { isSplashFinished = true },
            modifier = modifier.fillMaxSize()
        )
    } else {
        HomeScreen(
            viewModel = viewModel,
            onVideoSelected = { viewModel.selectVideo(it) },
            modifier = modifier.fillMaxSize()
        )

        // Overlay Built-in Player Screen on top of HomeScreen if a video is actively selected!
        selectedVideo?.let { video ->
            BuiltInPlayerScreen(
                video = video,
                onDismiss = { viewModel.selectVideo(null) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
