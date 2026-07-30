package com.polleg.gallery.gallery.data.mediastore

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.polleg.gallery.gallery.application.GalleryChangeSource
import com.polleg.gallery.gallery.domain.GalleryEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class MediaStoreChangeSource(
    private val contentResolver: ContentResolver,
) : GalleryChangeSource {
    override val events: Flow<GalleryEvent> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(GalleryEvent.MediaLibraryChanged)
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            true,
            observer,
        )

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.conflate()
}
