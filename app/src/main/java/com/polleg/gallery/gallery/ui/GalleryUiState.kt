package com.polleg.gallery.gallery.ui

import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.GalleryLocation
import com.polleg.gallery.gallery.domain.MediaFolder
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.ScrollPosition
import com.polleg.gallery.gallery.domain.StorageVolume
import com.polleg.gallery.gallery.platform.MediaPermissionStatus

sealed interface GalleryUiState {
    data object Starting : GalleryUiState

    data class PermissionRequired(
        val canRequest: Boolean = true,
    ) : GalleryUiState

    data class Ready(
        val location: GalleryLocation,
        val history: List<GalleryLocation>,
        val media: List<MediaItem>,
        val desiredCount: Int,
        val folderRoots: List<MediaFolder>,
        val pinnedFolderKeys: Set<String>,
        val navigationCollapsed: Boolean,
        val expandedFolderKeys: Set<String>,
        val storageVolumes: List<StorageVolume>,
        val hasMore: Boolean,
        val isRefreshing: Boolean,
        val isFolderTreeLoading: Boolean,
        val isLimitedAccess: Boolean,
        val scrollPosition: ScrollPosition,
    ) : GalleryUiState

    data class Failure(
        val detail: String?,
    ) : GalleryUiState
}

sealed interface GalleryAction {
    data class PermissionStatusChanged(val status: MediaPermissionStatus) : GalleryAction
    data object PermissionRequestSelected : GalleryAction
    data class LocationSelected(val location: GalleryLocation) : GalleryAction
    data class FolderExpanded(val folderId: FolderId) : GalleryAction
    data class PinToggled(val folderId: FolderId) : GalleryAction
    data object LoadMoreRequested : GalleryAction
    data object NavigationToggled : GalleryAction
    data object BackPressed : GalleryAction
    data class MediaSelected(val mediaItem: MediaItem) : GalleryAction
    data object RefreshRequested : GalleryAction
    data class ScrollPositionChanged(
        val itemIndex: Int,
        val itemOffset: Int,
        val anchorContentUri: String?,
    ) : GalleryAction

    data object AppForegrounded : GalleryAction
    data object AppBackgrounded : GalleryAction
    data object MediaOpenFailed : GalleryAction
}

sealed interface GalleryEffect {
    data object RequestMediaPermission : GalleryEffect
    data class OpenMedia(val mediaItem: MediaItem) : GalleryEffect
    data object ShowOpenFailed : GalleryEffect
    data class ShowError(val detail: String?) : GalleryEffect
}
