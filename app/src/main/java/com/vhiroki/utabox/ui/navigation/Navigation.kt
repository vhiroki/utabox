package com.vhiroki.utabox.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vhiroki.utabox.data.Song
import com.vhiroki.utabox.data.SongDatabase
import com.vhiroki.utabox.data.SongRepository
import com.vhiroki.utabox.ui.player.PlayerScreen
import com.vhiroki.utabox.ui.player.PlayerViewModel
import com.vhiroki.utabox.ui.songlist.SongListScreen
import com.vhiroki.utabox.ui.songlist.SongListViewModel
import com.vhiroki.utabox.util.VideoStorageHelper
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val SONG_LIST = "songList"
    const val PLAYER = "player/{musicId}/{artista}/{musica}/{inicio}"

    fun playerRoute(song: Song): String {
        val encode = { s: String -> URLEncoder.encode(s, StandardCharsets.UTF_8.toString()) }
        return "player/${encode(song.musicId)}/${encode(song.artista)}/${encode(song.musica)}/${encode(song.inicio)}"
    }
}

@Composable
fun UtaBoxNavHost(
    navController: NavHostController,
    onRequestFolderPicker: () -> Unit
) {
    val context = LocalContext.current

    // Create dependencies
    val database = remember { SongDatabase.getDatabase(context) }
    val repository = remember { SongRepository(database.songDao()) }
    val videoStorageHelper = remember { VideoStorageHelper(context) }

    NavHost(
        navController = navController,
        startDestination = Routes.SONG_LIST
    ) {
        composable(Routes.SONG_LIST) {
            val viewModel: SongListViewModel = viewModel(
                factory = SongListViewModel.Factory(repository)
            )

            // Recompute on each recomposition
            val videoSourceDescription = remember {
                androidx.compose.runtime.mutableStateOf(videoStorageHelper.getVideoSourceDescription())
            }
            // Update when screen is shown
            androidx.compose.runtime.LaunchedEffect(Unit) {
                videoSourceDescription.value = videoStorageHelper.getVideoSourceDescription()
            }

            SongListScreen(
                viewModel = viewModel,
                onSongClick = { song ->
                    navController.navigate(Routes.playerRoute(song))
                },
                videoSourceDescription = videoSourceDescription.value
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("musicId") { type = NavType.StringType },
                navArgument("artista") { type = NavType.StringType },
                navArgument("musica") { type = NavType.StringType },
                navArgument("inicio") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val decode = { s: String? -> URLDecoder.decode(s ?: "", StandardCharsets.UTF_8.toString()) }

            val song = Song(
                musicId = decode(backStackEntry.arguments?.getString("musicId")),
                artista = decode(backStackEntry.arguments?.getString("artista")),
                musica = decode(backStackEntry.arguments?.getString("musica")),
                inicio = decode(backStackEntry.arguments?.getString("inicio"))
            )

            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.Factory(videoStorageHelper)
            )

            PlayerScreen(
                viewModel = viewModel,
                song = song,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
