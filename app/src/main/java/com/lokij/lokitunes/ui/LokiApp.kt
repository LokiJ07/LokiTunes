package com.lokij.lokitunes.ui

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import android.media.MediaMetadataRetriever
import android.media.audiofx.AudioEffect
import com.lokij.lokitunes.lyrics.LrcParser
import com.lokij.lokitunes.lyrics.LyricDocument
import com.lokij.lokitunes.lyrics.LyricsStore
import com.lokij.lokitunes.lyrics.LrcLibClient
import com.lokij.lokitunes.lyrics.RemoteLyricHit
import com.lokij.lokitunes.lyrics.ResolvedLyrics
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.lokij.lokitunes.playback.AudioTuning
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokij.lokitunes.BuildConfig
import com.lokij.lokitunes.update.UpdatePhase
import com.lokij.lokitunes.update.UpdateUiState
import com.lokij.lokitunes.update.UpdateViewModel
import com.lokij.lokitunes.data.MusicPaths
import com.lokij.lokitunes.data.MusicTrack
import com.lokij.lokitunes.data.IndexedSong
import com.lokij.lokitunes.data.LibraryInsights
import com.lokij.lokitunes.data.SongSort
import com.lokij.lokitunes.data.SavedPlaylist
import com.lokij.lokitunes.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

private val albumGradient = Brush.linearGradient(listOf(Color(0xFF7655B7), Color(0xFF266C86), Color(0xFF135B60)))
private val heroGradient = Brush.linearGradient(listOf(Color(0xFF51348B), Color(0xFF194961), NightPanel))
private val tabs = listOf("Home", "Songs", "Folders", "Favorites", "Library")
private val tabIcons = listOf(Icons.Default.Home, Icons.Default.MusicNote, Icons.Default.Folder, Icons.Default.Favorite, Icons.Default.LibraryMusic)

@Composable
fun LokiApp(state: LokiUiState, vm: LokiViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    val currentTrack = state.tracks.firstOrNull { it.id == state.currentTrackId }
    val updater: UpdateViewModel = viewModel()
    val updateState by updater.state.collectAsState()
    val context = LocalContext.current
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var announcedRelease by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { updater.checkForUpdates(silent = true) }
    LaunchedEffect(updateState.phase, updateState.release?.tag) {
        val release = updateState.release
        if (updateState.phase == UpdatePhase.AVAILABLE && release != null && release.tag != announcedRelease) {
            announcedRelease = release.tag
            showUpdateDialog = true
        }
    }
    if (showUpdateDialog) {
        UpdateDialog(
            state = updateState,
            onClose = { showUpdateDialog = false },
            onCheck = { updater.checkForUpdates() },
            onDownload = { updater.downloadRelease() },
            onInstall = { updater.requestInstallation(context) },
            onReleases = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/LokiJ07/LokiTunes/releases")))
                }
            }
        )
    }

    val picking = state.tracks.firstOrNull { it.id == state.playlistPickerTrackId }
    if (picking != null) {
        AlertDialog(
            onDismissRequest = vm::hidePlaylistPicker,
            title = { Text("Add to playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(picking.title, fontSize = 13.sp, color = SoftText, maxLines = 2)
                    if (state.playlists.isEmpty()) {
                        Text("No playlists yet. Create one in Library → Playlists.", color = SoftText)
                    } else {
                        LazyColumn(Modifier.heightIn(max = 340.dp)) {
                            items(state.playlists, key = { it.id }) { playlist ->
                                TextButton(onClick = {
                                    vm.addTrackToPlaylist(playlist.id, picking.id)
                                    vm.hidePlaylistPicker()
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.PlaylistAdd, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(playlist.name, modifier = Modifier.weight(1f), maxLines = 1)
                                    if (picking.id in playlist.trackIds) Icon(Icons.Default.Check, null)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.hidePlaylistPicker(); selectedTab = 4 }) {
                    Text("Open Library")
                }
            },
            dismissButton = { TextButton(onClick = vm::hidePlaylistPicker) { Text("Close") } },
            containerColor = NightPanel
        )
    }

    BackHandler(enabled = showPlayer) { showPlayer = false }
    if (showPlayer && currentTrack != null) {
        NowPlayingScreen(currentTrack, state, vm, onClose = { showPlayer = false })
        return
    }

    Scaffold(
        containerColor = Midnight,
        bottomBar = {
            Column {
                if (currentTrack != null) {
                    MiniPlayer(currentTrack, state.isPlaying, vm, onOpen = { showPlayer = true })
                }
                NavigationBar(containerColor = NightPanel, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { index, name ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tabIcons[index], contentDescription = name, modifier = Modifier.size(21.dp)) },
                            label = { Text(name, fontSize = 10.sp, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Midnight,
                                selectedTextColor = Lavender,
                                indicatorColor = Lavender,
                                unselectedIconColor = SoftText,
                                unselectedTextColor = SoftText
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppHeader(
                onRefresh = vm::refresh,
                busy = state.isScanning,
                hasUpdate = updateState.phase == UpdatePhase.AVAILABLE || updateState.phase == UpdatePhase.READY,
                onUpdates = {
                    showUpdateDialog = true
                    if (updateState.phase == UpdatePhase.IDLE || updateState.phase == UpdatePhase.ERROR ||
                        updateState.phase == UpdatePhase.CURRENT) updater.checkForUpdates()
                }
            )
            if (state.error != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF442636)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.error, modifier = Modifier.weight(1f), color = Color(0xFFFFBEC5), fontSize = 12.sp)
                    IconButton(onClick = vm::dismissError, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Color.White)
                    }
                }
            }
            if (state.notice != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF173B38)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.notice, modifier = Modifier.weight(1f), color = Mint, fontSize = 12.sp)
                    IconButton(onClick = vm::dismissNotice, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Color.White)
                    }
                }
            }
            when (selectedTab) {
                0 -> HomeScreen(state, vm, openSongs = { selectedTab = 1 })
                1 -> SongsScreen(state, vm)
                2 -> FoldersScreen(state, vm)
                3 -> FavoritesScreen(state, vm)
                else -> LibraryScreen(state, vm)
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    state: UpdateUiState,
    onClose: () -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onReleases: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("LokiTunes updates 💜", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = "Installed version: v${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}",
                    fontSize = 12.sp,
                    color = SoftText
                )
                when (state.phase) {
                    UpdatePhase.CHECKING -> {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Checking GitHub Releases…")
                    }
                    UpdatePhase.CURRENT -> Text("You're up to date! Your music still works fully offline.")
                    UpdatePhase.AVAILABLE, UpdatePhase.DOWNLOADING, UpdatePhase.READY -> {
                        Text("New signed release: ${state.release?.tag.orEmpty()}", color = Lavender,
                            fontWeight = FontWeight.SemiBold)
                        val notes = state.release?.notes.orEmpty().trim()
                        if (notes.isNotEmpty()) Text(notes.take(600), maxLines = 10)
                        if (state.phase == UpdatePhase.DOWNLOADING) {
                            LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
                            Text("Downloading ${state.progress}%…")
                        }
                        if (state.phase == UpdatePhase.READY) Text("APK verified. Tap Install to continue in Android's installer.")
                    }
                    UpdatePhase.ERROR -> Text(state.message.ifEmpty { "Could not check for updates." }, color = Color(0xFFFFBEC5))
                    UpdatePhase.IDLE -> Text("Check the latest public release on GitHub.")
                }
                if (state.message.isNotBlank() && state.phase == UpdatePhase.READY) {
                    Text(state.message, color = Mint, fontSize = 12.sp)
                }
                Text("Only update checks/downloads require internet. Music playback stays offline.",
                    fontSize = 12.sp, color = SoftText)
                TextButton(onClick = onReleases, contentPadding = PaddingValues(0.dp)) {
                    Text("View releases on GitHub", color = Mint, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            when (state.phase) {
                UpdatePhase.AVAILABLE -> Button(onClick = onDownload) { Text("Download update") }
                UpdatePhase.READY -> Button(onClick = onInstall) { Text("Install update") }
                UpdatePhase.ERROR -> {
                    if (state.release != null) Button(onClick = onDownload) { Text("Retry download") }
                    else Button(onClick = onCheck) { Text("Retry") }
                }
                UpdatePhase.CURRENT, UpdatePhase.IDLE -> TextButton(onClick = onCheck) { Text("Check again") }
                else -> Unit
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Close") } },
        containerColor = NightPanel
    )
}

@Composable
private fun AppHeader(onRefresh: () -> Unit, busy: Boolean, hasUpdate: Boolean, onUpdates: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(albumGradient),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.GraphicEq, "LokiTunes", tint = Color.White) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("LokiTunes", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = Color.White)
            Text("OFFLINE · YOUR SOUNDTRACK", fontSize = 9.sp, letterSpacing = 1.7.sp, color = Mint)
        }
        IconButton(onClick = onUpdates) {
            Box {
                Icon(Icons.Default.SystemUpdate, contentDescription = "Check for LokiTunes updates", tint = if (hasUpdate) Mint else SoftText)
                if (hasUpdate) {
                    Box(Modifier.size(8.dp).align(Alignment.TopEnd).clip(CircleShape).background(Lavender))
                }
            }
        }
        IconButton(onClick = onRefresh) {
            if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Refresh, contentDescription = "Rescan music", tint = SoftText)
        }
    }
}

@Composable
private fun HomeScreen(state: LokiUiState, vm: LokiViewModel, openSongs: () -> Unit) {
    val recent = remember(state.tracks) { state.tracks.sortedByDescending { it.dateAdded }.take(7) }
    val last = state.tracks.firstOrNull { it.id == state.lastPlayedId }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    .clip(RoundedCornerShape(26.dp)).background(heroGradient).padding(22.dp)
            ) {
                Text("YOUR MUSIC, YOUR WORLD", fontSize = 10.sp, letterSpacing = 2.sp, color = Mint)
                Spacer(Modifier.height(11.dp))
                Text("Feel every\nbeat.", fontSize = 36.sp, lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("${state.tracks.size} tracks  ·  ${state.tracks.map { it.folderPath }.distinct().size} folders", color = Color(0xFFE1D5F8), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = openSongs, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Midnight)) {
                    Icon(Icons.Default.Headphones, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Explore your music", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (last != null) {
            item {
                SectionTitle("Jump back in", "Your last selected track")
                TrackRow(last, state.favorites.contains(last.id), onPlay = { vm.playTracks(listOf(last), 0) }, onFavorite = { vm.toggleFavorite(last.id) }, onPlayNext = { vm.queueTrack(last, true) }, onAddToQueue = { vm.queueTrack(last, false) }, onPickPlaylist = { vm.showPlaylistPicker(last.id) })
            }
        }
        item { SectionTitle("Fresh in your library", "Recently added") }
        if (recent.isEmpty()) item { EmptyState("No music found yet", "Copy some audio files to your phone, then tap refresh.") }
        items(recent, key = { it.id }) { track ->
            TrackRow(track, state.favorites.contains(track.id), onPlay = { vm.playTracks(recent, recent.indexOf(track)) }, onFavorite = { vm.toggleFavorite(track.id) }, onPlayNext = { vm.queueTrack(track, true) }, onAddToQueue = { vm.queueTrack(track, false) }, onPickPlaylist = { vm.showPlaylistPicker(track.id) })
        }
    }
}

@Composable
private fun SongsScreen(state: LokiUiState, vm: LokiViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    val songs = remember(state.tracks, query, state.songSort) {
        val filtered = state.tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) ||
                it.album.contains(query, true) || it.folderPath.contains(query, true)
        }
        val byId = filtered.associateBy { it.id }
        LibraryInsights.sorted(filtered.map { it.asIndexedSong() }, state.songSort).mapNotNull { byId[it.id] }
    }
    Column(Modifier.fillMaxSize()) {
        SectionHeader("All songs", "${state.tracks.size} tracks ready to play")
        SearchBox(query, { query = it })
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${songs.size} matches", color = SoftText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { sortMenu = true }) {
                    Icon(Icons.Default.Sort, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(state.songSort.label)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SongSort.entries.forEach { mode ->
                        DropdownMenuItem(text = { Text(mode.label) }, onClick = {
                            vm.setSongSort(mode)
                            sortMenu = false
                        }, trailingIcon = {
                            if (mode == state.songSort) Icon(Icons.Default.Check, null)
                        })
                    }
                }
            }
        }
        TrackList(songs, state.favorites, vm, "No matching songs", "Try another search or refresh the library.")
    }
}

private fun MusicTrack.asIndexedSong() = IndexedSong(id, title, artist, album, durationMs, dateAdded)

@Composable
private fun FoldersScreen(state: LokiUiState, vm: LokiViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var explorerPath by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = (mode == 0 && selectedFolder != null) || (mode == 1 && explorerPath.isNotEmpty())) {
        if (mode == 0) selectedFolder = null else explorerPath = explorerPath.substringBeforeLast('/', "")
    }
    Column(Modifier.fillMaxSize()) {
        SectionHeader("Your folders", "Browse where your songs actually live")
        Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Folder list") }, leadingIcon = { Icon(Icons.Default.Folder, null, Modifier.size(16.dp)) })
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Explorer") }, leadingIcon = { Icon(Icons.Default.AccountTree, null, Modifier.size(16.dp)) })
        }
        if (mode == 0) {
            if (selectedFolder == null) {
                val groups = remember(state.tracks) { state.tracks.groupBy { it.folderPath }.toSortedMap() }
                if (groups.isEmpty()) EmptyState("No music folders", "Tap refresh after adding music.")
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(groups.keys.toList()) { folder ->
                        FolderRow(MusicPaths.folderLabel(folder), folder, groups.getValue(folder).size) { selectedFolder = folder }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedFolder = null }) { Icon(Icons.Default.ArrowBack, "Back", tint = Mint) }
                    Text(selectedFolder.orEmpty(), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Lavender)
                }
                val tracks = state.tracks.filter { it.folderPath == selectedFolder }.sortedBy { it.title.lowercase() }
                TrackList(tracks, state.favorites, vm, "Empty folder", "No indexed audio here.")
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (explorerPath.isNotEmpty()) {
                    IconButton(onClick = { explorerPath = explorerPath.substringBeforeLast('/', "") }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Mint)
                    }
                }
                Text(if (explorerPath.isBlank()) "Indexed audio folders" else explorerPath,
                    color = Lavender, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = if (explorerPath.isBlank()) 12.dp else 0.dp))
            }
            val childFolders = remember(state.tracks, explorerPath) {
                state.tracks.mapNotNull { MusicPaths.immediateChildFolder(explorerPath, it.folderPath) }.distinct().sorted()
            }
            val direct = remember(state.tracks, explorerPath) {
                if (explorerPath.isBlank()) emptyList() else state.tracks.filter { MusicPaths.isDirectlyIn(explorerPath, it.folderPath) }.sortedBy { it.title.lowercase() }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(childFolders) { folder ->
                    val count = state.tracks.count { it.folderPath == folder || it.folderPath.startsWith("$folder/") }
                    FolderRow(MusicPaths.folderLabel(folder), folder, count) { explorerPath = folder }
                }
                items(direct, key = { it.id }) { track ->
                    TrackRow(track, state.favorites.contains(track.id), onPlay = { vm.playTracks(direct, direct.indexOf(track)) }, onFavorite = { vm.toggleFavorite(track.id) }, onPlayNext = { vm.queueTrack(track, true) }, onAddToQueue = { vm.queueTrack(track, false) }, onPickPlaylist = { vm.showPlaylistPicker(track.id) })
                }
                if (childFolders.isEmpty() && direct.isEmpty()) item { EmptyState("No audio here", "Go back to browse your folders.") }
            }
        }
    }
}

@Composable
private fun FavoritesScreen(state: LokiUiState, vm: LokiViewModel) {
    var sorting by rememberSaveable { mutableIntStateOf(0) }
    val exportFavorites = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.backupFavorites(uri) }
    val importFavorites = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.restoreFavorites(uri) }
    val favorites = remember(state.tracks, state.favorites, state.favoriteSince, sorting) {
        state.tracks.filter { it.id in state.favorites }.let { tracks ->
            when (sorting) {
                1 -> tracks.sortedWith(compareByDescending<MusicTrack> { state.favoriteSince[it.id] ?: 0L }
                    .thenBy { it.title.lowercase() })
                2 -> tracks.sortedWith(compareBy<MusicTrack> { it.artist.lowercase() }
                    .thenBy { it.title.lowercase() })
                else -> tracks.sortedBy { it.title.lowercase() }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        SectionHeader("Made for you", "${favorites.size} favorite songs")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { exportFavorites.launch("LokiTunes-favorites.json") },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.FileUpload, null, Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Backup", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { importFavorites.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.FileDownload, null, Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Restore", fontSize = 12.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort:", color = SoftText, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            val labels = listOf("A–Z", "Recently liked", "Artist")
            labels.forEachIndexed { index, label ->
                FilterChip(
                    selected = sorting == index,
                    onClick = { sorting = index },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.padding(end = 5.dp)
                )
            }
        }
        if (favorites.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Your liked tracks", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                FilledTonalButton(onClick = { vm.playTracks(favorites, 0) }) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp)); Text("Play all")
                }
            }
        }
        TrackList(favorites, state.favorites, vm, "Your favorites live here", "Tap the heart beside any song to save it.")
    }
}

@Composable
private fun LibraryScreen(state: LokiUiState, vm: LokiViewModel) {
    var category by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val tracks = state.tracks
    val groups = remember(tracks, category) {
        if (category < 2) tracks.groupBy { if (category == 0) it.album else it.artist }.toSortedMap()
        else emptyMap()
    }
    Column(Modifier.fillMaxSize()) {
        SectionHeader("The collection", "Albums, artists, playlists & library health")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Albums", "Artists", "Playlists", "Duplicates").forEachIndexed { index, label ->
                FilterChip(selected = category == index, onClick = { category = index; selected = null },
                    label = { Text(label) })
            }
        }
        when (category) {
            2 -> PlaylistsPane(state, vm)
            3 -> DuplicatesPane(state, vm)
            else -> if (selected == null) {
                LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
                    items(groups.keys.toList()) { name ->
                        FolderRow(name, "${groups.getValue(name).size} tracks", groups.getValue(name).size) { selected = name }
                    }
                    if (groups.isEmpty()) item { EmptyState("Nothing indexed yet", "Refresh after adding music.") }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selected = null }) { Icon(Icons.Default.ArrowBack, "Back", tint = Mint) }
                    Text(selected.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Lavender)
                }
                TrackList(groups[selected.orEmpty()].orEmpty(), state.favorites, vm, "No songs", "Try another album or artist.")
            }
        }
    }
}

@Composable
private fun PlaylistsPane(state: LokiUiState, vm: LokiViewModel) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    val selected = state.playlists.firstOrNull { it.id == selectedId }
    BackHandler(enabled = selected != null) { selectedId = null }

    if (creating || renaming) {
        val isRename = renaming
        AlertDialog(onDismissRequest = { creating = false; renaming = false },
            title = { Text(if (isRename) "Rename playlist" else "Create playlist") },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(80) },
                    label = { Text("Playlist name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = if (isRename) selected?.let { vm.renamePlaylist(it.id, draft) } ?: false
                             else vm.createPlaylist(draft)
                    if (ok) { creating = false; renaming = false; draft = "" }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { creating = false; renaming = false }) { Text("Cancel") }
            }, containerColor = NightPanel)
    }
    if (deleting && selected != null) {
        AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Delete playlist?") },
            text = { Text("Delete '${selected.name}'? Your music files and Favorites stay safe.") },
            confirmButton = { TextButton(onClick = {
                if (vm.deletePlaylist(selected.id)) selectedId = null
                deleting = false
            }) { Text("Delete", color = Color(0xFFFFA3A3)) } },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } },
            containerColor = NightPanel)
    }
    if (adding && selected != null) {
        val candidates = remember(state.tracks, search) {
            state.tracks.filter { it.title.contains(search, true) || it.artist.contains(search, true) }
                .sortedBy { it.title.lowercase() }
        }
        AlertDialog(onDismissRequest = { adding = false }, title = { Text("Add songs to ${selected.name}") },
            text = {
                Column {
                    OutlinedTextField(value = search, onValueChange = { search = it },
                        label = { Text("Search songs") }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(Modifier.heightIn(max = 345.dp)) {
                        items(candidates, key = { it.id }) { track ->
                            val inList = track.id in selected.trackIds
                            TextButton(onClick = { vm.addTrackToPlaylist(selected.id, track.id) },
                                enabled = !inList, modifier = Modifier.fillMaxWidth()) {
                                Icon(if (inList) Icons.Default.Check else Icons.Default.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = SoftText, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }, confirmButton = {
                TextButton(onClick = { adding = false; search = "" }) { Text("Done") }
            }, containerColor = NightPanel)
    }

    if (selected == null) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${state.playlists.size} playlists", color = SoftText, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { draft = ""; creating = true }) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("New playlist")
            }
        }
        if (state.playlists.isEmpty()) EmptyState("Your playlists live here", "Create a playlist to organize songs offline.")
        else LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
            items(state.playlists, key = { it.id }) { playlist ->
                FolderRow(playlist.name, "Custom playlist", playlist.trackIds.size) { selectedId = playlist.id }
            }
        }
    } else {
        val byId = remember(state.tracks) { state.tracks.associateBy { it.id } }
        val entries = remember(selected, state.tracks) { selected.trackIds.mapNotNull { byId[it] } }
        val unavailable = selected.trackIds.size - entries.size
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { selectedId = null }) { Icon(Icons.Default.ArrowBack, "Back", tint = Mint) }
            Text(selected.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { draft = selected.name; renaming = true }) {
                Icon(Icons.Default.Edit, "Rename playlist", tint = Lavender)
            }
            IconButton(onClick = { deleting = true }) {
                Icon(Icons.Default.DeleteOutline, "Delete playlist", tint = SoftText)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${entries.size} available songs", modifier = Modifier.weight(1f), color = SoftText, fontSize = 12.sp)
            FilledTonalButton(onClick = { adding = true }) {
                Icon(Icons.Default.PlaylistAdd, null, Modifier.size(18.dp)); Text("Add songs")
            }
            Spacer(Modifier.width(6.dp))
            if (entries.isNotEmpty()) IconButton(onClick = { vm.playTracks(entries, 0) }) {
                Icon(Icons.Default.PlayArrow, "Play playlist", tint = Mint)
            }
        }
        if (unavailable > 0) Text("$unavailable unavailable song(s) skipped. Nothing was deleted.",
            modifier = Modifier.padding(horizontal = 20.dp), color = SoftText, fontSize = 12.sp)
        TrackList(entries, state.favorites, vm, "Empty playlist", "Use Add songs to choose music.", selected.id)
    }
}

@Composable
private fun DuplicatesPane(state: LokiUiState, vm: LokiViewModel) {
    val byId = remember(state.tracks) { state.tracks.associateBy { it.id } }
    val groups = remember(state.tracks) {
        LibraryInsights.potentialDuplicates(state.tracks.map { it.asIndexedSong() })
            .map { group -> group.mapNotNull { byId[it.id] } }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Potential duplicates only: same title, artist, and similar duration. No files are deleted.",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp), color = SoftText, fontSize = 12.sp)
        if (groups.isEmpty()) EmptyState("No likely duplicates found", "Your library looks tidy based on song metadata.")
        else LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
            groups.forEachIndexed { index, group ->
                item { SectionTitle("Match ${index + 1} · ${group.first().title}", "${group.size} possible copies") }
                items(group, key = { it.id }) { track ->
                    TrackRow(track, state.favorites.contains(track.id),
                        onPlay = { vm.playTracks(group, group.indexOf(track)) },
                        onFavorite = { vm.toggleFavorite(track.id) },
                        onPlayNext = { vm.queueTrack(track, true) },
                        onAddToQueue = { vm.queueTrack(track, false) },
                        onPickPlaylist = { vm.showPlaylistPicker(track.id) })
                }
            }
        }
    }
}

@Composable
private fun TrackList(tracks: List<MusicTrack>, favorites: Set<String>, vm: LokiViewModel,
    title: String, subtitle: String, playlistId: String? = null) {
    if (tracks.isEmpty()) {
        EmptyState(title, subtitle)
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackRow(track, favorites.contains(track.id), onPlay = { vm.playTracks(tracks, index) },
                    onFavorite = { vm.toggleFavorite(track.id) },
                    onPlayNext = { vm.queueTrack(track, true) },
                    onAddToQueue = { vm.queueTrack(track, false) },
                    onPickPlaylist = { vm.showPlaylistPicker(track.id) },
                    onRemoveFromPlaylist = playlistId?.let { id -> { vm.removeTrackFromPlaylist(id, track.id); Unit } })
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: MusicTrack,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onPickPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumTile(52.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text("${track.artist}  ·  ${formatTime(track.durationMs)}", fontSize = 11.sp, color = SoftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                tint = if (isFavorite) Lavender else SoftText, modifier = Modifier.size(21.dp))
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options for ${track.title}",
                    tint = SoftText, modifier = Modifier.size(22.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Play next") }, leadingIcon = {
                    Icon(Icons.Default.PlaylistPlay, null)
                }, onClick = { menuOpen = false; onPlayNext() })
                DropdownMenuItem(text = { Text("Add to queue") }, leadingIcon = {
                    Icon(Icons.Default.QueueMusic, null)
                }, onClick = { menuOpen = false; onAddToQueue() })
                if (onPickPlaylist != null) DropdownMenuItem(text = { Text("Add to playlist") },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                    onClick = { menuOpen = false; onPickPlaylist() })
                if (onRemoveFromPlaylist != null) DropdownMenuItem(text = { Text("Remove from playlist") },
                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                    onClick = { menuOpen = false; onRemoveFromPlaylist() })
            }
        }
    }
}

@Composable
private fun FolderRow(name: String, path: String, count: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(51.dp).clip(RoundedCornerShape(15.dp)).background(NightCard),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Folder, null, tint = Lavender) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(path, fontSize = 11.sp, color = SoftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("$count", fontSize = 11.sp, color = Mint)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ChevronRight, null, tint = SoftText, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(17.dp)).background(NightPanel).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Lavender, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { input ->
                Box {
                    if (value.isBlank()) Text("Search songs, artists, folders...", color = SoftText, fontSize = 13.sp)
                    input()
                }
            }
        )
        if (value.isNotEmpty()) {
            Icon(Icons.Default.Close, "Clear search", Modifier.size(18.dp).clickable { onValueChange("") }, tint = SoftText)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)) {
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, fontSize = 12.sp, color = SoftText)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 12.sp, color = SoftText)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MusicOff, null, tint = Lavender, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = SoftText, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AlbumTile(size: androidx.compose.ui.unit.Dp, artwork: ImageBitmap? = null) {
    Box(Modifier.size(size).clip(RoundedCornerShape(15.dp)).background(albumGradient), contentAlignment = Alignment.Center) {
        if (artwork != null) Image(artwork, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
private fun MiniPlayer(track: MusicTrack, isPlaying: Boolean, vm: LokiViewModel, onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(NightCard)
            .clickable(onClick = onOpen).padding(start = 8.dp, end = 5.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumTile(43.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SoftText, fontSize = 11.sp)
        }
        IconButton(onClick = vm::playPause) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", tint = Mint)
        }
        IconButton(onClick = vm::next) { Icon(Icons.Default.SkipNext, "Next", tint = Color.White) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingScreen(track: MusicTrack, state: LokiUiState, vm: LokiViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val artworkVisual by produceState<ArtworkVisual?>(initialValue = null, track.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, track.uri)
                    val bytes = retriever.embeddedPicture ?: return@runCatching null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                    val factor = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 600)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = factor }) ?: return@runCatching null
                    val accent = sampleArtworkAccent(bitmap) ?: Lavender
                    ArtworkVisual(bitmap.asImageBitmap(), accent)
                } finally { retriever.release() }
            }.getOrNull()
        }
    }
    val currentArtwork = artworkVisual
    val desiredAccent = if (state.dynamicAlbumColors) currentArtwork?.accent ?: Lavender else Lavender
    val animatedAccent by animateColorAsState(targetValue = desiredAccent, label = "Album artwork accent")
    val topShade = lerp(Midnight, animatedAccent, 0.30f)
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSound by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    val duration = state.durationMs.takeIf { it > 0 } ?: track.durationMs
    var requestedProgress by remember(track.id) { mutableFloatStateOf(-1f) }
    val progress = if (requestedProgress >= 0f) requestedProgress else {
        if (duration > 0L) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(topShade, Midnight, Midnight)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "Back", tint = Color.White, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NOW PLAYING", letterSpacing = 2.sp, fontSize = 10.sp, color = Mint)
                Text("LokiTunes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = vm::toggleDynamicAlbumColors) {
                Icon(Icons.Default.Palette,
                    contentDescription = if (state.dynamicAlbumColors) "Disable album colors" else "Enable album colors",
                    tint = if (state.dynamicAlbumColors) animatedAccent else SoftText)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(if (state.dynamicAlbumColors) "Album colors · On" else "Midnight Aurora · Fixed colors",
            color = SoftText, fontSize = 11.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(29.dp)).background(albumGradient), contentAlignment = Alignment.Center) {
            if (currentArtwork != null) Image(currentArtwork.image, "Album cover", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.GraphicEq, null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(118.dp))
        }
        Spacer(Modifier.height(35.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(track.title, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(track.artist, color = SoftText, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { vm.toggleFavorite(track.id) }) {
                Icon(
                    if (track.id in state.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite", tint = if (track.id in state.favorites) Lavender else SoftText, modifier = Modifier.size(29.dp)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Slider(
            value = progress,
            onValueChange = { requestedProgress = it },
            onValueChangeFinished = {
                if (requestedProgress >= 0f && duration > 0L) {
                    vm.seekTo((requestedProgress * duration).roundToLong())
                }
                requestedProgress = -1f
            },
            enabled = duration > 0L,
            colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = animatedAccent, inactiveTrackColor = NightCard)
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(if (requestedProgress >= 0f) (requestedProgress * duration).toLong() else state.positionMs), fontSize = 11.sp, color = SoftText)
            TextButton(onClick = { vm.seekBy(-10_000L) }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                Icon(Icons.Default.Replay10, "Rewind 10 seconds", Modifier.size(17.dp))
                Text("10s", fontSize = 11.sp)
            }
            TextButton(onClick = { vm.seekBy(10_000L) }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                Icon(Icons.Default.Forward10, "Forward 10 seconds", Modifier.size(17.dp))
                Text("10s", fontSize = 11.sp)
            }
            Text(formatTime(duration), fontSize = 11.sp, color = SoftText)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = vm::toggleShuffle) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) Mint else SoftText)
            }
            IconButton(onClick = vm::previous) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(35.dp), tint = Color.White) }
            IconButton(onClick = vm::playPause, modifier = Modifier.size(78.dp).clip(CircleShape).background(Mint)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", Modifier.size(38.dp), tint = Midnight)
            }
            IconButton(onClick = vm::next) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(35.dp), tint = Color.White) }
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = vm::cycleRepeat) {
                    Icon(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat", tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) SoftText else Mint)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { showQueue = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.QueueMusic, null, Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("Queue (${state.queue.size})", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { showSleepTimer = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Bedtime, null, Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (state.sleepRemainingMs > 0L) "Timer on" else "Sleep timer", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { showLyrics = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Subtitles, null, Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("Lyrics", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { showSound = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Equalizer, null, Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sound", fontSize = 12.sp)
            }
        }
        if (state.sleepRemainingMs > 0L) {
            Spacer(Modifier.height(5.dp))
            Text("Music pauses in ${formatCountdown(state.sleepRemainingMs)}", color = Mint, fontSize = 12.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text("OFFLINE MODE  ·  NO ADS  ·  JUST MUSIC", color = SoftText, fontSize = 10.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(28.dp))
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }, containerColor = NightPanel) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Playing queue", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::clearUpcoming,
                        enabled = state.queueIndex >= 0 && state.queueIndex < state.queue.lastIndex) {
                        Text("Clear upcoming", fontSize = 11.sp)
                    }
                }
                Text("Tap to play · arrows to reorder · X to remove", color = SoftText, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                if (state.queue.isEmpty()) EmptyState("Queue is empty", "Choose a song to start listening.")
                else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 490.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(state.queue, key = { index, _ -> index }) { index, item ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (index == state.queueIndex) NightCard else NightPanel)
                                .clickable { vm.playQueueIndex(index) }
                                .padding(vertical = 5.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (index == state.queueIndex) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                null, tint = if (index == state.queueIndex) Mint else SoftText,
                                modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontSize = 13.sp,
                                    fontWeight = if (index == state.queueIndex) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.artist, color = SoftText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { vm.moveQueueItem(index, index - 1) }, enabled = index > 0,
                                modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "Move up", Modifier.size(20.dp))
                            }
                            IconButton(onClick = { vm.moveQueueItem(index, index + 1) }, enabled = index < state.queue.lastIndex,
                                modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "Move down", Modifier.size(20.dp))
                            }
                            IconButton(onClick = { vm.removeQueueItem(index) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Close, "Remove from queue", Modifier.size(19.dp), tint = SoftText)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showLyrics) {
        LyricsSheet(track = track, positionMs = state.positionMs,
            onClose = { showLyrics = false })
    }
    if (showSound) {
        SoundSheet(state, vm, onClose = { showSound = false })
    }
    if (showSleepTimer) {
        ModalBottomSheet(onDismissRequest = { showSleepTimer = false }, containerColor = NightPanel) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
                Text("Sleep timer", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("LokiTunes will pause the music automatically.", color = SoftText, fontSize = 12.sp)
                if (state.sleepRemainingMs > 0L) {
                    Spacer(Modifier.height(12.dp))
                    Text("Remaining: ${formatCountdown(state.sleepRemainingMs)}", color = Mint, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                listOf(10, 20, 30, 45, 60, 90).chunked(3).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { minutes ->
                            OutlinedButton(
                                onClick = { vm.setSleepTimer(minutes); showSleepTimer = false },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 3.dp)
                            ) { Text("$minutes min", fontSize = 12.sp) }
                        }
                    }
                }
                if (state.sleepTimerEndMs > 0L) {
                    TextButton(onClick = { vm.cancelSleepTimer(); showSleepTimer = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel sleep timer", color = Lavender)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSheet(track: MusicTrack, positionMs: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val lyricsStore = remember(context) { LyricsStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var selectedUri by remember(track.id) { mutableStateOf(lyricsStore.savedUri(track.id)) }
    var folderUri by remember(track.id) { mutableStateOf(lyricsStore.savedFolder()) }
    var revision by remember(track.id) { mutableIntStateOf(0) }
    var importError by remember(track.id) { mutableStateOf<String?>(null) }
    var onlineMode by remember(track.id) { mutableStateOf(false) }
    var queryTitle by remember(track.id) { mutableStateOf(track.title) }
    var queryArtist by remember(track.id) { mutableStateOf(if (track.artist == "Unknown artist") "" else track.artist) }
    var searching by remember(track.id) { mutableStateOf(false) }
    var saving by remember(track.id) { mutableStateOf(false) }
    var searched by remember(track.id) { mutableStateOf(false) }
    var searchResults by remember(track.id) { mutableStateOf<List<RemoteLyricHit>>(emptyList()) }
    var onlineError by remember(track.id) { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { lyricsStore.attach(track.id, uri) }
                .onSuccess { selectedUri = uri; revision++; importError = null }
                .onFailure { importError = "Could not read this lyric file. Try another one." }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { lyricsStore.setFolder(uri) }
                .onSuccess { folderUri = uri; revision++; importError = null }
                .onFailure { importError = "Folder access was denied. Select a different music folder." }
        }
    }
    val resolved by produceState<ResolvedLyrics?>(initialValue = null, track.id, selectedUri, folderUri, revision) {
        value = withContext(Dispatchers.IO) { lyricsStore.resolve(track) }
    }
    val document = resolved?.document
    val activeIndex = document?.let { LrcParser.activeIndex(it, positionMs) } ?: -1
    val lyricListState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(activeIndex, revision, selectedUri, folderUri) {
        if (activeIndex in 0 until (document?.lines?.size ?: 0)) {
            lyricListState.animateScrollToItem(activeIndex)
        }
    }

    ModalBottomSheet(onDismissRequest = onClose, containerColor = NightPanel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text("Smart lyrics", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(track.title, color = SoftText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            // Existing v0.5.0 manual file selection is kept intact.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Icon(Icons.Default.InsertDriveFile, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Local file", fontSize = 11.sp)
                }
                OutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Icon(Icons.Default.FolderOpen, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Music folder", fontSize = 11.sp)
                }
                Button(onClick = { onlineMode = !onlineMode }, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Icon(Icons.Default.Search, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Online", fontSize = 11.sp)
                }
            }
            Text("Offline auto-detection checks matching .lrc/.txt names in your selected folder and its subfolders.",
                color = SoftText, fontSize = 11.sp)
            if (folderUri != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ Music folder access saved", fontSize = 11.sp, color = Mint, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        lyricsStore.clearFolder()
                        folderUri = null
                        revision++
                    }) { Text("Remove folder", fontSize = 11.sp) }
                }
            }
            if (resolved != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(resolved!!.source, color = Mint, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    if (selectedUri != null || resolved!!.source.startsWith("Saved")) {
                        TextButton(onClick = {
                            lyricsStore.clear(track.id)
                            selectedUri = null
                            revision++
                            importError = null
                        }) { Text("Clear saved", fontSize = 11.sp) }
                    }
                }
            }
            if (importError != null) Text(importError.orEmpty(), color = Color(0xFFFFBEC5), fontSize = 12.sp)
            if (onlineMode) {
                Spacer(Modifier.height(8.dp))
                Text("Search LRCLIB (optional internet)", fontWeight = FontWeight.SemiBold, color = Mint)
                Text("Only your search title/artist is sent when you press Search; music files are never uploaded.",
                    fontSize = 11.sp, color = SoftText)
                OutlinedTextField(value = queryTitle, onValueChange = { queryTitle = it.take(120) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Song title") }, singleLine = true)
                OutlinedTextField(value = queryArtist, onValueChange = { queryArtist = it.take(120) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Artist (optional)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        searching = true
                        searched = false
                        onlineError = null
                        searchResults = emptyList()
                        scope.launch {
                            val response = withContext(Dispatchers.IO) {
                                runCatching { LrcLibClient.search(queryTitle, queryArtist) }
                            }
                            response.onSuccess { searchResults = it; searched = true }
                                .onFailure { onlineError = it.message ?: "Lyrics search unavailable. Check your internet." }
                            searching = false
                        }
                    }, enabled = !searching && !saving && queryTitle.isNotBlank()) {
                        Text(if (searching) "Searching…" else "Search lyrics")
                    }
                    if (searching || saving) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                if (onlineError != null) Text(onlineError.orEmpty(), color = Color(0xFFFFBEC5), fontSize = 12.sp)
                if (searched && searchResults.isEmpty()) Text("No lyrics found. Try a simpler title or different artist.",
                    color = SoftText, fontSize = 12.sp)
                if (searchResults.isNotEmpty()) {
                    Text("Select the correct song below to save its lyrics offline:", color = SoftText, fontSize = 12.sp)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 225.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(searchResults, key = { it.id }) { hit ->
                            Surface(color = Midnight, shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(hit.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                        Text("${hit.artist} · ${hit.durationSeconds}s · ${if (hit.synced != null) "Synced" else "Plain"}",
                                            color = SoftText, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = {
                                        val content = hit.bestText ?: return@TextButton
                                        saving = true
                                        onlineError = null
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                runCatching { lyricsStore.saveOnline(track.id, content) }
                                            }
                                            result.onSuccess {
                                                // Manual file takes precedence. Switch to the newly saved online selection.
                                                if (selectedUri != null) {
                                                    lyricsStore.clearManual(track.id)
                                                    selectedUri = null
                                                }
                                                revision++
                                                onlineMode = false
                                                searchResults = emptyList()
                                            }.onFailure { onlineError = "Could not save lyrics: ${it.message.orEmpty()}" }
                                            saving = false
                                        }
                                    }, enabled = !saving) { Text("Save", color = Mint) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            when {
                document == null -> Text("No matching lyrics yet. Select your Music folder, choose a file, or search online and save.",
                    color = SoftText, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                document.lines.isEmpty() -> Text("This lyrics file is empty.", color = SoftText, fontSize = 13.sp)
                else -> {
                    Text(if (document.synchronized) "● Synchronized lyrics" else "Plain lyrics", color = Mint, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(state = lyricListState, modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp),
                        contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(document.lines) { index, line ->
                            Text(line.text,
                                color = if (document.synchronized && index == activeIndex) Mint else Color.White.copy(alpha = 0.73f),
                                fontSize = if (document.synchronized && index == activeIndex) 20.sp else 15.sp,
                                fontWeight = if (document.synchronized && index == activeIndex) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Text("Lyrics source: local files or LRCLIB. Only save lyrics you have permission to use.",
                color = SoftText, fontSize = 10.sp, modifier = Modifier.padding(bottom = 22.dp, top = 8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundSheet(state: LokiUiState, vm: LokiViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    var deviceEqualizerError by remember { mutableStateOf(false) }
    val shownBands = AudioTuning.curve(state.equalizerPreset, state.customEqBands)
    ModalBottomSheet(onDismissRequest = onClose, containerColor = NightPanel) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text("LokiTunes Sound Studio", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Built-in audio tuning", color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enable equalizer", fontWeight = FontWeight.SemiBold)
                    Text("Applies to LokiTunes audio only", color = SoftText, fontSize = 11.sp)
                }
                Switch(checked = state.equalizerEnabled, onCheckedChange = vm::setEqualizerEnabled)
            }
            Spacer(Modifier.height(6.dp))
            Text("Sound profiles", color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            AudioTuning.presets.chunked(3).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.forEach { preset ->
                        FilterChip(
                            selected = state.equalizerPreset == preset,
                            onClick = { vm.selectEqPreset(preset) },
                            enabled = state.equalizerEnabled,
                            label = { Text(preset, fontSize = 12.sp) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("5-band tuning · –6 to +6 dB", color = SoftText, fontSize = 13.sp)
            AudioTuning.bandLabels.forEachIndexed { index, label ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(label, fontSize = 12.sp, modifier = Modifier.width(65.dp), color = SoftText)
                    Slider(
                        value = shownBands[index].toFloat(),
                        onValueChange = { vm.setCustomEqBand(index, it.toInt()) },
                        valueRange = -6f..6f,
                        steps = 11,
                        enabled = state.equalizerEnabled,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${shownBands[index]} dB", fontSize = 11.sp,
                        modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Bass boost: ${state.bassBoostPercent}%", fontWeight = FontWeight.Medium)
            Slider(
                value = state.bassBoostPercent.toFloat(),
                onValueChange = { vm.setBassBoost(it.toInt()) },
                valueRange = 0f..100f,
                enabled = state.equalizerEnabled
            )
            Text("Band frequencies and effect availability depend on your Android device. Loud boosts can distort audio; start low.",
                color = SoftText, fontSize = 11.sp)
            Spacer(Modifier.height(13.dp))
            HorizontalDivider(color = NightCard)
            Spacer(Modifier.height(11.dp))
            Text("Playback speed", color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            SoundPresets.values.chunked(3).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.forEach { speed ->
                        FilterChip(
                            selected = kotlin.math.abs(state.playbackSpeed - speed) < 0.01f,
                            onClick = { vm.setPlaybackSpeed(speed) },
                            label = { Text("${speed}×") }
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = {
                deviceEqualizerError = runCatching {
                    context.startActivity(Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    })
                }.isFailure
            }) {
                Icon(Icons.Default.Equalizer, null, Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open device equalizer")
            }
            Text("Optional external sound panel; not required for LokiTunes EQ.",
                fontSize = 11.sp, color = SoftText)
            if (deviceEqualizerError) Text("Your phone has no compatible external sound panel.",
                color = Color(0xFFFFBEC5), fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatCountdown(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) + 999L) / 1_000L
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val rest = seconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest)
        else "%02d:%02d".format(minutes, rest)
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
