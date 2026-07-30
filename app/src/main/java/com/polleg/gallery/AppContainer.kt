package com.polleg.gallery

import android.content.Context
import com.polleg.gallery.gallery.application.GetFolderTreeHandler
import com.polleg.gallery.gallery.application.GetMediaPageHandler
import com.polleg.gallery.gallery.application.GetStorageVolumesHandler
import com.polleg.gallery.gallery.application.DeleteMediaHandler
import com.polleg.gallery.gallery.application.MoveMediaHandler
import com.polleg.gallery.gallery.application.ObserveGalleryPreferencesHandler
import com.polleg.gallery.gallery.application.SetNavigationCollapsedHandler
import com.polleg.gallery.gallery.application.TogglePinnedFolderHandler
import com.polleg.gallery.gallery.data.mediastore.AndroidMediaRepository
import com.polleg.gallery.gallery.data.mediastore.AndroidMediaMutationRepository
import com.polleg.gallery.gallery.data.mediastore.MediaStoreChangeSource
import com.polleg.gallery.gallery.data.preferences.JsonGalleryPreferencesRepository
import com.polleg.gallery.gallery.domain.FolderTreeBuilder
import com.polleg.gallery.gallery.platform.CompositeGalleryChangeSource
import com.polleg.gallery.gallery.platform.StorageChangeSource

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val mediaRepository = AndroidMediaRepository(applicationContext)
    private val mediaMutationRepository =
        AndroidMediaMutationRepository(applicationContext)
    private val preferencesRepository = JsonGalleryPreferencesRepository(applicationContext)

    val getMediaPage = GetMediaPageHandler(mediaRepository)
    val getFolderTree = GetFolderTreeHandler(mediaRepository, FolderTreeBuilder())
    val getStorageVolumes = GetStorageVolumesHandler(mediaRepository)
    val observePreferences = ObserveGalleryPreferencesHandler(preferencesRepository)
    val togglePinnedFolder = TogglePinnedFolderHandler(preferencesRepository)
    val setNavigationCollapsed = SetNavigationCollapsedHandler(preferencesRepository)
    val deleteMedia = DeleteMediaHandler(mediaMutationRepository)
    val moveMedia = MoveMediaHandler(mediaMutationRepository)
    val galleryChangeSource = CompositeGalleryChangeSource(
        MediaStoreChangeSource(applicationContext.contentResolver),
        StorageChangeSource(applicationContext),
    )
}
