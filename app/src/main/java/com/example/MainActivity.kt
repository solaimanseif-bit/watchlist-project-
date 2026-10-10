package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalTextToolbar
import com.example.ui.CineTrackHomeScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.SafeTextToolbar

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalTextToolbar provides SafeTextToolbar) {
                MyApplicationTheme {
                    CineTrackHomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
