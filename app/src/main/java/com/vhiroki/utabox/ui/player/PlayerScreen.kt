package com.vhiroki.utabox.ui.player

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.vhiroki.utabox.data.Song

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    song: Song,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Prevent multiple back navigations
    var hasNavigatedBack by remember { mutableStateOf(false) }
    val safeBackClick: () -> Unit = {
        if (!hasNavigatedBack) {
            hasNavigatedBack = true
            onBackClick()
        }
    }

    // Load video when screen opens
    LaunchedEffect(song) {
        viewModel.loadVideo(song)
    }

    // Handle back button
    BackHandler(onBack = safeBackClick)

    // Create and remember ExoPlayer (only for local videos)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Update player when URI changes (only for local videos)
    LaunchedEffect(uiState.videoUri) {
        uiState.videoUri?.let { uri ->
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
    }

    // Clean up player when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = safeBackClick) {
                        Text("Go Back")
                    }
                }
            }
            uiState.isYouTube -> {
                // YouTube player using android-youtube-player library
                YouTubePlayerComposable(
                    videoId = uiState.youtubeUrl!!,
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier.fillMaxSize()
                )

                // Song info overlay at top
                SongInfoOverlay(
                    song = song,
                    onBackClick = safeBackClick,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            else -> {
                // Local video player
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Song info overlay at top
                SongInfoOverlay(
                    song = song,
                    onBackClick = safeBackClick,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

@Composable
private fun YouTubePlayerComposable(
    videoId: String,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State for error handling
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    android.util.Log.d("YouTubePlayer", "YouTubePlayerComposable called with videoId: $videoId")

    Box(modifier = modifier.background(Color.Black)) {
        if (hasError) {
            // Show error with option to open in YouTube app
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cannot play video",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.youtube.com/watch?v=$videoId")
                        )
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
                ) {
                    Text("Open in YouTube")
                }
            }
        } else {
            // Use key to ensure stable view
            key(videoId) {
                AndroidView(
                    factory = { ctx ->
                        android.util.Log.d("YouTubePlayer", "Creating YouTubePlayerView")
                        YouTubePlayerView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                            // Register lifecycle observer
                            lifecycleOwner.lifecycle.addObserver(this)

                            // Add player listener
                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    android.util.Log.d("YouTubePlayer", "onReady called, loading video: $videoId")
                                    youTubePlayer.loadVideo(videoId, 0f)
                                }

                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError
                                ) {
                                    android.util.Log.e("YouTubePlayer", "onError: $error")
                                    hasError = true
                                    errorMessage = "Error: $error"
                                }

                                override fun onStateChange(
                                    youTubePlayer: YouTubePlayer,
                                    state: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
                                ) {
                                    android.util.Log.d("YouTubePlayer", "onStateChange: $state")
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SongInfoOverlay(
    song: Song,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    if (song.isYouTube) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFF0000),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "YouTube",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${song.artist} • ${song.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
