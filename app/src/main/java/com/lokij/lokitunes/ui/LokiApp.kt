package com.lokij.lokitunes.ui

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokij.lokitunes.BuildConfig
import com.lokij.lokitunes.update.UpdatePhase
import com.lokij.lokitunes.update.UpdateUiState
import com.lokij.lokitunes.update.UpdateViewModel
import com.lokij.lokitunes.data.MusicPaths
import com.lokij.lokitunes.data.MusicTrack
import com.lokij.lokitunes.ui.theme.*
import kotlinx.coroutines.Dispatchers
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
                TrackRow(last, state.favorites.contains(last.id), onPlay = { vm.playTracks(listOf(last), 0) }, onFavorite = { vm.toggleFavorite(last.id) })
            }
        }
        item { SectionTitle("Fresh in your library", "Recently added") }
        if (recent.isEmpty()) item { EmptyState("No music found yet", "Copy some audio files to your phone, then tap refresh.") }
        items(recent, key = { it.id }) { track ->
            TrackRow(track, state.favorites.contains(track.id), onPlay = { vm.playTracks(recent, recent.indexOf(track)) }, onFavorite = { vm.toggleFavorite(track.id) })
        }
    }
}

@Composable
private fun SongsScreen(state: LokiUiState, vm: LokiViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var alphabetical by rememberSaveable { mutableStateOf(true) }
    val songs = remember(state.tracks, query, alphabetical) {
        state.tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.folderPath.contains(query, true)
        }.let { filtered ->
            if (alphabetical) filtered.sortedBy { it.title.lowercase() }
            else filtered.sortedByDescending { it.dateAdded }
        }
    }
    Column(Modifier.fillMaxSize()) {
        SectionHeader("All songs", "${state.tracks.size} tracks ready to play")
        SearchBox(query, { query = it })
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${songs.size} matches", color = SoftText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { alphabetical = !alphabetical }) {
                Icon(Icons.Default.Sort, null, Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (alphabetical) "A – Z" else "Newest")
            }
        }
        TrackList(songs, state.favorites, vm, "No matching songs", "Try another search or refresh the library.")
    }
}

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
                    TrackRow(track, state.favorites.contains(track.id), onPlay = { vm.playTracks(direct, direct.indexOf(track)) }, onFavorite = { vm.toggleFavorite(track.id) })
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
        tracks.groupBy { if (category == 0) it.album else it.artist }.toSortedMap()
    }
    Column(Modifier.fillMaxSize()) {
        SectionHeader("The collection", "Find your sound by album or artist")
        Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = category == 0, onClick = { category = 0; selected = null }, label = { Text("Albums") })
            FilterChip(selected = category == 1, onClick = { category = 1; selected = null }, label = { Text("Artists") })
        }
        if (selected == null) {
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

@Composable
private fun TrackList(tracks: List<MusicTrack>, favorites: Set<String>, vm: LokiViewModel, title: String, subtitle: String) {
    if (tracks.isEmpty()) {
        EmptyState(title, subtitle)
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(tracks, key = { it.id }) { track ->
                TrackRow(track, favorites.contains(track.id), onPlay = { vm.playTracks(tracks, tracks.indexOf(track)) }, onFavorite = { vm.toggleFavorite(track.id) })
            }
        }
    }
}

@Composable
private fun TrackRow(track: MusicTrack, isFavorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
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
        IconButton(onClick = onFavorite, modifier = Modifier.size(42.dp)) {
            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                tint = if (isFavorite) Lavender else SoftText, modifier = Modifier.size(22.dp))
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
    val artwork by produceState<ImageBitmap?>(initialValue = null, track.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, track.uri)
                    val bytes = retriever.embeddedPicture ?: return@runCatching null
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    val factor = maxOf(1, maxOf(options.outWidth, options.outHeight) / 600)
                    options.inJustDecodeBounds = false
                    options.inSampleSize = factor
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
                } finally { retriever.release() }
            }.getOrNull()
        }
    }
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    val duration = state.durationMs.takeIf { it > 0 } ?: track.durationMs
    var requestedProgress by remember(track.id) { mutableFloatStateOf(-1f) }
    val progress = if (requestedProgress >= 0f) requestedProgress else {
        if (duration > 0L) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    Column(
        Modifier.fillMaxSize().background(Midnight).verticalScroll(rememberScrollState())
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
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.height(38.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(29.dp)).background(albumGradient), contentAlignment = Alignment.Center) {
            if (artwork != null) Image(artwork!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
            colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Lavender, inactiveTrackColor = NightCard)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(if (requestedProgress >= 0f) (requestedProgress * duration).toLong() else state.positionMs), fontSize = 11.sp, color = SoftText)
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
                Text("Playing queue", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Tap to play. Use arrows to move songs or X to remove.", color = SoftText, fontSize = 12.sp)
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
