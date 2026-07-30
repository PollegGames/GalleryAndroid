package com.polleg.gallery.gallery.application

import com.polleg.gallery.gallery.domain.FolderId

data class TogglePinnedFolderCommand(
    val folderId: FolderId,
)

data class SetNavigationCollapsedCommand(
    val collapsed: Boolean,
)

class TogglePinnedFolderHandler(
    private val preferencesRepository: GalleryPreferencesRepository,
) {
    suspend fun handle(command: TogglePinnedFolderCommand) {
        preferencesRepository.togglePinnedFolder(command.folderId.stableKey)
    }
}

class SetNavigationCollapsedHandler(
    private val preferencesRepository: GalleryPreferencesRepository,
) {
    suspend fun handle(command: SetNavigationCollapsedCommand) {
        preferencesRepository.setNavigationCollapsed(command.collapsed)
    }
}
