# UtaBox 🎤

A Karaoke player Android app for tablets that plays video files (.mp4) from USB storage or local folders, plus YouTube karaoke videos.

![Android](https://img.shields.io/badge/Android-13%2B-green) ![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple)

## Features

- 🎵 **Song Catalog** - Browse thousands of karaoke songs with search functionality
- 🔍 **Smart Search** - Filter by song ID, title, or artist name in real-time
- 📺 **Video Playback** - Full-screen karaoke video player with Media3 ExoPlayer
- 🎬 **YouTube Support** - Play YouTube karaoke videos with embedded player
- 💾 **USB Support** - Auto-detects USB flash drives with video files
- 📱 **Tablet Optimized** - Landscape-only UI designed for tablets
- 🔢 **Numpad Entry** - Quick song selection by code using the on-screen numpad

## Screenshots

*Coming soon*

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose (Material 3) |
| Video Player | Media3 ExoPlayer |
| YouTube Player | android-youtube-player 13.0.0 |
| Data Source | CSV files (local + GitHub) |
| Architecture | MVVM |
| Navigation | Navigation Compose |
| Min SDK | API 33 (Android 13) |

## Project Structure

```
app/src/main/java/com/vhiroki/utabox/
├── MainActivity.kt
├── data/
│   ├── Song.kt                 # Song data class
│   ├── CsvSongReader.kt        # Local CSV file parser
│   ├── YouTubeSongLoader.kt    # GitHub CSV loader for YouTube songs
│   └── SongRepository.kt       # Repository pattern
├── ui/
│   ├── navigation/
│   │   └── Navigation.kt       # NavHost setup
│   ├── songlist/
│   │   ├── SongListScreen.kt   # Song list UI with numpad
│   │   └── SongListViewModel.kt
│   ├── player/
│   │   ├── PlayerScreen.kt     # Video player UI (local + YouTube)
│   │   └── PlayerViewModel.kt
│   └── theme/
│       └── Theme.kt
└── util/
    └── VideoStorageHelper.kt   # USB/storage detection

youtube-songs/                   # YouTube song catalog (on GitHub)
├── index.txt                   # List of CSV files to load
├── international.csv           # International songs
└── *.csv                       # Additional song lists
```

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 33+
- Kotlin 2.0+

### Video Files Location

The app searches for video files in the following locations (in order):

1. **User-selected folder**: Via Android's document picker (recommended)
2. **Test folder**: `/sdcard/Download/karaoke/` (for development)
3. **USB drive**: Auto-detects removable storage with a `videoke/` folder

Video files should be named `{code}.mp4` (e.g., `02017.mp4`, `05340.mp4`).

### Song List (CSV)

Place one or more CSV files in the same folder as your video files. The app reads all `.csv` files and combines them.

**CSV Format:**
```csv
code,filename,artist,title,lyrics_preview
02017,02017.mp4,Ai,STORY,Kagirareta toki no naka de
02015,02015.mp4,Akikawa Masafumi,SEN NO KAZE NI NATTE,Watashi no ohaka no mae de
```

- `code` - Song ID (used for search and display)
- `filename` - Video filename
- `artist` - Artist name
- `title` - Song title
- `lyrics_preview` - Optional, currently not displayed

### Building

The project includes a convenient build script for generating APKs.

#### Using the Build Script (Recommended)

```bash
# Build debug APK
./build-apk.sh

# Clean build (removes previous artifacts first)
./build-apk.sh --clean

# Build and install on connected device
./build-apk.sh --install

# Clean build and install
./build-apk.sh --clean --install

# Show all options
./build-apk.sh --help
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

#### Using Gradle Directly

```bash
# Clone the repository
git clone https://github.com/yourusername/UtaBox.git

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Song Data

The app reads song information from CSV files in the video folder. No pre-populated database is needed - just place your CSV file(s) alongside the video files.

### YouTube Songs

YouTube karaoke songs are loaded automatically from this repository's `youtube-songs/` folder. The app fetches CSV files on startup and when you tap the refresh button.

**To add YouTube songs:**
1. Create or edit a CSV file in `youtube-songs/` with format:
   ```csv
   code,title,artist,url
   YT001,Yellow,Coldplay,https://www.youtube.com/watch?v=yKNxeF4KMsY
   ```
2. Add the CSV filename to `youtube-songs/index.txt`
3. Push to GitHub - the app will load the new songs on refresh

YouTube songs are displayed with a red "YouTube" badge in the song list.

## Usage

1. Prepare your karaoke files:
   - Video files (`.mp4`) named by song code (e.g., `02017.mp4`)
   - CSV file(s) with song information (see format above)
2. Copy files to USB drive or device storage
3. Launch UtaBox and grant storage permissions
4. Tap the folder icon (⚙️) to select your video folder
5. Search for songs by code, title, or artist
6. Tap a song to play the video
7. Use the refresh button (🔄) to reload after adding new files

## Permissions

- `READ_MEDIA_VIDEO` - Access video files on device storage
- USB Host - Access USB flash drives

## Roadmap

- [x] YouTube video playback integration
- [ ] Song queue system (playlist)
- [ ] Favorites / recently played
- [ ] Remote song selection (companion app)
- [ ] Smartphone layout support

## License

This project is for personal use.

## Acknowledgments

- [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) for video playback
- [android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player) for YouTube integration
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI
