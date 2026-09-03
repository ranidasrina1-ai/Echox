# Echo Tune

A native Android YouTube-based **video + music player** built with Kotlin, Jetpack Compose, and [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor). No official YouTube API key required — NewPipeExtractor scrapes YouTube directly. UI inspired by Spotify and the Echo open-source music player.

> **Note:** This project is for educational purposes. Streaming from YouTube via NewPipeExtractor may violate YouTube's Terms of Service; use responsibly and check applicable laws in your region.

---

## Features (v1)

- **Two modes** — toggle between 🎥 Video and 🎵 Music at the top of the app.
- **Search** — type a query, results load from NewPipeExtractor's YouTube search.
- **Results list** — each row shows thumbnail, title, uploader, duration/views; whole row tappable.
- **Video playback** — fetches stream info and plays the 360p (or closest) video stream in a Media3 `PlayerView`.
- **Music playback** — Spotify-style full-screen layout: large album art, seekbar with current/total time, play/pause. Audio continues in the background via a foreground `MediaSessionService`.
- **Persistent mini-player** — a bottom bar (thumbnail + title + play/pause) stays visible while you browse; tap it to reopen the full player.

Out of scope for v1 (planned for later): downloads, playlists, favorites, login.

---

## Tech stack

| Layer | Library |
|-------|---------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose (Material 3) |
| Media | Media3 / ExoPlayer |
| YouTube data | NewPipeExtractor (via JitPack) + jsoup |
| HTTP | OkHttp (NewPipeExtractor downloader) |
| Images | Coil |
| Async | Kotlin Coroutines |

---

## Build

### Prerequisites
- JDK 17
- Android SDK (compileSdk 34)
- Internet access (JitPack + Google Maven for dependencies)

### Local build
```bash
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

### CI build (GitHub Actions)
A workflow is included at `.github/workflows/build.yml`. It:
- Triggers on every push to `main`, and supports manual `workflow_dispatch`.
- Uses `ubuntu-latest`, JDK 17 (temurin).
- Runs `./gradlew assembleDebug`.
- Uploads `app-debug.apk` as a downloadable workflow artifact.

Push the repo to GitHub and the Action will produce an installable debug APK — no local build needed.

---

## Project structure

```
app/src/main/java/com/echotune/app/
├── MainActivity.kt                 # Entry point; wires Compose UI to the ViewModel
├── data/
│   ├── Models.kt                   # SearchResult, PlayableStream data classes
│   ├── NewPipeRepository.kt        # Search + stream-info fetching via NewPipeExtractor
│   └── DownloaderImpl.kt           # OkHttp-backed Downloader for NewPipeExtractor
├── playback/
│   ├── PlayerHolder.kt             # Shared ExoPlayer instance
│   └── PlaybackService.kt          # Foreground MediaSessionService (background audio)
└── ui/
    ├── EchoViewModel.kt            # App-wide state: mode, search, now-playing
    ├── theme/Colors.kt
    ├── components/
    │   ├── ModeToggle.kt           # Video / Music toggle
    │   └── MiniPlayer.kt           # Persistent bottom mini-player
    └── screens/
        ├── SearchScreen.kt          # Search bar + results list
        └── PlayerScreen.kt          # Video (PlayerView) + Music (Spotify-style) layouts
```

---

## How it works

1. **Search** — `NewPipeRepository.search()` calls `SearchInfo.getInfo()` on the YouTube service; results are mapped to `SearchResult`s.
2. **Tap a result** — `EchoViewModel.play()` calls `NewPipeRepository.fetchStream()` to resolve stream URLs via `StreamInfo.getInfo()`:
   - Video mode picks the lowest-resolution stream ≥ 360p.
   - Music mode picks the highest-bitrate audio-only stream.
3. **Playback** — the shared `ExoPlayer` (in `PlayerHolder`) loads the URL; the Video screen embeds a Media3 `PlayerView`, the Music screen renders album art + seekbar.
4. **Background** — `PlaybackService` (a `MediaSessionService`) keeps audio alive when the app is backgrounded.
5. **Mini-player** — whenever something is playing and the full player is closed, the mini-player bar shows at the bottom.

---

## License

This project is provided as-is for educational use. NewPipeExtractor is licensed under GPL-3.0.
