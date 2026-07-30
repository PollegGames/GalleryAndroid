package com.polleg.gallery.gallery.application

import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaMovePolicy
import kotlinx.coroutines.CancellationException

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

data class DeleteMediaCommand(
    val contentUris: List<String>,
) {
    init {
        require(contentUris.size <= MaxMediaMutationSize) {
            "Une opération est limitée à $MaxMediaMutationSize médias."
        }
    }
}

data class DeleteMediaResult(
    val deletedCount: Int,
    val failedCount: Int,
)

class DeleteMediaHandler(
    private val repository: MediaMutationRepository,
) {
    suspend fun handle(command: DeleteMediaCommand): DeleteMediaResult {
        var deleted = 0
        command.contentUris.distinct().forEach { uri ->
            val succeeded = try {
                repository.delete(uri)
            } catch (security: SecurityException) {
                throw security
            } catch (_: Throwable) {
                false
            }
            if (succeeded) deleted += 1
        }
        return DeleteMediaResult(
            deletedCount = deleted,
            failedCount = command.contentUris.distinct().size - deleted,
        )
    }
}

data class MoveMediaCommand(
    val media: List<MediaItem>,
    val destination: FolderId,
) {
    init {
        require(media.size <= MaxMediaMutationSize) {
            "Une opération est limitée à $MaxMediaMutationSize médias."
        }
    }
}

data class MoveMediaResult(
    val movedCount: Int,
    val failedCount: Int,
    val failureDetails: List<String> = emptyList(),
)

class InvalidMoveException(message: String) : IllegalArgumentException(message)

class MoveMediaHandler(
    private val repository: MediaMutationRepository,
) {
    suspend fun handle(command: MoveMediaCommand): MoveMediaResult {
        if (command.destination.relativePath.isBlank()) {
            throw InvalidMoveException("La racine du stockage ne peut pas être une destination.")
        }
        if (!MediaMovePolicy.canMoveTo(command.media, command.destination)) {
            throw InvalidMoveException(
                "Ce dossier n’accepte pas tous les types de médias sélectionnés.",
            )
        }

        var moved = 0
        val failures = mutableListOf<String>()
        val uniqueMedia = command.media.distinctBy(MediaItem::contentUri)
        uniqueMedia.forEach { media ->
            val succeeded = try {
                repository.move(media, command.destination)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (security: SecurityException) {
                throw security
            } catch (error: Throwable) {
                failures += buildString {
                    append(media.displayName)
                    append(" : ")
                    append(
                        error.message
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                            ?.take(180)
                            ?: "erreur MediaStore inconnue",
                    )
                }
                false
            }
            if (succeeded) moved += 1
        }
        return MoveMediaResult(
            movedCount = moved,
            failedCount = uniqueMedia.size - moved,
            failureDetails = failures,
        )
    }
}

const val MaxMediaMutationSize = 2_000
