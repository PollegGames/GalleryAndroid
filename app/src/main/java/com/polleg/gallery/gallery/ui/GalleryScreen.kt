package com.polleg.gallery.gallery.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.polleg.gallery.R
import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.FolderMonogram
import com.polleg.gallery.gallery.domain.GalleryLocation
import com.polleg.gallery.gallery.domain.MediaFolder
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind
import com.polleg.gallery.gallery.domain.MediaMovePolicy
import com.polleg.gallery.gallery.domain.StorageAvailability
import com.polleg.gallery.gallery.domain.StorageKind
import com.polleg.gallery.gallery.domain.findFolder
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onAction: (GalleryAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (state) {
            GalleryUiState.Starting -> LoadingScreen()
            is GalleryUiState.PermissionRequired -> PermissionScreen(onAction)
            is GalleryUiState.Ready -> ReadyGallery(state, onAction)
            is GalleryUiState.Failure -> FailureScreen(state, onAction)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PermissionScreen(onAction: (GalleryAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permission_explanation),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onAction(GalleryAction.PermissionRequestSelected) }) {
            Text(stringResource(R.string.grant_access))
        }
    }
}

@Composable
private fun FailureScreen(
    state: GalleryUiState.Failure,
    onAction: (GalleryAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = state.detail ?: stringResource(R.string.generic_error),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onAction(GalleryAction.RefreshRequested) }) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun ReadyGallery(
    state: GalleryUiState.Ready,
    onAction: (GalleryAction) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        val compact = maxWidth < 600.dp
        val availableWidth = maxWidth
        if (!compact) {
            Row(Modifier.fillMaxSize()) {
                GalleryNavigation(
                    state = state,
                    width = 320.dp,
                    collapsed = false,
                    allowCollapse = false,
                    onAction = onAction,
                )
                VerticalDivider(Modifier.fillMaxHeight())
                GalleryContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (state.navigationCollapsed) {
            Row(Modifier.fillMaxSize()) {
                GalleryNavigation(
                    state = state,
                    width = 62.dp,
                    collapsed = true,
                    allowCollapse = true,
                    onAction = onAction,
                )
                VerticalDivider(Modifier.fillMaxHeight())
                GalleryContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                GalleryContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable { onAction(GalleryAction.NavigationToggled) },
                )
                GalleryNavigation(
                    state = state,
                    width = availableWidth * 0.96f,
                    collapsed = false,
                    allowCollapse = true,
                    onAction = onAction,
                )
            }
        }
        if (state.isMovePickerVisible) {
            MoveDestinationDialog(state, onAction)
        }
    }
}

@Composable
private fun GalleryNavigation(
    state: GalleryUiState.Ready,
    width: Dp,
    collapsed: Boolean,
    allowCollapse: Boolean,
    onAction: (GalleryAction) -> Unit,
) {
    val folderRows = remember(state.folderRoots, state.expandedFolderKeys) {
        visibleFolderRows(state.folderRoots, state.expandedFolderKeys)
    }
    val pinnedRows = remember(state.folderRoots, state.pinnedFolderKeys) {
        state.pinnedFolderKeys.mapNotNull { key ->
            val id = FolderId.fromStableKey(key) ?: return@mapNotNull null
            val folder = state.folderRoots.findFolder(id)
            NavigationFolder(
                id = id,
                label = folder?.name ?: id.fallbackLabel(),
                count = folder?.totalMediaCount,
                available = folder != null,
            )
        }.sortedBy { it.label.lowercase() }
    }
    val collapsedRows = remember(pinnedRows, state.folderRoots) {
        (pinnedRows + state.folderRoots.map { root ->
            NavigationFolder(
                id = root.id,
                label = root.name,
                count = root.totalMediaCount,
                available = true,
            )
        }).distinctBy { it.id }
    }
    val monograms = remember(collapsedRows) {
        FolderMonogram.assign(listOf("Récents") + collapsedRows.map(NavigationFolder::label))
    }
    val expandedListState = rememberLazyListState()

    Surface(
        modifier = Modifier
            .width(width)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        if (collapsed) {
            CollapsedNavigation(
                state = state,
                rows = collapsedRows,
                monograms = monograms,
                onAction = onAction,
            )
        } else {
            ExpandedNavigation(
                state = state,
                pinnedRows = pinnedRows,
                folderRows = folderRows,
                allowCollapse = allowCollapse,
                listState = expandedListState,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun CollapsedNavigation(
    state: GalleryUiState.Ready,
    rows: List<NavigationFolder>,
    monograms: List<String>,
    onAction: (GalleryAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "toggle") {
            IconButton(onClick = { onAction(GalleryAction.NavigationToggled) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.expand_navigation),
                )
            }
        }
        item(key = "recent") {
            MonogramButton(
                text = monograms.firstOrNull() ?: "R",
                selected = state.location == GalleryLocation.Recent,
                contentDescription = stringResource(R.string.recent),
                onClick = { onAction(GalleryAction.LocationSelected(GalleryLocation.Recent)) },
            )
        }
        items(
            items = rows,
            key = { it.id.stableKey },
        ) { row ->
            val index = rows.indexOf(row) + 1
            MonogramButton(
                text = monograms.getOrElse(index) { row.label.take(2).uppercase() },
                selected = state.location == GalleryLocation.Folder(row.id),
                enabled = row.available,
                contentDescription = row.label,
                onClick = {
                    onAction(GalleryAction.LocationSelected(GalleryLocation.Folder(row.id)))
                },
            )
        }
    }
}

@Composable
private fun ExpandedNavigation(
    state: GalleryUiState.Ready,
    pinnedRows: List<NavigationFolder>,
    folderRows: List<FolderRow>,
    allowCollapse: Boolean,
    listState: LazyListState,
    onAction: (GalleryAction) -> Unit,
) {
    Box(Modifier.fillMaxHeight()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (allowCollapse) {
                    IconButton(onClick = { onAction(GalleryAction.NavigationToggled) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.collapse_navigation),
                        )
                    }
                }
            }
        }

        item(key = "recent") {
            NavigationRow(
                label = stringResource(R.string.recent),
                count = null,
                depth = 0,
                selected = state.location == GalleryLocation.Recent,
                leadingIcon = {
                    Icon(Icons.Default.Image, contentDescription = null)
                },
                onClick = {
                    onAction(GalleryAction.LocationSelected(GalleryLocation.Recent))
                },
            )
        }

        if (pinnedRows.isNotEmpty()) {
            item(key = "pinned-title") {
                NavigationSectionTitle(stringResource(R.string.pinned))
            }
            items(
                items = pinnedRows,
                key = { "pinned:${it.id.stableKey}" },
            ) { row ->
                NavigationRow(
                    label = row.label,
                    count = row.count,
                    depth = 0,
                    selected = state.location == GalleryLocation.Folder(row.id),
                    enabled = row.available,
                    leadingIcon = {
                        Icon(Icons.Default.PushPin, contentDescription = null)
                    },
                    onClick = {
                        onAction(GalleryAction.LocationSelected(GalleryLocation.Folder(row.id)))
                    },
                )
            }
        }

        item(key = "folders-title") {
            NavigationSectionTitle(stringResource(R.string.folders))
        }

        if (state.isFolderTreeLoading && folderRows.isEmpty()) {
            item(key = "folder-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }

        items(
            items = folderRows,
            key = { it.folder.id.stableKey },
        ) { row ->
            FolderNavigationRow(
                row = row,
                selected = state.location == GalleryLocation.Folder(row.folder.id),
                expanded = row.folder.id.stableKey in state.expandedFolderKeys,
                pinned = row.folder.id.stableKey in state.pinnedFolderKeys,
                onAction = onAction,
            )
        }

        val unavailableSdCards = state.storageVolumes.filter {
            it.kind == StorageKind.SdCard &&
                it.availability == StorageAvailability.Unavailable
        }
        items(
            items = unavailableSdCards,
            key = { "missing:${it.mediaStoreName}" },
        ) {
            NavigationRow(
                label = stringResource(R.string.sd_card_unavailable),
                count = null,
                depth = 0,
                selected = false,
                enabled = false,
                leadingIcon = {
                    Icon(Icons.Default.SdStorage, contentDescription = null)
                },
                onClick = {},
            )
        }
        }
        NavigationScrollIndicator(
            state = listState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(vertical = 8.dp, horizontal = 2.dp),
        )
    }
}

@Composable
private fun NavigationScrollIndicator(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val metrics by remember(state) {
        derivedStateOf {
            val total = state.layoutInfo.totalItemsCount
            val visible = state.layoutInfo.visibleItemsInfo.size
            val fraction = if (total <= visible || total == 0) 0f else {
                visible.toFloat() / total
            }
            val progress = if (total <= visible || total == 0) 0f else {
                state.firstVisibleItemIndex.toFloat() / (total - visible).coerceAtLeast(1)
            }
            fraction to progress
        }
    }
    if (metrics.first > 0f) {
        Canvas(
            modifier = modifier
                .width(3.dp)
                .fillMaxHeight(),
        ) {
            val thumbHeight = size.height * metrics.first.coerceIn(0.08f, 1f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = androidx.compose.ui.geometry.Offset(
                    0f,
                    (size.height - thumbHeight) * metrics.second.coerceIn(0f, 1f),
                ),
                size = androidx.compose.ui.geometry.Size(size.width, thumbHeight),
                cornerRadius = CornerRadius(size.width),
            )
        }
    }
}

@Composable
private fun NavigationSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun FolderNavigationRow(
    row: FolderRow,
    selected: Boolean,
    expanded: Boolean,
    pinned: Boolean,
    onAction: (GalleryAction) -> Unit,
) {
    NavigationRow(
        label = row.folder.name,
        count = row.folder.totalMediaCount,
        depth = row.depth,
        selected = selected,
        leadingIcon = {
            if (row.folder.hasChildren) {
                IconButton(
                    onClick = {
                        onAction(GalleryAction.FolderExpanded(row.folder.id))
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.ExpandMore
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        },
                        contentDescription = stringResource(
                            if (expanded) R.string.collapse_folder else R.string.expand_folder,
                            row.folder.name,
                        ),
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Spacer(Modifier.width(28.dp))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { onAction(GalleryAction.PinToggled(row.folder.id)) },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = stringResource(
                        if (pinned) R.string.unpin_folder else R.string.pin_folder,
                    ),
                    modifier = Modifier.size(17.dp),
                    tint = if (pinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    },
                )
            }
        },
        onClick = {
            onAction(GalleryAction.LocationSelected(GalleryLocation.Folder(row.folder.id)))
        },
    )
}

@Composable
private fun NavigationRow(
    label: String,
    count: Int?,
    depth: Int,
    selected: Boolean,
    enabled: Boolean = true,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(start = (4 + depth.coerceAtMost(4) * 10).dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = { leadingIcon() },
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
            },
        )
        count?.let {
            Text(
                text = compactCount(it),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailingIcon?.invoke()
    }
}

@Composable
private fun MonogramButton(
    text: String,
    selected: Boolean,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .size(44.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GalleryContent(
    state: GalleryUiState.Ready,
    onAction: (GalleryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = state.scrollPosition.itemIndex,
        initialFirstVisibleItemScrollOffset = state.scrollPosition.itemOffset,
    )

    LaunchedEffect(state.scrollPosition.restorationGeneration) {
        if (state.media.isNotEmpty()) {
            gridState.scrollToItem(
                index = state.scrollPosition.itemIndex.coerceIn(0, state.media.lastIndex),
                scrollOffset = state.scrollPosition.itemOffset,
            )
        }
    }

    TrackGridPosition(
        gridState = gridState,
        media = state.media,
        location = state.location,
        onAction = onAction,
    )

    Column(modifier.fillMaxHeight()) {
        GalleryToolbar(state, onAction)

        if (state.isRefreshing) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            Spacer(Modifier.height(4.dp))
        }

        if (state.isLimitedAccess) {
            Text(
                text = stringResource(R.string.permission_limited),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = state.media,
                key = MediaItem::contentUri,
            ) { media ->
                MediaTile(
                    media = media,
                    selected = media.contentUri in state.selectedMediaUris,
                    onClick = { onAction(GalleryAction.MediaSelected(media)) },
                    onLongClick = { onAction(GalleryAction.MediaLongPressed(media)) },
                )
            }

            if (state.media.isEmpty() && !state.isRefreshing) {
                item(
                    key = "empty",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    EmptyGallery()
                }
            }

            if (state.hasMore) {
                item(
                    key = "load-more",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        FilledTonalButton(
                            onClick = { onAction(GalleryAction.LoadMoreRequested) },
                            enabled = !state.isRefreshing,
                        ) {
                            Text(stringResource(R.string.load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryToolbar(
    state: GalleryUiState.Ready,
    onAction: (GalleryAction) -> Unit,
) {
    val title = when (val location = state.location) {
        GalleryLocation.Recent -> stringResource(R.string.recent)
        is GalleryLocation.Folder ->
            state.folderRoots.findFolder(location.id)?.name ?: location.id.fallbackLabel()
    }
    val countText = pluralStringResource(
        R.plurals.media_count,
        state.media.size,
        state.media.size,
    )

    if (state.selectedMediaUris.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onAction(GalleryAction.SelectionClosed) }) {
                Icon(Icons.Default.Close, stringResource(R.string.close_selection))
            }
            Text(
                text = state.selectedMediaUris.size.toString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { onAction(GalleryAction.MoveSelected) }) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.move))
            }
            IconButton(onClick = { onAction(GalleryAction.DeleteSelected) }) {
                Icon(Icons.Default.Delete, stringResource(R.string.delete))
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.history.isNotEmpty()) {
            IconButton(onClick = { onAction(GalleryAction.BackPressed) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (state.history.isEmpty()) 9.dp else 0.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onAction(GalleryAction.RefreshRequested) },
            enabled = !state.isRefreshing,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.refresh),
            )
        }
    }
}

@Composable
private fun MediaTile(
    media: MediaItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val description = stringResource(
        if (media.kind == MediaKind.Video) {
            R.string.video_description
        } else {
            R.string.image_description
        },
        media.displayName,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.contentUri)
                .memoryCacheKey(media.contentUri)
                .build(),
            contentDescription = description,
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
            contentScale = ContentScale.Crop,
        )

        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)),
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.selected_media),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(25.dp),
                tint = MaterialTheme.colorScheme.primaryContainer,
            )
        }

        if (media.kind == MediaKind.Video) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = Color.White,
                )
                media.durationMillis?.let {
                    Text(
                        text = formatDuration(it),
                        color = Color.White,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveDestinationDialog(
    state: GalleryUiState.Ready,
    onAction: (GalleryAction) -> Unit,
) {
    val selectedMedia = remember(state.media, state.selectedMediaUris) {
        state.media.filter { it.contentUri in state.selectedMediaUris }
    }
    val storageRoots = remember(state.folderRoots, state.storageVolumes, selectedMedia) {
        state.storageVolumes.map { volume ->
            state.folderRoots.firstOrNull { it.id.volumeName == volume.mediaStoreName }
                ?: MediaFolder(
                    id = FolderId.of(volume.mediaStoreName, ""),
                    name = volume.displayName,
                    directMediaCount = 0,
                    totalMediaCount = 0,
                    children = emptyList(),
                    storage = volume,
                )
        }
            .distinctBy { it.id }
            .map { root -> root.withDefaultMoveDestinations(selectedMedia) }
    }
    var folderPath by remember(state.isMovePickerVisible) {
        mutableStateOf(emptyList<MediaFolder>())
    }
    val currentFolder = folderPath.lastOrNull()
    val children = remember(currentFolder, storageRoots, selectedMedia) {
        if (currentFolder == null) {
            storageRoots
        } else {
            currentFolder.children.filter { folder ->
                MediaMovePolicy.containsCompatibleDestination(folder, selectedMedia)
            }
        }
    }
    val canChooseCurrent = currentFolder != null &&
        MediaMovePolicy.canMoveTo(selectedMedia, currentFolder.id) &&
        selectedMedia.any { media ->
            media.volumeName != currentFolder.id.volumeName ||
                media.relativePath != currentFolder.id.relativePath
        }

    Dialog(
        onDismissRequest = { onAction(GalleryAction.MovePickerDismissed) },
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
                            if (folderPath.isEmpty()) {
                                onAction(GalleryAction.MovePickerDismissed)
                            } else {
                                folderPath = folderPath.dropLast(1)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (folderPath.isEmpty()) {
                                Icons.Default.Close
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            },
                            contentDescription = stringResource(
                                if (folderPath.isEmpty()) R.string.cancel else R.string.back,
                            ),
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
                            text = if (currentFolder == null) {
                                pluralStringResource(
                                    R.plurals.selected_count,
                                    selectedMedia.size,
                                    selectedMedia.size,
                                )
                            } else {
                                currentFolder.id.relativePath
                                    .trimEnd('/')
                                    .ifBlank { currentFolder.name }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider()

                if (children.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_move_destination),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(children, key = { it.id.stableKey }) { folder ->
                            val isStorageAvailable =
                                folder.storage?.availability != StorageAvailability.Unavailable
                            val hasCompatibleFolder =
                                MediaMovePolicy.containsCompatibleDestination(
                                    folder,
                                    selectedMedia,
                                )
                            val enabled = isStorageAvailable && hasCompatibleFolder
                            MoveDestinationRow(
                                folder = folder,
                                enabled = enabled,
                                unavailableReason = when {
                                    !isStorageAvailable ->
                                        stringResource(R.string.storage_unavailable)
                                    !hasCompatibleFolder ->
                                        stringResource(R.string.no_compatible_folder)
                                    else -> null
                                },
                                onClick = { folderPath = folderPath + folder },
                            )
                        }
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
                    TextButton(
                        onClick = { onAction(GalleryAction.MovePickerDismissed) },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        enabled = canChooseCurrent,
                        onClick = {
                            currentFolder?.let { folder ->
                                onAction(GalleryAction.MoveDestinationSelected(folder.id))
                            }
                        },
                    ) {
                        Text(stringResource(R.string.move_here))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveDestinationRow(
    folder: MediaFolder,
    enabled: Boolean,
    unavailableReason: String?,
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
            imageVector = if (folder.storage?.kind == StorageKind.SdCard) {
                Icons.Default.SdStorage
            } else {
                Icons.Default.Folder
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
                text = unavailableReason
                    ?: pluralStringResource(
                        R.plurals.media_count,
                        folder.totalMediaCount,
                        folder.totalMediaCount,
                    ),
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

@Composable
private fun EmptyGallery() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.empty_gallery),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun TrackGridPosition(
    gridState: LazyGridState,
    media: List<MediaItem>,
    location: GalleryLocation,
    onAction: (GalleryAction) -> Unit,
) {
    LaunchedEffect(gridState, media, location) {
        snapshotFlow {
            val index = gridState.firstVisibleItemIndex
            Triple(
                index,
                gridState.firstVisibleItemScrollOffset,
                media.getOrNull(index)?.contentUri,
            )
        }
            .distinctUntilChanged()
            .debounce(250L)
            .collect { (index, offset, anchor) ->
                onAction(
                    GalleryAction.ScrollPositionChanged(
                        itemIndex = index,
                        itemOffset = offset,
                        anchorContentUri = anchor,
                    ),
                )
            }
    }
}

private data class FolderRow(
    val folder: MediaFolder,
    val depth: Int,
)

private data class NavigationFolder(
    val id: FolderId,
    val label: String,
    val count: Int?,
    val available: Boolean,
)

private fun visibleFolderRows(
    roots: List<MediaFolder>,
    expandedFolderKeys: Set<String>,
): List<FolderRow> = buildList {
    fun append(folder: MediaFolder, depth: Int) {
        add(FolderRow(folder, depth))
        if (folder.id.stableKey in expandedFolderKeys) {
            folder.children.forEach { append(it, depth + 1) }
        }
    }
    roots.forEach { append(it, 0) }
}

private fun FolderId.fallbackLabel(): String =
    relativePath
        .trimEnd('/')
        .substringAfterLast('/')
        .ifBlank {
            if (volumeName == "external_primary") "Téléphone" else "Carte SD"
        }

private fun compactCount(count: Int): String = when {
    count < 1_000 -> count.toString()
    count < 1_000_000 -> "${count / 1_000}k"
    else -> "${count / 1_000_000}M"
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
