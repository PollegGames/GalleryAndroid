package com.polleg.gallery

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.material.icons.filled.FastForward
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
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
                                knownVolumeNames = media
                                    .mapTo(mutableSetOf()) { it.item.volumeName }
                                    .filterTo(mutableSetOf()) { it.isNotBlank() },
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
            is MediaMutationOutcome.Deleted -> {
                setResult(Activity.RESULT_OK)
                finish()
            }

            is MediaMutationOutcome.Moved -> {
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

        private fun readInternalMedia(intent: Intent): List<ViewerMedia> {
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
                ViewerMedia(
                    item = MediaItem(
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
                    ),
                    canMove = true,
                    canDelete = true,
                )
            }
        }
    }

    private fun readMedia(intent: Intent): List<ViewerMedia> {
        val internalMedia = readInternalMedia(intent)
        if (internalMedia.isNotEmpty()) return internalMedia

        if (intent.action != Intent.ACTION_VIEW) return emptyList()
        val uri = intent.data ?: return emptyList()
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return emptyList()
        val resolvedMimeType = runCatching { contentResolver.getType(uri) }.getOrNull()
            ?: intent.type
        if (!ExternalImageIntentPolicy.accepts(intent.action, resolvedMimeType)) {
            return emptyList()
        }

        val mediaStoreMetadata = readMediaStoreMetadata(uri)
        val displayName = mediaStoreMetadata?.displayName
            ?: readDisplayName(uri)
            ?: uri.lastPathSegment
            ?: getString(R.string.external_image)
        val mimeType = mediaStoreMetadata?.mimeType
            ?: resolvedMimeType
            ?: "image/*"
        val directMediaStoreUri = uri.scheme == ContentResolver.SCHEME_CONTENT &&
            uri.authority == MediaStore.AUTHORITY &&
            mediaStoreMetadata != null

        return listOf(
            ViewerMedia(
                item = MediaItem(
                    id = uri.toString(),
                    mediaStoreId = mediaStoreMetadata?.id ?: 0L,
                    contentUri = uri.toString(),
                    displayName = displayName,
                    mimeType = mimeType,
                    kind = MediaKind.Image,
                    volumeName = mediaStoreMetadata?.volumeName.orEmpty(),
                    relativePath = mediaStoreMetadata?.relativePath.orEmpty(),
                    dates = MediaDate(null, null, null),
                    durationMillis = null,
                    width = mediaStoreMetadata?.width,
                    height = mediaStoreMetadata?.height,
                ),
                canMove = directMediaStoreUri &&
                    !mediaStoreMetadata?.volumeName.isNullOrBlank(),
                canDelete = directMediaStoreUri,
            ),
        )
    }

    private fun readDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                .takeIf { it >= 0 }
                ?.let(cursor::getString)
        }
    }.getOrNull()

    private fun readMediaStoreMetadata(uri: Uri): MediaStoreMetadata? {
        if (
            uri.scheme != ContentResolver.SCHEME_CONTENT ||
            uri.authority != MediaStore.AUTHORITY
        ) {
            return null
        }

        return runCatching {
            val volumeName = MediaStore.getVolumeName(uri)
            contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                MediaStoreMetadata(
                    id = cursor.longOrNull(MediaStore.MediaColumns._ID)
                        ?: runCatching { ContentUris.parseId(uri) }.getOrDefault(0L),
                    displayName = cursor.stringOrNull(MediaStore.MediaColumns.DISPLAY_NAME),
                    mimeType = cursor.stringOrNull(MediaStore.MediaColumns.MIME_TYPE),
                    volumeName = volumeName,
                    relativePath = cursor.stringOrNull(MediaStore.MediaColumns.RELATIVE_PATH)
                        .orEmpty(),
                    width = cursor.intOrNull(MediaStore.MediaColumns.WIDTH),
                    height = cursor.intOrNull(MediaStore.MediaColumns.HEIGHT),
                )
            }
        }.getOrNull()
    }
}

private data class ViewerMedia(
    val item: MediaItem,
    val canMove: Boolean,
    val canDelete: Boolean,
)

private data class MediaStoreMetadata(
    val id: Long,
    val displayName: String?,
    val mimeType: String?,
    val volumeName: String,
    val relativePath: String,
    val width: Int?,
    val height: Int?,
)

internal object ExternalImageIntentPolicy {
    fun accepts(action: String?, mimeType: String?): Boolean =
        action == Intent.ACTION_VIEW && mimeType?.startsWith("image/", ignoreCase = true) == true
}

private fun android.database.Cursor.stringOrNull(columnName: String): String? =
    getColumnIndex(columnName)
        .takeIf { it >= 0 && !isNull(it) }
        ?.let { getString(it) }

private fun android.database.Cursor.longOrNull(columnName: String): Long? =
    getColumnIndex(columnName)
        .takeIf { it >= 0 && !isNull(it) }
        ?.let { getLong(it) }

private fun android.database.Cursor.intOrNull(columnName: String): Int? =
    getColumnIndex(columnName)
        .takeIf { it >= 0 && !isNull(it) }
        ?.let { getInt(it) }

@Composable
private fun ViewerScreen(
    media: List<ViewerMedia>,
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
    var currentImageZoomed by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<MediaItem?>(null) }
    var folderTree by remember { mutableStateOf<FolderTreeResult?>(null) }
    var folderLoadFailed by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    LaunchedEffect(Unit) {
        if (media.none(ViewerMedia::canMove)) return@LaunchedEffect
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
                currentImageZoomed = false
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
            key = { media[it].item.contentUri },
            userScrollEnabled = !currentImageZoomed,
        ) { page ->
            val item = media[page].item
            when {
                item.kind == MediaKind.Video && page == pagerState.currentPage -> {
                    VideoPage(
                        item = item,
                        controlsVisible = controlsVisible,
                        onToggleControls = toggleControls,
                        onInteraction = showControls,
                        onPlayingChanged = { currentVideoPlaying = it },
                    )
                }

                item.kind == MediaKind.Video -> {
                    AsyncImage(
                        model = item.contentUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }

                else -> {
                    ImagePage(
                        item = item,
                        onToggleControls = toggleControls,
                        isCurrent = page == pagerState.currentPage,
                        onZoomChanged = { zoomed ->
                            if (page == pagerState.currentPage) currentImageZoomed = zoomed
                        },
                    )
                }
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
                            text = currentItem.item.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${media.size}",
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
                            onShare(currentItem.item)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ViewerAction(
                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                        label = stringResource(R.string.move),
                        onClick = {
                            showControls()
                            moveTarget = currentItem.item
                        },
                        modifier = Modifier.weight(1f),
                        enabled = currentItem.canMove,
                    )
                    ViewerAction(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.delete),
                        onClick = {
                            showControls()
                            onDelete(currentItem.item)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = currentItem.canDelete,
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

            folderLoadFailed -> Dialog(onDismissRequest = { moveTarget = null }) {
                Surface(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.mutation_failed))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { moveTarget = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
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
    isCurrent: Boolean,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember(item.contentUri) { mutableFloatStateOf(ZoomBounds.MinScale) }
    var offset by remember(item.contentUri) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember(item.contentUri) { mutableStateOf(IntSize.Zero) }
    val currentOnToggleControls by rememberUpdatedState(onToggleControls)
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    LaunchedEffect(item.contentUri, isCurrent) {
        scale = ZoomBounds.MinScale
        offset = Offset.Zero
        if (isCurrent) currentOnZoomChanged(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewportSize = size
                offset = ZoomBounds.clampOffset(offset, scale, size)
            }
            .pointerInput(item.contentUri, isCurrent) {
                awaitEachGesture {
                    var hasPressedPointers: Boolean
                    do {
                        val event = awaitPointerEvent()
                        val panChange = event.calculatePan()
                        val pressedPointers = event.changes.count { it.pressed }
                        val isTransforming = pressedPointers > 1 ||
                            (scale > ZoomBounds.MinScale && panChange != Offset.Zero)

                        if (isTransforming) {
                            val nextScale = ZoomBounds.clampScale(
                                scale * event.calculateZoom(),
                            )
                            val nextOffset = if (nextScale == ZoomBounds.MinScale) {
                                Offset.Zero
                            } else {
                                ZoomBounds.clampOffset(
                                    offset + panChange,
                                    nextScale,
                                    viewportSize,
                                )
                            }
                            scale = nextScale
                            offset = nextOffset
                            if (isCurrent) {
                                currentOnZoomChanged(nextScale > ZoomBounds.MinScale)
                            }
                            event.changes.forEach { it.consume() }
                        }
                        hasPressedPointers = event.changes.any { it.pressed }
                    } while (hasPressedPointers)
                }
            }
            .pointerInput(item.contentUri, isCurrent) {
                detectTapGestures(
                    onTap = { currentOnToggleControls() },
                    onDoubleTap = {
                        if (scale > ZoomBounds.MinScale) {
                            scale = ZoomBounds.MinScale
                            offset = Offset.Zero
                            if (isCurrent) currentOnZoomChanged(false)
                        } else {
                            scale = ZoomBounds.DoubleTapScale
                            offset = Offset.Zero
                            if (isCurrent) currentOnZoomChanged(true)
                        }
                    },
                )
            },
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit,
        )
    }
}

internal object ZoomBounds {
    const val MinScale = 1f
    const val MaxScale = 5f
    const val DoubleTapScale = 2.5f

    fun clampScale(scale: Float): Float = scale.coerceIn(MinScale, MaxScale)

    fun clampOffset(offset: Offset, scale: Float, viewportSize: IntSize): Offset {
        if (scale <= MinScale || viewportSize == IntSize.Zero) return Offset.Zero

        val maximumX = viewportSize.width * (scale - MinScale) / 2f
        val maximumY = viewportSize.height * (scale - MinScale) / 2f
        return Offset(
            x = offset.x.coerceIn(-maximumX, maximumX),
            y = offset.y.coerceIn(-maximumY, maximumY),
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
                    icon = Icons.Default.FastForward,
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
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick)
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
