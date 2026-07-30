package com.polleg.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.polleg.gallery.gallery.platform.MediaPermissionGateway
import com.polleg.gallery.gallery.platform.NativeMediaOpener
import com.polleg.gallery.gallery.platform.MediaMutationLauncher
import com.polleg.gallery.gallery.platform.MediaMutationOutcome
import com.polleg.gallery.gallery.ui.GalleryAction
import com.polleg.gallery.gallery.ui.GalleryRoute
import com.polleg.gallery.gallery.ui.GalleryViewModel
import com.polleg.gallery.gallery.ui.theme.GalleryTheme

class MainActivity : ComponentActivity() {
    private val permissionGateway by lazy { MediaPermissionGateway(this) }
    private val mediaOpener by lazy { NativeMediaOpener(this) }
    private val galleryViewModel: GalleryViewModel by viewModels {
        GalleryViewModel.factory((application as GalleryApplication).container)
    }
    private lateinit var mediaMutationLauncher: MediaMutationLauncher

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        galleryViewModel.onAction(
            GalleryAction.PermissionStatusChanged(permissionGateway.currentStatus()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GalleryApplication).container
        mediaMutationLauncher = MediaMutationLauncher(
            activity = this,
            deleteMedia = container.deleteMedia,
            moveMedia = container.moveMedia,
            onOutcome = ::onMutationOutcome,
        )

        setContent {
            GalleryTheme {
                GalleryRoute(
                    viewModel = galleryViewModel,
                    onRequestPermissions = {
                        permissionLauncher.launch(permissionGateway.permissionsToRequest())
                    },
                    onOpenMedia = mediaOpener::open,
                    onDeleteMedia = mediaMutationLauncher::delete,
                    onMoveMedia = mediaMutationLauncher::move,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        galleryViewModel.onAction(
            GalleryAction.PermissionStatusChanged(permissionGateway.currentStatus()),
        )
    }

    private fun onMutationOutcome(outcome: MediaMutationOutcome) {
        val action = when (outcome) {
            is MediaMutationOutcome.Deleted -> GalleryAction.MutationFinished(
                succeeded = true,
                message = mutationResultMessage(
                    successPlural = R.plurals.delete_success_count,
                    successCount = outcome.result.deletedCount,
                    failureCount = outcome.result.failedCount,
                ),
            )
            is MediaMutationOutcome.Moved -> GalleryAction.MutationFinished(
                succeeded = true,
                message = mutationResultMessage(
                    successPlural = R.plurals.move_success_count,
                    successCount = outcome.result.movedCount,
                    failureCount = outcome.result.failedCount,
                ),
            )
            MediaMutationOutcome.Cancelled -> GalleryAction.MutationFinished(
                succeeded = false,
                message = getString(R.string.operation_cancelled),
            )
            is MediaMutationOutcome.Failed -> GalleryAction.MutationFinished(
                succeeded = false,
                message = outcome.detail ?: getString(R.string.mutation_failed),
            )
        }
        galleryViewModel.onAction(action)
    }

    private fun mutationResultMessage(
        successPlural: Int,
        successCount: Int,
        failureCount: Int,
    ): String = buildString {
        append(resources.getQuantityString(successPlural, successCount, successCount))
        append(" · ")
        append(
            resources.getQuantityString(
                R.plurals.failure_count,
                failureCount,
                failureCount,
            ),
        )
    }
}
