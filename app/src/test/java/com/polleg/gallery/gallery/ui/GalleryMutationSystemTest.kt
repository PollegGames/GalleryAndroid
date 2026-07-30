package com.polleg.gallery.gallery.ui

import androidx.lifecycle.SavedStateHandle
import com.polleg.gallery.gallery.application.GalleryChangeSource
import com.polleg.gallery.gallery.application.GalleryPreferencesRepository
import com.polleg.gallery.gallery.application.GetFolderTreeHandler
import com.polleg.gallery.gallery.application.GetMediaPageHandler
import com.polleg.gallery.gallery.application.GetStorageVolumesHandler
import com.polleg.gallery.gallery.application.MediaRepository
import com.polleg.gallery.gallery.application.ObserveGalleryPreferencesHandler
import com.polleg.gallery.gallery.application.SetNavigationCollapsedHandler
import com.polleg.gallery.gallery.application.TogglePinnedFolderHandler
import com.polleg.gallery.gallery.domain.FolderPathRecord
import com.polleg.gallery.gallery.domain.FolderTreeBuilder
import com.polleg.gallery.gallery.domain.GalleryEvent
import com.polleg.gallery.gallery.domain.GalleryLocation
import com.polleg.gallery.gallery.domain.GalleryPreferences
import com.polleg.gallery.gallery.domain.MediaDate
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind
import com.polleg.gallery.gallery.domain.MediaPage
import com.polleg.gallery.gallery.domain.StorageAvailability
import com.polleg.gallery.gallery.domain.StorageKind
import com.polleg.gallery.gallery.domain.StorageVolume
import com.polleg.gallery.gallery.platform.MediaPermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryMutationSystemTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `long press delete approval clears selection and refreshes media and folders`() = runTest {
        val mediaRepository = CountingMediaRepository()
        val preferences = FakePreferencesRepository()
        val viewModel = GalleryViewModel(
            savedStateHandle = SavedStateHandle(),
            getMediaPage = GetMediaPageHandler(mediaRepository),
            getFolderTree = GetFolderTreeHandler(mediaRepository, FolderTreeBuilder()),
            getStorageVolumes = GetStorageVolumesHandler(mediaRepository),
            observePreferences = ObserveGalleryPreferencesHandler(preferences),
            togglePinnedFolder = TogglePinnedFolderHandler(preferences),
            setNavigationCollapsed = SetNavigationCollapsedHandler(preferences),
            galleryChangeSource = object : GalleryChangeSource {
                override val events: Flow<GalleryEvent> = emptyFlow()
            },
        )

        viewModel.onAction(
            GalleryAction.PermissionStatusChanged(
                MediaPermissionStatus(hasAccess = true, isLimited = false),
            ),
        )
        advanceUntilIdle()
        val item = (viewModel.uiState.value as GalleryUiState.Ready).media.single()

        viewModel.onAction(GalleryAction.MediaLongPressed(item))
        viewModel.onAction(GalleryAction.DeleteSelected)

        val effect = viewModel.effects.first()
        assertTrue(effect is GalleryEffect.RequestDelete)
        assertEquals(setOf(item.contentUri), ready(viewModel).selectedMediaUris)

        viewModel.onAction(GalleryAction.MutationFinished(succeeded = true))
        advanceUntilIdle()

        assertTrue(ready(viewModel).selectedMediaUris.isEmpty())
        assertTrue(mediaRepository.mediaLoads >= 2)
        assertTrue(mediaRepository.folderLoads >= 2)
    }

    private fun ready(viewModel: GalleryViewModel) =
        viewModel.uiState.value as GalleryUiState.Ready

    private class CountingMediaRepository : MediaRepository {
        var mediaLoads = 0
        var folderLoads = 0
        private val item = MediaItem(
            id = "external_primary:1",
            mediaStoreId = 1,
            contentUri = "content://media/external_primary/1",
            displayName = "photo.jpg",
            mimeType = "image/jpeg",
            kind = MediaKind.Image,
            volumeName = "external_primary",
            relativePath = "DCIM/",
            dates = MediaDate(null, 1, null),
            durationMillis = null,
            width = 100,
            height = 100,
        )

        override suspend fun getMediaPage(
            location: GalleryLocation,
            desiredCount: Int,
        ): MediaPage {
            mediaLoads += 1
            return MediaPage(listOf(item), hasMore = false)
        }

        override suspend fun getFolderPathRecords(): List<FolderPathRecord> {
            folderLoads += 1
            return listOf(FolderPathRecord("external_primary", "DCIM/"))
        }

        override suspend fun getStorageVolumes(
            knownVolumeNames: Set<String>,
        ): List<StorageVolume> = listOf(
            StorageVolume(
                mediaStoreName = "external_primary",
                displayName = "Téléphone",
                kind = StorageKind.Phone,
                availability = StorageAvailability.Available,
            ),
        )
    }

    private class FakePreferencesRepository : GalleryPreferencesRepository {
        private val mutablePreferences = MutableStateFlow(GalleryPreferences())
        override val preferences: Flow<GalleryPreferences> = mutablePreferences

        override suspend fun togglePinnedFolder(folderKey: String) = Unit

        override suspend fun setNavigationCollapsed(collapsed: Boolean) {
            mutablePreferences.value = mutablePreferences.value.copy(
                navigationCollapsed = collapsed,
            )
        }
    }
}
