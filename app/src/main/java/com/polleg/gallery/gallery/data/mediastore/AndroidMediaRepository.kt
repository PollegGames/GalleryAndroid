package com.polleg.gallery.gallery.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.polleg.gallery.gallery.application.MediaRepository
import com.polleg.gallery.gallery.domain.FolderPathRecord
import com.polleg.gallery.gallery.domain.GalleryLocation
import com.polleg.gallery.gallery.domain.MediaDate
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind
import com.polleg.gallery.gallery.domain.MediaPage
import com.polleg.gallery.gallery.domain.MediaPageAssembler
import com.polleg.gallery.gallery.domain.StorageAvailability
import com.polleg.gallery.gallery.domain.StorageKind
import com.polleg.gallery.gallery.domain.StorageVolume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AndroidMediaRepository(
    private val context: Context,
    private val contentResolver: ContentResolver = context.contentResolver,
    private val mediaPageAssembler: MediaPageAssembler = MediaPageAssembler(),
) : MediaRepository {
    override suspend fun getMediaPage(
        location: GalleryLocation,
        desiredCount: Int,
    ): MediaPage = withContext(Dispatchers.IO) {
        require(desiredCount > 0) { "The desired media count must be positive." }
        val candidateLimit = desiredCount + 1

        val batches = listOf(
            queryMedia(
                location = location,
                sortColumn = MediaStore.Images.ImageColumns.DATE_TAKEN,
                requiredDateColumn = MediaStore.Images.ImageColumns.DATE_TAKEN,
                fallbackDatesOnly = false,
                limit = candidateLimit,
            ),
            queryMedia(
                location = location,
                sortColumn = MediaStore.MediaColumns.DATE_ADDED,
                requiredDateColumn = MediaStore.MediaColumns.DATE_ADDED,
                fallbackDatesOnly = false,
                limit = candidateLimit,
            ),
            queryMedia(
                location = location,
                sortColumn = MediaStore.MediaColumns.DATE_MODIFIED,
                requiredDateColumn = null,
                fallbackDatesOnly = true,
                limit = candidateLimit,
            ),
        )

        mediaPageAssembler.assemble(
            candidateBatches = batches.map(QueryBatch::items),
            desiredCount = desiredCount,
        )
    }

    override suspend fun getFolderPathRecords(): List<FolderPathRecord> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.MediaColumns.VOLUME_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH,
            )
            val selection = mediaTypeSelection()
            val arguments = mediaTypeArguments()
            val queryArgs = queryArguments(
                selection = selection,
                selectionArgs = arguments,
                sortOrder = null,
                limit = null,
            )

            contentResolver.query(
                allExternalFilesUri(),
                projection,
                queryArgs,
                null,
            )?.use { cursor ->
                val volumeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)
                val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)

                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            FolderPathRecord(
                                volumeName = cursor.getString(volumeIndex),
                                relativePath = cursor.getString(pathIndex).orEmpty(),
                            ),
                        )
                    }
                }
            }.orEmpty()
        }

    override suspend fun getStorageVolumes(
        knownVolumeNames: Set<String>,
    ): List<StorageVolume> = withContext(Dispatchers.IO) {
        val mountedNames = MediaStore.getExternalVolumeNames(context)
        val storageManager = context.getSystemService(StorageManager::class.java)
        val resolved = linkedMapOf<String, StorageVolume>()

        storageManager.storageVolumes.forEach { platformVolume ->
            val mediaStoreName = when {
                platformVolume.isPrimary -> MediaStore.VOLUME_EXTERNAL_PRIMARY
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    platformVolume.mediaStoreVolumeName
                else -> platformVolume.uuid?.lowercase(Locale.ROOT)
            } ?: return@forEach

            val isAvailable =
                mediaStoreName in mountedNames &&
                    platformVolume.state == Environment.MEDIA_MOUNTED

            resolved[mediaStoreName] = StorageVolume(
                mediaStoreName = mediaStoreName,
                displayName = if (platformVolume.isPrimary) {
                    "Téléphone"
                } else {
                    platformVolume.getDescription(context).ifBlank { "Carte SD" }
                },
                kind = if (platformVolume.isPrimary) StorageKind.Phone else StorageKind.SdCard,
                availability = if (isAvailable) {
                    StorageAvailability.Available
                } else {
                    StorageAvailability.Unavailable
                },
            )
        }

        mountedNames.forEach { mountedName ->
            resolved.putIfAbsent(
                mountedName,
                StorageVolume(
                    mediaStoreName = mountedName,
                    displayName = if (mountedName == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                        "Téléphone"
                    } else {
                        "Carte SD"
                    },
                    kind = if (mountedName == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                        StorageKind.Phone
                    } else {
                        StorageKind.SdCard
                    },
                    availability = StorageAvailability.Available,
                ),
            )
        }

        knownVolumeNames.forEach { knownName ->
            resolved.putIfAbsent(
                knownName,
                StorageVolume(
                    mediaStoreName = knownName,
                    displayName = if (knownName == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                        "Téléphone"
                    } else {
                        "Carte SD"
                    },
                    kind = if (knownName == MediaStore.VOLUME_EXTERNAL_PRIMARY) {
                        StorageKind.Phone
                    } else {
                        StorageKind.SdCard
                    },
                    availability = StorageAvailability.Unavailable,
                ),
            )
        }

        resolved.values.sortedWith(
            compareBy<StorageVolume> { it.kind != StorageKind.Phone }
                .thenBy(StorageVolume::displayName),
        )
    }

    private fun queryMedia(
        location: GalleryLocation,
        sortColumn: String,
        requiredDateColumn: String?,
        fallbackDatesOnly: Boolean,
        limit: Int,
    ): QueryBatch {
        val selectionParts = mutableListOf(mediaTypeSelection())
        val arguments = mediaTypeArguments().toMutableList()

        if (requiredDateColumn != null) {
            selectionParts += "$requiredDateColumn > 0"
        }
        if (fallbackDatesOnly) {
            selectionParts +=
                "(${MediaStore.Images.ImageColumns.DATE_TAKEN} IS NULL OR " +
                "${MediaStore.Images.ImageColumns.DATE_TAKEN} <= 0)"
            selectionParts +=
                "(${MediaStore.MediaColumns.DATE_ADDED} IS NULL OR " +
                "${MediaStore.MediaColumns.DATE_ADDED} <= 0)"
        }
        if (location is GalleryLocation.Folder) {
            selectionParts += "${MediaStore.MediaColumns.VOLUME_NAME} = ?"
            selectionParts += "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? ESCAPE '\\'"
            arguments += location.id.volumeName
            arguments += "${location.id.relativePath.escapeSqlLike()}%"
        }

        val sortOrder = buildString {
            append(sortColumn)
            append(" DESC, ")
            append(MediaStore.MediaColumns.VOLUME_NAME)
            append(" ASC, ")
            append(MediaStore.MediaColumns._ID)
            append(" DESC")
        }
        val queryArgs = queryArguments(
            selection = selectionParts.joinToString(" AND "),
            selectionArgs = arguments.toTypedArray(),
            sortOrder = sortOrder,
            limit = limit,
        )

        val items = contentResolver.query(
            allExternalFilesUri(),
            MediaProjection,
            queryArgs,
            null,
        )?.use { cursor ->
            buildList {
                while (size < limit && cursor.moveToNext()) {
                    add(cursor.toMediaItem())
                }
            }
        }.orEmpty()

        return QueryBatch(items)
    }

    private fun Cursor.toMediaItem(): MediaItem {
        val rowId = long(MediaStore.MediaColumns._ID)
        val volumeName = string(MediaStore.MediaColumns.VOLUME_NAME)
        val mediaType = int(MediaStore.Files.FileColumns.MEDIA_TYPE)
        val kind = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            MediaKind.Video
        } else {
            MediaKind.Image
        }
        val itemUri = when (kind) {
            MediaKind.Image -> MediaStore.Images.Media.getContentUri(volumeName)
            MediaKind.Video -> MediaStore.Video.Media.getContentUri(volumeName)
        }.let { collection -> ContentUris.withAppendedId(collection, rowId) }

        return MediaItem(
            id = "$volumeName:$rowId",
            mediaStoreId = rowId,
            contentUri = itemUri.toString(),
            displayName = nullableString(MediaStore.MediaColumns.DISPLAY_NAME)
                ?: "Média $rowId",
            mimeType = nullableString(MediaStore.MediaColumns.MIME_TYPE)
                ?: if (kind == MediaKind.Video) "video/*" else "image/*",
            kind = kind,
            volumeName = volumeName,
            relativePath = nullableString(MediaStore.MediaColumns.RELATIVE_PATH).orEmpty(),
            dates = MediaDate(
                takenAtMillis = nullableLong(MediaStore.Images.ImageColumns.DATE_TAKEN),
                addedAtMillis = nullableLong(MediaStore.MediaColumns.DATE_ADDED).secondsToMillis(),
                modifiedAtMillis = nullableLong(MediaStore.MediaColumns.DATE_MODIFIED).secondsToMillis(),
            ),
            durationMillis = if (kind == MediaKind.Video) {
                nullableLong(MediaStore.Video.VideoColumns.DURATION)
            } else {
                null
            },
            width = nullableInt(MediaStore.MediaColumns.WIDTH),
            height = nullableInt(MediaStore.MediaColumns.HEIGHT),
        )
    }

    private fun mediaTypeSelection(): String =
        "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND " +
            "${MediaStore.MediaColumns.IS_PENDING} = 0"

    private fun mediaTypeArguments(): Array<String> = arrayOf(
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
    )

    private fun queryArguments(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String?,
        limit: Int?,
    ): Bundle = Bundle().apply {
        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
        sortOrder?.let { putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, it) }
        limit?.let { putInt(ContentResolver.QUERY_ARG_LIMIT, it) }
    }

    private fun allExternalFilesUri() =
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private fun Cursor.columnIndex(column: String): Int = getColumnIndexOrThrow(column)
    private fun Cursor.long(column: String): Long = getLong(columnIndex(column))
    private fun Cursor.int(column: String): Int = getInt(columnIndex(column))
    private fun Cursor.string(column: String): String = getString(columnIndex(column))

    private fun Cursor.nullableLong(column: String): Long? {
        val index = columnIndex(column)
        return if (isNull(index)) null else getLong(index)
    }

    private fun Cursor.nullableInt(column: String): Int? {
        val index = columnIndex(column)
        return if (isNull(index)) null else getInt(index)
    }

    private fun Cursor.nullableString(column: String): String? {
        val index = columnIndex(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun Long?.secondsToMillis(): Long? =
        this?.takeIf { it > 0L }?.let { seconds ->
            if (seconds > Long.MAX_VALUE / 1_000L) Long.MAX_VALUE else seconds * 1_000L
        }

    private fun String.escapeSqlLike(): String =
        replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    private data class QueryBatch(
        val items: List<MediaItem>,
    )

    companion object {
        private val MediaProjection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.VOLUME_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
        )
    }
}
