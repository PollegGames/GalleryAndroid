package com.polleg.gallery.gallery.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind

class MediaShareLauncher(
    private val activity: Activity,
) {
    fun share(media: List<MediaItem>): Boolean {
        val uniqueMedia = media.distinctBy(MediaItem::contentUri)
        if (uniqueMedia.isEmpty()) return false

        val uris = ArrayList(uniqueMedia.map { it.contentUri.toUri() })
        val sendIntent = Intent(
            if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE,
        ).apply {
            type = MediaSharePolicy.mimeType(uniqueMedia)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = uris.toClipData()
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.single())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }

        val chooser = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            activity.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun List<Uri>.toClipData(): ClipData =
        ClipData.newUri(activity.contentResolver, "shared_media", first()).apply {
            drop(1).forEach { addItem(ClipData.Item(it)) }
        }
}

internal object MediaSharePolicy {
    fun mimeType(media: List<MediaItem>): String {
        if (media.size == 1) return media.single().mimeType.ifBlank { "*/*" }
        return when {
            media.all { it.kind == MediaKind.Image } -> "image/*"
            media.all { it.kind == MediaKind.Video } -> "video/*"
            else -> "*/*"
        }
    }
}
