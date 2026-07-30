package com.polleg.gallery.gallery.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import com.polleg.gallery.gallery.domain.MediaItem

class NativeMediaOpener(
    private val activity: Activity,
) {
    fun open(item: MediaItem): Boolean {
        val mediaUri = item.contentUri.toUri()
        val exactIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(mediaUri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            activity.startActivity(exactIntent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(mediaUri, "${item.kind.name.lowercase()}/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
}
