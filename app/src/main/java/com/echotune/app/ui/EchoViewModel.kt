package com.echotune.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echotune.app.data.HistoryStore
import com.echotune.app.data.NewPipeRepository
import com.echotune.app.data.PlayableStream
import com.echotune.app.data.SearchResult
import com.echotune.app.playback.PlayerHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Mode { VIDEO, MUSIC }

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<SearchResult>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val results: List<SearchResult>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class EchoViewModel(app: Application) : AndroidViewModel(app) {

    private val holder = PlayerHolder(app)

    private var currentSourceUrl: String? = null
    private var retryCount = 0
    private val maxAutoRetries = 2

    private val _history = MutableStateFlow<List<SearchResult>>(emptyList())
    val history: StateFlow<List<SearchResult>> = _history.asStateFlow()

    init {
        holder.onPlaybackError = { positionMs, _ -> retryCurrentStream(positionMs) }
        _history.value = HistoryStore.getHistory(app)
        loadHome()
    }

    // ---- Bottom nav ----

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun selectScreen(screen: Screen) { _currentScreen.value = screen }

    // ---- Video/Music mode ----

    private val _mode = MutableStateFlow(Mode.MUSIC)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    fun switchMode(newMode: Mode) {
        if (_mode.value == newMode) return
        _mode.value = newMode
        val stream = _nowPlaying.value ?: return
        val url = when (newMode) {
            Mode.VIDEO -> stream.videoUrl ?: stream.audioUrl
            Mode.MUSIC -> stream.audioUrl ?: stream.videoUrl
        } ?: return
        val position = holder.player.currentPosition
        holder.playUrl(url, startPositionMs = position)
        _isPlaying.value = true
    }

    // ---- Home (trending) ----

    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private fun loadHome() {
        viewModelScope.launch {
            _homeState.value = HomeUiState.Loading
            try {
                val results = withContext(Dispatchers.IO) { NewPipeRepository.trending() }
                _homeState.value = HomeUiState.Success(results)
            } catch (t: Throwable) {
                _homeState.value = HomeUiState.Error(t.message ?: "Couldn't load trending")
            }
        }
    }

    // ---- Search ----

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun updateQuery(q: String) { _query.value = q }

    fun submitSearch() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        _searchState.value = SearchUiState.Loading
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) { NewPipeRepository.search(q) }
                _searchState.value = SearchUiState.Success(results)
            } catch (t: Throwable) {
                _searchState.value = SearchUiState.Error(t.message ?: "Search failed")
            }
        }
    }

    // ---- Library ----

    fun clearHistory() {
        HistoryStore.clear(getApplication())
        _history.value = emptyList()
    }

    // ---- Playback ----

    private val _nowPlaying = MutableStateFlow<PlayableStream?>(null)
    val nowPlaying: StateFlow<PlayableStream?> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    fun play(result: SearchResult) {
        currentSourceUrl = result.url
        retryCount = 0
        viewModelScope.launch {
            try {
                val stream = withContext(Dispatchers.IO) {
                    NewPipeRepository.fetchStream(result.url)
                }
                _nowPlaying.value = stream
                val url = when (_mode.value) {
                    Mode.VIDEO -> stream.videoUrl ?: stream.audioUrl
                    Mode.MUSIC -> stream.audioUrl ?: stream.videoUrl
                }
                if (url != null) {
                    holder.playUrl(url)
                    _isPlaying.value = true
                    _showFullPlayer.value = true
                    HistoryStore.addToHistory(getApplication(), result)
                    _history.value = HistoryStore.getHistory(getApplication())
                }
            } catch (t: Throwable) {
                _searchState.value = SearchUiState.Error(t.message ?: "Playback failed")
            }
        }
    }

    private fun retryCurrentStream(positionMs: Long) {
        val sourceUrl = currentSourceUrl ?: return
        if (retryCount >= maxAutoRetries) {
            _searchState.value = SearchUiState.Error("Playback failed after retrying — link may be region-locked or unavailable.")
            return
        }
        retryCount++
        viewModelScope.launch {
            try {
                val stream = withContext(Dispatchers.IO) {
                    NewPipeRepository.refreshStream(sourceUrl)
                }
                _nowPlaying.value = stream
                val url = when (_mode.value) {
                    Mode.VIDEO -> stream.videoUrl ?: stream.audioUrl
                    Mode.MUSIC -> stream.audioUrl ?: stream.videoUrl
                }
                if (url != null) {
                    holder.playUrl(url, startPositionMs = positionMs)
                    _isPlaying.value = true
                }
            } catch (t: Throwable) {
                _searchState.value = SearchUiState.Error(t.message ?: "Playback failed")
            }
        }
    }

    fun togglePlayPause() {
        holder.togglePlayPause()
        _isPlaying.value = holder.player.isPlaying
    }

    fun openFullPlayer() { _showFullPlayer.value = true }
    fun hideFullPlayer() { _showFullPlayer.value = false }

    fun playerHolder(): PlayerHolder = holder

    override fun onCleared() {
        holder.release()
        super.onCleared()
    }
}
