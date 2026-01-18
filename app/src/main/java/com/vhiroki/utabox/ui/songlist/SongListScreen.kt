package com.vhiroki.utabox.ui.songlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vhiroki.utabox.data.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    viewModel: SongListViewModel,
    onSongClick: (Song) -> Unit,
    onSelectFolder: () -> Unit,
    videoSourceDescription: String,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // State for numpad input
    var numpadInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UtaBox Karaoke") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onSelectFolder) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Select folder",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { viewModel.reload() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload songs",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left side - Song list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Video source indicator
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = videoSourceDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search by ID, song name, or artist...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                // Results count
                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Song list
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = songs,
                            key = { it.code }
                        ) { song ->
                            SongCard(
                                song = song,
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
            }

            // Right side - Numpad panel
            NumpadPanel(
                input = numpadInput,
                onInputChange = { numpadInput = it },
                onEnterClick = {
                    // Find song by ID and trigger click
                    val song = songs.find { it.code == numpadInput }
                    if (song != null) {
                        onSongClick(song)
                    } else {
                        // Search for the entered ID
                        viewModel.onSearchQueryChanged(numpadInput)
                    }
                    numpadInput = ""
                },
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song ID badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = song.code,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // YouTube indicator on the right
            if (song.isYouTube) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = Color(0xFFFF0000),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "YouTube",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "YouTube",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadPanel(
    input: String,
    onInputChange: (String) -> Unit,
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Song Code",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display for input
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = input.ifEmpty { "—" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (input.isEmpty())
                        MaterialTheme.colorScheme.outline
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numpad buttons
            val buttonSize = 72.dp

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: 1, 2, 3
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumpadButton("1", onClick = { onInputChange(input + "1") }, modifier = Modifier.size(buttonSize))
                    NumpadButton("2", onClick = { onInputChange(input + "2") }, modifier = Modifier.size(buttonSize))
                    NumpadButton("3", onClick = { onInputChange(input + "3") }, modifier = Modifier.size(buttonSize))
                }

                // Row 2: 4, 5, 6
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumpadButton("4", onClick = { onInputChange(input + "4") }, modifier = Modifier.size(buttonSize))
                    NumpadButton("5", onClick = { onInputChange(input + "5") }, modifier = Modifier.size(buttonSize))
                    NumpadButton("6", onClick = { onInputChange(input + "6") }, modifier = Modifier.size(buttonSize))
                }

                // Row 3: 7, 8, 9
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumpadButton("7", onClick = { onInputChange(input + "7") }, modifier = Modifier.size(buttonSize))
                    NumpadButton("8", onClick = { onInputChange(input + "8") }, modifier = Modifier.size(buttonSize))
                    NumpadButton("9", onClick = { onInputChange(input + "9") }, modifier = Modifier.size(buttonSize))
                }

                // Row 4: Clear, 0, Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumpadButton(
                        text = "C",
                        onClick = { onInputChange("") },
                        modifier = Modifier.size(buttonSize),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                    NumpadButton("0", onClick = { onInputChange(input + "0") }, modifier = Modifier.size(buttonSize))
                    NumpadButton(
                        text = "⌫",
                        onClick = { if (input.isNotEmpty()) onInputChange(input.dropLast(1)) },
                        modifier = Modifier.size(buttonSize)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enter button
            Button(
                onClick = onEnterClick,
                enabled = input.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Enter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NumpadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
