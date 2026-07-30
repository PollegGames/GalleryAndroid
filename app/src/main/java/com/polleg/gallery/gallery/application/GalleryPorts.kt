package com.polleg.gallery.gallery.application

import com.polleg.gallery.gallery.domain.FolderPathRecord
import com.polleg.gallery.gallery.domain.GalleryEvent
import com.polleg.gallery.gallery.domain.GalleryLocation
import com.polleg.gallery.gallery.domain.GalleryPreferences
import com.polleg.gallery.gallery.domain.MediaPage
import com.polleg.gallery.gallery.domain.StorageVolume
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun getMediaPage(location: GalleryLocation, desiredCount: Int): MediaPage
    suspend fun getFolderPathRecords(): List<FolderPathRecord>
    suspend fun getStorageVolumes(knownVolumeNames: Set<String> = emptySet()): List<StorageVolume>
}

/**
 * Mutates existing MediaStore rows only. Deliberately exposes no stream or insert API,
 * which keeps move operations as RELATIVE_PATH updates and makes copying impossible.
 */
interface MediaMutationRepository {
    suspend fun delete(contentUri: String): Boolean
    suspend fun move(contentUri: String, destinationRelativePath: String): Boolean
}

interface GalleryPreferencesRepository {
    val preferences: Flow<GalleryPreferences>

    suspend fun togglePinnedFolder(folderKey: String)
    suspend fun setNavigationCollapsed(collapsed: Boolean)
}

interface GalleryChangeSource {
    val events: Flow<GalleryEvent>
}
