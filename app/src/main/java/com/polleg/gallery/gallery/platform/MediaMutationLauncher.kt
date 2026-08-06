package com.polleg.gallery.gallery.platform

import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import com.polleg.gallery.gallery.application.DeleteMediaCommand
import com.polleg.gallery.gallery.application.DeleteMediaHandler
import com.polleg.gallery.gallery.application.DeleteMediaResult
import com.polleg.gallery.gallery.application.MoveMediaCommand
import com.polleg.gallery.gallery.application.MoveMediaHandler
import com.polleg.gallery.gallery.application.MoveMediaResult
import com.polleg.gallery.gallery.application.MaxMediaMutationSize
import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaMovePolicy
import kotlinx.coroutines.launch

sealed interface MediaMutationOutcome {
    data class Deleted(val result: DeleteMediaResult) : MediaMutationOutcome
    data class Moved(val result: MoveMediaResult) : MediaMutationOutcome
    data object Cancelled : MediaMutationOutcome
    data class Failed(val detail: String?) : MediaMutationOutcome
}

/**
 * Owns Android's confirmation protocol. Compose only emits an intent and MainActivity
 * delegates it here; actual mutations remain in command handlers/repositories.
 */
class MediaMutationLauncher(
    private val activity: ComponentActivity,
    private val deleteMedia: DeleteMediaHandler,
    private val moveMedia: MoveMediaHandler,
    private val onOutcome: (MediaMutationOutcome) -> Unit,
) {
    private sealed interface Pending {
        data class DeleteRequest(val count: Int) : Pending
        data class MoveRequest(
            val media: List<MediaItem>,
            val destination: FolderId,
        ) : Pending

        data class Android10Delete(
            val media: List<MediaItem>,
            val index: Int,
            val deleted: Int,
            val failed: Int,
        ) : Pending

        data class Android10Move(
            val media: List<MediaItem>,
            val destination: FolderId,
            val index: Int,
            val moved: Int,
            val failed: Int,
            val failureDetails: List<String>,
        ) : Pending
    }

    private var pending: Pending? = null
    private val confirmationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val operation = pending
        if (result.resultCode != Activity.RESULT_OK || operation == null) {
            pending = null
            onOutcome(MediaMutationOutcome.Cancelled)
            return@registerForActivityResult
        }

        when (operation) {
            is Pending.DeleteRequest -> {
                pending = null
                onOutcome(
                    MediaMutationOutcome.Deleted(
                        DeleteMediaResult(operation.count, failedCount = 0),
                    ),
                )
            }

            is Pending.MoveRequest -> executeAuthorizedMove(operation)
            is Pending.Android10Delete -> processAndroid10Delete(operation)
            is Pending.Android10Move -> processAndroid10Move(operation)
        }
    }

    fun delete(media: List<MediaItem>) {
        if (media.isEmpty()) return
        if (media.distinctBy(MediaItem::contentUri).size > MaxMediaMutationSize) {
            onOutcome(MediaMutationOutcome.Failed("La limite est de $MaxMediaMutationSize médias."))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uniqueUris = media.distinctBy(MediaItem::contentUri).map {
                it.contentUri.toUri()
            }
            pending = Pending.DeleteRequest(uniqueUris.size)
            val request = MediaStore.createDeleteRequest(
                activity.contentResolver,
                uniqueUris,
            )
            confirmationLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            processAndroid10Delete(
                Pending.Android10Delete(media.distinctBy(MediaItem::contentUri), 0, 0, 0),
            )
        }
    }

    fun move(media: List<MediaItem>, destination: FolderId) {
        if (media.isEmpty()) return
        val uniqueMedia = media.distinctBy(MediaItem::contentUri)
        if (
            uniqueMedia.size > MaxMediaMutationSize ||
            !MediaMovePolicy.canMoveTo(uniqueMedia, destination)
        ) {
            onOutcome(
                MediaMutationOutcome.Failed(
                    "Ce dossier n’accepte pas tous les types de médias sélectionnés.",
                ),
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pending = Pending.MoveRequest(uniqueMedia, destination)
            val request = MediaStore.createWriteRequest(
                activity.contentResolver,
                uniqueMedia.map { it.contentUri.toUri() },
            )
            confirmationLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            processAndroid10Move(
                Pending.Android10Move(
                    media = uniqueMedia,
                    destination = destination,
                    index = 0,
                    moved = 0,
                    failed = 0,
                    failureDetails = emptyList(),
                ),
            )
        }
    }

    private fun executeAuthorizedMove(operation: Pending.MoveRequest) {
        activity.lifecycleScope.launch {
            runCatching {
                moveMedia.handle(MoveMediaCommand(operation.media, operation.destination))
            }.onSuccess {
                pending = null
                reportMoveResult(it)
            }.onFailure {
                pending = null
                onOutcome(MediaMutationOutcome.Failed(it.message))
            }
        }
    }

    private fun processAndroid10Delete(operation: Pending.Android10Delete) {
        activity.lifecycleScope.launch {
            var index = operation.index
            var deleted = operation.deleted
            var failed = operation.failed
            try {
                while (index < operation.media.size) {
                    val item = operation.media[index]
                    val result = deleteMedia.handle(DeleteMediaCommand(listOf(item.contentUri)))
                    deleted += result.deletedCount
                    failed += result.failedCount
                    index += 1
                }
                pending = null
                onOutcome(MediaMutationOutcome.Deleted(DeleteMediaResult(deleted, failed)))
            } catch (security: RecoverableSecurityException) {
                pending = Pending.Android10Delete(operation.media, index, deleted, failed)
                confirmationLauncher.launch(
                    IntentSenderRequest.Builder(
                        security.userAction.actionIntent.intentSender,
                    ).build(),
                )
            } catch (error: Throwable) {
                pending = null
                onOutcome(MediaMutationOutcome.Failed(error.message))
            }
        }
    }

    private fun processAndroid10Move(operation: Pending.Android10Move) {
        activity.lifecycleScope.launch {
            var index = operation.index
            var moved = operation.moved
            var failed = operation.failed
            val failureDetails = operation.failureDetails.toMutableList()
            try {
                while (index < operation.media.size) {
                    val item = operation.media[index]
                    val result = moveMedia.handle(
                        MoveMediaCommand(listOf(item), operation.destination),
                    )
                    moved += result.movedCount
                    failed += result.failedCount
                    failureDetails += result.failureDetails
                    index += 1
                }
                pending = null
                reportMoveResult(
                    MoveMediaResult(
                        movedCount = moved,
                        failedCount = failed,
                        failureDetails = failureDetails,
                    ),
                )
            } catch (security: RecoverableSecurityException) {
                pending = Pending.Android10Move(
                    operation.media,
                    operation.destination,
                    index,
                    moved,
                    failed,
                    failureDetails,
                )
                confirmationLauncher.launch(
                    IntentSenderRequest.Builder(
                        security.userAction.actionIntent.intentSender,
                    ).build(),
                )
            } catch (error: Throwable) {
                pending = null
                onOutcome(MediaMutationOutcome.Failed(error.message))
            }
        }
    }

    private fun reportMoveResult(result: MoveMediaResult) {
        if (result.movedCount == 0 && result.failedCount > 0) {
            onOutcome(
                MediaMutationOutcome.Failed(
                    result.failureDetails.firstOrNull()
                        ?: "MediaStore n’a déplacé aucun média.",
                ),
            )
        } else {
            onOutcome(MediaMutationOutcome.Moved(result))
        }
    }
}
