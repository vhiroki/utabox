# AI Agent Instructions for UtaBox

This document provides AI-specific guidance. See [README.md](README.md) for project overview, tech stack, and setup instructions.

## Quick Reference

| Task | Command |
|------|---------|
| Build APK | `./build-apk.sh` |
| Clean build | `./build-apk.sh --clean` |
| Build & install | `./build-apk.sh --install` |
| Compile check | `./gradlew :app:compileDebugKotlin` |
| Check errors | `./gradlew :app:compileDebugKotlin 2>&1 \| tail -50` |

**APK Output**: `app/build/outputs/apk/debug/app-debug.apk`

## Key Files

| Purpose | Path |
|---------|------|
| Song list UI | `app/src/main/java/com/vhiroki/utabox/ui/songlist/SongListScreen.kt` |
| Video player | `app/src/main/java/com/vhiroki/utabox/ui/player/PlayerScreen.kt` |
| Navigation | `app/src/main/java/com/vhiroki/utabox/ui/navigation/Navigation.kt` |
| Song data class | `app/src/main/java/com/vhiroki/utabox/data/Song.kt` |
| Local CSV parser | `app/src/main/java/com/vhiroki/utabox/data/CsvSongReader.kt` |
| YouTube CSV loader | `app/src/main/java/com/vhiroki/utabox/data/YouTubeSongLoader.kt` |
| Repository | `app/src/main/java/com/vhiroki/utabox/data/SongRepository.kt` |
| Storage helper | `app/src/main/java/com/vhiroki/utabox/util/VideoStorageHelper.kt` |
| Dependencies | `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| YouTube songs | `youtube-songs/*.csv`, `youtube-songs/index.txt` |

## Common Tasks

### Adding a New Screen
1. Create `ui/yourfeature/YourScreen.kt` and `YourViewModel.kt`
2. Add navigation route in `ui/navigation/Navigation.kt`

### Modifying Song Data
1. Update `Song` data class in `data/Song.kt`
2. Update CSV parsing in `data/CsvSongReader.kt` (local) or `data/YouTubeSongLoader.kt` (YouTube)
3. Update UI components that display song info

### Adding YouTube Songs
1. Create/edit a CSV file in `youtube-songs/` folder with format: `code,title,artist,url`
2. Add the filename to `youtube-songs/index.txt`
3. Push to GitHub - app loads on startup and refresh

### Adding Dependencies
1. Add version to `gradle/libs.versions.toml`
2. Add dependency in `app/build.gradle.kts`

## Data Flow

```
Local CSV Files (in video folder)     YouTube CSVs (from GitHub)
           ↓                                    ↓
     CsvSongReader                      YouTubeSongLoader
           ↓                                    ↓
           └──────────→ SongRepository ←────────┘
                              ↓
                    SongListViewModel (StateFlow)
                              ↓
                    SongListScreen (Compose UI)
                              ↓
                    PlayerScreen (ExoPlayer / YouTube)
```

## Song Sources

| Source | Data Location | Video Playback |
|--------|--------------|----------------|
| Local | CSV in video folder | ExoPlayer (.mp4 files) |
| YouTube | `youtube-songs/*.csv` on GitHub | android-youtube-player |

## Code Style

- **UI**: Jetpack Compose only (no XML layouts)
- **State**: `StateFlow` in ViewModels, `collectAsState()` in Composables
- **Local state**: `remember { mutableStateOf() }`
- **Components**: Use `@Composable private fun` for sub-components
- **Naming**: `*Screen.kt`, `*ViewModel.kt`, `*Dao.kt`, `*Repository.kt`

## Workflow

1. Make changes
2. Run `./gradlew :app:compileDebugKotlin` to check for errors
3. Run `./build-apk.sh` to build APK
4. Test with `./build-apk.sh --install` if device connected

## Troubleshooting

```bash
# Dependency issues
./gradlew --refresh-dependencies

# Full clean rebuild
./build-apk.sh --clean

# Check YouTube loader logs
adb logcat | grep "YouTubeSongLoader"

# Check YouTube player logs
adb logcat | grep "YouTubePlayer"
```
