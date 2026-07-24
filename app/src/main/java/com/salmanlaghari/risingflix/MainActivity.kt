package com.salmanlaghari.risingflix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.salmanlaghari.risingflix.ui.screens.MainAppScreen
import com.salmanlaghari.risingflix.ui.theme.RisingFlixTheme
import com.salmanlaghari.risingflix.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RisingFlixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(viewModel = mainViewModel)
                }
            }
        }
    }
}
