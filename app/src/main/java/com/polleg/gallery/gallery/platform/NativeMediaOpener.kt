package com.polleg.gallery.gallery.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import com.polleg.gallery.ViewerActivity
import com.polleg.gallery.gallery.domain.MediaItem

class NativeMediaOpener(
    private val activity: Activity,
) {
    fun open(items: List<MediaItem>, startIndex: Int): Boolean {
        val uniqueItems = items.distinctBy(MediaItem::contentUri)
        if (uniqueItems.isEmpty()) return false

        val safeStartIndex = startIndex.coerceIn(uniqueItems.indices)
        return try {
            activity.startActivity(
                ViewerActivity.intent(
                    context = activity,
                    media = uniqueItems,
                    startIndex = safeStartIndex,
                ),
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
