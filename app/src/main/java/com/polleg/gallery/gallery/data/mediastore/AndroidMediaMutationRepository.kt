package com.polleg.gallery.gallery.data.mediastore

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.polleg.gallery.gallery.application.MediaMutationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        contentUri: String,
        destinationRelativePath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        contentResolver.update(
            contentUri.toUri(),
            ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, destinationRelativePath)
            },
            null,
            null,
        ) > 0
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
}
