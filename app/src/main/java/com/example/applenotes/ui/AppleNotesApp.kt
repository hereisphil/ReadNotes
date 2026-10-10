package com.example.applenotes.ui

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.Intent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.applenotes.auth.AppleNotesSession
import com.example.applenotes.auth.CookieReader
import com.example.applenotes.auth.ICloudSession
import com.example.applenotes.auth.USER_AGENT
import com.example.applenotes.client.AppleNotesClient
import com.example.applenotes.client.FolderSummary
import com.example.applenotes.client.NoteRecord
import com.example.applenotes.client.NoteSummary
import com.example.applenotes.R
import com.example.applenotes.proto.NoteBodyEditor
import com.example.applenotes.proto.NoteBodyLoader
import com.example.applenotes.proto.ResolvedNoteBody
import com.example.applenotes.theme.ThemeMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "AppleNotesUi"
private const val ICLOUD_NOTES_URL = "https://www.icloud.com/notes"

private sealed interface ScreenState {
    data object Splash : ScreenState
    data object Login : ScreenState
    data class Loading(val msg: String) : ScreenState
    data class Error(
        val msg: String,
        /** When set, ErrorScreen offers Back that restores this list. */
        val restoreList: NotesList? = null,
    ) : ScreenState
    data class NotesList(
        val session: ICloudSession,
        val notes: List<NoteSummary>,
        val folders: List<FolderSummary>,
        /**
         * Currently-selected folder recordName. Null = "All notes" pseudo-folder
         * (every note across user-visible folders). Use [TRASH_RECORD_NAME] to
         * show Recently Deleted explicitly.
         */
        val selectedFolder: String? = "DefaultFolder-CloudKit",
    ) : ScreenState
    /**
     * Detail keeps the cached [notesCache] from the list view so back-nav
     * restores the list instantly without a `fetchRecents` round trip.
     * `notesCache` is updated in place when we save (replacing the just-saved
     * note's summary) so the user sees the new snippet on return.
     */
    data class Detail(
        val session: ICloudSession,
        val record: NoteRecord,
        val notesCache: List<NoteSummary>,
        val folders: List<FolderSummary>,
        val selectedFolder: String?,
        /**
         * Set when this Detail represents a brand-new note that has NOT yet
         * been written to iCloud. The first save calls `createNote` with the
         * user's typed content (title + body in one shot); subsequent saves
         * use `modifyNoteBody`. We match iCloud.com's pattern: it never does
         * empty-create-then-modify — it only creates the record once there's
         * content to write. Empty-create + modify races Mac's notesync
         * housekeeping and produces records Mac later trashes.
         *
         * When non-null, [record] is a stub (recordChangeTag = null,
         * recordName = "", empty fields).
         */
        val pendingFolderRef: kotlinx.serialization.json.JsonElement? = null,
        /**
         * Pre-resolved body (inline TextDataEncrypted or downloaded
         * TextDataAsset). Null only for local drafts that have not been
         * created on the server yet.
         */
        val resolvedBody: ResolvedNoteBody? = null,
    ) : ScreenState
}

private const val TRASH_RECORD_NAME = "TrashFolder-CloudKit"
private const val DEFAULT_FOLDER_RECORD_NAME = "DefaultFolder-CloudKit"

@Composable
fun AppleNotesApp(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    val cookieReader = remember { CookieReader() }
    val httpClient = remember {
        HttpClient(OkHttp) {
            expectSuccess = false
        }
    }

    var state by remember { mutableStateOf<ScreenState>(ScreenState.Splash) }
    // Full-text search index: recordName -> visible note body. Built in the
    // background after the list loads so search can match inside note content,
    // not just CloudKit's short title/snippet fields.
    var bodyIndex by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bodyIndexJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun kickBodyIndex(session: ICloudSession, notes: List<NoteSummary>) {
        val liveIds = notes.mapTo(HashSet()) { it.recordName }
        if (bodyIndex.keys.any { it !in liveIds }) {
            bodyIndex = bodyIndex.filterKeys { it in liveIds }
        }
        // Locked notes have no searchable body here — mark them indexed empty
        // so we don't hammer CloudKit / trip the protobuf walker on ciphertext.
        val lockedMissing = notes.filter { it.locked && it.recordName !in bodyIndex }
        if (lockedMissing.isNotEmpty()) {
            bodyIndex = bodyIndex + lockedMissing.associate { it.recordName to "" }
        }
        val missing = notes.filter { !it.locked && it.recordName !in bodyIndex }
        if (missing.isEmpty()) return
        if (bodyIndexJob?.isActive == true) return
        bodyIndexJob = scope.launch {
            val client = AppleNotesClient(httpClient, session)
            for (note in missing) {
                if (!isActive) break
                try {
                    val rec = client.lookupNote(note.recordName)
                    if (rec.isPasswordLocked()) {
                        bodyIndex = bodyIndex + (note.recordName to "")
                        continue
                    }
                    val resolved = NoteBodyLoader.resolve(client, rec)
                    bodyIndex = bodyIndex + (note.recordName to resolved.visibleText)
                } catch (e: Throwable) {
                    Log.w(TAG, "body index failed for ${note.recordName}: ${e.message}")
                    // Avoid retrying the same broken record forever this session.
                    bodyIndex = bodyIndex + (note.recordName to "")
                }
            }
        }
    }

    // First-launch alpha disclaimer. Block all app behaviour behind this — we
    // don't even start the auth bootstrap until the user has acknowledged.
    var alphaAck by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        alphaAck = com.example.applenotes.AlphaWarning.isAcknowledged(context)
    }
    if (alphaAck == false) {
        AlphaWarningDialog(onAcknowledge = {
            scope.launch {
                com.example.applenotes.AlphaWarning.acknowledge(context)
                alphaAck = true
            }
        })
        return
    }
    if (alphaAck == null) {
        // Still loading the ack flag — render nothing; the splash theme is
        // still showing underneath.
        return
    }

    suspend fun bootstrap(): ScreenState = try {
        val session = AppleNotesSession(httpClient, cookieReader).bootstrap().getOrThrow()
        val client = AppleNotesClient(httpClient, session)
        val notes = client.fetchRecents(limit = 1000)
            .filter { !it.deleted }
            .sortedByDescending { it.modificationTimestampMs ?: 0L }
        val folderIds = notes.mapNotNull { it.folderRecordName }.toSet()
        val folders = runCatching { client.lookupFolders(folderIds) }.getOrElse {
            Log.w(TAG, "lookupFolders failed; falling back to ID-only list", it)
            folderIds.map { rn ->
                FolderSummary(
                    recordName = rn,
                    displayName = null,
                    isTrash = rn == TRASH_RECORD_NAME,
                    isDefault = rn == DEFAULT_FOLDER_RECORD_NAME,
                )
            }
        }
        ScreenState.NotesList(session, notes, folders)
    } catch (e: Throwable) {
        Log.e(TAG, "bootstrap failed", e)
        ScreenState.Error(e.message ?: e.toString())
    }

    suspend fun refreshList(session: ICloudSession): List<NoteSummary> =
        AppleNotesClient(httpClient, session).fetchRecents(limit = 1000)
            .filter { !it.deleted }
            .sortedByDescending { it.modificationTimestampMs ?: 0L }

    LaunchedEffect(Unit) {
        // On launch, check for existing session cookies and try to restore.
        val cookies = cookieReader.cookieMap()
        state = if ("X-APPLE-WEBAUTH-TOKEN" in cookies) {
            ScreenState.Loading("Restoring session…").also {
                scope.launch { state = bootstrap() }
            }
        } else {
            ScreenState.Login
        }
    }

    when (val s = state) {
        ScreenState.Splash -> CenteredSpinner("Starting up…")
        ScreenState.Login -> LoginScreen(
            onCookiesArrived = {
                scope.launch {
                    state = ScreenState.Loading("Signing in…")
                    state = bootstrap()
                }
            },
        )
        is ScreenState.Loading -> CenteredSpinner(s.msg)
        is ScreenState.Error -> ErrorScreen(
            message = s.msg,
            canGoBack = s.restoreList != null,
            onBack = {
                s.restoreList?.let { state = it }
            },
            onRetry = {
                scope.launch {
                    state = ScreenState.Loading("Retrying…")
                    state = bootstrap()
                }
            },
            onSignOut = {
                cookieReader.clearAuthCookies()
                state = ScreenState.Login
            },
        )
        is ScreenState.NotesList -> {
            LaunchedEffect(s.session, s.notes.map { it.recordName }) {
                kickBodyIndex(s.session, s.notes)
            }
            NotesListScreen(
                allNotes = s.notes,
                folders = s.folders,
                selectedFolder = s.selectedFolder,
                bodyIndex = bodyIndex,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onSelectFolder = { folderRn ->
                    state = s.copy(selectedFolder = folderRn)
                },
                onSelect = { note ->
                    // Locked notes are intercepted in NotesListScreen (dialog).
                    scope.launch {
                        val listSnapshot = s
                        state = ScreenState.Loading("Loading note…")
                        state = try {
                            val client = AppleNotesClient(httpClient, listSnapshot.session)
                            val rec = client.lookupNote(note.recordName)
                            if (rec.isPasswordLocked()) {
                                // List missed the Crypto* keys — still don't crash.
                                listSnapshot
                            } else {
                                val resolved = NoteBodyLoader.resolve(client, rec)
                                bodyIndex = bodyIndex + (rec.recordName to resolved.visibleText)
                                ScreenState.Detail(
                                    session = listSnapshot.session,
                                    record = rec,
                                    notesCache = listSnapshot.notes,
                                    folders = listSnapshot.folders,
                                    selectedFolder = listSnapshot.selectedFolder,
                                    resolvedBody = resolved,
                                )
                            }
                        } catch (e: Throwable) {
                            Log.e(TAG, "open note failed", e)
                            ScreenState.Error(
                                msg = e.message ?: e.toString(),
                                restoreList = listSnapshot,
                            )
                        }
                    }
                },
                onRefresh = {
                    scope.launch {
                        val listSnapshot = s
                        state = ScreenState.Loading("Refreshing…")
                        state = try {
                            val client = AppleNotesClient(httpClient, listSnapshot.session)
                            val notes = client.fetchRecents(limit = 1000)
                                .filter { !it.deleted }
                                .sortedByDescending { it.modificationTimestampMs ?: 0L }
                            val folderIds = notes.mapNotNull { it.folderRecordName }.toSet()
                            val folders = runCatching { client.lookupFolders(folderIds) }.getOrElse { listSnapshot.folders }
                            // Allow a fresh index pass for any notes not yet cached.
                            bodyIndexJob?.cancel()
                            bodyIndexJob = null
                            ScreenState.NotesList(listSnapshot.session, notes, folders, listSnapshot.selectedFolder)
                        } catch (e: Throwable) {
                            ScreenState.Error(
                                msg = e.message ?: e.toString(),
                                restoreList = listSnapshot,
                            )
                        }
                    }
                },
                onSignOut = {
                    cookieReader.clearAuthCookies()
                    bodyIndex = emptyMap()
                    bodyIndexJob?.cancel()
                    bodyIndexJob = null
                    state = ScreenState.Login
                },
            )
        }
        is ScreenState.Detail -> NoteDetailScreen(
            record = s.record,
            resolvedBody = s.resolvedBody,
            onFetchAttachment = { rn ->
                runCatching {
                    AppleNotesClient(httpClient, s.session).lookupNote(rn)
                }.getOrNull()
            },
            onFetchAsset = { url ->
                AppleNotesClient(httpClient, s.session).fetchAssetBytes(url)
            },
            onBack = {
                state = ScreenState.NotesList(s.session, s.notesCache, s.folders, s.selectedFolder)
            },
        )
    }
}

@Composable
private fun CenteredSpinner(msg: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(msg, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorScreen(
    message: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    androidx.activity.compose.BackHandler(enabled = canGoBack, onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes of Fruit") },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to notes",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canGoBack) {
                    Button(onClick = onBack) { Text("Back to notes") }
                }
                Button(onClick = onRetry) { Text("Retry") }
                Button(onClick = onSignOut) { Text("Sign in again") }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginScreen(onCookiesArrived: () -> Unit) {
    var detected by remember { mutableStateOf(false) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // Configure cookies for the embedded idmsa.apple.com auth iframe.
            CookieManager.getInstance().setAcceptCookie(true)
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = USER_AGENT
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Watch for X-APPLE-WEBAUTH-TOKEN landing in the cookie jar.
                        val cookies = CookieManager.getInstance().getCookie("https://www.icloud.com").orEmpty()
                        if (!detected && "X-APPLE-WEBAUTH-TOKEN" in cookies) {
                            detected = true
                            Log.i(TAG, "Login cookies detected (post URL=$url)")
                            onCookiesArrived()
                        }
                    }
                }
                loadUrl(ICLOUD_NOTES_URL)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListScreen(
    allNotes: List<NoteSummary>,
    folders: List<FolderSummary>,
    selectedFolder: String?,
    bodyIndex: Map<String, String>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onSelect: (NoteSummary) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    // Notes filtered to the currently-selected folder. null = "All notes"
    // (every folder except trash). TRASH_RECORD_NAME = trash only.
    val notesInFolder = remember(allNotes, selectedFolder) {
        when (selectedFolder) {
            null -> allNotes.filter { it.folderRecordName != TRASH_RECORD_NAME }
            else -> allNotes.filter { it.folderRecordName == selectedFolder }
        }
    }
    val filtered = remember(notesInFolder, query, bodyIndex) {
        if (query.isBlank()) notesInFolder
        else notesInFolder.filter { note ->
            val q = query
            (note.title?.contains(q, ignoreCase = true) == true) ||
                (note.snippet?.contains(q, ignoreCase = true) == true) ||
                (bodyIndex[note.recordName]?.contains(q, ignoreCase = true) == true)
        }
    }
    val indexedCount = bodyIndex.size
    val indexTotal = allNotes.size
    val indexing = indexedCount < indexTotal
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    var lockedNoteTitle by remember { mutableStateOf<String?>(null) }
    // System back closes the drawer if it's open; otherwise let it fall
    // through (the activity-default behaviour exits the app from the list).
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }
    if (lockedNoteTitle != null) {
        LockedNoteDialog(
            title = lockedNoteTitle ?: "This note",
            onDismiss = { lockedNoteTitle = null },
        )
    }
    val selectedFolderName = remember(selectedFolder, folders) {
        when (selectedFolder) {
            null -> "All notes"
            TRASH_RECORD_NAME -> "Recently Deleted"
            DEFAULT_FOLDER_RECORD_NAME -> folders.firstOrNull { it.recordName == DEFAULT_FOLDER_RECORD_NAME }
                ?.displayName ?: "Notes"
            else -> folders.firstOrNull { it.recordName == selectedFolder }?.displayName ?: "Folder"
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FoldersDrawer(
                folders = folders,
                allNotes = allNotes,
                selectedFolder = selectedFolder,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onClose = { drawerScope.launch { drawerState.close() } },
                onSelect = { rn ->
                    onSelectFolder(rn)
                    drawerScope.launch { drawerState.close() }
                },
                onSignOut = {
                    drawerScope.launch { drawerState.close() }
                    onSignOut()
                },
            )
        },
    ) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        drawerScope.launch { drawerState.open() }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Folders")
                    }
                },
                title = {
                    Column {
                        Text(selectedFolderName, style = MaterialTheme.typography.displayLarge)
                        Text(
                            "${notesInFolder.size} note${if (notesInFolder.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                // Material3's default scrolled container applies a surface tint
                // that reads purple/lavender on our warm cream light theme.
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar — matches title, snippet, and full indexed body text.
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = MaterialTheme.shapes.large,
            )
            if (indexing) {
                Text(
                    text = "Indexing note contents for search… $indexedCount/$indexTotal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
            if (filtered.isEmpty()) {
                if (query.isNotBlank() && indexing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Still indexing — try again in a moment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    EmptyNotesState(query.isNotBlank())
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.recordName }) { note ->
                        NoteCard(
                            note = note,
                            onClick = {
                                if (note.locked) {
                                    lockedNoteTitle = note.title?.takeIf { it.isNotBlank() } ?: "This note"
                                } else {
                                    onSelect(note)
                                }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
    }  // ModalNavigationDrawer
}

@Composable
private fun FoldersDrawer(
    folders: List<FolderSummary>,
    allNotes: List<NoteSummary>,
    selectedFolder: String?,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClose: () -> Unit,
    onSelect: (String?) -> Unit,
    onSignOut: () -> Unit,
) {
    // Per-folder note counts (excluding deleted=true outliers, which we
    // already filter at fetch time).
    val countByFolder = remember(allNotes) {
        allNotes.groupingBy { it.folderRecordName }.eachCount()
    }
    val nonTrashCount = remember(allNotes) {
        allNotes.count { it.folderRecordName != TRASH_RECORD_NAME }
    }
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Notes of Fruit",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_panel_close),
                        contentDescription = "Close menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // "All notes" pseudo-folder
            DrawerFolderRow(
                label = "All notes",
                count = nonTrashCount,
                selected = selectedFolder == null,
                onClick = { onSelect(null) },
            )
            // User-visible folders (not trash)
            for (folder in folders.filter { !it.isTrash }) {
                val display = folder.displayName ?: when {
                    folder.isDefault -> "Notes"
                    else -> folder.recordName.take(8)
                }
                DrawerFolderRow(
                    label = display,
                    count = countByFolder[folder.recordName] ?: 0,
                    selected = selectedFolder == folder.recordName,
                    onClick = { onSelect(folder.recordName) },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerFolderRow(
                label = "Recently Deleted",
                count = countByFolder[TRASH_RECORD_NAME] ?: 0,
                selected = selectedFolder == TRASH_RECORD_NAME,
                onClick = { onSelect(TRASH_RECORD_NAME) },
                icon = Icons.Default.Delete,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onSignOut)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Sign out",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ThemeModeSwitcher(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSwitcher(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val (icon, label) = when (themeMode) {
        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to "System theme"
        ThemeMode.LIGHT -> Icons.Default.LightMode to "Light theme"
        ThemeMode.DARK -> Icons.Default.DarkMode to "Dark theme"
    }
    IconButton(
        onClick = {
            val next = when (themeMode) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
            onThemeModeChange(next)
        },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$label. Tap to cycle dark, light, and system.",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DrawerFolderRow(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(bg, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyNotesState(filtering: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Note,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (filtering) "No notes match your search." else "No notes yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!filtering) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap + to create a note, or sync one from Mac or iCloud.com.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(note: NoteSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (note.locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = note.title?.takeIf { it.isNotBlank() } ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                note.modificationTimestampMs?.let { ts ->
                    Text(
                        text = humanRelative(ts),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (note.locked) {
                    Text(
                        text = "Locked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (!note.snippet.isNullOrBlank()) {
                    Text(
                        text = note.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedNoteDialog(title: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = { Text("Note is locked") },
        text = {
            Text(
                "“$title” is password-protected on your Apple devices. " +
                    "Notes of Fruit can’t unlock or show its contents — open it in " +
                    "Apple Notes (or iCloud.com) instead.",
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

private fun humanRelative(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMs
    val s = diff / 1000
    return when {
        s < 60 -> "just now"
        s < 3600 -> "${s / 60}m"
        s < 86_400 -> "${s / 3600}h"
        s < 604_800 -> "${s / 86_400}d"
        else -> {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
            val month = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.getDefault()) ?: "?"
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            "$month $day"
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = clickable(onClick = onClick)

/**
 * Apple Notes derives Title and Snippet from the body's first non-empty line
 * (= title) and what follows (= snippet preview). The CloudKit `TitleEncrypted`
 * and `SnippetEncrypted` fields are server-cached copies of these — Apple's
 * clients (Mac, iCloud.com list view) treat them as the canonical visible
 * preview. Updating them along with TextDataEncrypted on every edit keeps
 * lists fresh and gives Mac a clean signal that the note actually changed.
 */
internal fun computeTitleAndSnippet(body: String): Pair<String, String> {
    val lines = body.split("\n")
    val title = lines.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val titleIdx = lines.indexOfFirst { it.isNotBlank() }
    val rest = if (titleIdx >= 0) lines.drop(titleIdx + 1) else emptyList()
    val snippet = rest.firstOrNull { it.isNotBlank() }?.trim()?.take(120).orEmpty()
    return title to snippet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteDetailScreen(
    record: NoteRecord,
    resolvedBody: ResolvedNoteBody? = null,
    onBack: () -> Unit,
    onFetchAttachment: (suspend (String) -> NoteRecord?)? = null,
    onFetchAsset: (suspend (String) -> ByteArray?)? = null,
) {
    val shareContext = LocalContext.current
    val visibleText = resolvedBody?.visibleText.orEmpty()
    val title = remember(visibleText, resolvedBody) {
        resolvedBody?.displayTitle
            ?: visibleText.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
            ?: record.decodedTitle()?.takeIf { it.isNotBlank() }
            ?: "Untitled"
    }
    val bodyText = remember(visibleText) {
        val firstNl = visibleText.indexOf('\n')
        when {
            visibleText.isEmpty() -> ""
            firstNl < 0 -> ""
            else -> visibleText.substring(firstNl + 1)
        }
    }
    val attrSpans = resolvedBody?.attrSpans.orEmpty()
    val bodyAttrSpans = remember(visibleText, attrSpans) {
        val firstNl = visibleText.indexOf('\n')
        val skip = if (firstNl < 0) 0 else firstNl + 1
        attrSpansAfter(attrSpans, skip)
    }

    val attachmentCache = remember(record.recordName, record.recordChangeTag) {
        mutableStateOf<Map<String, AttachmentContent>>(emptyMap())
    }
    LaunchedEffect(record.recordName, record.recordChangeTag, onFetchAttachment) {
        val fetcher = onFetchAttachment ?: return@LaunchedEffect
        val assetFetcher = onFetchAsset ?: { _ -> null }
        val recordNames = bodyAttrSpans.mapNotNull { it.attachmentRecordName }.toSet()
        for (rn in recordNames) {
            if (attachmentCache.value.containsKey(rn)) continue
            val rec = runCatching { fetcher(rn) }.getOrNull() ?: continue
            val content = decodeAttachment(rec, assetFetcher, fetcher)
            if (content != null) {
                attachmentCache.value = attachmentCache.value + (rn to content)
            }
        }
    }

    androidx.activity.compose.BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 2,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, visibleText.ifBlank { title })
                                if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
                            }
                            val chooser = Intent.createChooser(sendIntent, null).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            shareContext.startActivity(chooser)
                        },
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (bodyText.isEmpty() && visibleText.isEmpty()) {
                val emptyMsg = resolvedBody?.statusMessage
                    ?: "This note has no readable body."
                Text(
                    text = emptyMsg,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                FormattedNoteBody(
                    fullBody = bodyText,
                    titlePrefixLength = 0,
                    attrSpans = bodyAttrSpans,
                    attachmentContent = attachmentCache.value,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val modDate = record.stringField("ModificationDate")?.toLongOrNull()
                Text(
                    text = modDate?.let { "Modified ${humanRelative(it)}" } ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${visibleText.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


/**
 * Formatted, selectable preview of a note's body. Walks the visible text
 * paragraph by paragraph, applies the matching attribute_run style (body /
 * bullet / checkbox / numbered / heading / title), and renders each paragraph
 * as a styled Text or Row composable. Long-press to select and copy.
 */
/** Holds decoded payload for an attachment record (table grid, image bytes, etc). */
sealed class AttachmentContent {
    data class Table(val grid: com.example.applenotes.proto.MergeableDataDecoder.TableGrid) : AttachmentContent()
    data class Image(val bytes: ByteArray, val mimeHint: String?) : AttachmentContent()
    data class Generic(val title: String?, val mime: String?) : AttachmentContent()
}

private suspend fun decodeAttachment(
    rec: NoteRecord,
    fetchAssetBytes: suspend (String) -> ByteArray?,
    fetchRecord: suspend (String) -> NoteRecord? = { null },
): AttachmentContent? {
    val uti = rec.stringField("UTI")?.let {
        runCatching { kotlin.io.encoding.Base64.decode(it).decodeToString() }.getOrNull()
    } ?: rec.stringField("UTIEncrypted")?.let {
        runCatching { kotlin.io.encoding.Base64.decode(it).decodeToString() }.getOrNull()
    }
    val title = rec.stringField("TitleEncrypted")?.let {
        runCatching { kotlin.io.encoding.Base64.decode(it).decodeToString() }.getOrNull()
    }
    val mergB64 = rec.stringField("MergeableDataEncrypted")
    if (mergB64 != null && (uti?.contains("table", ignoreCase = true) == true || title?.equals("Table", ignoreCase = true) == true)) {
        val grid = com.example.applenotes.proto.MergeableDataDecoder.decodeTableBase64(mergB64)
        if (grid != null) return AttachmentContent.Table(grid)
    }
    // Image attachment — fetch the Media CKAsset.
    val isImage = uti?.let {
        it.startsWith("public.png", ignoreCase = true) ||
            it.startsWith("public.jpeg", ignoreCase = true) ||
            it.startsWith("public.heic", ignoreCase = true) ||
            it.startsWith("public.image", ignoreCase = true) ||
            it.contains("gallery", ignoreCase = true)
    } == true
    if (isImage) {
        // The Attachment record's "Media" is a CKReference to a Media record
        // which actually carries the binary as a CKAsset. Follow the reference.
        val mediaField = rec.rawFields["Media"]
        val mediaRefName = mediaField?.let {
            com.example.applenotes.client.extractReferenceRecordName(it)
        }
        android.util.Log.i("AppleNotesUI", "Image attachment ${rec.recordName}: mediaRef=$mediaRefName uti=$uti")
        if (mediaRefName != null) {
            val mediaRec = fetchRecord(mediaRefName)
            // Media record's asset typically lives in field "MediaEncrypted" or "Media".
            val candidateFields = listOf("MediaEncrypted", "Media", "Asset", "MediaURL")
            for (fname in candidateFields) {
                val candidate = mediaRec?.rawFields?.get(fname) ?: continue
                val downloadUrl = com.example.applenotes.client.extractAssetDownloadUrl(candidate)
                if (downloadUrl != null) {
                    val bytes = fetchAssetBytes(downloadUrl)
                    if (bytes != null) return AttachmentContent.Image(bytes, uti)
                }
            }
        }
        // Direct asset URL on this record (rare, but check).
        val direct = mediaField?.let { com.example.applenotes.client.extractAssetDownloadUrl(it) }
        if (direct != null) {
            val bytes = fetchAssetBytes(direct)
            if (bytes != null) return AttachmentContent.Image(bytes, uti)
        }
        // Fall back to the first preview if Media isn't downloadable.
        val previews = rec.rawFields["PreviewImages"] as? kotlinx.serialization.json.JsonObject
        val firstPreviewUrl = previews?.get("value")?.let {
            (it as? kotlinx.serialization.json.JsonArray)?.firstOrNull()
                ?.let { ele -> com.example.applenotes.client.extractAssetDownloadUrl(ele) }
        }
        if (firstPreviewUrl != null) {
            val bytes = fetchAssetBytes(firstPreviewUrl)
            if (bytes != null) return AttachmentContent.Image(bytes, uti)
        }
    }
    return AttachmentContent.Generic(title, uti)
}

@Composable
private fun AlphaWarningDialog(onAcknowledge: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* mandatory: must tap continue */ },
        icon = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp),
            )
        },
        title = {
            Text(
                "Alpha — read this first",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Notes of Fruit is an unofficial, reverse-engineered, alpha-quality " +
                        "Android client for iCloud Notes. It is not affiliated with Apple.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "This build is read-only: you can browse and copy notes, but not " +
                        "create, edit, or delete them from the app.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "It might:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text("• stop working at any time if Apple changes their API",
                    style = MaterialTheme.typography.bodyMedium)
                Text("• fail to decode some modern or locked notes",
                    style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Use at your own risk.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text("I understand — continue")
            }
        },
    )
}

@Composable
private fun FormattedNoteBody(
    fullBody: String,
    titlePrefixLength: Int,
    attrSpans: List<NoteBodyEditor.AttrSpan>,
    attachmentContent: Map<String, AttachmentContent> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    // Map each char index -> (style, checked). Walk attrSpans in order and
    // assign each char its span's style. Total span lengths usually match
    // fullBody.length; if they don't (which can happen if Apple emits trailing
    // ARs covering chars we don't render), we tolerate the mismatch by
    // clamping.
    val styleAt = remember(fullBody, attrSpans) {
        val arr = arrayOfNulls<NoteBodyEditor.AttrSpan>(fullBody.length)
        var pos = 0
        for (span in attrSpans) {
            val end = (pos + span.length).coerceAtMost(fullBody.length)
            for (i in pos until end) arr[i] = span
            pos = end
            if (pos >= fullBody.length) break
        }
        arr
    }

    // Split into paragraphs. Each paragraph is the chars between consecutive
    // newlines (the newline goes WITH its preceding paragraph).
    val paragraphs = remember(fullBody) {
        val out = mutableListOf<IntRange>()
        var start = 0
        for ((i, c) in fullBody.withIndex()) {
            if (c == '\n') {
                out.add(start..i)
                start = i + 1
            }
        }
        if (start < fullBody.length) out.add(start until fullBody.length)
        else if (start == fullBody.length && fullBody.endsWith('\n')) {
            out.add(start until start) // trailing empty paragraph
        }
        out
    }

    SelectionContainer(modifier = modifier) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        var listCounter = 0
        for (range in paragraphs) {
            // Detect an "attachment paragraph" — one whose only visible char is
            // U+FFFC and whose span carries f12 (attachment_info). Render those
            // as a labeled card (Table / Image / etc.) instead of inline text.
            // Mac/iCloud also lay these out as block elements.
            val attachmentSpan = run {
                val firstNonNl = (range.first..range.last).firstOrNull {
                    it < fullBody.length && fullBody[it] != '\n'
                } ?: return@run null
                val s = if (firstNonNl < styleAt.size) styleAt[firstNonNl] else null
                if (s?.isAttachment == true && fullBody[firstNonNl] == '￼') s else null
            }
            if (attachmentSpan != null) {
                val content = attachmentSpan.attachmentRecordName
                    ?.let { attachmentContent[it] }
                AttachmentBlock(attachmentSpan, content)
                continue
            }

            // The dominant style of a paragraph = the style of its first char.
            val firstSpan = if (range.first < styleAt.size) styleAt[range.first] else null
            val effectiveStyle = when {
                range.first < titlePrefixLength -> NoteBodyEditor.ParagraphStyle.TITLE
                firstSpan != null -> firstSpan.style
                else -> NoteBodyEditor.ParagraphStyle.BODY
            }
            // Reset numbered-list counter when the previous para wasn't part of
            // the same list.
            if (effectiveStyle != NoteBodyEditor.ParagraphStyle.NUMBERED_LIST) {
                listCounter = 0
            }

            // Build an AnnotatedString for the paragraph's text, applying
            // per-character bold/italic/underline/strike/link/monospace styling
            // from each AttrSpan that intersects the paragraph. The newline at
            // the end of the range is dropped — paragraph spacing is the
            // wrapper's job.
            val annotated = buildParagraphAnnotated(fullBody, range, styleAt)

            when (effectiveStyle) {
                NoteBodyEditor.ParagraphStyle.TITLE -> {
                    if (annotated.isNotEmpty()) {
                        Text(
                            annotated,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                NoteBodyEditor.ParagraphStyle.HEADING -> {
                    Text(
                        annotated,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                NoteBodyEditor.ParagraphStyle.SUBHEADING -> {
                    Text(
                        annotated,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                }
                NoteBodyEditor.ParagraphStyle.MONOSPACED -> {
                    Text(
                        annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                NoteBodyEditor.ParagraphStyle.BULLETED_LIST -> {
                    BulletRow("•", annotated)
                }
                NoteBodyEditor.ParagraphStyle.DASHED_LIST -> {
                    BulletRow("–", annotated)
                }
                NoteBodyEditor.ParagraphStyle.CHECKBOX_LIST -> {
                    val checked = firstSpan?.checked ?: false
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (checked) "Checked" else "Unchecked",
                            tint = if (checked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 8.dp, top = 2.dp)
                                .size(28.dp),
                        )
                        Text(
                            annotated,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
                NoteBodyEditor.ParagraphStyle.NUMBERED_LIST -> {
                    listCounter++
                    BulletRow("$listCounter.", annotated)
                }
                NoteBodyEditor.ParagraphStyle.BODY -> {
                    if (annotated.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Text(
                            annotated,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        if (paragraphs.isEmpty()) {
            Text(
                "Empty note",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    }
}

@Composable
private fun AttachmentBlock(span: NoteBodyEditor.AttrSpan, content: AttachmentContent?) {
    when (content) {
        is AttachmentContent.Table -> TableGridView(content.grid)
        is AttachmentContent.Image -> ImageView(content)
        else -> AttachmentPlaceholder(span)
    }
}

@Composable
private fun TableGridView(grid: com.example.applenotes.proto.MergeableDataDecoder.TableGrid) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            )
            .androidxBorder(
                borderColor = borderColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            for ((rIdx, row) in grid.cells.withIndex()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for ((cIdx, cell) in row.withIndex()) {
                        val cellModifier = Modifier
                            .weight(1f)
                            .background(if (rIdx == 0) headerBg else MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                        Text(
                            text = cell,
                            style = if (rIdx == 0)
                                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            else
                                MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = cellModifier,
                        )
                        if (cIdx < row.size - 1) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(borderColor),
                            )
                        }
                    }
                }
                if (rIdx < grid.cells.size - 1) {
                    HorizontalDivider(color = borderColor)
                }
            }
        }
    }
}

private fun Modifier.androidxBorder(
    borderColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
): Modifier = this.border(
    width = 1.dp,
    color = borderColor,
    shape = shape,
)

@Composable
private fun ImageView(content: AttachmentContent.Image) {
    val bitmap = remember(content.bytes) {
        runCatching {
            android.graphics.BitmapFactory.decodeByteArray(content.bytes, 0, content.bytes.size)
        }.getOrNull()
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image attachment",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                ),
        )
    } else {
        AttachmentPlaceholder(NoteBodyEditor.AttrSpan(
            length = 1,
            style = NoteBodyEditor.ParagraphStyle.BODY,
            isAttachment = true,
            attachmentType = content.mimeHint,
        ))
    }
}

@Composable
private fun AttachmentPlaceholder(span: NoteBodyEditor.AttrSpan) {
    val (icon, label) = labelForAttachmentType(span.attachmentType)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                icon,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = span.attachmentType ?: "attachment"
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun labelForAttachmentType(uti: String?): Pair<String, String> = when {
    uti == null -> "📎" to "Attachment"
    uti.contains("table", ignoreCase = true) -> "📋" to "Table"
    uti.contains("gallery", ignoreCase = true) ||
        uti.contains("image", ignoreCase = true) ||
        uti.startsWith("public.png", ignoreCase = true) ||
        uti.startsWith("public.jpeg", ignoreCase = true) ||
        uti.startsWith("public.heic", ignoreCase = true) -> "🖼" to "Image"
    uti.contains("sketch", ignoreCase = true) ||
        uti.contains("drawing", ignoreCase = true) -> "✏️" to "Drawing"
    uti.contains("map", ignoreCase = true) -> "🗺" to "Map"
    uti.startsWith("public.url", ignoreCase = true) ||
        uti.contains("link", ignoreCase = true) -> "🔗" to "Link preview"
    uti.contains("audio", ignoreCase = true) -> "🎵" to "Audio"
    uti.contains("video", ignoreCase = true) -> "🎞" to "Video"
    uti.contains("pdf", ignoreCase = true) -> "📄" to "PDF"
    else -> "📎" to "Attachment"
}

@Composable
private fun BulletRow(marker: String, content: AnnotatedString) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "$marker  ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Build an AnnotatedString for the chars at [range] in [fullBody], applying
 * inline styles (bold/italic/underline/strike/link/monospace) from the
 * AttrSpan covering each char. Coalesces consecutive chars that share an
 * AttrSpan so we only emit one styled span per run. Drops the trailing
 * newline if present (paragraphs include their newline; we don't want it
 * inside the visible text).
 */
private fun buildParagraphAnnotated(
    fullBody: String,
    range: IntRange,
    styleAt: Array<NoteBodyEditor.AttrSpan?>,
): AnnotatedString = buildAnnotatedString {
    var i = range.first
    val end = (range.last + 1).coerceAtMost(fullBody.length)
    while (i < end) {
        // Skip the trailing newline — only the very last char of the
        // paragraph range can be '\n', and we don't want it in the rendered
        // text.
        if (i == end - 1 && fullBody[i] == '\n') break
        val span = if (i < styleAt.size) styleAt[i] else null
        var j = i + 1
        while (j < end &&
            !(j == end - 1 && fullBody[j] == '\n') &&
            (if (j < styleAt.size) styleAt[j] else null) === span
        ) j++
        val sub = fullBody.substring(i, j)
        // U+FFFC stays inline only when the surrounding paragraph isn't
        // already being rendered as an attachment card (the caller checks
        // that and skips this codepath); show a small marker so the position
        // isn't invisible.
        val display = if (span?.isAttachment == true) sub.replace('￼', '⧉') else sub
        val style = inlineSpanStyle(span)
        if (span?.linkUrl != null) {
            val link = LinkAnnotation.Url(
                url = span.linkUrl,
                styles = TextLinkStyles(style = style ?: SpanStyle()),
            )
            withLink(link) { append(display) }
        } else if (style != null) {
            withStyle(style) { append(display) }
        } else {
            append(display)
        }
        i = j
    }
}

private fun inlineSpanStyle(s: NoteBodyEditor.AttrSpan?): SpanStyle? {
    if (s == null) return null
    val decorations = mutableListOf<TextDecoration>()
    if (s.underline) decorations.add(TextDecoration.Underline)
    if (s.strikethrough) decorations.add(TextDecoration.LineThrough)
    val hasInline = s.bold || s.italic || s.underline || s.strikethrough ||
        s.linkUrl != null || s.style == NoteBodyEditor.ParagraphStyle.MONOSPACED
    if (!hasInline) return null
    return SpanStyle(
        fontWeight = if (s.bold) FontWeight.Bold else null,
        fontStyle = if (s.italic) FontStyle.Italic else null,
        textDecoration = when (decorations.size) {
            0 -> if (s.linkUrl != null) TextDecoration.Underline else null
            1 -> decorations[0]
            else -> TextDecoration.combine(decorations)
        },
        color = if (s.linkUrl != null) Color(0xFF1A73E8) else Color.Unspecified,
        fontFamily = if (s.style == NoteBodyEditor.ParagraphStyle.MONOSPACED) FontFamily.Monospace else null,
    )
}

/**
 * Drop the first [skipChars] characters' worth of attribute spans, splitting
 * the boundary span if the cut falls inside one. Used to align spans parsed
 * from the full visible body (which includes the title line) with the body
 * text we render below the app bar.
 */
private fun attrSpansAfter(
    spans: List<NoteBodyEditor.AttrSpan>,
    skipChars: Int,
): List<NoteBodyEditor.AttrSpan> {
    if (skipChars <= 0) return spans
    val out = mutableListOf<NoteBodyEditor.AttrSpan>()
    var consumed = 0
    for (s in spans) {
        if (out.isNotEmpty()) {
            out.add(s)
            continue
        }
        val end = consumed + s.length
        when {
            end <= skipChars -> consumed = end
            consumed >= skipChars -> out.add(s)
            else -> {
                out.add(s.copy(length = end - skipChars))
                consumed = end
            }
        }
    }
    return out
}
