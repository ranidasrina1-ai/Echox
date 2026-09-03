package com.echotune.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.echotune.app.ui.EchoViewModel
import com.echotune.app.ui.Screen
import com.echotune.app.ui.components.BottomNavBar
import com.echotune.app.ui.components.MiniPlayer
import com.echotune.app.ui.screens.HomeScreen
import com.echotune.app.ui.screens.LibraryScreen
import com.echotune.app.ui.screens.PlayerScreen
import com.echotune.app.ui.screens.SearchScreen

class MainActivity : ComponentActivity() {

    private val viewModel: EchoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EchoTuneApp(viewModel)
        }
    }
}

@Composable
private fun EchoTuneApp(viewModel: EchoViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val history by viewModel.history.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val showFullPlayer by viewModel.showFullPlayer.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Base layer: whichever tab is active, mini player, bottom nav.
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    Screen.HOME -> HomeScreen(
                        state = homeState,
                        onResultClick = { viewModel.play(it) }
                    )
                    Screen.SEARCH -> SearchScreen(
                        query = query,
                        onQueryChange = { viewModel.updateQuery(it) },
                        onSearch = { viewModel.submitSearch() },
                        state = searchState,
                        onResultClick = { viewModel.play(it) }
                    )
                    Screen.LIBRARY -> LibraryScreen(
                        history = history,
                        onResultClick = { viewModel.play(it) },
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }
            }

            nowPlaying?.let { stream ->
                MiniPlayer(
                    stream = stream,
                    isPlaying = isPlaying,
                    onClick = { viewModel.openFullPlayer() },
                    onTogglePlay = { viewModel.togglePlayPause() }
                )
            }

            BottomNavBar(current = currentScreen, onSelect = { viewModel.selectScreen(it) })
        }

        // Overlay: full player slides over everything (nav + mini player) when open.
        if (showFullPlayer) {
            nowPlaying?.let { stream ->
                PlayerScreen(
                    mode = mode,
                    stream = stream,
                    isPlaying = isPlaying,
                    playerHolder = viewModel.playerHolder(),
                    onBack = { viewModel.hideFullPlayer() },
                    onToggle = { viewModel.togglePlayPause() },
                    onModeChange = { viewModel.switchMode(it) }
                )
            }
        }
    }
}
