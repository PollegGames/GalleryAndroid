package com.polleg.gallery.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.polleg.gallery.R
import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.MediaItem

@Composable
fun GalleryRoute(
    viewModel: GalleryViewModel,
    onRequestPermissions: () -> Unit,
    onOpenMedia: (MediaItem) -> Boolean,
    onDeleteMedia: (List<MediaItem>) -> Unit,
    onMoveMedia: (List<MediaItem>, FolderId) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val openFailedMessage = stringResource(R.string.open_failed)
    val genericErrorMessage = stringResource(R.string.generic_error)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                GalleryEffect.RequestMediaPermission -> onRequestPermissions()
                is GalleryEffect.OpenMedia -> {
                    if (!onOpenMedia(effect.mediaItem)) {
                        viewModel.onAction(GalleryAction.MediaOpenFailed)
                    }
                }
                is GalleryEffect.RequestDelete -> onDeleteMedia(effect.media)
                is GalleryEffect.RequestMove -> onMoveMedia(effect.media, effect.destination)
                is GalleryEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.message)

                GalleryEffect.ShowOpenFailed ->
                    snackbarHostState.showSnackbar(openFailedMessage)

                is GalleryEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.detail ?: genericErrorMessage)
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        var active = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (active) viewModel.onAction(GalleryAction.AppForegrounded)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    active = true
                    viewModel.onAction(GalleryAction.AppForegrounded)
                }

                Lifecycle.Event.ON_STOP -> {
                    active = false
                    viewModel.onAction(GalleryAction.AppBackgrounded)
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (active) viewModel.onAction(GalleryAction.AppBackgrounded)
        }
    }

    val ready = state as? GalleryUiState.Ready
    BackHandler(
        enabled = ready?.selectedMediaUris?.isNotEmpty() == true ||
            ready?.history?.isNotEmpty() == true,
    ) {
        viewModel.onAction(GalleryAction.BackPressed)
    }

    Box(Modifier.fillMaxSize()) {
        GalleryScreen(
            state = state,
            onAction = viewModel::onAction,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
