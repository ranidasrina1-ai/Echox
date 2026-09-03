package com.echotune.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.echotune.app.data.SearchResult
import com.echotune.app.ui.HomeUiState
import com.echotune.app.ui.components.MediaRow

@Composable
fun HomeScreen(state: HomeUiState, onResultClick: (SearchResult) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Trending",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        when (state) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            }
            is HomeUiState.Error -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("⚠ ${state.message}", color = Color(0xFFFF6B6B))
            }
            is HomeUiState.Success -> {
                if (state.results.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Nothing trending right now.", color = Color(0xFFA7A7A7))
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(state.results) { item -> MediaRow(item, onResultClick) }
                    }
                }
            }
        }
    }
}
