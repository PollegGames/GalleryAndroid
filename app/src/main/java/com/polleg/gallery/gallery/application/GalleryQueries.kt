package com.polleg.gallery.gallery.application

import com.polleg.gallery.gallery.domain.FolderTreeBuilder
import com.polleg.gallery.gallery.domain.GalleryLocation
import com.polleg.gallery.gallery.domain.GalleryPreferences
import com.polleg.gallery.gallery.domain.MediaFolder
import com.polleg.gallery.gallery.domain.MediaPage
import com.polleg.gallery.gallery.domain.StorageVolume
import kotlinx.coroutines.flow.Flow

data class GetMediaPageQuery(
    val location: GalleryLocation,
    val desiredCount: Int,
)

data class GetFolderTreeQuery(
    val knownVolumeNames: Set<String> = emptySet(),
)

data class FolderTreeResult(
    val roots: List<MediaFolder>,
    val volumes: List<StorageVolume>,
)

data class GetStorageVolumesQuery(
    val knownVolumeNames: Set<String> = emptySet(),
)

data object ObserveGalleryPreferencesQuery

class GetMediaPageHandler(
    private val mediaRepository: MediaRepository,
) {
    suspend fun handle(query: GetMediaPageQuery): MediaPage =
        mediaRepository.getMediaPage(query.location, query.desiredCount)
}

class GetFolderTreeHandler(
    private val mediaRepository: MediaRepository,
    private val folderTreeBuilder: FolderTreeBuilder,
) {
    suspend fun handle(query: GetFolderTreeQuery): FolderTreeResult {
        val volumes = mediaRepository.getStorageVolumes(query.knownVolumeNames)
        val records = mediaRepository.getFolderPathRecords()
        return FolderTreeResult(
            roots = folderTreeBuilder.build(records, volumes),
            volumes = volumes,
        )
    }
}

class GetStorageVolumesHandler(
    private val mediaRepository: MediaRepository,
) {
    suspend fun handle(query: GetStorageVolumesQuery): List<StorageVolume> =
        mediaRepository.getStorageVolumes(query.knownVolumeNames)
}

class ObserveGalleryPreferencesHandler(
    private val preferencesRepository: GalleryPreferencesRepository,
) {
    fun handle(@Suppress("UNUSED_PARAMETER") query: ObserveGalleryPreferencesQuery): Flow<GalleryPreferences> =
        preferencesRepository.preferences
}
