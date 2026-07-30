package com.polleg.gallery.gallery.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.polleg.gallery.AppContainer
import com.polleg.gallery.gallery.application.GetFolderTreeHandler
import com.polleg.gallery.gallery.application.GetFolderTreeQuery
import com.polleg.gallery.gallery.application.GetMediaPageHandler
import com.polleg.gallery.gallery.application.GetMediaPageQuery
import com.polleg.gallery.gallery.application.GetStorageVolumesHandler
import com.polleg.gallery.gallery.application.GetStorageVolumesQuery
import com.polleg.gallery.gallery.application.ObserveGalleryPreferencesHandler
import com.polleg.gallery.gallery.application.ObserveGalleryPreferencesQuery
import com.polleg.gallery.gallery.application.SetNavigationCollapsedCommand
import com.polleg.gallery.gallery.application.SetNavigationCollapsedHandler
import com.polleg.gallery.gallery.application.TogglePinnedFolderCommand
import com.polleg.gallery.gallery.application.TogglePinnedFolderHandler
import com.polleg.gallery.gallery.application.GalleryChangeSource
import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.GalleryEvent
import com.polleg.gallery.gallery.domain.GalleryLocation
import com.polleg.gallery.gallery.domain.GalleryLocationCodec
import com.polleg.gallery.gallery.domain.GalleryPreferences
import com.polleg.gallery.gallery.domain.MediaPage
import com.polleg.gallery.gallery.domain.ScrollPosition
import com.polleg.gallery.gallery.platform.MediaPermissionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getMediaPage: GetMediaPageHandler,
    private val getFolderTree: GetFolderTreeHandler,
    private val getStorageVolumes: GetStorageVolumesHandler,
    private val observePreferences: ObserveGalleryPreferencesHandler,
    private val togglePinnedFolder: TogglePinnedFolderHandler,
    private val setNavigationCollapsed: SetNavigationCollapsedHandler,
    private val galleryChangeSource: GalleryChangeSource,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Starting)
    val uiState: StateFlow<GalleryUiState> = mutableUiState.asStateFlow()

    private val effectChannel = Channel<GalleryEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var preferences = GalleryPreferences()
    private var permissionStatus = MediaPermissionStatus(hasAccess = false, isLimited = false)
    private var mediaLoadJob: Job? = null
    private var folderLoadJob: Job? = null
    private var changeObservationJob: Job? = null
    private var wasBackgrounded = false
    private var currentScrollPosition = restoredScrollPosition()

    init {
        viewModelScope.launch {
            observePreferences.handle(ObserveGalleryPreferencesQuery).collect { updated ->
                preferences = updated
                val ready = mutableUiState.value as? GalleryUiState.Ready
                if (ready != null) {
                    mutableUiState.value = ready.copy(
                        pinnedFolderKeys = updated.pinnedFolderKeys,
                        navigationCollapsed = updated.navigationCollapsed,
                    )
                }
            }
        }
    }

    fun onAction(action: GalleryAction) {
        when (action) {
            is GalleryAction.PermissionStatusChanged -> onPermissionStatusChanged(action.status)
            GalleryAction.PermissionRequestSelected ->
                effectChannel.trySend(GalleryEffect.RequestMediaPermission)

            is GalleryAction.LocationSelected -> selectLocation(action.location)
            is GalleryAction.FolderExpanded -> toggleFolderExpansion(action.folderId)
            is GalleryAction.PinToggled -> togglePin(action.folderId)
            GalleryAction.LoadMoreRequested -> loadMore()
            GalleryAction.NavigationToggled -> toggleNavigation()
            GalleryAction.BackPressed -> navigateBack()
            is GalleryAction.MediaSelected ->
                effectChannel.trySend(GalleryEffect.OpenMedia(action.mediaItem))

            GalleryAction.RefreshRequested -> loadGallery(
                showStarting = false,
                preserveScroll = true,
                rebuildFolders = true,
            )

            is GalleryAction.ScrollPositionChanged -> saveScrollPosition(action)
            GalleryAction.AppForegrounded -> onForegrounded()
            GalleryAction.AppBackgrounded -> onBackgrounded()
            GalleryAction.MediaOpenFailed ->
                effectChannel.trySend(GalleryEffect.ShowOpenFailed)
        }
    }

    private fun onPermissionStatusChanged(status: MediaPermissionStatus) {
        val previouslyHadAccess = permissionStatus.hasAccess
        permissionStatus = status

        if (!status.hasAccess) {
            mediaLoadJob?.cancel()
            folderLoadJob?.cancel()
            mutableUiState.value = GalleryUiState.PermissionRequired()
            return
        }

        val ready = mutableUiState.value as? GalleryUiState.Ready
        if (ready != null) {
            mutableUiState.value = ready.copy(isLimitedAccess = status.isLimited)
        }

        if (!previouslyHadAccess || mutableUiState.value !is GalleryUiState.Ready) {
            loadGallery(showStarting = true, preserveScroll = true, rebuildFolders = true)
        }
    }

    private fun selectLocation(location: GalleryLocation) {
        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return
        if (location == ready.location) return

        val history = (ready.history + ready.location).takeLast(MaxHistoryEntries)
        currentScrollPosition = ScrollPosition(
            restorationGeneration = ready.scrollPosition.restorationGeneration + 1,
        )
        persistNavigation(location, history, InitialMediaCount)
        persistScrollPosition(currentScrollPosition)

        mutableUiState.value = ready.copy(
            location = location,
            history = history,
            media = emptyList(),
            desiredCount = InitialMediaCount,
            hasMore = false,
            isRefreshing = true,
            scrollPosition = currentScrollPosition,
        )
        loadGallery(showStarting = false, preserveScroll = false, rebuildFolders = false)
    }

    private fun navigateBack() {
        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return
        val previous = ready.history.lastOrNull() ?: return
        val remainingHistory = ready.history.dropLast(1)

        currentScrollPosition = ScrollPosition(
            restorationGeneration = ready.scrollPosition.restorationGeneration + 1,
        )
        persistNavigation(previous, remainingHistory, InitialMediaCount)
        persistScrollPosition(currentScrollPosition)
        mutableUiState.value = ready.copy(
            location = previous,
            history = remainingHistory,
            media = emptyList(),
            desiredCount = InitialMediaCount,
            hasMore = false,
            isRefreshing = true,
            scrollPosition = currentScrollPosition,
        )
        loadGallery(showStarting = false, preserveScroll = false, rebuildFolders = false)
    }

    private fun loadMore() {
        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return
        if (!ready.hasMore || ready.isRefreshing) return

        val desiredCount = (ready.desiredCount + AdditionalMediaCount)
            .coerceAtMost(MaximumRestoredMediaCount)
        savedStateHandle[DesiredCountKey] = desiredCount
        mutableUiState.value = ready.copy(
            desiredCount = desiredCount,
            isRefreshing = true,
        )
        loadGallery(showStarting = false, preserveScroll = true, rebuildFolders = false)
    }

    private fun toggleFolderExpansion(folderId: FolderId) {
        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return
        val key = folderId.stableKey
        val expanded = ready.expandedFolderKeys.toMutableSet().apply {
            if (!add(key)) remove(key)
        }
        mutableUiState.value = ready.copy(expandedFolderKeys = expanded)
    }

    private fun togglePin(folderId: FolderId) {
        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return
        val key = folderId.stableKey
        val optimisticPins = ready.pinnedFolderKeys.toMutableSet().apply {
            if (!add(key)) remove(key)
        }
        preferences = preferences.copy(pinnedFolderKeys = optimisticPins)
        mutableUiState.value = ready.copy(pinnedFolderKeys = optimisticPins)

        viewModelScope.launch {
            runCatching {
                togglePinnedFolder.handle(TogglePinnedFolderCommand(folderId))
            }.onFailure(::reportNonFatalFailure)
        }
    }

    private fun toggleNavigation() {
        val ready = mutableUiState.value as? GalleryUiState.Ready ?: return
        val collapsed = !ready.navigationCollapsed
        preferences = preferences.copy(navigationCollapsed = collapsed)
        mutableUiState.value = ready.copy(navigationCollapsed = collapsed)

        viewModelScope.launch {
            runCatching {
                setNavigationCollapsed.handle(SetNavigationCollapsedCommand(collapsed))
            }.onFailure(::reportNonFatalFailure)
        }
    }

    private fun saveScrollPosition(action: GalleryAction.ScrollPositionChanged) {
        currentScrollPosition = ScrollPosition(
            itemIndex = action.itemIndex.coerceAtLeast(0),
            itemOffset = action.itemOffset.coerceAtLeast(0),
            anchorContentUri = action.anchorContentUri,
            restorationGeneration = currentScrollPosition.restorationGeneration,
        )
        persistScrollPosition(currentScrollPosition)
    }

    private fun loadGallery(
        showStarting: Boolean,
        preserveScroll: Boolean,
        rebuildFolders: Boolean,
    ) {
        if (!permissionStatus.hasAccess) return

        mediaLoadJob?.cancel()
        mediaLoadJob = viewModelScope.launch {
            val previousReady = mutableUiState.value as? GalleryUiState.Ready
            if (showStarting) {
                mutableUiState.value = GalleryUiState.Starting
            } else if (previousReady != null) {
                mutableUiState.value = previousReady.copy(isRefreshing = true)
            }

            try {
                val location = previousReady?.location ?: restoredLocation()
                val history = previousReady?.history ?: restoredHistory()
                val desiredCount = previousReady?.desiredCount ?: restoredDesiredCount()
                val knownVolumes = knownVolumeNames(
                    location = location,
                    history = history,
                    previousVolumes = previousReady
                        ?.storageVolumes
                        .orEmpty()
                        .mapTo(mutableSetOf()) { it.mediaStoreName },
                )

                val result = coroutineScope {
                    val page = async {
                        getMediaPage.handle(GetMediaPageQuery(location, desiredCount))
                    }
                    val volumes = async {
                        getStorageVolumes.handle(GetStorageVolumesQuery(knownVolumes))
                    }
                    page.await() to volumes.await()
                }

                val scrollPosition = resolvedScrollPosition(
                    page = result.first,
                    previous = previousReady,
                    preserveScroll = preserveScroll,
                )
                currentScrollPosition = scrollPosition
                persistNavigation(location, history, desiredCount)
                persistScrollPosition(scrollPosition)

                mutableUiState.value = GalleryUiState.Ready(
                    location = location,
                    history = history,
                    media = result.first.items,
                    desiredCount = desiredCount,
                    folderRoots = previousReady?.folderRoots.orEmpty(),
                    pinnedFolderKeys = preferences.pinnedFolderKeys,
                    navigationCollapsed = preferences.navigationCollapsed,
                    expandedFolderKeys = previousReady?.expandedFolderKeys.orEmpty(),
                    storageVolumes = result.second,
                    hasMore = result.first.hasMore,
                    isRefreshing = false,
                    isFolderTreeLoading = previousReady?.folderRoots.isNullOrEmpty(),
                    isLimitedAccess = permissionStatus.isLimited,
                    scrollPosition = scrollPosition,
                )

                if (rebuildFolders || previousReady?.folderRoots.isNullOrEmpty()) {
                    loadFolderTree(knownVolumes)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: SecurityException) {
                permissionStatus = MediaPermissionStatus(hasAccess = false, isLimited = false)
                mutableUiState.value = GalleryUiState.PermissionRequired()
            } catch (error: Throwable) {
                if (previousReady == null) {
                    mutableUiState.value = GalleryUiState.Failure(error.message)
                } else {
                    mutableUiState.value = previousReady.copy(isRefreshing = false)
                    effectChannel.trySend(GalleryEffect.ShowError(error.message))
                }
            }
        }
    }

    private fun loadFolderTree(knownVolumes: Set<String>) {
        folderLoadJob?.cancel()
        folderLoadJob = viewModelScope.launch {
            val ready = mutableUiState.value as? GalleryUiState.Ready ?: return@launch
            mutableUiState.value = ready.copy(isFolderTreeLoading = true)

            try {
                val result = getFolderTree.handle(GetFolderTreeQuery(knownVolumes))
                val latest = mutableUiState.value as? GalleryUiState.Ready ?: return@launch
                mutableUiState.value = latest.copy(
                    folderRoots = result.roots,
                    storageVolumes = result.volumes,
                    isFolderTreeLoading = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val latest = mutableUiState.value as? GalleryUiState.Ready ?: return@launch
                mutableUiState.value = latest.copy(isFolderTreeLoading = false)
                effectChannel.trySend(GalleryEffect.ShowError(error.message))
            }
        }
    }

    private fun resolvedScrollPosition(
        page: MediaPage,
        previous: GalleryUiState.Ready?,
        preserveScroll: Boolean,
    ): ScrollPosition {
        val generation = (previous?.scrollPosition?.restorationGeneration
            ?: currentScrollPosition.restorationGeneration) + 1
        if (!preserveScroll) return ScrollPosition(restorationGeneration = generation)

        val anchorIndex = currentScrollPosition.anchorContentUri
            ?.let { uri -> page.items.indexOfFirst { it.contentUri == uri } }
            ?.takeIf { it >= 0 }
        val resolvedIndex = (anchorIndex ?: currentScrollPosition.itemIndex)
            .coerceIn(0, page.items.lastIndex.coerceAtLeast(0))

        return currentScrollPosition.copy(
            itemIndex = resolvedIndex,
            restorationGeneration = generation,
        )
    }

    @OptIn(FlowPreview::class)
    private fun onForegrounded() {
        if (changeObservationJob == null) {
            changeObservationJob = viewModelScope.launch {
                galleryChangeSource.events
                    .debounce(ChangeDebounceMillis)
                    .collect { event ->
                        loadGallery(
                            showStarting = false,
                            preserveScroll = true,
                            rebuildFolders = when (event) {
                                GalleryEvent.MediaLibraryChanged,
                                GalleryEvent.StorageAvailabilityChanged,
                                -> true
                            },
                        )
                    }
            }
        }

        if (wasBackgrounded && permissionStatus.hasAccess) {
            loadGallery(showStarting = false, preserveScroll = true, rebuildFolders = true)
        }
        wasBackgrounded = false
    }

    private fun onBackgrounded() {
        wasBackgrounded = true
        changeObservationJob?.cancel()
        changeObservationJob = null
    }

    private fun knownVolumeNames(
        location: GalleryLocation,
        history: List<GalleryLocation>,
        previousVolumes: Set<String> = emptySet(),
    ): Set<String> = buildSet {
        addAll(previousVolumes)

        preferences.pinnedFolderKeys
            .mapNotNull(FolderId::fromStableKey)
            .mapTo(this, FolderId::volumeName)

        (history + location)
            .filterIsInstance<GalleryLocation.Folder>()
            .mapTo(this) { it.id.volumeName }
    }

    private fun restoredLocation(): GalleryLocation =
        GalleryLocationCodec.decode(savedStateHandle[LocationKey])

    private fun restoredHistory(): List<GalleryLocation> =
        savedStateHandle.get<ArrayList<String>>(HistoryKey)
            .orEmpty()
            .map(GalleryLocationCodec::decode)
            .takeLast(MaxHistoryEntries)

    private fun restoredDesiredCount(): Int =
        savedStateHandle.get<Int>(DesiredCountKey)
            ?.coerceIn(InitialMediaCount, MaximumRestoredMediaCount)
            ?: InitialMediaCount

    private fun restoredScrollPosition(): ScrollPosition = ScrollPosition(
        itemIndex = savedStateHandle.get<Int>(ScrollIndexKey)?.coerceAtLeast(0) ?: 0,
        itemOffset = savedStateHandle.get<Int>(ScrollOffsetKey)?.coerceAtLeast(0) ?: 0,
        anchorContentUri = savedStateHandle[ScrollAnchorKey],
    )

    private fun persistNavigation(
        location: GalleryLocation,
        history: List<GalleryLocation>,
        desiredCount: Int,
    ) {
        savedStateHandle[LocationKey] = GalleryLocationCodec.encode(location)
        savedStateHandle[HistoryKey] = ArrayList(history.map(GalleryLocationCodec::encode))
        savedStateHandle[DesiredCountKey] = desiredCount
    }

    private fun persistScrollPosition(position: ScrollPosition) {
        savedStateHandle[ScrollIndexKey] = position.itemIndex
        savedStateHandle[ScrollOffsetKey] = position.itemOffset
        savedStateHandle[ScrollAnchorKey] = position.anchorContentUri
    }

    private fun reportNonFatalFailure(error: Throwable) {
        effectChannel.trySend(GalleryEffect.ShowError(error.message))
    }

    companion object {
        private const val InitialMediaCount = 10
        private const val AdditionalMediaCount = 50
        private const val MaximumRestoredMediaCount = 5_000
        private const val MaxHistoryEntries = 50
        private const val ChangeDebounceMillis = 350L

        private const val LocationKey = "gallery.location"
        private const val HistoryKey = "gallery.history"
        private const val DesiredCountKey = "gallery.desiredCount"
        private const val ScrollIndexKey = "gallery.scroll.index"
        private const val ScrollOffsetKey = "gallery.scroll.offset"
        private const val ScrollAnchorKey = "gallery.scroll.anchor"

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    require(modelClass.isAssignableFrom(GalleryViewModel::class.java))
                    return GalleryViewModel(
                        savedStateHandle = extras.createSavedStateHandle(),
                        getMediaPage = container.getMediaPage,
                        getFolderTree = container.getFolderTree,
                        getStorageVolumes = container.getStorageVolumes,
                        observePreferences = container.observePreferences,
                        togglePinnedFolder = container.togglePinnedFolder,
                        setNavigationCollapsed = container.setNavigationCollapsed,
                        galleryChangeSource = container.galleryChangeSource,
                    ) as T
                }
            }
    }
}
