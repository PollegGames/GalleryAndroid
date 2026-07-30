package com.polleg.gallery.gallery.data.mediastore

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.polleg.gallery.gallery.application.MediaMutationRepository
import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind
import com.polleg.gallery.gallery.domain.MediaMovePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AndroidMediaMutationRepository(
    context: Context,
) : MediaMutationRepository {
    private val contentResolver: ContentResolver = context.contentResolver
    private val packageName = context.packageName

    override suspend fun delete(contentUri: String): Boolean = withContext(Dispatchers.IO) {
        val uri = contentUri.toUri()
        // Android 10 has no createDeleteRequest. Media owned by this app could otherwise
        // be deleted silently, so refuse it; non-owned rows trigger the required
        // RecoverableSecurityException and are retried only after system approval.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && isOwnedByThisApp(uri)) {
            return@withContext false
        }
        contentResolver.delete(uri, null, null) > 0
    }

    override suspend fun move(
        media: MediaItem,
        destination: FolderId,
    ): Boolean = withContext(Dispatchers.IO) {
        require(MediaMovePolicy.canMoveTo(listOf(media), destination)) {
            "Le dossier choisi n’accepte pas ce type de média."
        }

        if (
            media.volumeName == destination.volumeName &&
            media.relativePath == destination.relativePath
        ) {
            return@withContext true
        }

        if (MediaMovePolicy.requiresTransfer(media, destination)) {
            return@withContext transferThenDelete(media, destination)
        }

        try {
            val updated = contentResolver.update(
                media.contentUri.toUri(),
                ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, destination.relativePath)
                },
                null,
                null,
            )
            if (updated > 0) {
                true
            } else {
                Log.w(Tag, "MediaStore returned 0 for a direct move; using safe fallback.")
                transferThenDelete(media, destination)
            }
        } catch (recoverable: RecoverableSecurityException) {
            throw recoverable
        } catch (security: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                throw security
            }
            Log.w(Tag, "Direct MediaStore move was refused; using safe fallback.", security)
            transferThenDelete(media, destination)
        } catch (error: IllegalArgumentException) {
            Log.w(Tag, "Direct MediaStore move was rejected; using safe fallback.", error)
            transferThenDelete(media, destination)
        }
    }

    /**
     * Moving to another volume requires writing a new MediaStore row because VOLUME_NAME
     * is read-only. The same transaction is also used for protected Android/media sources.
     * The destination is fully written and published before the source is deleted; any
     * failure before deletion rolls the destination back, so the operation never becomes
     * a user-visible copy.
     */
    private fun transferThenDelete(
        media: MediaItem,
        destination: FolderId,
    ): Boolean {
        val sourceUri = media.contentUri.toUri()
        var destinationUri: Uri? = null

        try {
            destinationUri = contentResolver.insert(
                collectionUri(media.kind, destination.volumeName),
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, media.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, media.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, destination.relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                },
            ) ?: throw IOException("MediaStore n’a pas créé le média de destination.")

            val input = contentResolver.openInputStream(sourceUri)
                ?: throw IOException("Impossible de lire le média source.")
            val output = contentResolver.openOutputStream(destinationUri, "w")
                ?: throw IOException("Impossible d’écrire le média de destination.")
            input.use { source ->
                output.use { target ->
                    source.copyTo(target)
                }
            }

            val published = contentResolver.update(
                destinationUri,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                },
                null,
                null,
            )
            if (published <= 0) {
                throw IOException("MediaStore n’a pas publié le média déplacé.")
            }

            val deleted = contentResolver.delete(sourceUri, null, null)
            if (deleted <= 0) {
                throw IOException("MediaStore n’a pas supprimé le média source.")
            }
            return true
        } catch (error: Throwable) {
            destinationUri?.let(::rollbackDestination)
            throw error
        }
    }

    private fun collectionUri(kind: MediaKind, volumeName: String): Uri = when (kind) {
        MediaKind.Image -> MediaStore.Images.Media.getContentUri(volumeName)
        MediaKind.Video -> MediaStore.Video.Media.getContentUri(volumeName)
    }

    private fun rollbackDestination(uri: Uri) {
        runCatching {
            contentResolver.delete(uri, null, null)
        }.onFailure { rollbackError ->
            Log.e(Tag, "Unable to roll back a staged media move.", rollbackError)
        }
    }

    private fun isOwnedByThisApp(uri: android.net.Uri): Boolean =
        contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.OWNER_PACKAGE_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            cursor.moveToFirst() &&
                cursor.getString(0) == packageName
        } == true

    private companion object {
        const val Tag = "GalleryMediaMove"
    }
}
