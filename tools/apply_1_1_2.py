from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    destination = ROOT / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str, marker: str | None = None) -> None:
    content = read(path)
    if marker is not None and marker in content:
        return
    if old not in content:
        raise RuntimeError(f"Expected source block not found in {path}: {old[:120]!r}")
    write(path, content.replace(old, new, 1))


replace_once(
    "app/build.gradle.kts",
    '        versionCode = 3\n        versionName = "1.1.1"',
    '        versionCode = 4\n        versionName = "1.1.2-test"',
    marker='versionName = "1.1.2-test"',
)

replace_once(
    "app/src/main/AndroidManifest.xml",
    '        <activity\n            android:name=".MainActivity"',
    '        <activity\n            android:name=".ViewerActivity"\n            android:exported="false" />\n        <activity\n            android:name=".MainActivity"',
    marker='android:name=".ViewerActivity"',
)

write(
    "app/src/main/java/com/polleg/gallery/gallery/platform/NativeMediaOpener.kt",
    dedent(
        '''\
        package com.polleg.gallery.gallery.platform

        import android.app.Activity
        import android.content.ActivityNotFoundException
        import com.polleg.gallery.ViewerActivity
        import com.polleg.gallery.gallery.domain.MediaItem

        class NativeMediaOpener(
            private val activity: Activity,
        ) {
            fun open(items: List<MediaItem>, startIndex: Int): Boolean {
                val uniqueItems = items.distinctBy(MediaItem::contentUri)
                if (uniqueItems.isEmpty()) return false

                val safeStartIndex = startIndex.coerceIn(uniqueItems.indices)
                return try {
                    activity.startActivity(
                        ViewerActivity.intent(
                            context = activity,
                            media = uniqueItems,
                            startIndex = safeStartIndex,
                        ),
                    )
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                }
            }
        }
        '''
    ),
)

replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryUiState.kt",
    '    data object DeleteSelected : GalleryAction\n    data object MoveSelected : GalleryAction',
    '    data object ShareSelected : GalleryAction\n    data object DeleteSelected : GalleryAction\n    data object MoveSelected : GalleryAction',
    marker='data object ShareSelected',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryUiState.kt",
    '    data class OpenMedia(val mediaItem: MediaItem) : GalleryEffect',
    '    data class OpenMedia(\n        val media: List<MediaItem>,\n        val startIndex: Int,\n    ) : GalleryEffect',
    marker='val startIndex: Int',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryUiState.kt",
    '    data class RequestDelete(val media: List<MediaItem>) : GalleryEffect',
    '    data class RequestShare(val media: List<MediaItem>) : GalleryEffect\n    data class RequestDelete(val media: List<MediaItem>) : GalleryEffect',
    marker='data class RequestShare',
)

replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryViewModel.kt",
    '            GalleryAction.SelectionClosed -> clearSelection()\n            GalleryAction.DeleteSelected -> requestDelete()',
    '            GalleryAction.SelectionClosed -> clearSelection()\n            GalleryAction.ShareSelected -> requestShare()\n            GalleryAction.DeleteSelected -> requestDelete()',
    marker='GalleryAction.ShareSelected -> requestShare()',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryViewModel.kt",
    '        if (result.openMedia) {\n            effectChannel.trySend(GalleryEffect.OpenMedia(mediaItem))\n        } else {',
    '        if (result.openMedia) {\n            val media = ready.media.distinctBy(MediaItem::contentUri)\n            val startIndex = media.indexOfFirst { it.contentUri == mediaItem.contentUri }\n                .coerceAtLeast(0)\n            effectChannel.trySend(\n                GalleryEffect.OpenMedia(\n                    media = media,\n                    startIndex = startIndex,\n                ),\n            )\n        } else {',
    marker='GalleryEffect.OpenMedia(\n                    media = media',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryViewModel.kt",
    '    private fun requestDelete() {\n        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return\n        val selected = ready.media.filter { it.contentUri in ready.selectedMediaUris }\n        if (selected.isNotEmpty()) effectChannel.trySend(GalleryEffect.RequestDelete(selected))\n    }',
    '    private fun requestShare() {\n        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return\n        val selected = ready.media.filter { it.contentUri in ready.selectedMediaUris }\n        if (selected.isNotEmpty()) effectChannel.trySend(GalleryEffect.RequestShare(selected))\n    }\n\n    private fun requestDelete() {\n        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return\n        val selected = ready.media.filter { it.contentUri in ready.selectedMediaUris }\n        if (selected.isNotEmpty()) effectChannel.trySend(GalleryEffect.RequestDelete(selected))\n    }',
    marker='private fun requestShare()',
)

replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryRoute.kt",
    '    onOpenMedia: (MediaItem) -> Boolean,\n    onDeleteMedia: (List<MediaItem>) -> Unit,',
    '    onOpenMedia: (List<MediaItem>, Int) -> Boolean,\n    onShareMedia: (List<MediaItem>) -> Unit,\n    onDeleteMedia: (List<MediaItem>) -> Unit,',
    marker='onShareMedia: (List<MediaItem>) -> Unit',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryRoute.kt",
    '                is GalleryEffect.OpenMedia -> {\n                    if (!onOpenMedia(effect.mediaItem)) {\n                        viewModel.onAction(GalleryAction.MediaOpenFailed)\n                    }\n                }\n                is GalleryEffect.RequestDelete -> onDeleteMedia(effect.media)',
    '                is GalleryEffect.OpenMedia -> {\n                    if (!onOpenMedia(effect.media, effect.startIndex)) {\n                        viewModel.onAction(GalleryAction.MediaOpenFailed)\n                    }\n                }\n                is GalleryEffect.RequestShare -> onShareMedia(effect.media)\n                is GalleryEffect.RequestDelete -> onDeleteMedia(effect.media)',
    marker='is GalleryEffect.RequestShare -> onShareMedia(effect.media)',
)

replace_once(
    "app/src/main/java/com/polleg/gallery/MainActivity.kt",
    'import com.polleg.gallery.gallery.platform.MediaPermissionGateway\nimport com.polleg.gallery.gallery.platform.NativeMediaOpener',
    'import com.polleg.gallery.gallery.platform.MediaPermissionGateway\nimport com.polleg.gallery.gallery.platform.MediaShareLauncher\nimport com.polleg.gallery.gallery.platform.NativeMediaOpener',
    marker='import com.polleg.gallery.gallery.platform.MediaShareLauncher',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/MainActivity.kt",
    '    private val mediaOpener by lazy { NativeMediaOpener(this) }\n    private val galleryViewModel',
    '    private val mediaOpener by lazy { NativeMediaOpener(this) }\n    private val mediaShareLauncher by lazy { MediaShareLauncher(this) }\n    private val galleryViewModel',
    marker='private val mediaShareLauncher',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/MainActivity.kt",
    '                    onOpenMedia = mediaOpener::open,\n                    onDeleteMedia = mediaMutationLauncher::delete,',
    '                    onOpenMedia = mediaOpener::open,\n                    onShareMedia = mediaShareLauncher::share,\n                    onDeleteMedia = mediaMutationLauncher::delete,',
    marker='onShareMedia = mediaShareLauncher::share',
)

replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryScreen.kt",
    'import androidx.compose.material.icons.filled.SdStorage\nimport androidx.compose.material.icons.filled.VideoLibrary',
    'import androidx.compose.material.icons.filled.SdStorage\nimport androidx.compose.material.icons.filled.Share\nimport androidx.compose.material.icons.filled.VideoLibrary',
    marker='import androidx.compose.material.icons.filled.Share',
)
replace_once(
    "app/src/main/java/com/polleg/gallery/gallery/ui/GalleryScreen.kt",
    '            IconButton(onClick = { onAction(GalleryAction.MoveSelected) }) {\n                Icon(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.move))\n            }',
    '            IconButton(onClick = { onAction(GalleryAction.ShareSelected) }) {\n                Icon(Icons.Default.Share, stringResource(R.string.share))\n            }\n            IconButton(onClick = { onAction(GalleryAction.MoveSelected) }) {\n                Icon(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.move))\n            }',
    marker='GalleryAction.ShareSelected',
)

replace_once(
    "app/src/main/res/values/strings.xml",
    '    <string name="selected_media">Média sélectionné</string>\n    <string name="move">Déplacer</string>',
    '    <string name="selected_media">Média sélectionné</string>\n    <string name="share">Partager</string>\n    <string name="move">Déplacer</string>',
    marker='<string name="share">Partager</string>',
)

write(
    "app/src/main/java/com/polleg/gallery/gallery/platform/MediaShareLauncher.kt",
    dedent(
        '''\
        package com.polleg.gallery.gallery.platform

        import android.app.Activity
        import android.content.ActivityNotFoundException
        import android.content.ClipData
        import android.content.Intent
        import android.net.Uri
        import androidx.core.net.toUri
        import com.polleg.gallery.gallery.domain.MediaItem
        import com.polleg.gallery.gallery.domain.MediaKind

        class MediaShareLauncher(
            private val activity: Activity,
        ) {
            fun share(media: List<MediaItem>): Boolean {
                val uniqueMedia = media.distinctBy(MediaItem::contentUri)
                if (uniqueMedia.isEmpty()) return false

                val uris = ArrayList(uniqueMedia.map { it.contentUri.toUri() })
                val sendIntent = Intent(
                    if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE,
                ).apply {
                    type = MediaSharePolicy.mimeType(uniqueMedia)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = uris.toClipData()
                    if (uris.size == 1) {
                        putExtra(Intent.EXTRA_STREAM, uris.single())
                    } else {
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    }
                }

                val chooser = Intent.createChooser(sendIntent, null).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                return try {
                    activity.startActivity(chooser)
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                }
            }

            private fun List<Uri>.toClipData(): ClipData =
                ClipData.newUri(activity.contentResolver, "shared_media", first()).apply {
                    drop(1).forEach { addItem(ClipData.Item(it)) }
                }
        }

        internal object MediaSharePolicy {
            fun mimeType(media: List<MediaItem>): String {
                if (media.size == 1) return media.single().mimeType.ifBlank { "*/*" }
                return when {
                    media.all { it.kind == MediaKind.Image } -> "image/*"
                    media.all { it.kind == MediaKind.Video } -> "video/*"
                    else -> "*/*"
                }
            }
        }
        '''
    ),
)

write(
    "app/src/main/java/com/polleg/gallery/ViewerActivity.kt",
    dedent(
        '''\
        package com.polleg.gallery

        import android.app.Activity
        import android.content.Context
        import android.content.Intent
        import android.media.MediaPlayer
        import android.net.Uri
        import android.os.Bundle
        import android.widget.Toast
        import android.widget.VideoView
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.BackHandler
        import androidx.activity.compose.setContent
        import androidx.activity.enableEdgeToEdge
        import androidx.compose.animation.AnimatedVisibility
        import androidx.compose.animation.fadeIn
        import androidx.compose.animation.fadeOut
        import androidx.compose.foundation.background
        import androidx.compose.foundation.clickable
        import androidx.compose.foundation.gestures.detectTapGestures
        import androidx.compose.foundation.layout.Arrangement
        import androidx.compose.foundation.layout.Box
        import androidx.compose.foundation.layout.Column
        import androidx.compose.foundation.layout.PaddingValues
        import androidx.compose.foundation.layout.Row
        import androidx.compose.foundation.layout.Spacer
        import androidx.compose.foundation.layout.WindowInsets
        import androidx.compose.foundation.layout.fillMaxSize
        import androidx.compose.foundation.layout.fillMaxWidth
        import androidx.compose.foundation.layout.height
        import androidx.compose.foundation.layout.navigationBars
        import androidx.compose.foundation.layout.padding
        import androidx.compose.foundation.layout.size
        import androidx.compose.foundation.layout.statusBars
        import androidx.compose.foundation.layout.width
        import androidx.compose.foundation.layout.windowInsetsPadding
        import androidx.compose.foundation.lazy.LazyColumn
        import androidx.compose.foundation.lazy.items
        import androidx.compose.foundation.pager.HorizontalPager
        import androidx.compose.foundation.pager.rememberPagerState
        import androidx.compose.material.icons.Icons
        import androidx.compose.material.icons.automirrored.filled.ArrowBack
        import androidx.compose.material.icons.automirrored.filled.DriveFileMove
        import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
        import androidx.compose.material.icons.filled.Close
        import androidx.compose.material.icons.filled.Delete
        import androidx.compose.material.icons.filled.Folder
        import androidx.compose.material.icons.filled.Forward15
        import androidx.compose.material.icons.filled.Pause
        import androidx.compose.material.icons.filled.PlayArrow
        import androidx.compose.material.icons.filled.Replay5
        import androidx.compose.material.icons.filled.SdStorage
        import androidx.compose.material.icons.filled.Share
        import androidx.compose.material.icons.filled.Smartphone
        import androidx.compose.material.icons.filled.VolumeOff
        import androidx.compose.material.icons.filled.VolumeUp
        import androidx.compose.material3.Button
        import androidx.compose.material3.CircularProgressIndicator
        import androidx.compose.material3.HorizontalDivider
        import androidx.compose.material3.Icon
        import androidx.compose.material3.IconButton
        import androidx.compose.material3.MaterialTheme
        import androidx.compose.material3.Slider
        import androidx.compose.material3.Surface
        import androidx.compose.material3.Text
        import androidx.compose.material3.TextButton
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.DisposableEffect
        import androidx.compose.runtime.LaunchedEffect
        import androidx.compose.runtime.getValue
        import androidx.compose.runtime.mutableIntStateOf
        import androidx.compose.runtime.mutableLongStateOf
        import androidx.compose.runtime.mutableStateOf
        import androidx.compose.runtime.remember
        import androidx.compose.runtime.rememberUpdatedState
        import androidx.compose.runtime.setValue
        import androidx.compose.runtime.snapshotFlow
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.input.pointer.pointerInput
        import androidx.compose.ui.layout.ContentScale
        import androidx.compose.ui.res.stringResource
        import androidx.compose.ui.text.font.FontWeight
        import androidx.compose.ui.text.style.TextOverflow
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp
        import androidx.compose.ui.viewinterop.AndroidView
        import androidx.compose.ui.window.Dialog
        import androidx.compose.ui.window.DialogProperties
        import coil3.compose.AsyncImage
        import com.polleg.gallery.gallery.application.FolderTreeResult
        import com.polleg.gallery.gallery.application.GetFolderTreeQuery
        import com.polleg.gallery.gallery.domain.FolderId
        import com.polleg.gallery.gallery.domain.MediaDate
        import com.polleg.gallery.gallery.domain.MediaFolder
        import com.polleg.gallery.gallery.domain.MediaItem
        import com.polleg.gallery.gallery.domain.MediaKind
        import com.polleg.gallery.gallery.domain.MediaMovePolicy
        import com.polleg.gallery.gallery.domain.StorageAvailability
        import com.polleg.gallery.gallery.domain.StorageKind
        import com.polleg.gallery.gallery.platform.MediaMutationLauncher
        import com.polleg.gallery.gallery.platform.MediaMutationOutcome
        import com.polleg.gallery.gallery.platform.MediaShareLauncher
        import com.polleg.gallery.gallery.ui.theme.GalleryTheme
        import kotlinx.coroutines.delay
        import kotlinx.coroutines.flow.distinctUntilChanged
        import kotlinx.coroutines.isActive

        class ViewerActivity : ComponentActivity() {
            private val container by lazy { (application as GalleryApplication).container }
            private val shareLauncher by lazy { MediaShareLauncher(this) }
            private lateinit var mutationLauncher: MediaMutationLauncher

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()

                val media = readMedia(intent)
                if (media.isEmpty()) {
                    finish()
                    return
                }
                val startIndex = intent.getIntExtra(ExtraStartIndex, 0).coerceIn(media.indices)

                mutationLauncher = MediaMutationLauncher(
                    activity = this,
                    deleteMedia = container.deleteMedia,
                    moveMedia = container.moveMedia,
                    onOutcome = ::onMutationOutcome,
                )

                setContent {
                    GalleryTheme {
                        ViewerScreen(
                            media = media,
                            startIndex = startIndex,
                            loadFolderTree = {
                                container.getFolderTree.handle(
                                    GetFolderTreeQuery(
                                        knownVolumeNames = media.mapTo(mutableSetOf(), MediaItem::volumeName),
                                    ),
                                )
                            },
                            onClose = ::finish,
                            onShare = { item ->
                                if (!shareLauncher.share(listOf(item))) {
                                    Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onMove = { item, destination ->
                                mutationLauncher.move(listOf(item), destination)
                            },
                            onDelete = { item -> mutationLauncher.delete(listOf(item)) },
                        )
                    }
                }
            }

            private fun onMutationOutcome(outcome: MediaMutationOutcome) {
                when (outcome) {
                    is MediaMutationOutcome.Deleted,
                    is MediaMutationOutcome.Moved,
                    -> {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }

                    MediaMutationOutcome.Cancelled ->
                        Toast.makeText(this, R.string.operation_cancelled, Toast.LENGTH_SHORT).show()

                    is MediaMutationOutcome.Failed ->
                        Toast.makeText(
                            this,
                            outcome.detail ?: getString(R.string.mutation_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            companion object {
                private const val ExtraUris = "viewer.uris"
                private const val ExtraNames = "viewer.names"
                private const val ExtraMimeTypes = "viewer.mime_types"
                private const val ExtraKinds = "viewer.kinds"
                private const val ExtraVolumes = "viewer.volumes"
                private const val ExtraPaths = "viewer.paths"
                private const val ExtraStartIndex = "viewer.start_index"

                fun intent(
                    context: Context,
                    media: List<MediaItem>,
                    startIndex: Int,
                ): Intent = Intent(context, ViewerActivity::class.java).apply {
                    putStringArrayListExtra(ExtraUris, ArrayList(media.map(MediaItem::contentUri)))
                    putStringArrayListExtra(ExtraNames, ArrayList(media.map(MediaItem::displayName)))
                    putStringArrayListExtra(ExtraMimeTypes, ArrayList(media.map(MediaItem::mimeType)))
                    putStringArrayListExtra(ExtraKinds, ArrayList(media.map { it.kind.name }))
                    putStringArrayListExtra(ExtraVolumes, ArrayList(media.map(MediaItem::volumeName)))
                    putStringArrayListExtra(ExtraPaths, ArrayList(media.map(MediaItem::relativePath)))
                    putExtra(ExtraStartIndex, startIndex)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                private fun readMedia(intent: Intent): List<MediaItem> {
                    val uris = intent.getStringArrayListExtra(ExtraUris).orEmpty()
                    val names = intent.getStringArrayListExtra(ExtraNames).orEmpty()
                    val mimeTypes = intent.getStringArrayListExtra(ExtraMimeTypes).orEmpty()
                    val kinds = intent.getStringArrayListExtra(ExtraKinds).orEmpty()
                    val volumes = intent.getStringArrayListExtra(ExtraVolumes).orEmpty()
                    val paths = intent.getStringArrayListExtra(ExtraPaths).orEmpty()
                    val size = listOf(
                        uris.size,
                        names.size,
                        mimeTypes.size,
                        kinds.size,
                        volumes.size,
                        paths.size,
                    ).minOrNull() ?: 0

                    return (0 until size).mapNotNull { index ->
                        val kind = runCatching { MediaKind.valueOf(kinds[index]) }.getOrNull()
                            ?: return@mapNotNull null
                        MediaItem(
                            id = uris[index],
                            mediaStoreId = 0L,
                            contentUri = uris[index],
                            displayName = names[index],
                            mimeType = mimeTypes[index],
                            kind = kind,
                            volumeName = volumes[index],
                            relativePath = paths[index],
                            dates = MediaDate(null, null, null),
                            durationMillis = null,
                            width = null,
                            height = null,
                        )
                    }
                }
            }
        }

        @Composable
        private fun ViewerScreen(
            media: List<MediaItem>,
            startIndex: Int,
            loadFolderTree: suspend () -> FolderTreeResult,
            onClose: () -> Unit,
            onShare: (MediaItem) -> Unit,
            onMove: (MediaItem, FolderId) -> Unit,
            onDelete: (MediaItem) -> Unit,
        ) {
            val pagerState = rememberPagerState(
                initialPage = startIndex.coerceIn(media.indices),
                pageCount = { media.size },
            )
            var controlsVisible by remember { mutableStateOf(true) }
            var controlsEpoch by remember { mutableIntStateOf(0) }
            var currentVideoPlaying by remember { mutableStateOf(false) }
            var moveTarget by remember { mutableStateOf<MediaItem?>(null) }
            var folderTree by remember { mutableStateOf<FolderTreeResult?>(null) }
            var folderLoadFailed by remember { mutableStateOf(false) }

            BackHandler(onBack = onClose)

            LaunchedEffect(Unit) {
                try {
                    folderTree = loadFolderTree()
                } catch (_: Throwable) {
                    folderLoadFailed = true
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect {
                        controlsVisible = true
                        controlsEpoch += 1
                        currentVideoPlaying = false
                    }
            }

            LaunchedEffect(
                controlsVisible,
                currentVideoPlaying,
                controlsEpoch,
                pagerState.currentPage,
            ) {
                if (controlsVisible && currentVideoPlaying) {
                    delay(3_000)
                    controlsVisible = false
                }
            }

            val currentItem = media[pagerState.currentPage.coerceIn(media.indices)]
            val showControls = {
                controlsVisible = true
                controlsEpoch += 1
            }
            val toggleControls = {
                controlsVisible = !controlsVisible
                if (controlsVisible) controlsEpoch += 1
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { media[it].contentUri },
                ) { page ->
                    val item = media[page]
                    if (item.kind == MediaKind.Video && page == pagerState.currentPage) {
                        VideoPage(
                            item = item,
                            controlsVisible = controlsVisible,
                            onToggleControls = toggleControls,
                            onInteraction = showControls,
                            onPlayingChanged = { currentVideoPlaying = it },
                        )
                    } else {
                        ImagePage(
                            item = item,
                            onToggleControls = toggleControls,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        contentColor = Color.White,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .height(72.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = currentItem.displayName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${media.size} · 1.1.2-test",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f),
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.74f),
                        contentColor = Color.White,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .height(88.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ViewerAction(
                                icon = Icons.Default.Share,
                                label = stringResource(R.string.share),
                                onClick = {
                                    showControls()
                                    onShare(currentItem)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ViewerAction(
                                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                                label = stringResource(R.string.move),
                                onClick = {
                                    showControls()
                                    moveTarget = currentItem
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ViewerAction(
                                icon = Icons.Default.Delete,
                                label = stringResource(R.string.delete),
                                onClick = {
                                    showControls()
                                    onDelete(currentItem)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            moveTarget?.let { target ->
                val tree = folderTree
                when {
                    tree != null -> ViewerMovePicker(
                        target = target,
                        roots = tree.roots,
                        onDismiss = { moveTarget = null },
                        onMove = { destination ->
                            moveTarget = null
                            onMove(target, destination)
                        },
                    )

                    folderLoadFailed -> {
                        moveTarget = null
                        LaunchedEffect(Unit) {
                            Toast.makeText(
                                (this@ViewerScreen as? Context),
                                R.string.mutation_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }

                    else -> Dialog(onDismissRequest = { moveTarget = null }) {
                        Surface(shape = MaterialTheme.shapes.large) {
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun ImagePage(
            item: MediaItem,
            onToggleControls: () -> Unit,
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.contentUri,
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(item.contentUri) {
                            detectTapGestures(onTap = { onToggleControls() })
                        },
                )
            }
        }

        @Composable
        private fun VideoPage(
            item: MediaItem,
            controlsVisible: Boolean,
            onToggleControls: () -> Unit,
            onInteraction: () -> Unit,
            onPlayingChanged: (Boolean) -> Unit,
        ) {
            var videoView by remember(item.contentUri) { mutableStateOf<VideoView?>(null) }
            var mediaPlayer by remember(item.contentUri) { mutableStateOf<MediaPlayer?>(null) }
            var prepared by remember(item.contentUri) { mutableStateOf(false) }
            var playing by remember(item.contentUri) { mutableStateOf(false) }
            var muted by remember(item.contentUri) { mutableStateOf(false) }
            var duration by remember(item.contentUri) { mutableLongStateOf(0L) }
            var position by remember(item.contentUri) { mutableLongStateOf(0L) }
            val currentPlayingCallback by rememberUpdatedState(onPlayingChanged)

            DisposableEffect(item.contentUri) {
                onDispose {
                    runCatching { videoView?.stopPlayback() }
                    currentPlayingCallback(false)
                }
            }

            LaunchedEffect(prepared, videoView) {
                while (isActive && prepared) {
                    val view = videoView
                    val isNowPlaying = runCatching { view?.isPlaying == true }.getOrDefault(false)
                    if (playing != isNowPlaying) {
                        playing = isNowPlaying
                        currentPlayingCallback(isNowPlaying)
                    }
                    position = runCatching { view?.currentPosition?.toLong() ?: 0L }
                        .getOrDefault(0L)
                    duration = runCatching { view?.duration?.toLong() ?: duration }
                        .getOrDefault(duration)
                        .coerceAtLeast(0L)
                    delay(250)
                }
            }

            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.contentUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setOnPreparedListener { player ->
                                mediaPlayer = player
                                duration = player.duration.toLong().coerceAtLeast(0L)
                                player.isLooping = false
                                prepared = true
                            }
                            setOnCompletionListener {
                                playing = false
                                position = 0L
                                currentPlayingCallback(false)
                            }
                            setVideoURI(Uri.parse(item.contentUri))
                        }.also { videoView = it }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(item.contentUri) {
                            detectTapGestures(onTap = { onToggleControls() })
                        },
                )

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaybackButton(
                            icon = Icons.Default.Replay5,
                            onClick = {
                                onInteraction()
                                val next = (position - 5_000L).coerceAtLeast(0L)
                                videoView?.seekTo(next.toInt())
                                position = next
                            },
                        )
                        PlaybackButton(
                            icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            size = 74,
                            onClick = {
                                onInteraction()
                                if (playing) {
                                    videoView?.pause()
                                    playing = false
                                    currentPlayingCallback(false)
                                } else if (prepared) {
                                    if (duration > 0L && position >= duration - 250L) {
                                        videoView?.seekTo(0)
                                    }
                                    videoView?.start()
                                    playing = true
                                    currentPlayingCallback(true)
                                }
                            },
                        )
                        PlaybackButton(
                            icon = Icons.Default.Forward15,
                            onClick = {
                                onInteraction()
                                val next = (position + 15_000L).coerceAtMost(duration.coerceAtLeast(0L))
                                videoView?.seekTo(next.toInt())
                                position = next
                            },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 82.dp, end = 14.dp),
                ) {
                    PlaybackButton(
                        icon = if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        onClick = {
                            onInteraction()
                            muted = !muted
                            val volume = if (muted) 0f else 1f
                            mediaPlayer?.setVolume(volume, volume)
                        },
                    )
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 18.dp, end = 18.dp, bottom = 94.dp),
                ) {
                    Column {
                        Slider(
                            value = position.toFloat().coerceAtMost(duration.toFloat().coerceAtLeast(1f)),
                            onValueChange = { value ->
                                onInteraction()
                                position = value.toLong()
                                videoView?.seekTo(position.toInt())
                            },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                text = formatDuration(position),
                                color = Color.White,
                                fontSize = 11.sp,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = formatDuration(duration),
                                color = Color.White,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun PlaybackButton(
            icon: androidx.compose.ui.graphics.vector.ImageVector,
            size: Int = 58,
            onClick: () -> Unit,
        ) {
            Surface(
                modifier = Modifier
                    .size(size.dp)
                    .clickable(onClick = onClick),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color(0xFF20243A).copy(alpha = 0.78f),
                contentColor = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size((size * 0.48f).dp))
                }
            }
        }

        @Composable
        private fun ViewerAction(
            icon: androidx.compose.ui.graphics.vector.ImageVector,
            label: String,
            onClick: () -> Unit,
            modifier: Modifier = Modifier,
        ) {
            Column(
                modifier = modifier
                    .clickable(onClick = onClick)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(5.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }

        @Composable
        private fun ViewerMovePicker(
            target: MediaItem,
            roots: List<MediaFolder>,
            onDismiss: () -> Unit,
            onMove: (FolderId) -> Unit,
        ) {
            val storageRoots = remember(roots, target.contentUri) {
                roots.map { it.withDefaultMoveDestinations(listOf(target)) }
            }
            var folderPath by remember(target.contentUri) { mutableStateOf(emptyList<MediaFolder>()) }
            val currentFolder = folderPath.lastOrNull()
            val children = remember(currentFolder, storageRoots, target.contentUri) {
                if (currentFolder == null) {
                    storageRoots
                } else {
                    currentFolder.children.filter {
                        MediaMovePolicy.containsCompatibleDestination(it, listOf(target))
                    }
                }
            }
            val canChooseCurrent = currentFolder != null &&
                MediaMovePolicy.canMoveTo(listOf(target), currentFolder.id) &&
                (target.volumeName != currentFolder.id.volumeName ||
                    target.relativePath != currentFolder.id.relativePath)

            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    if (folderPath.isEmpty()) onDismiss()
                                    else folderPath = folderPath.dropLast(1)
                                },
                            ) {
                                Icon(
                                    imageVector = if (folderPath.isEmpty()) {
                                        Icons.Default.Close
                                    } else {
                                        Icons.AutoMirrored.Filled.ArrowBack
                                    },
                                    contentDescription = null,
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = currentFolder?.name
                                        ?: stringResource(R.string.storage_locations),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = currentFolder?.id?.relativePath
                                        ?.trimEnd('/')
                                        ?.ifBlank { currentFolder.name }
                                        ?: target.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        HorizontalDivider()

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(children, key = { it.id.stableKey }) { folder ->
                                val storageAvailable =
                                    folder.storage?.availability != StorageAvailability.Unavailable
                                val compatible = MediaMovePolicy.containsCompatibleDestination(
                                    folder,
                                    listOf(target),
                                )
                                ViewerMoveFolderRow(
                                    folder = folder,
                                    enabled = storageAvailable && compatible,
                                    onClick = { folderPath = folderPath + folder },
                                )
                            }
                        }

                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(
                                enabled = canChooseCurrent,
                                onClick = { currentFolder?.let { onMove(it.id) } },
                            ) {
                                Text(stringResource(R.string.move_here))
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun ViewerMoveFolderRow(
            folder: MediaFolder,
            enabled: Boolean,
            onClick: () -> Unit,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = when (folder.storage?.kind) {
                        StorageKind.SdCard -> Icons.Default.SdStorage
                        StorageKind.Phone -> Icons.Default.Smartphone
                        null -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                    )
                    Text(
                        text = "${folder.totalMediaCount} éléments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        private fun MediaFolder.withDefaultMoveDestinations(
            media: List<MediaItem>,
        ): MediaFolder {
            if (!isStorageRoot || storage?.availability == StorageAvailability.Unavailable) return this

            val existingNames = children.map { it.name.lowercase() }.toSet()
            val defaults = MediaMovePolicy.defaultTopLevelDirectories(media)
                .filterNot { it.lowercase() in existingNames }
                .map { name ->
                    MediaFolder(
                        id = id.child(name),
                        name = name,
                        directMediaCount = 0,
                        totalMediaCount = 0,
                        children = emptyList(),
                        storage = null,
                    )
                }
            return copy(children = (children + defaults).sortedBy { it.name.lowercase() })
        }

        private fun formatDuration(milliseconds: Long): String {
            val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
            val hours = totalSeconds / 3_600L
            val minutes = (totalSeconds % 3_600L) / 60L
            val seconds = totalSeconds % 60L
            return if (hours > 0L) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
        '''
    ),
)

write(
    "app/src/test/java/com/polleg/gallery/gallery/platform/MediaSharePolicyTest.kt",
    dedent(
        '''\
        package com.polleg.gallery.gallery.platform

        import com.polleg.gallery.gallery.domain.MediaDate
        import com.polleg.gallery.gallery.domain.MediaItem
        import com.polleg.gallery.gallery.domain.MediaKind
        import org.junit.Assert.assertEquals
        import org.junit.Test

        class MediaSharePolicyTest {
            @Test
            fun `single media keeps its exact mime type`() {
                assertEquals(
                    "video/mp4",
                    MediaSharePolicy.mimeType(listOf(media(MediaKind.Video, "video/mp4"))),
                )
            }

            @Test
            fun `several images use image wildcard`() {
                assertEquals(
                    "image/*",
                    MediaSharePolicy.mimeType(
                        listOf(
                            media(MediaKind.Image, "image/jpeg"),
                            media(MediaKind.Image, "image/png"),
                        ),
                    ),
                )
            }

            @Test
            fun `mixed selection uses generic mime type`() {
                assertEquals(
                    "*/*",
                    MediaSharePolicy.mimeType(
                        listOf(
                            media(MediaKind.Image, "image/jpeg"),
                            media(MediaKind.Video, "video/mp4"),
                        ),
                    ),
                )
            }

            private fun media(kind: MediaKind, mimeType: String) = MediaItem(
                id = "$kind:$mimeType",
                mediaStoreId = 1L,
                contentUri = "content://media/$kind/1",
                displayName = "media",
                mimeType = mimeType,
                kind = kind,
                volumeName = "external_primary",
                relativePath = "DCIM/",
                dates = MediaDate(null, null, null),
                durationMillis = null,
                width = null,
                height = null,
            )
        }
        '''
    ),
)

print("Galerie 1.1.2 source changes applied.")
