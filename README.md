# UtaBox 🎤

A Karaoke player Android app for tablets that plays video files (.mp4) from USB storage or local folders.

![Android](https://img.shields.io/badge/Android-13%2B-green) ![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple)

## Features

- 🎵 **Song Catalog** - Browse 1,500+ karaoke songs with search functionality
- 🔍 **Smart Search** - Filter by song ID, title, or artist name in real-time
- 📺 **Video Playback** - Full-screen karaoke video player with Media3 ExoPlayer
- 💾 **USB Support** - Auto-detects USB flash drives with video files
- 📱 **Tablet Optimized** - Landscape-only UI designed for tablets

## Screenshots

*Coming soon*

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose (Material 3) |
| Video Player | Media3 ExoPlayer |
| Database | Room |
| Architecture | MVVM |
| Navigation | Navigation Compose |
| Min SDK | API 33 (Android 13) |

## Project Structure

```
app/src/main/java/com/vhiroki/utabox/
├── MainActivity.kt
├── data/
│   ├── Song.kt                 # Room entity
│   ├── SongDao.kt              # Data Access Object
│   ├── SongDatabase.kt         # Room database
│   └── SongRepository.kt       # Repository pattern
├── ui/
│   ├── navigation/
│   │   └── Navigation.kt       # NavHost setup
│   ├── songlist/
│   │   ├── SongListScreen.kt   # Song list UI
│   │   └── SongListViewModel.kt
│   ├── player/
│   │   ├── PlayerScreen.kt     # Video player UI
│   │   └── PlayerViewModel.kt
│   └── theme/
│       └── Theme.kt
└── util/
    └── VideoStorageHelper.kt   # USB/storage detection
```

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 33+
- Kotlin 2.0+

### Video Files Location

The app searches for video files in the following locations (in order):

1. **Test folder**: `/sdcard/Download/karaoke/` (for development)
2. **USB drive**: Auto-detects removable storage with a `videoke/` folder
3. **User-selected folder**: Via Android's document picker

Video files should be named `{music_id}.mp4` (e.g., `02017.mp4`, `05340.mp4`).

### Building

```bash
# Clone the repository
git clone https://github.com/yourusername/UtaBox.git

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Database

The app comes with a pre-populated SQLite database containing ~1,550 songs.

## Usage

1. Copy karaoke video files to your USB drive in a `videoke/` folder
2. Connect USB to your Android tablet (via OTG adapter if needed)
3. Launch UtaBox and grant storage permissions
4. Search for songs by ID, title, or artist
5. Tap a song to play the video

## Permissions

- `READ_MEDIA_VIDEO` - Access video files on device storage
- USB Host - Access USB flash drives

## Roadmap

- [ ] YouTube video playback integration
- [ ] Song queue system (playlist)
- [ ] Favorites / recently played
- [ ] Remote song selection (companion app)
- [ ] Smartphone layout support

## License

This project is for personal use.

## Acknowledgments

- [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) for video playback
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI
- [Room](https://developer.android.com/training/data-storage/room) for database management
